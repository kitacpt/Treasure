#!/usr/bin/env bash
# 启个静态服务器看 prototype/project/Treasure.html
# 直接 file:// 打开也能跑（原型用 unpkg 拿 React），但有些浏览器
# 对 file:// 下的 type=text/babel 的处理有限制，走 http 最稳。

set -euo pipefail

REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${PORT:-7000}"

cd "$REPO_ROOT/prototype/project"

echo "→ Serving prototype at http://localhost:$PORT/Treasure.html"
echo "  (Ctrl+C to stop)"
echo

if command -v python3 >/dev/null 2>&1; then
  python3 -m http.server "$PORT"
elif command -v python >/dev/null 2>&1; then
  python -m SimpleHTTPServer "$PORT"
else
  echo "需要 python3 (或 python 2.x) 来起静态服务器" >&2
  exit 1
fi
