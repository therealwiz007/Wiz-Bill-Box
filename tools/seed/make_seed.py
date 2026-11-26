#!/usr/bin/env python3
"""Builds the demo data used for the Play screenshots (debug builds only).

Writes <out>/bills.json, <out>/photos/*.jpg, <out>/docs/*.pdf and <out>/demo-mode.
Receipt "photos" are rendered from HTML with headless Chrome (paper receipts on a desk); the PDF bill is printed from HTML.
Dates are relative to --today so warranty states (active / expiring / expired) always look right.
Usage: python3 tools/seed/make_seed.py <out-dir> [--today YYYY-MM-DD]
"""
import json, os, subprocess, sys, uuid, datetime, shutil, html, hashlib

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
out = sys.argv[1]
today = datetime.date.fromisoformat(sys.argv[sys.argv.index("--today") + 1]) if "--today" in sys.argv else datetime.date.today()
os.makedirs(out + "/photos", exist_ok=True); os.makedirs(out + "/docs", exist_ok=True)
tmp = out + "/_tmp"; os.makedirs(tmp, exist_ok=True)

def d(days): return (today + datetime.timedelta(days=days)).isoformat()
def uid(seed): return str(uuid.UUID(hashlib.md5(seed.encode()).hexdigest()))

# (key, store, product, invoice offset days, amount, invoice no, category, payment, warranty offset days or None, gstin, notes, items, photo?)
BILLS = [
 ("tv",   "Croma",            'LG 43" 4K Smart TV',        -55, 32999, "CR-26-118204", "ELECTRONICS", "UPI",         365*3-55, "07AAACC1234F1Z5", "Serial 7XK2291. Wall mount fitted by Croma.",
     [("LG 43UR7550 TV", 1, 32999)], True),
 ("wash", "Reliance Digital", "Samsung 7 kg Washing Machine", -349, 28490, "RD-25-77031", "APPLIANCES", "CARD",     16, "27AABCR5678K1ZQ", "Model WW70T. Installation done.",
     [("Samsung WW70T Front Load", 1, 28490)], True),
 ("mixer","Bajaj Store",      "Bajaj Rex 500W Mixer Grinder", -360, 3290, "BJ-25-40552", "APPLIANCES", "CASH",      4, "", "",
     [("Bajaj Rex 500W", 1, 3290)], True),
 ("phone","iPlanet",          "iPhone 15 128 GB",           -727, 69900, "IP-24-90113", "ELECTRONICS", "EMI",        -26, "29AAKCI9012L1ZV", "AppleCare not taken.",
     [("iPhone 15 128GB Blue", 1, 69900)], False),
 ("fryer","Vijay Sales",      "Havells Prolife Air Fryer",   -192, 7490, "VS-26-11870", "APPLIANCES", "UPI",         365-192, "27AAACV3456D1Z2", "",
     [("Havells Prolife 4L", 1, 7490)], False),
 ("desk", "Urban Ladder",     "Oak Study Desk 120 cm",       -104, 18999, "UL-26-55021", "FURNITURE", "NET_BANKING", 365*2-104, "29AAHCU7890M1ZR", "Assembly by UL team.",
     [("Oak Study Desk 120cm", 1, 18999)], True),
 ("pdf",  "Amazon.in",        "boAt Rockerz 450 Headphones", -14, 1799, "IN-BLR8-4417823", "ELECTRONICS", "CARD",    351, "29AALCA5555Q1ZN", "E-bill (PDF) from Amazon.",
     [("boAt Rockerz 450", 1, 1799)], False),
 ("dmart","DMart",            "Weekly groceries",            -6, 3184.5, "DM-8821-3342", "GROCERY", "UPI",           None, "27AABCD1111G1ZH", "",
     [("Atta 10kg", 1, 485), ("Basmati rice 5kg", 1, 640), ("Toor dal 2kg", 1, 312), ("Amul butter", 2, 114.5), ("Milk 6 pk", 1, 408), ("Misc", 1, 1225)], True),
 ("fuel", "Indian Oil",       "Petrol 24.3 L",               -8, 2400, "IOC-0042-7731", "FUEL", "CARD",               None, "", "",
     [("Petrol 24.3 L @ 98.76", 1, 2400)], True),
 ("pharm","Apollo Pharmacy",  "Monthly medicines",           -17, 865, "AP-3310-9012", "MEDICAL", "UPI",              None, "29AAACA2222B1Z9", "",
     [("Metformin 500", 2, 120), ("Vitamin D3", 1, 245), ("Cetirizine", 1, 38), ("BP monitor strips", 1, 382)], True),
]

