#!/usr/bin/env bash
# Downloads the app under test (WebdriverIO's official native demo app) next to pom.xml.
set -euo pipefail
cd "$(dirname "$0")/.."
APK=android.wdio.native.app.v2.2.0.apk
if [ ! -s "$APK" ]; then
  curl -fL --retry 3 -o "$APK" \
    "https://github.com/webdriverio/native-demo-app/releases/download/v2.2.0/$APK"
fi
ls -la "$APK"
