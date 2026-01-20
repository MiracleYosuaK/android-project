#
# Server Flask untuk Aplikasi Mobile Wazuh (GUNICORN READY VERSION)
# - Cleaned: Removed "Connection: close" headers (Let Gunicorn handle keep-alive)
# - Optimized: Pure JSON responses
#
import hashlib
import json
import os
import uuid
import logging
from base64 import b64encode
from datetime import datetime, timedelta, timezone

import mysql.connector
import requests
import urllib3
from flask import Flask, jsonify, request
from openai import OpenAI
from functools import wraps
from dotenv import load_dotenv
# ==============================================================================
# KONFIGURASI
# ==============================================================================
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
load_dotenv()
app = Flask(__name__)

DB_CONFIG = {
    'user': 'ekel',
    'password': '2004',
    'host': 'localhost',
    'database': 'wazuh_app_db'
}

# API KEY OPENAI
OPENAI_API_KEY = "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"

try:
    openai_client = OpenAI(api_key=OPENAI_API_KEY)
except TypeError:
    logging.warning("OpenAI Client failed to initialize.")
    openai_client = None

# ==============================================================================
# FUNGSI BANTUAN
# ==============================================================================

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        # Cek Header Authorization
        if 'Authorization' in request.headers:
            auth_header = request.headers['Authorization']
            if "Bearer " in auth_header:
                token = auth_header.split(" ")[1] # Ambil tokennya aja
        
        if not token:
            return jsonify({'message': 'Token is missing!'}), 401
        
        return f(*args, **kwargs)
    return decorated

def get_db_connection():
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as err:
        logging.error(f"Database connection failed: {err}")
        return None

def get_user_by_username(app_username):
    conn = get_db_connection()
    if not conn: return None
    try:
        with conn.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM users WHERE app_username = %s", (app_username,))
            return cursor.fetchone()
    except mysql.connector.Error as err:
        logging.error(f"Database error while fetching user data: {err}")
        return None
    finally:
        if conn and conn.is_connected(): conn.close()

def get_all_wazuh_credentials(user_id):
    conn = get_db_connection()
    if not conn: return []
    try:
        with conn.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM wazuh_credentials WHERE user_id = %s", (user_id,))
            return cursor.fetchall()
    except mysql.connector.Error as err:
        logging.error(f"Database error while fetching Wazuh credentials: {err}")
        return []
    finally:
        if conn and conn.is_connected(): conn.close()

def get_new_wazuh_token(creds):
    if not creds: return None
    host = creds.get('wazuh_host')
    port = creds.get('wazuh_port', 55000)
    
    login_url = f"https://{host}:{port}/security/user/authenticate?raw=true"
    basic_auth = f"{creds['wazuh_api_username']}:{creds['wazuh_api_password']}".encode()
    login_headers = {'Authorization': f'Basic {b64encode(basic_auth).decode()}'}
    
    try:
        response = requests.post(login_url, headers=login_headers, verify=False, timeout=10)
        response.raise_for_status()
        return response.text
    except requests.exceptions.RequestException as e:
        logging.error(f"Error getting Wazuh token for '{creds.get('credential_name')}': {e}")
        return None

def get_indexer_data(query_payload, creds):
    if not all([creds.get('wazuh_indexer_username'), creds.get('wazuh_indexer_password')]):
        logging.warning(f"Missing Indexer credentials for '{creds.get('credential_name')}'.")
        return None
        
    host = creds.get('wazuh_host')
    port = creds.get('wazuh_indexer_port', 9200)
    
    indexer_url = f"https://{host}:{port}/wazuh-alerts-*/_search"
    auth = (creds['wazuh_indexer_username'], creds['wazuh_indexer_password'])
    
    try:
        response = requests.post(indexer_url, auth=auth, json=query_payload, verify=False, timeout=15)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        logging.error(f"Indexer API Error for '{creds.get('credential_name')}': {e}")
        return None


# ==============================================================================
# ENDPOINTS API
# ==============================================================================

