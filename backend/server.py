from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import re
import os
from dotenv import load_dotenv
from google import genai
from backend.memory import save_memory, get_memory

load_dotenv("backend/.env")
client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

def clean_text(text):
    return re.sub(r'\*+', '', text or '').strip()

class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/":
            result = {
                "status": "ok",
                "message": "AI Agent backend is running"
            }
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode("utf-8"))
        else:
            self.send_error(404)

    def do_HEAD(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_POST(self):
        if self.path != "/chat":
            self.send_error(404)
            return

        length = int(self.headers.get("Content-Length", 0))
        data = json.loads(self.rfile.read(length))
        prompt = data.get("message", "")
        if prompt.strip() in ["ফোনের সেটিং খোলো", "সেটিং খোলো", "settings খোলো"]:
              result = {"reply": "Settings খুলছি..."}
              self.send_response(200)
              self.send_header("Content-Type", "application/json")
              self.send_header("Access-Control-Allow-Origin", "*")
              self.end_headers()
              self.wfile.write(json.dumps(result).encode())
              return

        if (prompt.lower().startswith("my name is ") or prompt.startswith("আমার নাম ")) and prompt.strip().rstrip(chr(63)+chr(33)+"।") not in ["আমার নাম কি", "আমার নাম কী"]:
            save_memory("name", prompt[11:].strip() if prompt.lower().startswith("my name is ") else prompt[9:].strip())
            result = {"reply": "ঠিক আছে, তোমার নাম " + str(get_memory("name")) + " মনে রাখব।"}
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode())
            return
        elif prompt.strip().lower().rstrip("?!।").strip() in ["what is my name", "amar name ki", "amar nam ki", "amar naam ki", "আমার নাম কি", "আমার নাম কী"]:
            saved_name = get_memory("name")
            if saved_name:
                result = {"reply": f"তোমার নাম {saved_name}।"}
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps(result).encode())
                return

        try:
            import time
            last_error = None
            for attempt in range(3):
                try:
                    response = client.models.generate_content(model="gemini-3.8-flash", contents=prompt)
                    result = {"reply": clean_text(response.text)}
                    last_error = None
                    break
                except Exception as e:
                    last_error = e
                    if "503" in str(e) and attempt < 2:
                        time.sleep(3)
            if last_error is not None:
                raise last_error
        except Exception as e:
            result = {
                "reply": "Gemini is temporarily unavailable. Please try again.",
                "error": str(e)
            }

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(result).encode())

if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", int(os.environ.get("PORT", 8006))), Handler)
    print("AI Agent backend running on port configured by Render")
    server.serve_forever()
