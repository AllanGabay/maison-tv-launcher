"""Build the native TV launcher using verified, workspace-local tools."""
import argparse
import os
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / 'launcher'
ENV = ROOT / 'tools' / 'build-env'
BUILD = SOURCE / 'build'
BUILD.mkdir(parents=True, exist_ok=True)

def find(name, scope=ENV):
    found = list(scope.rglob(name))
    if len(found) != 1:
        raise RuntimeError('Ambiguous/missing tool ' + name + ': ' + str(found))
    return found[0]

def run(*command):
    p = subprocess.run([str(x) for x in command], cwd=SOURCE, capture_output=True)
    if p.stdout:
        print(p.stdout.decode('utf-8', errors='replace'))
    if p.returncode:
        raise RuntimeError(p.stderr.decode('utf-8', errors='replace'))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tools-root', type=Path, default=ENV)
    parser.add_argument('--wallpaper', type=Path, default=ROOT / 'assets' / 'wallpaper.png')
    args = parser.parse_args()
    ENV = args.tools_root.resolve()
    # Capture ENV here because find()'s original default was set before parsing.
    _find = find
    def find(name):
        return _find(name, ENV)
    password = os.environ.get('MAISON_TV_DEBUG_PASSWORD', 'android')
    java = find('java.exe')
    javac = find('javac.exe')
    jar = find('jar.exe')
    android = find('android.jar')
    aapt = find('aapt2.exe')
    d8 = find('d8.jar')
    signer = find('apksigner.jar')
    aligner = find('zipalign.exe')
    classes = BUILD / 'classes'
    dex = BUILD / 'dex'
    assets = SOURCE / 'assets'
    for directory in (classes, dex, assets):
        directory.mkdir(exist_ok=True)
    shutil.copy2(args.wallpaper, assets / 'maison.png')
    shutil.copy2(args.wallpaper, SOURCE / 'res' / 'drawable' / 'banner.png')
    compiled = BUILD / 'resources.zip'
    run(aapt, 'compile', '--dir', SOURCE / 'res', '-o', compiled)
    unsigned = BUILD / 'unsigned.apk'
    run(aapt, 'link', '-o', unsigned, '-I', android, '--manifest', SOURCE / 'AndroidManifest.xml', '-A', assets, compiled)
    files = sorted((SOURCE / 'src').rglob('*.java'))
    run(javac, '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', android, '-d', classes, *files)
    classjar = BUILD / 'classes.jar'
    run(jar, 'cf', classjar, '-C', classes, '.')
    run(java, '-cp', d8, 'com.android.tools.r8.D8', '--lib', android, '--min-api', '23', '--output', dex, classjar)
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as apk:
        apk.write(dex / 'classes.dex', 'classes.dex')
    aligned = BUILD / 'aligned.apk'
    run(aligner, '-f', '-p', '4', unsigned, aligned)
    keys = ROOT / 'tools' / 'signing'
    keys.mkdir(parents=True, exist_ok=True)
    key = keys / 'local-debug.p12'
    if not key.exists():
        run(find('keytool.exe'), '-genkeypair', '-keystore', key, '-storetype', 'PKCS12', '-storepass', password, '-alias', 'maison', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=Maison TV Local Debug')
    output = BUILD / 'maison-tv.apk'
    run(java, '-jar', signer, 'sign', '--ks', key, '--ks-pass', 'pass:' + password, '--ks-key-alias', 'maison', '--out', output, aligned)
    run(java, '-jar', signer, 'verify', '--verbose', output)
    manifest = {'package': 'fr.maisontv.launcher', 'version': '1.0', 'apk': output.relative_to(ROOT).as_posix(), 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(), 'bytes': output.stat().st_size}
    (BUILD / 'artifact.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(json.dumps(manifest, indent=2))
