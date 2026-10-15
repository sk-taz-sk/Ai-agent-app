import json
from pathlib import Path

MEMORY_FILE = Path(__file__).parent / "memory.json"

def load_memory():
    if MEMORY_FILE.exists():
        try:
            return json.loads(MEMORY_FILE.read_text())
        except:
            return {}
    return {}

def save_memory(key, value):
    memory = load_memory()
    memory[key] = value
    MEMORY_FILE.write_text(json.dumps(memory, indent=2))
    return value

def get_memory(key):
    return load_memory().get(key)
