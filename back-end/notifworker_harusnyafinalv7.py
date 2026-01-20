import time
import json
import logging
import requests
import mysql.connector
import urllib3
import os
import firebase_admin
from firebase_admin import credentials, messaging
from datetime import datetime, timedelta, timezone

# ==============================================================================
# KONFIGURASI
# ==============================================================================
CHECK_INTERVAL = 3          # Cek setiap 3 detik (Agresif)
LOOKBACK_MINUTES = 5        # Cek mundur 5 menit saat restart (Safety)

# ==============================================================================
# SYSTEM CONFIG
# ==============================================================================
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s', datefmt='%H:%M:%S')
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

DB_CONFIG = {
    'user': 'ekel',
    'password': '2004',
    'host': 'localhost',
    'database': 'wazuh_app_db'
}

FIREBASE_CRED_PATH = "wazuh-mobile-e8e28-firebase-adminsdk-fbsvc-527eafc1d1.json"

if not firebase_admin._apps:
    try:
        cred = credentials.Certificate(FIREBASE_CRED_PATH)
        firebase_admin.initialize_app(cred)
        logging.info("✅ Firebase Initialized.")
    except Exception as e:
        logging.error(f"❌ Failed to initialize Firebase: {e}")
        exit(1)

last_check_memory = {}

def get_db_connection():
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as err:
        logging.error(f"❌ DB Connection Error: {err}")
        return None

def get_users_with_tokens():
    conn = get_db_connection()
    if not conn: return []
    users_data = []
    try:
        with conn.cursor(dictionary=True) as cursor:
            # AMBIL KOLOM min_severity JUGA
            cursor.execute("SELECT id, app_username, fcm_token, min_severity FROM users WHERE fcm_token IS NOT NULL AND fcm_token != ''")
            users = cursor.fetchall()
            for user in users:
                cursor.execute("SELECT * FROM wazuh_credentials WHERE user_id = %s", (user['id'],))
                creds = cursor.fetchall()
                if creds:
                    users_data.append({"user": user, "credentials": creds})
        return users_data
    except Exception as e:
        logging.error(f"❌ Error fetching users: {e}")
        return []
    finally:
        if conn.is_connected(): conn.close()

def check_new_alerts(cred, last_check_time, user_min_severity):
    if not cred.get('wazuh_indexer_username') or not cred.get('wazuh_indexer_password'):
        return []

    host = cred.get('wazuh_host')
    port = cred.get('wazuh_indexer_port', 9200)
    indexer_url = f"https://{host}:{port}/wazuh-alerts-*/_search"
    
    # Query dinamis berdasarkan user_min_severity
    query_payload = {
        "size": 5, 
        "sort": [{"@timestamp": "desc"}],
        "query": {
            "bool": {
                "must": [
                    {"range": {"rule.level": {"gte": user_min_severity}}},
                    {"range": {"@timestamp": {"gt": last_check_time}}}
                ]
            }
        }
    }

    auth = (cred['wazuh_indexer_username'], cred['wazuh_indexer_password'])
    
    try:
        response = requests.post(indexer_url, auth=auth, json=query_payload, verify=False, timeout=5)
        if response.status_code == 200:
            hits = response.json().get('hits', {}).get('hits', [])
            return hits
    except Exception:
        pass
    return []

def send_fcm_notification(token, title, body):
    try:
        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            token=token,
        )
        response = messaging.send(message)
        logging.info(f"   >>> 📲 Notifikasi TERKIRIM ke HP! (Msg ID: {response.split('/')[-1]})")
    except Exception as e:
        logging.error(f"   >>> ❌ Gagal kirim notif: {e}")

def main():
    # =========================================================
    # LOGIKA WAKTU (KUNCI KEBERHASILAN V7)
    # =========================================================
    # Kita mundurkan waktu start 5 menit ke belakang.
    # Jadi alert yang muncul pas worker mati/restart tetap kena ciduk.
    start_time_buffer = datetime.now(timezone.utc) - timedelta(minutes=LOOKBACK_MINUTES)
    initial_time = start_time_buffer.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
    
    logging.info("="*50)
    logging.info(f"🚀 WORKER START | Interval: {CHECK_INTERVAL}s | Min Level: Dynamic")
    logging.info(f"👀 Scanning alerts since: {initial_time} ({LOOKBACK_MINUTES} min lookback)")
    logging.info("="*50)
    
    while True:
        try:
            users_list = get_users_with_tokens()
            
            for item in users_list:
                user = item['user']
                credentials = item['credentials']
                fcm_token = user['fcm_token']
                
                # Ambil settingan user (Default 12 jika belum diset/error)
                user_min_severity = user.get('min_severity')
                if not user_min_severity or user_min_severity < 1:
                    user_min_severity = 12

                for cred in credentials:
                    cred_id = cred['id']
                    
                    # Logic Pintar:
                    # Kalau belum pernah cek (baru nyala), pakai initial_time (5 menit lalu).
                    # Kalau sudah pernah cek, pakai waktu terakhir dari memori.
                    last_check = last_check_memory.get(cred_id, initial_time)
                    
                    # Cek Alert dengan threshold khusus user ini
                    new_alerts = check_new_alerts(cred, last_check, user_min_severity)
                    
                    if new_alerts:
                        # Update memori ke timestamp alert paling baru
                        newest_alert_time = new_alerts[0]['_source']['@timestamp']
                        last_check_memory[cred_id] = newest_alert_time
                        
                        count = len(new_alerts)
                        first_alert = new_alerts[0]['_source']
                        rule = first_alert.get('rule', {})
                        agent = first_alert.get('agent', {})
                        
                        rule_desc = rule.get('description', 'Critical Alert')
                        level = rule.get('level', 'N/A')
                        rule_id = rule.get('id', 'N/A')
                        agent_name = agent.get('name', 'Unknown')
                        
                        # --- LOGGING KEREN ---
                        logging.info("\n" + "!"*50)
                        logging.info(f"🚨  DETEKSI ALERT (Min Lvl: {user_min_severity})")
                        logging.info("!"*50)
                        logging.info(f"   [+] User       : {user['app_username']}")
                        logging.info(f"   [+] Server     : {cred['credential_name']}")
                        logging.info(f"   [+] Rule ID    : {rule_id}")
                        logging.info(f"   [+] Level      : {level}")
                        logging.info(f"   [+] Agent      : {agent_name}")
                        logging.info(f"   [+] Pesan      : {rule_desc}")
                        if count > 1:
                            logging.info(f"   [+] Tambahan   : +{count-1} alert lainnya...")
                        logging.info("-" * 50)
                        
                        # Kirim Notif
                        title = f"🚨 Alert Level {level} Detected"
                        body = f"{rule_desc}"
                        if count > 1: body += f" (+{count-1} more)"
                        
                        send_fcm_notification(fcm_token, title, body)
                        logging.info("="*50 + "\n")
            
            time.sleep(CHECK_INTERVAL)
            
        except KeyboardInterrupt:
            logging.info("Worker stopped by user.")
            break
        except Exception as e:
            logging.error(f"Worker Loop Error: {e}")
            time.sleep(10)

if __name__ == "__main__":
    main()