#!/usr/bin/env python3
"""
RoadLens AI - Raspberry Pi auto-capture & upload agent
Presented by G-Lab

Setup:
  sudo apt install -y python3-picamera2 python3-requests gpsd gpsd-clients python3-gps
  export ROADLENS_URL="https://id-preview--8e6c55ef-bc05-4e5d-bc64-6fe5848b3cd7.lovable.app"
  export ROADLENS_DEVICE_ID="YOUR_DEVICE_ID"
  export ROADLENS_DEVICE_KEY="paste-key-shown-once-in-Devices"
  python3 roadlens_agent.py

Run on boot:  sudo cp roadlens.service /etc/systemd/system/ && sudo systemctl enable --now roadlens
"""
import os, time, uuid, json, hashlib, pathlib, datetime, requests

URL = os.environ.get("ROADLENS_URL", "https://id-preview--8e6c55ef-bc05-4e5d-bc64-6fe5848b3cd7.lovable.app").rstrip("/")
DEVICE_ID = os.environ["ROADLENS_DEVICE_ID"]
DEVICE_KEY = os.environ["ROADLENS_DEVICE_KEY"]
VERSION = "1.0.0"
QUEUE = pathlib.Path(os.environ.get("ROADLENS_QUEUE", "/home/pi/roadlens_queue"))
QUEUE.mkdir(parents=True, exist_ok=True)
HEADERS = {"X-Device-Id": DEVICE_ID, "X-Device-Key": DEVICE_KEY}

# ---------- camera ----------
try:
    from picamera2 import Picamera2
    cam = Picamera2()
    cam.configure(cam.create_still_configuration(main={"size": (1640, 1232)}))
    cam.start(); time.sleep(2)
    CAMERA_OK = True
except Exception as e:
    print("Camera unavailable:", e); cam = None; CAMERA_OK = False

# ---------- GPS (gpsd) ----------
def read_gps(timeout=3):
    try:
        import gps
        s = gps.gps(mode=gps.WATCH_ENABLE)
        end = time.time() + timeout
        while time.time() < end:
            r = s.next()
            if r.get("class") == "TPV" and getattr(r, "mode", 0) >= 2:
                return float(r.lat), float(r.lon)
    except Exception:
        pass
    return None  # never fabricate coordinates

def capture():
    event_id = f"{DEVICE_ID[:8]}-{datetime.datetime.utcnow().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:6]}"
    img = QUEUE / f"{event_id}.jpg"
    cam.capture_file(str(img))
    meta = {"event_id": event_id, "timestamp": datetime.datetime.utcnow().isoformat() + "Z"}
    g = read_gps()
    if g: meta["latitude"], meta["longitude"] = g
    (QUEUE / f"{event_id}.json").write_text(json.dumps(meta))
    print("Captured", event_id, "GPS" if g else "no GPS")

def flush_queue():
    for meta_file in sorted(QUEUE.glob("*.json")):
        meta = json.loads(meta_file.read_text())
        img = QUEUE / f"{meta['event_id']}.jpg"
        if not img.exists(): meta_file.unlink(); continue
        try:
            with open(img, "rb") as f:
                r = requests.post(f"{URL}/api/public/pi/upload", headers=HEADERS,
                                  data=meta, files={"image": (img.name, f, "image/jpeg")}, timeout=60)
            if r.status_code == 200 and r.json().get("ok"):
                img.unlink(); meta_file.unlink()
                print("Uploaded", meta["event_id"], r.json())
            elif r.status_code in (400, 401):
                print("Rejected:", r.text); 
                if r.status_code == 400: img.unlink(); meta_file.unlink()
                return
            else:
                print("Server busy", r.status_code); return
        except requests.RequestException as e:
            print("Offline, will retry:", e); return

def heartbeat():
    try:
        r = requests.post(f"{URL}/api/public/pi/heartbeat", headers=HEADERS, timeout=15,
                          json={"camera_ok": CAMERA_OK, "gps_ok": read_gps(1) is not None, "version": VERSION})
        return r.json() if r.ok else {}
    except requests.RequestException:
        return {}

interval, paused, last_hb = 10, False, 0
while True:
    if time.time() - last_hb > 30:
        cfg = heartbeat(); last_hb = time.time()
        interval = cfg.get("capture_interval_sec", interval); paused = cfg.get("paused", paused)
    if CAMERA_OK and not paused:
        try: capture()
        except Exception as e: print("Capture error:", e)
    flush_queue()
    time.sleep(interval)