@app.route('/api/register', methods=['POST'])
def register():
        data = request.json
        
        app_username = data.get('app_username')
        app_password = data.get('app_password')
        if not all([app_username, app_password]):
            return jsonify({"error": "Missing app_username or app_password"}), 400

        w_host = data.get('wazuh_host')
        w_api_user = data.get('wazuh_api_username')
        has_wazuh_data = w_host and w_api_user

        app_password_salt = uuid.uuid4().hex
        app_password_hash = hashlib.sha256((app_password + app_password_salt).encode()).hexdigest()

        conn = get_db_connection()
        if not conn: return jsonify({"error": "Database connection failed"}), 500

        try:
            conn.start_transaction()
            
            with conn.cursor() as cursor:
                cursor.execute("SELECT id FROM users WHERE app_username = %s", (app_username,))
                if cursor.fetchone():
                    conn.rollback()
                    return jsonify({"error": "Username already exists"}), 409
                
                sql_user = "INSERT INTO users (app_username, app_password_hash, app_password_salt) VALUES (%s, %s, %s)"
                cursor.execute(sql_user, (app_username, app_password_hash, app_password_salt))
                new_user_id = cursor.lastrowid
                
                if has_wazuh_data:
                    sql_cred = """
                        INSERT INTO wazuh_credentials (
                            user_id, credential_name, 
                            wazuh_host, wazuh_port, 
                            wazuh_api_username, wazuh_api_password, 
                            wazuh_indexer_username, wazuh_indexer_password, wazuh_indexer_port, 
                            created_at
                        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
                    """
                    cursor.execute(sql_cred, (
                        new_user_id,
                        data.get('credential_name', 'Default Server'),
                        w_host,
                        data.get('wazuh_port', 55000),
                        w_api_user,
                        data.get('wazuh_api_password', ''),
                        data.get('wazuh_indexer_username', ''),
                        data.get('wazuh_indexer_password', ''),
                        data.get('wazuh_indexer_port', 9200)
                    ))
                
                conn.commit()
                
            return jsonify({"message": "Registration successful", "user_id": new_user_id}), 201
            
        except mysql.connector.Error as err:
            conn.rollback()
            logging.error(f"Registration DB error: {err}")
            return jsonify({"error": "Database error during registration"}), 500
        finally:
            if conn and conn.is_connected(): conn.close()

@app.route('/api/login', methods=['POST'])
def login():
    data = request.json
    app_username = data.get('app_username')
    app_password = data.get('app_password')
    
    if not all([app_username, app_password]):
        return jsonify({"error": "Missing credentials"}), 400

    user = get_user_by_username(app_username)
    if not user:
        return jsonify({"error": "Invalid credentials"}), 401

    input_hash = hashlib.sha256((app_password + user['app_password_salt']).encode()).hexdigest()
    if input_hash != user['app_password_hash']:
        return jsonify({"error": "Invalid credentials"}), 401
    
    user_credentials = get_all_wazuh_credentials(user['id'])
    wazuh_token = None
    
    if user_credentials and len(user_credentials) > 0:
        wazuh_token = get_new_wazuh_token(user_credentials[0])

    return jsonify({
        "session_token": wazuh_token, 
        "app_username": app_username,
        "has_credentials": len(user_credentials) > 0,
	"min_severity": user.get('min_severity', 12),
        "message": "Login successful"
    }), 200

