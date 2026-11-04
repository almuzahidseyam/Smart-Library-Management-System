"""Pinned JDBC download, portable JDK compilation, and real SQLite checks."""
from pathlib import Path
import hashlib
import os
import subprocess
import sys
import urllib.request

ROOT=Path(__file__).resolve().parent
JAR=ROOT/'.deps'/'sqlite-jdbc-3.54.1.0.jar'
SHA='f8dce420a6016278497f4f0560f65602b9db858de141b541cf91a4644ebe4639'
JAR.parent.mkdir(exist_ok=True)
if not JAR.exists():
    with urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/'+JAR.name,timeout=60) as response:
        payload=response.read(20*1024*1024+1)
    if hashlib.sha256(payload).hexdigest()!=SHA: raise SystemExit('JDBC checksum mismatch')
    JAR.write_bytes(payload)
if hashlib.sha256(JAR.read_bytes()).hexdigest()!=SHA: raise SystemExit('JDBC checksum mismatch')
classes=ROOT/'build'/'classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',str(classes),*[str(p) for folder in ['src','tests'] for p in (ROOT/folder).glob('*.java')]],check=True)
classpath=os.pathsep.join([str(classes),str(JAR)])
subprocess.run(['java','-cp',classpath,'Login' if '--run' in sys.argv else 'LoginFlowTest' if '--ui' in sys.argv else 'AccountsTest'],check=True,timeout=None if '--run' in sys.argv else 45)
