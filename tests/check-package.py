from pathlib import Path
import json, subprocess, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
main=ROOT/'app/src/main'
ns='{http://schemas.android.com/apk/res/android}'
m=ET.parse(main/'AndroidManifest.xml').getroot()
permissions={e.get(ns+'name') for e in m.findall('uses-permission')}
assert 'android.permission.INTERNET' not in permissions
assert 'android.permission.WAKE_LOCK' in permissions
assert 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK' in permissions
service=m.find('application/service')
assert service.get(ns+'foregroundServiceType')=='mediaPlayback'
assert service.get(ns+'stopWithTask')=='false'
assert len(m.findall('application/receiver'))==2
for p in (main/'res').rglob('*.xml'):ET.parse(p)
assert len(list((main/'assets/audio').glob('*.ogg')))==12
for p in (main/'assets/audio').glob('*.ogg'):
    result=json.loads(subprocess.check_output(['ffprobe','-v','quiet','-show_format','-show_streams','-of','json',str(p)]))
    assert 47<float(result['format']['duration'])<49
    assert result['streams'][0]['channels']==2
source='\n'.join(p.read_text() for p in (main/'java').rglob('*.java'))
assert 'android.webkit' not in source and 'WebView' not in source
assert 'prepareAsync()' in source
assert 'PowerManager.PARTIAL_WAKE_LOCK' in source
assert 'new MediaSession' in source and 'Notification.MediaStyle' in source
assert 'setChronometerCountDown' in source
assert 'state.remainingNow()' in (main/'java/com/serein/app/MainActivity.java').read_text()
print('Manifest, 2 widgets, native playback, visible countdown, and 12 stereo assets validated.')