CSS = """
body{margin:0;width:1200px;height:1600px;background:radial-gradient(circle at 30% 20%,#b08a5e,#7d5a37 70%);display:flex;align-items:center;justify-content:center;font-family:'Courier New',monospace}
.r{width:620px;background:#fffdf7;padding:46px 44px 60px;box-shadow:12px 18px 40px rgba(0,0,0,.45);transform:rotate(ROTdeg);color:#2a2a2a;position:relative}
.r:after{content:'';position:absolute;left:0;right:0;bottom:-14px;height:14px;background:linear-gradient(135deg,#fffdf7 50%,transparent 50%) 0 0/20px 14px,linear-gradient(225deg,#fffdf7 50%,transparent 50%) 0 0/20px 14px}
h1{margin:0;text-align:center;font-size:44px;letter-spacing:2px}.c{text-align:center;font-size:21px;color:#555;margin:6px 0}
hr{border:0;border-top:2px dashed #999;margin:18px 0}.row{display:flex;justify-content:space-between;font-size:24px;margin:8px 0}
.t{font-weight:700;font-size:32px}.s{font-size:19px;color:#666}
"""
def receipt_html(b, rot):
    key, store, product, off, amt, inv, cat, pay, w, gst, notes, items, photo = b
    rows = "".join(f"<div class='row'><span>{html.escape(n)}{' x%d'%q if q>1 else ''}</span><span>{p*q:,.2f}</span></div>" for n,q,p in items)
    return f"""<html><head><meta charset='utf-8'><style>{CSS.replace('ROT', str(rot))}</style></head><body><div class='r'>
<h1>{html.escape(store.upper())}</h1><div class='c'>Tax Invoice · GST included</div>{f"<div class='c'>GSTIN {gst}</div>" if gst else ''}
<hr><div class='row s'><span>Inv: {inv}</span><span>{d(off)}</span></div><hr>{rows}<hr>
<div class='row t'><span>TOTAL</span><span>₹ {amt:,.2f}</span></div><div class='row s'><span>Paid by</span><span>{pay.replace('_',' ')}</span></div>
<hr><div class='c'>Thank you. Keep this bill for warranty.</div></div></body></html>"""

def chrome(args): subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars"] + args, check=True, capture_output=True)

bills = []; now = int(datetime.datetime.now().timestamp() * 1000)
for i, b in enumerate(BILLS):
    key, store, product, off, amt, inv, cat, pay, w, gst, notes, items, photo = b
    bid = uid("bill-" + key); photos = []; files = []
    if photo:
        pid = uid("photo-" + key)
        h = f"{tmp}/{key}.html"; open(h, "w").write(receipt_html(b, [-2.5, 1.8, -1.2, 2.2][i % 4]))
        png = f"{tmp}/{key}.png"
        chrome([f"--screenshot={png}", "--window-size=1200,1600", "file://" + h])
        subprocess.run(["sips", "-s", "format", "jpeg", "-s", "formatOptions", "80", png, "--out", f"{out}/photos/{pid}.jpg"], check=True, capture_output=True)
        photos.append({"id": pid, "driveId": "demo-" + pid, "localName": pid + ".jpg"})
    if key == "pdf":
        fid = uid("file-" + key)
        h = f"{tmp}/{key}.html"
        open(h, "w").write(f"""<html><head><meta charset='utf-8'><style>body{{font-family:Helvetica,Arial,sans-serif;margin:48px;color:#222}}
h1{{color:#232f3e;margin:0}}table{{width:100%;border-collapse:collapse;margin-top:24px}}td,th{{border-bottom:1px solid #ddd;padding:10px;text-align:left}}</style></head><body>
<h1>Tax Invoice</h1><p>Sold by: Appario Retail Pvt Ltd · GSTIN {gst}<br>Invoice No: {inv} · Date: {d(off)}</p>
<table><tr><th>Item</th><th>Qty</th><th>Amount</th></tr><tr><td>{html.escape(product)}</td><td>1</td><td>₹{amt:,.2f}</td></tr>
<tr><td colspan=2><b>Total</b></td><td><b>₹{amt:,.2f}</b></td></tr></table><p>Warranty: 1 year from invoice date.</p></body></html>""")
        chrome([f"--print-to-pdf={out}/docs/{fid}.pdf", "--no-pdf-header-footer", "file://" + h])
        files.append({"id": fid, "name": f"Invoice {inv}.pdf", "mime": "application/pdf", "driveId": "demo-" + fid, "localName": fid + ".pdf"})
    created = now - (len(BILLS) - i) * 3600_000
    bills.append({
        "format": 1, "app": "Wiz Bill Box", "id": bid, "store": store, "product": product, "invoiceDate": d(off),
        "amount": amt, "invoiceNumber": inv, "category": cat, "paymentMode": pay,
        "warrantyUntil": d(w) if w is not None else None, "gstin": gst, "notes": notes,
        "createdAt": created, "updatedAt": created, "photos": photos, **({"files": files} if files else {}),
        "driveFolderId": "demo-f-" + key, "driveJsonId": "demo-j-" + key, "dirty": False,
    })
data = {"version": 1, "theme": "SYSTEM", "account": {"email": "riya.sharma@gmail.com", "name": "Riya Sharma"}, "rootFolderId": "demo-root",
        "lastSyncAt": now - 120_000, "warrantyReminderDays": 30, "skippedSignIn": False, "pendingDeletes": [], "bills": bills}
json.dump(data, open(out + "/bills.json", "w"), ensure_ascii=False)
open(out + "/demo-mode", "w").write("1")
shutil.rmtree(tmp)
print("seed ready:", out, len(bills), "bills")
