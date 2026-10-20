#!/usr/bin/env python3
"""Create a private PKCS12 Android signing key; requires Python cryptography."""
from datetime import datetime, timedelta, timezone
from pathlib import Path
import json
import os
import secrets

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

root = Path(__file__).resolve().parents[1]
directory = root / 'release-signing'
directory.mkdir(mode=0o700, exist_ok=True)
os.chmod(directory, 0o700)
key_path = directory / 'serein-release.p12'
password_path = directory / 'credentials.json'
if key_path.exists() or password_path.exists():
    raise SystemExit('Release signing material already exists; refusing to replace it.')

key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Serein Release')])
now = datetime.now(timezone.utc)
certificate = (x509.CertificateBuilder().subject_name(name).issuer_name(name)
               .public_key(key.public_key()).serial_number(x509.random_serial_number())
               .not_valid_before(now - timedelta(days=1))
               .not_valid_after(now + timedelta(days=365 * 30))
               .sign(key, hashes.SHA256()))
password = secrets.token_urlsafe(48)
data = pkcs12.serialize_key_and_certificates(
    b'serein-release', key, certificate, None,
    serialization.BestAvailableEncryption(password.encode()))
for path, content in [
    (key_path, data),
    (password_path, (json.dumps({'store_password': password, 'key_password': password,
                               'key_alias': 'serein-release'}, indent=2) + '\n').encode()),
]:
    with path.open('xb') as output:
        os.chmod(path, 0o600)
        output.write(content)
fingerprint = certificate.fingerprint(hashes.SHA256()).hex()
(directory / 'certificate-sha256.txt').write_text(fingerprint + '\n')
print(f'Created private release key in {directory}')
print(f'Certificate SHA-256: {fingerprint}')
print('Back up this directory securely. It is excluded from Git.')
