#!/usr/bin/env bash
# Builds index.html from src/app.html and stamps the app version into sw.js.
# To release an update: change APP_VERSION in src/app.html, run ./build.sh, commit and push.
set -euo pipefail
cd "$(dirname "$0")"

VERSION=$(grep -o 'const APP_VERSION="[^"]*"' src/app.html | sed 's/.*="\(.*\)"/\1/')
[ -n "$VERSION" ] || { echo "APP_VERSION not found in src/app.html" >&2; exit 1; }

{
  cat <<'HEAD'
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<meta name="theme-color" content="#070b18">
<meta name="description" content="AETHER AI - your personal assistant with voice, AI mentors, alerts and a WhatsApp helper.">
<link rel="manifest" href="manifest.webmanifest">
<link rel="icon" type="image/png" href="icons/favicon-64.png">
<link rel="apple-touch-icon" href="icons/apple-touch-icon.png">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<meta name="apple-mobile-web-app-title" content="AETHER">
<style>:root{color-scheme:light;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0;font:14px system-ui,sans-serif}img{max-width:100%}[hidden]{display:none!important}</style>
</head>
<body>
HEAD
  cat src/app.html
  printf '\n</body>\n</html>\n'
} > index.html

sed -i "s/^const VERSION = \"[^\"]*\";/const VERSION = \"$VERSION\";/" sw.js
echo "Built AETHER AI v$VERSION"
