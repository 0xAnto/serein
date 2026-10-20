# Publishing Android releases

Public downloads live at <https://github.com/0xAnto/serein/releases>. The README's download button points to the latest release's `Serein.apk`.

## First publication from the prepared local folder

If the private key has already been generated locally, run this from a normal terminal:

```sh
./publish-github.sh
```

It authenticates the GitHub CLI if needed, initializes Git on `main`, pushes the source, sets the repository description, uploads signing secrets, and starts and monitors the release workflow. It does not force-push or replace a published version. If GitHub rejects pushing workflow files because your existing token lacks the workflow scope, run `gh auth refresh --hostname github.com --scopes workflow` and retry.

## One-time signing setup

Keep the same release key for all updates. Back up the entire ignored `release-signing/` directory securely, including its credentials. Losing the key prevents updating existing installations.

If there is no release key yet, generate one with Python 3 and the `cryptography` package:

```sh
python3 scripts/create-release-key.py
```

This creates a password-protected PKCS12 keystore with a 3072-bit RSA key and a certificate valid for 30 years. It refuses to overwrite an existing key. Never commit signing material or passwords.

With the GitHub CLI authenticated as the repository owner, configure the repository description and signing secrets:

```sh
gh auth login
python3 scripts/configure-github-release.py
```

The script uploads these encrypted GitHub Actions secrets without printing their values:

| Secret | Value |
| --- | --- |
| `SEREIN_KEYSTORE_BASE64` | Base64-encoded `serein-release.p12` |
| `SEREIN_KEYSTORE_PASSWORD` | Keystore password from local credentials |
| `SEREIN_KEY_ALIAS` | `serein-release` |
| `SEREIN_KEY_PASSWORD` | Key password from local credentials |

You can also add these under **Settings → Secrets and variables → Actions**. GitHub Actions must be enabled for the repository.

## Publish a version

1. Update `versionName` and increment `versionCode` in `app/build.gradle`. Also update the version arguments in `build-apk.sh` for direct development builds.
2. Add notes at `docs/releases/v<VERSION>.md` and complete [DEVICE-CHECK.md](DEVICE-CHECK.md).
3. Commit and push the changes, then push the matching tag:

```sh
git tag v1.1.0
git push origin v1.1.0
```

For the first release, you can instead open **Actions → Release APK → Run workflow** on `main`. It reads the Gradle version and creates the matching tag and release at that commit. Later releases can use either method.

The workflow tests the app, builds a signed release APK, verifies its signature and alignment, and calculates its SHA-256 checksum. It uploads `Serein.apk` and `SHA256SUMS` to a draft, then publishes it as latest. Rerunning a failed run resumes an existing draft. It refuses to replace an already published version.

Download page: <https://github.com/0xAnto/serein/releases/latest>

Direct APK: <https://github.com/0xAnto/serein/releases/latest/download/Serein.apk>

Verify both downloaded assets together with `shasum -a 256 -c SHA256SUMS` (macOS) or `sha256sum -c SHA256SUMS` (Linux).

## Build a signed release locally

Install JDK 17, SDK Platform 36, and Build Tools 36.0.0. Set `ANDROID_SDK_ROOT` and these environment variables using your private keystore and credentials:

```sh
export SEREIN_KEYSTORE_FILE="$PWD/release-signing/serein-release.p12"
export SEREIN_KEY_ALIAS=serein-release
# Set SEREIN_KEYSTORE_PASSWORD and SEREIN_KEY_PASSWORD from your private credentials.
./build-release.sh
```

Output: `build/release/Serein.apk` and `build/release/SHA256SUMS`.
