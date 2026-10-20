#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${SEREIN_KEYSTORE_FILE:?Set SEREIN_KEYSTORE_FILE to your private PKCS12 keystore}"
: "${SEREIN_KEYSTORE_PASSWORD:?Set SEREIN_KEYSTORE_PASSWORD}"
: "${SEREIN_KEY_ALIAS:?Set SEREIN_KEY_ALIAS}"
: "${SEREIN_KEY_PASSWORD:?Set SEREIN_KEY_PASSWORD}"
if [[ ! -f "$SEREIN_KEYSTORE_FILE" ]]; then
    echo 'Release keystore not found.' >&2
    exit 1
fi
sdk_path="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
: "${sdk_path:?Set ANDROID_SDK_ROOT or ANDROID_HOME}"
tools_path="${SEREIN_BUILD_TOOLS:-$sdk_path/build-tools/36.0.0}"
./gradlew --no-daemon assembleRelease
"$tools_path/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
"$tools_path/zipalign" -c -P 16 -v 4 app/build/outputs/apk/release/app-release.apk
mkdir -p build/release
cp app/build/outputs/apk/release/app-release.apk build/release/Serein.apk
python3 - <<'PY'
from pathlib import Path
import hashlib
apk = Path('build/release/Serein.apk')
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
apk.with_name('SHA256SUMS').write_text(f'{digest}  Serein.apk\n')
PY
echo "Signed release: $PWD/build/release/Serein.apk"
