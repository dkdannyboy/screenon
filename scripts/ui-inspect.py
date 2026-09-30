#!/usr/bin/env python3
"""Small read-only UI hierarchy helper for manual emulator QA."""
import os
import subprocess
import xml.etree.ElementTree as ET
adb = os.environ.get('ADB', 'adb')
subprocess.run([adb, 'shell', 'uiautomator', 'dump', '/sdcard/screenon-qa.xml'], check=True, stdout=subprocess.DEVNULL)
data = subprocess.check_output([adb, 'shell', 'cat', '/sdcard/screenon-qa.xml'])
for n in ET.fromstring(data).iter('node'):
    if n.get('text') or n.get('content-desc'):
        print(n.get('text'), '|', n.get('content-desc'), '|', n.get('resource-id'), '|', n.get('bounds'), '| click=', n.get('clickable'))
