#!/usr/bin/env bash
# 开发环境自检 + 提示
# cycle 0001 还没有 Gradle 工程；这个脚本暂时只做"环境是否就绪"的检查
# 等 android/ 真正建起来之后，再扩展为：装 SDK 组件、跑 gradle wrapper 等。

set -euo pipefail

REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }
red() { printf '\033[31m%s\033[0m\n' "$*"; }

check() {
  local name="$1" cmd="$2"
  if command -v "$cmd" >/dev/null 2>&1; then
    green "  ✓ $name"
  else
    red   "  ✗ $name  ($cmd 不在 PATH 里)"
    return 1
  fi
}

echo "Treasure · 环境自检"
echo

missing=0

echo "看原型："
check "python3 (跑 prototype-serve.sh)"   python3 || missing=$((missing+1))

echo
echo "Android 工程（cycle 0001 起会用到）："
check "java (>=17, Gradle 用)"             java     || missing=$((missing+1))
check "git"                                git      || missing=$((missing+1))

if [ -d "$REPO_ROOT/android" ] && [ -f "$REPO_ROOT/android/gradlew" ]; then
  echo
  green "android/gradlew 存在 —— 工程已建起"
elif [ -d "$REPO_ROOT/android" ]; then
  echo
  yellow "android/ 目录存在但还没有 gradlew —— 在 cycle 0001 里 'gradle init' 一下"
else
  echo
  yellow "android/ 还没建 —— 这是 cycle 0001 的第一步"
fi

echo
if [ "$missing" -gt 0 ]; then
  red "$missing 项缺失，先把上面 ✗ 的装好。"
  exit 1
else
  green "环境就绪。下一步看 openspec/0001-mvp-portal-grid-detail-add/spec.md"
fi
