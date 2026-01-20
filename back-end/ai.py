# ==============================================================================
# ENDPOINT: AI SUMMARY (OTAK DARI APLIKASI)
# ==============================================================================
@app.route('/api/ai_summary', methods=['POST'])
@token_required  # [SECURITY] Decorator untuk memastikan cuma user yang login yang bisa akses
def get_ai_summary():
    
    # 1. CEK KETERSEDIAAN AI CLIENT
    # Tujuannya: Mencegah aplikasi crash kalau API Key salah/expired saat server start.
    if not openai_client:
        return jsonify({"error": "OpenAI client unavailable."}), 503

    # 2. AMBIL DATA USER
    data = request.json
    app_username = data.get('app_username')
    user = get_user_by_username(app_username)
    
    if not user: 
        return jsonify({"error": "User not found"}), 405
    
    # 3. AMBIL KREDENSIAL WAZUH (MULTI-TENANCY)
    # [PENTING SIDANG] Aplikasi ini support banyak server sekaligus.
    # Kita ambil semua server yang didaftarkan oleh user ini.
    all_credentials = get_all_wazuh_credentials(user['id'])
    
    if not all_credentials:
        return jsonify({"summary": "No credentials configured."})
    
    # List penampung teks log dari berbagai server
    aggregated_alerts_text = []

    # 4. LOOPING KE SETIAP SERVER WAZUH (DATA MINING)
    for creds in all_credentials:
        
        # Ambil waktu 3 jam terakhir (Context Window)
        three_hours_ago = (datetime.now(timezone.utc) - timedelta(hours=3)).isoformat()
        
        # [PENTING SIDANG] QUERY OPTIMIZATION
        # Kita tidak mengambil semua log. Kita filter hanya log KRITIKAL (Level >= 7).
        # Alasan: Hemat token OpenAI dan fokus pada ancaman nyata (bukan noise).
        query_payload = {
            "size": 500,  # Batasi maksimal 500 log biar memori server aman
            "sort": [{"@timestamp": "desc"}],
            "query": {"range": {"rule.level": {"gte": 7}}} 
        }
        
        # Tembak ke Wazuh Indexer (Database Log)
        indexer_response = get_indexer_data(query_payload, creds)
        
        if indexer_response:
            hits = indexer_response.get('hits', {}).get('hits', [])
            
            if hits:
                # Penanda asal server (biar AI tau ini log dari server mana)
                aggregated_alerts_text.append(f"\n[SOURCE: {creds['credential_name']}]")
                
                # 5. DATA PRE-PROCESSING (PEMERASAN DATA)
                for hit in hits:
                    source = hit.get('_source', {})
                    rule = source.get('rule', {})
                    agent = source.get('agent', {})
                    
                    # Ekstrak info vital
                    agent_name = agent.get('name', 'Unknown-Host')
                    agent_ip = agent.get('ip', 'Unknown-IP')
                    src_ip = source.get('src_ip', '') # IP Penyerang (kalau ada)
                    full_log = source.get('full_log', '') # Log mentah (bisa panjang banget)
                    
                    # Susun kalimat log yang efisien
                    log_entry = f"- [Lvl {rule.get('level')}] {rule.get('description')}"
                    log_entry += f" | Victim: {agent_name} ({agent_ip})"
                    
                    if src_ip:
                        log_entry += f" | Attacker: {src_ip}"
                        
                    # [PENTING SIDANG] TRUNCATION / PEMOTONGAN LOG
                    # Masalah: Log Wazuh bisa ribuan karakter. Bikin AI lemot (30s+).
                    # Solusi: Kita potong cuma ambil 300 karakter pertama.
                    # Hasil: Info penting (Hash, Path) dapet, tapi durasi jadi cepet (5-10s).
                    if full_log:
                        log_entry += f" | RAW: {full_log[:300]}..." 
                    
                    # Masukkan ke list
                    aggregated_alerts_text.append(log_entry)

    # 6. EARLY EXIT (EFISIENSI BIAYA)
    # Kalau ternyata gak ada serangan level tinggi, JANGAN panggil AI.
    # Hemat kuota API dan respon instan ke user.
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

    # Gabung semua log jadi satu string panjang buat dikirim ke AI
    alerts_text = "\n".join(aggregated_alerts_text)
    

    # 7. PROMPT ENGINEERING (INSTRUKSI KE OTAK AI)
    # [PENTING SIDANG] Kita pakai teknik "Persona" (Senior SOC Analyst)
    # dan "Structured Output" (Wajib JSON).
    prompt = f"""
    Act as a Senior SOC Analyst. Analyze these raw Wazuh logs (last 3 hours):
    
    {alerts_text}

    OBJECTIVE:
    Perform deep forensic analysis to extract specific Indicators of Compromise (IoC)...
    
    INSTRUCTIONS:
    1. 📄 FILE ARTIFACTS: Look for MD5/SHA Hashes...
    2. 🌐 NETWORK ARTIFACTS: Look for malicious Domains...
    ...
    REQUIRED OUTPUT FORMAT (Return ONLY a valid JSON Object):
    {{
      "threat_level": "One of [LOW, MEDIUM, HIGH, CRITICAL]",
      "headline": "Brief, punchy title",
      ...
    }}
    """

    try:
        # 8. PEMANGGILAN API OPENAI (INFERENCE)
        completion = openai_client.chat.completions.create(
            model="gpt-4o-mini",      # [OPTIMASI] Model paling cepat & murah saat ini
            messages=[
                {"role": "system", "content": "You are a SOC assistant exporting JSON data."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,          # [ANTI-HALUSINASI] Rendah = Faktual, Tinggi = Ngarang. Kita pilih Rendah.
            max_tokens=1000,          # Batas panjang jawaban biar gak kepotong
            response_format={"type": "json_object"} # [STABILITAS] Paksa output jadi JSON murni biar gak error di Android
        )
        
        # 9. PARSING & RETURN
        # Ambil teks JSON dari AI
        ai_response_content = completion.choices[0].message.content.strip()
        
        # Ubah string JSON jadi Object Python (Dictionary)
        parsed_json = json.loads(ai_response_content)
        
        # Kirim ke Android
        return jsonify(parsed_json)

    except Exception as e:
        # Error Handling: Kalau AI error/timeout, jangan biarkan aplikasi crash.
        logging.error(f"OpenAI/JSON Error: {e}")
        return jsonify({
            "threat_level": "UNKNOWN",
            "headline": "Error Analysis",
            "executive_summary": "Failed to process logs.",
            "ioc": {},
            "mitigation": ["Retry"]
        }), 500
