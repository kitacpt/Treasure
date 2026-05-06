#!/usr/bin/env bash
# Serve the latest debug APK over HTTP so you can grab it from the phone.
# Usage:  ./scripts/serve-apk.sh
# Then on the phone, open http://<dev-machine-ip>:8000/treasure.apk
# (vivo's built-in browser works; the phone will prompt to install on download.)

set -euo pipefail

REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$REPO_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
PORT="${PORT:-8000}"

if [ ! -f "$APK" ]; then
  echo "找不到 APK：$APK"
  echo "先在 android/ 目录下跑：./gradlew :app:assembleDebug"
  exit 1
fi

# Stage the APK with a friendly filename in a tmp dir, serve from there.
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
cp "$APK" "$STAGE/treasure.apk"
SIZE="$(du -h "$APK" | cut -f1)"

# Try to print a useful URL based on default-route IP.
IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
[ -z "$IP" ] && IP="<dev-machine-ip>"

echo
echo "  Treasure debug APK  ($SIZE)"
echo "  → http://$IP:$PORT/treasure.apk"
echo
echo "  在手机上用浏览器打开上面的链接，下载完成会弹出"
echo "  '是否安装' 的提示。第一次需要给浏览器授权"
echo "  '安装外部来源应用'。"
echo
echo "  Ctrl+C 关闭"
echo

cd "$STAGE"
if command -v python3 >/dev/null 2>&1; then
  python3 -m http.server "$PORT"
else
  python -m SimpleHTTPServer "$PORT"
fi
