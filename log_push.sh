#!/bin/bash

# ─────────────────────────────────────────
#   CONFIG — edit these values once
# ─────────────────────────────────────────
CONTAINER_ID="17c73ab66819"
GITHUB_USER="vamshii9989"
GITHUB_TOKEN=$GITHUB_TOKEN
REPO_NAME="Expense_Tracker"
LOG_DIR="logs"
# ─────────────────────────────────────────

TIMESTAMP=$(date '+%Y-%m-%d_%H-%M-%S')
RAW_LOG="$LOG_DIR/mongo_raw_$TIMESTAMP.log"
CLEAN_LOG="$LOG_DIR/mongo_clean_$TIMESTAMP.log"

echo "🔄 Step 1: Setting GitHub remote..."
git remote set-url origin https://$GITHUB_USER:$GITHUB_TOKEN@github.com/$GITHUB_USER/$REPO_NAME.git

echo "📦 Step 2: Extracting container logs..."
mkdir -p $LOG_DIR
docker logs $CONTAINER_ID > $RAW_LOG 2>&1

echo "🧹 Step 3: Formatting logs..."
python3 - < $RAW_LOG > $CLEAN_LOG << 'PYEOF'
import json, sys
from datetime import datetime

lines = sys.stdin.read().strip().split('\n')
SEV = {"I": "INFO", "W": "WARN", "E": "ERR ", "F": "FATAL", "D": "DBG "}
out = []

out.append("╔══════════════════════════════════════════════════════════════╗")
out.append("║          MONGODB LOG REPORT — " + datetime.now().strftime('%Y-%m-%d %H:%M:%S') + "          ║")
out.append("╚══════════════════════════════════════════════════════════════╝")
out.append("")

conn_open = conn_close = 0
warns = []
collections = []
errors = []

for line in lines:
    line = line.strip()
    if not line:
        continue
    try:
        e = json.loads(line)
        t   = e.get("t", {}).get("$date", "")[:19].replace("T", " ")
        sev = SEV.get(e.get("s", "I"), "INFO")
        cmp = e.get("c", "").strip()[:8].ljust(8)
        msg = e.get("msg", "")
        attr = e.get("attr", {})

        # Skip noisy checkpoint lines
        if "WiredTiger message" in msg and "checkpoint" in str(attr):
            continue

        detail = ""
        if "remote" in attr and "connectionId" in attr:
            detail = f"| {attr['remote']} conn#{attr['connectionId']}"
        elif "namespace" in attr:
            detail = f"| {attr['namespace']}"
        elif "port" in attr and "ssl" in attr:
            detail = f"| port {attr['port']}  ssl={attr['ssl']}"
        elif "pid" in attr:
            detail = f"| pid={attr['pid']}  host={attr.get('host','')}"
        elif "newVersion" in attr:
            detail = f"| version={attr['newVersion']}"
        elif "error" in attr:
            detail = f"| ⚠ {attr['error']}"
        elif "durationMillis" in attr:
            detail = f"| took {attr['durationMillis']}ms"
        elif isinstance(attr.get("message"), dict):
            detail = f"| {attr['message'].get('msg','')[:60]}"

        icon = {"INFO": "✅", "WARN": "⚠️ ", "ERR ": "❌", "FATAL": "💀", "DBG ": "🔍"}.get(sev, "  ")
        out.append(f"{icon} [{t}] [{sev}] [{cmp}]  {msg} {detail}")

        # summary counters
        if msg == "Connection accepted":   conn_open  += 1
        if msg == "Connection ended":      conn_close += 1
        if e.get("s") == "W":             warns.append(msg)
        if msg == "createCollection" and "namespace" in attr:
            collections.append(attr["namespace"])
        if e.get("s") in ("E", "F"):      errors.append(msg)

    except:
        pass

out.append("")
out.append("━" * 65)
out.append("  SUMMARY")
out.append("━" * 65)
out.append(f"  🔗 Connections Opened  : {conn_open}")
out.append(f"  🔌 Connections Closed  : {conn_close}")
out.append(f"  🗂  Collections Created : {', '.join(collections) or 'None'}")
out.append(f"  ⚠️  Warnings            : {len(warns)}")
for w in set(warns):
    out.append(f"       → {w}")
out.append(f"  ❌ Errors              : {len(errors) or 'None'}")
out.append("━" * 65)
print('\n'.join(out))
PYEOF

echo "✅ Step 4: Clean log saved → $CLEAN_LOG"
echo ""
cat $CLEAN_LOG
echo ""

echo "🚀 Step 5: Pushing to GitHub..."
git add $LOG_DIR/
git commit -m "logs: update MongoDB logs [$TIMESTAMP]"
git push

echo ""
echo "✅ Done! Logs pushed to GitHub → $LOG_DIR/"