@app.route('/api/credentials', methods=['POST'])
@token_required
def add_credential():
    data = request.json
    app_username = data.get('app_username')
    if not app_username:
        return jsonify({"error": "app_username is required"}), 400
        
    user = get_user_by_username(app_username)
    if not user:
        return jsonify({"error": "User not found"}), 404

    required = ['wazuh_host', 'wazuh_api_username', 'wazuh_api_password']
    if not all(field in data for field in required):
        return jsonify({"error": "Missing required wazuh fields"}), 400

    conn = get_db_connection()
    if not conn: return jsonify({"error": "Database connection failed"}), 500
    try:
        with conn.cursor() as cursor:
            sql = """INSERT INTO wazuh_credentials (
                        user_id, credential_name, 
                        wazuh_host, wazuh_port, 
                        wazuh_api_username, wazuh_api_password, 
                        wazuh_indexer_username, wazuh_indexer_password, wazuh_indexer_port,
                        created_at
                     ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())"""
            
            cursor.execute(sql, (
                user['id'], 
                data.get('credential_name', 'New Server'), 
                data['wazuh_host'], 
                data.get('wazuh_port', 55000), 
                data['wazuh_api_username'], 
                data['wazuh_api_password'], 
                data.get('wazuh_indexer_username', ''), 
                data.get('wazuh_indexer_password', ''), 
                data.get('wazuh_indexer_port', 9200)
            ))
            conn.commit()
        return jsonify({'message': 'Credential added successfully!'}), 201
    except mysql.connector.Error as err:
        logging.error(f"Add Credential Error: {err}")
        return jsonify({"error": "Failed to add credential"}), 500
    finally:
        if conn and conn.is_connected(): conn.close()

@app.route('/api/credentials', methods=['GET'])
@token_required
def get_credentials():
    app_username = request.args.get('app_username')
    if not app_username:
        return jsonify({"error": "app_username query parameter is required"}), 400
    
    user = get_user_by_username(app_username)
    if not user: return jsonify({"error": "User not found"}), 404
        
    credentials = get_all_wazuh_credentials(user['id'])
    
    safe_credentials = []
    for cred in credentials:
        safe_cred = cred.copy()
        if 'wazuh_api_password' in safe_cred: del safe_cred['wazuh_api_password']
        if 'wazuh_indexer_password' in safe_cred: del safe_cred['wazuh_indexer_password']
        safe_credentials.append(safe_cred)
        
    return jsonify(safe_credentials)

@app.route('/api/credentials/<int:cred_id>', methods=['DELETE'])
@token_required
def delete_credential(cred_id):
    data = request.json
    app_username = data.get('app_username')
    
    user = get_user_by_username(app_username)
    if not user: return jsonify({"error": "User not found"}), 404

    conn = get_db_connection()
    if not conn: return jsonify({"error": "Database error"}), 500
    try:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM wazuh_credentials WHERE id = %s AND user_id = %s", (cred_id, user['id']))
            conn.commit()
            if cursor.rowcount == 0:
                return jsonify({'message': 'Credential not found'}), 404
        return jsonify({'message': 'Credential deleted successfully!'})
    finally:
        if conn and conn.is_connected(): conn.close()


@app.route('/api/dashboard', methods=['POST'])
@token_required
def get_aggregated_dashboard():
    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    
    if not user: return jsonify({"error": "User not found"}), 404
        
    all_credentials = get_all_wazuh_credentials(user['id'])

    if not all_credentials:
        return jsonify({
            "api_status": "No Credentials", 
            "total_events_3h": 0, 
            "high_critical_alerts_count": 0, 
            "high_priority_alerts": [],
            "message": "Please add Wazuh server credentials first."
        })

    total_events = 0
    total_high_alerts = 0
    aggregated_priority_alerts = []
    
    three_hours_ago = (datetime.now(timezone.utc) - timedelta(hours=3)).isoformat()

    for creds in all_credentials:
        query_payload = {
            "size": 0,
            "query": {"range": {"@timestamp": {"gte": three_hours_ago}}},
            "aggs": {"alerts_by_level": {"terms": {"field": "rule.level", "size": 20}}}
        }
        indexer_response = get_indexer_data(query_payload, creds)
        if indexer_response:
            total_events += indexer_response.get('hits', {}).get('total', {}).get('value', 0)
            for bucket in indexer_response.get('aggregations', {}).get('alerts_by_level', {}).get('buckets', []):
                if int(bucket.get('key', 0)) >= 12:
                    total_high_alerts += bucket.get('doc_count', 0)
        
        high_priority_query = {
            "size": 5, "sort": [{"@timestamp": "desc"}],
            "query": {"range": {"rule.level": {"gte": 15}}}
        }
        high_priority_response = get_indexer_data(high_priority_query, creds)
        if high_priority_response:
            for hit in high_priority_response.get('hits', {}).get('hits', []):
                source = hit.get('_source', {})
                rule = source.get('rule', {})
                alert = {
                    'title': rule.get('description', 'No Title'),
                    'description': source.get('full_log', 'No Details'),
                    'level': rule.get('level', 'N/A'),
                    'timeAgo': source.get('@timestamp', ''),
                    'credential_name': creds['credential_name']
                }
                aggregated_priority_alerts.append(alert)

    aggregated_priority_alerts.sort(key=lambda x: x['timeAgo'], reverse=True)

    # Return pure JSON (No manual Close Header)
    return jsonify({
        "api_status": "Connected",
        "total_events_3h": total_events,
        "high_critical_alerts_count": total_high_alerts,
        "high_priority_alerts": aggregated_priority_alerts[:5]
    })

