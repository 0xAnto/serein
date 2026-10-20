#!/usr/bin/env bash
# Run once from a normal terminal to publish the source and first signed APK.
set -euo pipefail
cd "$(dirname "$0")"
for tool in git gh python3; do
    command -v "$tool" >/dev/null || { echo "Install $tool first." >&2; exit 1; }
done
if [[ ! -f release-signing/serein-release.p12 || ! -f release-signing/credentials.json ]]; then
    echo 'Missing private signing material. See RELEASING.md.' >&2
    exit 1
fi
if ! gh auth status --hostname github.com >/dev/null 2>&1; then
    gh auth login --hostname github.com --git-protocol https --web --scopes workflow
fi
gh auth setup-git --hostname github.com
if [[ ! -e .git ]]; then
    git init -b main
fi
if [[ "$(git rev-parse --show-toplevel)" != "$PWD" ]]; then
    echo 'Git root does not match the Serein directory.' >&2
    exit 1
fi
if [[ "$(git symbolic-ref --short HEAD)" != main ]]; then
    echo 'Switch to the main branch before publishing.' >&2
    exit 1
fi
if git remote get-url origin >/dev/null 2>&1; then
    case "$(git remote get-url origin)" in
        https://github.com/0xAnto/serein|https://github.com/0xAnto/serein.git|git@github.com:0xAnto/serein.git) ;;
        *) echo 'Origin points to another repository; check it before publishing.' >&2; exit 1 ;;
    esac
else
    git remote add origin https://github.com/0xAnto/serein.git
fi
git check-ignore -q release-signing/serein-release.p12
git check-ignore -q release-signing/credentials.json
git add -- .gitignore .github README.md LICENSE RELEASING.md DEVICE-CHECK.md \
    app build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat \
    build-apk.sh build-release.sh publish-github.sh scripts tests docs
if git diff --cached --name-only | python3 -c 'import sys; sys.exit(any(p.startswith("release-signing/") or p.rstrip().endswith((".p12", ".jks", ".keystore", ".pem")) for p in sys.stdin))'; then
    :
else
    echo 'Signing material is staged. Remove it from the Git index before publishing.' >&2
    exit 1
fi
if ! git diff --cached --quiet; then
    git commit -m 'Open source Serein with signed APK release automation'
fi
git push --set-upstream origin main
python3 scripts/configure-github-release.py
gh workflow run release.yml --repo 0xAnto/serein --ref main
commit_sha="$(git rev-parse HEAD)"
run_id=''
for attempt in {1..12}; do
    run_id="$(gh run list --repo 0xAnto/serein --workflow release.yml \
        --event workflow_dispatch --branch main --commit "$commit_sha" \
        --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
    [[ -z "$run_id" ]] || break
    sleep 5
done
if [[ -z "$run_id" ]]; then
    echo 'Release started. Check https://github.com/0xAnto/serein/actions/workflows/release.yml'
    exit 0
fi
gh run watch "$run_id" --repo 0xAnto/serein --exit-status
echo 'Published: https://github.com/0xAnto/serein/releases/latest'
echo 'Download: https://github.com/0xAnto/serein/releases/latest/download/Serein.apk'
