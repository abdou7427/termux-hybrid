import sqlite3
import sys
import json
import os

DB_PATH = os.path.expanduser("~/agent_project/conversations.db")

def list_conversations():
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT c.id, c.start_time, c.message_count, 
                   (SELECT m.content FROM messages m WHERE m.conv_id = c.id AND m.role='user' LIMIT 1) 
            FROM conversations c 
            ORDER BY c.start_time DESC LIMIT 30
        """)
        data = []
        for row in cursor.fetchall():
            preview = row[3] if row[3] else "New Chat"
            data.append({
                "id": row[0], 
                "start_time": row[1], 
                "message_count": row[2], 
                "preview": preview[:50] + "..." if len(preview) > 50 else preview
            })
        conn.close()
        print(json.dumps(data))
    except Exception as e:
        print(json.dumps({"error": str(e)}))

def get_conversation(conv_id):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT role, content FROM messages WHERE conv_id = ? ORDER BY timestamp ASC", (conv_id,))
        data = [{"role": row[0], "content": row[1]} for row in cursor.fetchall()]
        conn.close()
        print(json.dumps(data))
    except Exception as e:
        print(json.dumps({"error": str(e)}))

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(1)
    if sys.argv[1] == "list":
        list_conversations()
    elif sys.argv[1] == "get" and len(sys.argv) == 3:
        get_conversation(sys.argv[2])
