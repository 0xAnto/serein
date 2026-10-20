#!/usr/bin/env python3
"""Set repository metadata and upload local signing material as GitHub secrets."""
from pathlib import Path
import base64
import json
import subprocess

root = Path(__file__).resolve().parents[1]
directory = root / 'release-signing'
credentials = json.loads((directory / 'credentials.json').read_text())
repo = '0xAnto/serein'
subprocess.run(['gh', 'api', 'user', '--jq', '.login'], check=True)
subprocess.run([
    'gh', 'api', '--method', 'PATCH', f'repos/{repo}',
    '-f', 'description=An offline Android ambient sound mixer for sleep, focus, and unwinding. No ads, accounts, or analytics.',
], check=True, stdout=subprocess.DEVNULL)
values = {
    'SEREIN_KEYSTORE_BASE64': base64.b64encode((directory / 'serein-release.p12').read_bytes()).decode(),
    'SEREIN_KEYSTORE_PASSWORD': credentials['store_password'],
    'SEREIN_KEY_ALIAS': credentials['key_alias'],
    'SEREIN_KEY_PASSWORD': credentials['key_password'],
}
for name, value in values.items():
    subprocess.run(['gh', 'secret', 'set', name, '--repo', repo], input=value,
                   text=True, check=True)
    print(f'Configured {name}')
print(f'Ready: https://github.com/{repo}/actions/workflows/release.yml')