@app.route('/api/agents', methods=['POST'])
@token_required
def get_aggregated_agents():
    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    if not user: return jsonify({"error": "User not found"}), 404
    
    all_credentials = get_all_wazuh_credentials(user['id'])
    aggregated_agents = []

    for creds in all_credentials:
        wazuh_token = get_new_wazuh_token(creds)
        if not wazuh_token: continue

        url = f"https://{creds['wazuh_host']}:{creds['wazuh_port']}/agents?pretty=true"
        headers = {'Authorization': f'Bearer {wazuh_token}'}
        try:
            response = requests.get(url, headers=headers, verify=False, timeout=10)
            if response.status_code == 200:
                agents_data = response.json()
                for agent in agents_data.get('data', {}).get('affected_items', []):
                    aggregated_agents.append({
                        'id': agent.get('id'), 
                        'name': agent.get('name'), 
                        'ip': agent.get('ip'), 
                        'status': agent.get('status'),
                        'credential_name': creds['credential_name']
                    })
        except Exception:
            pass

    # Return pure JSON (No manual Close Header)
    return jsonify({"agents": aggregated_agents})

@app.route('/api/ai_summary', methods=['POST'])
@token_required
def get_ai_summary():
    if not openai_client:
        return jsonify({"error": "OpenAI client unavailable."}), 503

    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    if not user: return jsonify({"error": "User not found"}), 405
    
    all_credentials = get_all_wazuh_credentials(user['id'])
    if not all_credentials:
        return jsonify({"summary": "No credentials configured."})
    
    aggregated_alerts_text = []

    for creds in all_credentials:
        three_hours_ago = (datetime.now(timezone.utc) - timedelta(hours=3)).isoformat()
        query_payload = {
            "size": 500, 
            "sort": [{"@timestamp": "desc"}],
            "query": {"range": {"rule.level": {"gte": 7}}}
        }
        
        indexer_response = get_indexer_data(query_payload, creds)
        
        if indexer_response:
            hits = indexer_response.get('hits', {}).get('hits', [])
            if hits:
                aggregated_alerts_text.append(f"\n[SOURCE: {creds['credential_name']}]")
                for hit in hits:
                    source = hit.get('_source', {})
                    rule = source.get('rule', {})
                    
                    agent = source.get('agent', {})
                    agent_name = agent.get('name', 'Unknown-Host')
                    agent_ip = agent.get('ip', 'Unknown-IP')
                    src_ip = source.get('src_ip', '')
                    
                    # Penting: Ambil 'full_log' sepanjang mungkin agar Hash/Path terbaca
                    full_log = source.get('full_log', '')
                    
                    log_entry = f"- [Lvl {rule.get('level')}] {rule.get('description')}"
                    log_entry += f" | Victim: {agent_name} ({agent_ip})"
                    if src_ip:
                        log_entry += f" | Attacker: {src_ip}"
                    if full_log:
                        # Limit 300 char untuk mengakomodasi File Hash/Registry Key yang panjang
                        log_entry += f" | RAW: {full_log[:300]}..." 
                    
                    aggregated_alerts_text.append(log_entry)

    if not aggregated_alerts_text:
        return jsonify({
            "threat_level": "LOW",
            "headline": "System Healthy",
            "executive_summary": "✅ No significant security events detected in the last 3 hours.",
            "ioc": {
                "attacker_ips": [], "target_users": [], "infected_endpoints": [],
                "file_artifacts": [], "network_artifacts": [], "system_artifacts": [], "email_artifacts": []
            },
            "mitigation": ["No action needed."]
        })

    alerts_text = "\n".join(aggregated_alerts_text)
    

    prompt = f"""
    Act as a Senior SOC Analyst. Analyze these raw Wazuh logs (last 3 hours):
    
    {alerts_text}

    OBJECTIVE:
    Perform deep forensic analysis to extract specific Indicators of Compromise (IoC) based on these 6 categories:
    
    INSTRUCTIONS:
    1. 📄 FILE ARTIFACTS: Look for MD5/SHA Hashes, suspicious Filenames (e.g. invoice.exe), or suspicious Paths (e.g. C:\\Temp\\svchost.exe).
    2. 🌐 NETWORK ARTIFACTS: Look for malicious Domains, URLs (C2), or User-Agent strings (e.g. sqlmap, curl).
    3. ⚙️ SYSTEM ARTIFACTS: Look for Registry Key changes (Persistence) or Service creations.
    4. 📧 EMAIL ARTIFACTS: Look for Phishing Subjects or Sender addresses.
    5. 🧠 CORRELATE: Connect the Attacker IP to these specific artifacts.

    REQUIRED OUTPUT FORMAT (Return ONLY a valid JSON Object):
    {{
      "threat_level": "One of [LOW, MEDIUM, HIGH, CRITICAL]",
      "headline": "Brief, punchy title (max 10 words)",
      "executive_summary": "A tactical paragraph. Mention the attack vector and ANY technical artifacts found (Hash/Domain/RegKey). Use emojis.",
      "ioc": {{
        "attacker_ips": ["List strings: IP Address + (Attack Label)"],
        "target_users": ["List strings: Usernames"],
        "infected_endpoints": ["List strings: Victim Name (IP)"],
        
        "file_artifacts": ["List strings: 'Hash: [md5]', 'File: [name]', or 'Path: [path]'"],
        "network_artifacts": ["List strings: 'Domain: [url]', 'User-Agent: [string]'"],
        "system_artifacts": ["List strings: 'RegKey: [key]'"],
        "email_artifacts": ["List strings: 'Subject: [text]', 'Sender: [email]'"]
      }},
      "mitigation": ["List specific actions (e.g. 'Block Hash X', 'Delete RegKey Y')"]
    }}
    """

    try:
        completion = openai_client.chat.completions.create(
            model="gpt-4o-mini", 
            messages=[
                {"role": "system", "content": "You are a SOC assistant exporting JSON data."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2, 
            max_tokens=1000,
            response_format={"type": "json_object"}
        )
        
        ai_response_content = completion.choices[0].message.content.strip()
        parsed_json = json.loads(ai_response_content)
        
        return jsonify(parsed_json)

    except Exception as e:
        logging.error(f"OpenAI/JSON Error: {e}")
        return jsonify({
            "threat_level": "UNKNOWN",
            "headline": "Error Analysis",
            "executive_summary": "Failed to process logs.",
            "ioc": {},
            "mitigation": ["Retry"]
        }), 500 

@app.route('/api/fcm_token', methods=['POST'])
@token_required
def update_user_settings():
    data = request.json
    app_username = data.get('app_username')
    fcm_token = data.get('fcm_token')
    # Ambil min_severity, default ke 12 kalau tidak dikirim
    min_severity = data.get('min_severity', 12) 

    conn = get_db_connection()
    if conn:
        try:
            with conn.cursor() as cursor:
                # Update Token DAN Min Severity sekaligus
                cursor.execute("""
                    UPDATE users 
                    SET fcm_token = %s, min_severity = %s 
                    WHERE app_username = %s
                """, (fcm_token, min_severity, app_username))
            conn.commit()
            return jsonify({"message": "Settings updated"}), 200
        finally:
            conn.close()
    return jsonify({"error": "DB Error"}), 500

# ... (kode endpoint lain di atas biarkan saja) ...

@app.route('/api/events', methods=['POST'])
@token_required
def get_recent_events():
    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    
    if not user: return jsonify({"error": "User not found"}), 404
        
    all_credentials = get_all_wazuh_credentials(user['id'])
    if not all_credentials:
        return jsonify({"events": []})

    all_events = []

    for creds in all_credentials:
        # Ambil 20 event terakhir dari setiap server (Level >= 3)
        # Level 3 itu alert standar (Login sukses, error aplikasi, dll)
        query_payload = {
            "size": 100, 
            "sort": [{"@timestamp": "desc"}],
            "query": {"range": {"rule.level": {"gte": 3}}}
        }
        
        indexer_response = get_indexer_data(query_payload, creds)
        
        if indexer_response:
            hits = indexer_response.get('hits', {}).get('hits', [])
            for hit in hits:
                source = hit.get('_source', {})
                rule = source.get('rule', {})
                agent = source.get('agent', {})
                
                event = {
                    'title': rule.get('description', 'Unknown Event'),
                    'description': source.get('full_log', 'No Details'),
                    'level': rule.get('level', '0'),
                    'timeAgo': source.get('@timestamp', ''),
                    'credential_name': creds['credential_name'],
                    'agent_name': agent.get('name', 'System')
                }
                all_events.append(event)

    # Sort gabungan event berdasarkan waktu terbaru (biar nyampur antar server)
    all_events.sort(key=lambda x: x['timeAgo'], reverse=True)

    # Return top 50 biar aplikasi gak berat
    return jsonify({"events": all_events[:50]})


@app.route('/api/notifications', methods=['POST'])
@token_required
def get_notification_history():
    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    
    if not user: return jsonify({"error": "User not found"}), 404
        
    all_credentials = get_all_wazuh_credentials(user['id'])
    if not all_credentials:
        return jsonify({"alerts": []})

    # 1. AMBIL SETTINGAN LEVEL USER (Kuncinya Disini)
    # Kalau user set 5, kita ambil 5. Kalau null, default 12.
    user_min_severity = user.get('min_severity')
    if not user_min_severity or user_min_severity < 1:
        user_min_severity = 12

    all_alerts = []

    for creds in all_credentials:
        # Query: Ambil Alert yang levelnya >= user_min_severity
        query_payload = {
            "size": 20, # Ambil 20 terakhir per server
            "sort": [{"@timestamp": "desc"}],
            "query": {"range": {"rule.level": {"gte": user_min_severity}}}
        }
        
        indexer_response = get_indexer_data(query_payload, creds)
        
        if indexer_response:
            hits = indexer_response.get('hits', {}).get('hits', [])
            for hit in hits:
                source = hit.get('_source', {})
                rule = source.get('rule', {})
                agent = source.get('agent', {})
                
                alert = {
                    'title': rule.get('description', 'Unknown Alert'),
                    'description': source.get('full_log', 'No Details'),
                    'level': rule.get('level', '0'),
                    'timeAgo': source.get('@timestamp', ''),
                    'credential_name': creds['credential_name'],
                    'agent_name': agent.get('name', 'System')
                }
                all_alerts.append(alert)

    # Sort gabungan alert berdasarkan waktu terbaru
    all_alerts.sort(key=lambda x: x['timeAgo'], reverse=True)

    return jsonify({"alerts": all_alerts[:50]})

if __name__ == '__main__':
    logging.info("Starting Flask server...")
    app.run(host='0.0.0.0', port=5000, threaded=True)
