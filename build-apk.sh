#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk_path="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk_path" ]]; then echo 'Set ANDROID_SDK_ROOT to an Android SDK with platform 36 and build-tools.' >&2; exit 1; fi
tools_path="${SEREIN_BUILD_TOOLS:-$sdk_path/build-tools/36.0.0}"
platform_jar="$sdk_path/platforms/android-36/android.jar"
mkdir -p build/gen build/classes build/dex test-signing
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/AndroidManifest.xml').read_text()
s=s.replace('<manifest ', '<manifest package="com.serein.app" ',1)
Path('build/AndroidManifest.xml').write_text(s)
PY
"$tools_path/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$tools_path/aapt2" link -o build/base.apk -I "$platform_jar" --manifest build/AndroidManifest.xml --java build/gen --min-sdk-version 31 --target-sdk-version 36 --version-code 2 --version-name 1.1.0 -A app/src/main/assets -0 ogg build/resources.zip
python3 - <<'PY'
from pathlib import Path
Path('build/sources.txt').write_text('\n'.join(str(p) for root in ['app/src/main/java','build/gen'] for p in Path(root).rglob('*.java')))
PY
java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -classpath "$platform_jar" -d build/classes @build/sources.txt
python3 - <<'PY'
import pathlib,zipfile
with zipfile.ZipFile('build/classes.jar','w') as z:
    for p in pathlib.Path('build/classes').rglob('*.class'): z.write(p,p.relative_to('build/classes'))
PY
"$tools_path/d8" --release --min-api 31 --lib "$platform_jar" --output build/dex build/classes.jar
cp build/base.apk build/dex.apk
python3 - <<'PY'
import pathlib,zipfile
with zipfile.ZipFile('build/dex.apk','a',compression=zipfile.ZIP_DEFLATED) as z:
    for p in pathlib.Path('build/dex').glob('*.dex'):z.write(p,p.name)
PY
"$tools_path/zipalign" -f -p 4 build/dex.apk build/aligned.apk
if [[ ! -f test-signing/serein-test.jks ]]; then
    keytool -genkeypair -keystore test-signing/serein-test.jks -storepass android -keypass android -alias serein-test -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Serein Local Test' >/dev/null 2>&1
fi
"$tools_path/apksigner" sign --ks test-signing/serein-test.jks --ks-pass pass:android --key-pass pass:android --ks-key-alias serein-test --out build/Serein-Android.apk build/aligned.apk
"$tools_path/apksigner" verify --verbose build/Serein-Android.apk
echo "APK: $PWD/build/Serein-Android.apk"
