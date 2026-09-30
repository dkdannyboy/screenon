#!/usr/bin/env python3
"""One-time local release key generation. Never commits or prints credentials."""
import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parent.parent
folder = root / '.signing'
folder.mkdir(mode=0o700, exist_ok=True)
key = folder / 'screenon-release.jks'
config = folder / 'release.properties'
if key.exists() or config.exists():
    raise SystemExit('Signing material already exists; preserving it without changes.')
password = secrets.token_urlsafe(36)
env = os.environ.copy()
env['SCREENON_KEY_PASSWORD'] = password
subprocess.run([
    'keytool', '-genkeypair', '-keystore', str(key), '-storetype', 'JKS',
    '-storepass:env', 'SCREENON_KEY_PASSWORD', '-keypass:env', 'SCREENON_KEY_PASSWORD',
    '-alias', 'screenon', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
    '-dname', 'CN=ScreenOn, OU=Android, O=ScreenOn', '-noprompt',
], check=True, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
key.chmod(0o600)
config.write_text(f'storeFile=.signing/screenon-release.jks\nstorePassword={password}\nkeyAlias=screenon\nkeyPassword={password}\n')
config.chmod(0o600)
print('Created local release signing material in ignored .signing/. Back it up securely for future updates.')
