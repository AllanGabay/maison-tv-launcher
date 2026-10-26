"""Fetch portable Android/Java build tools from their official publishers."""
import concurrent.futures as cf
import hashlib
import json
from pathlib import Path
import urllib.request as req
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent / 'tools' / 'build-env'
ROOT.mkdir(parents=True, exist_ok=True)

def get(url):
    with req.urlopen(req.Request(url, headers={'User-Agent': 'MaisonTV-Setup/1.0'}), timeout=90) as r:
        return r.read()

def fetch(item):
    name, url, algo, expected = item
    archive = ROOT / (name + '.zip')
    if not archive.exists() or hashlib.new(algo, archive.read_bytes()).hexdigest() != expected:
        print('Downloading ' + name, flush=True)
        data = get(url)
        if hashlib.new(algo, data).hexdigest() != expected:
            raise RuntimeError('Checksum mismatch: ' + name)
        archive.write_bytes(data)
    dest = ROOT / name
    with zipfile.ZipFile(archive) as z:
        z.extractall(dest)
    print('Verified and extracted ' + name, flush=True)
    return {'name': name, 'url': url, algo: expected, 'destination': str(dest)}

if __name__ == '__main__':
    repository = ET.fromstring(get('https://dl.google.com/android/repository/repository2-1.xml'))
    items = []
    for wanted, label in [('platforms;android-30', 'platform'), ('build-tools;35.0.0', 'build-tools')]:
        package = next(p for p in repository if p.tag.endswith('remotePackage') and p.get('path') == wanted)
        for archive in package.findall('./archives/archive'):
            os = archive.findtext('host-os')
            if os not in (None, 'windows'):
                continue
            complete = archive.find('complete')
            checksum = complete.find('checksum')
            items.append((label, 'https://dl.google.com/android/repository/' + complete.findtext('url'),
                          checksum.get('type', 'sha1'), checksum.text))
            break
    releases = json.loads(get('https://api.github.com/repos/adoptium/temurin21-binaries/releases?per_page=5'))
    release, java = next((r, a) for r in releases if not r['prerelease'] for a in r['assets'] if a['name'].startswith('OpenJDK21U-jdk_x64_windows_hotspot_') and a['name'].endswith('.zip'))
    checksum_asset = next(a for a in release['assets'] if a['name'] == java['name'] + '.sha256.txt')
    checksum = get(checksum_asset['browser_download_url']).decode().split()[0]
    items.append(('jdk', java['browser_download_url'], 'sha256', checksum))
    with cf.ThreadPoolExecutor(max_workers=3) as ex:
        manifest = list(ex.map(fetch, items))
    (ROOT / 'sources.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
