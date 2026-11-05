#!/usr/bin/env python3
"""Run packaged client/server acceptance with and without optional Ponder on official CR 0.4.0."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent.parent
GRADLE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
LOCK = json.loads((ROOT / "dependency-lock.json").read_text())
IDENTITY = json.loads((ROOT / "legacy-identity.json").read_text())


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def optional(name):
    info = LOCK[name]
    target = ROOT / "build/dependencies/verification" / info["file"]
    candidates = [target]
    if name == "ponder":
        candidates.append(ROOT / "build/dependencies/ponder" / info["file"])
    if "group" in info:
        candidates += list((GRADLE / "caches/modules-2/files-2.1" / info["group"] / info["module"] / info["version"]).glob("*/*"))
    for candidate in candidates:
        if candidate.is_file() and candidate.suffix == ".jar" and digest(candidate) == info["sha256"]:
            return candidate
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(".part")
    with urllib.request.urlopen(info["url"], timeout=90) as source, temporary.open("wb") as output:
        shutil.copyfileobj(source, output)
    if digest(temporary) != info["sha256"]:
        raise RuntimeError("Dependency checksum mismatch: " + name)
    temporary.replace(target)
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--with-hei", action="store_true")
    parser.add_argument("--side", choices=["client", "server", "both"], default="both")
    parser.add_argument("--port", type=int, default=25871)
    args = parser.parse_args()
    if args.side != "server" and not os.environ.get("DISPLAY"):
        parser.error("Client acceptance needs DISPLAY, for example an Xvfb display")
    jdk = Path(os.environ.get("PROBABILITY_JAVA_HOME", WORKSPACE / ".dev/jdk-25.0.5+8"))
    environment = os.environ.copy()
    if (jdk / "bin/java").is_file():
        environment["JAVA_HOME"] = str(jdk)
    environment["ALSOFT_DRIVERS"] = "null"
    version = IDENTITY["release_tag"].removeprefix("v")
    artifact = ROOT / "build/libs" / f"{IDENTITY['modid']}-{version}.jar"
    harness = ROOT / "build/verification/statpatterns-verification.jar"
    ae2s = ROOT / "build/dependencies/ae2-supergiant" / LOCK["ae2s"]["runtime"]["file"]
    for file in (artifact, harness, ae2s):
        if not file.is_file():
            parser.error("Build first with ./tools/dev.sh build reobfRuntimeHarnessJar: " + str(file))
    if digest(ae2s) != LOCK["ae2s"]["runtime"]["sha256"]:
        raise RuntimeError("AE2S installation jar differs from the pinned version")
    summary = {"cleanroom": "0.4.0", "artifact_sha256": digest(artifact), "scenarios": {}}
    sides = ("server", "client") if args.side == "both" else (args.side,)
    for ponder in (False, True):
        for side in sides:
            variant = f"{side}-{'with' if ponder else 'without'}-ponder"
            jars = [artifact, ae2s, harness]
            if ponder:
                jars += [optional("ponder"), optional("crafttweaker")]
            if args.with_hei and side == "client":
                jars.append(optional("hei"))
            command = [sys.executable, str(ROOT / "tools/verify-cleanroom.py"), side,
                       "--name", variant, "--port", str(args.port), "--timeout", "360",
                       "--report", "statpatterns-verification/report.json"]
            for jar in jars:
                command += ["--mod", str(jar)]
            subprocess.run(command, cwd=ROOT, env=environment, check=True)
            run = ROOT / "run/cleanroom-0.4.0" / variant / side
            destination = ROOT / "verification/cleanroom-0.4.0" / variant
            destination.mkdir(parents=True, exist_ok=True)
            for name in ("result.json", "runtime.json", "console.log"):
                shutil.copy2(run / name, destination / name)
            shutil.copytree(run / "statpatterns-verification", destination / "gameplay", dirs_exist_ok=True)
            summary["scenarios"][variant] = json.loads((run / "result.json").read_text())["result"]
    (ROOT / "verification/summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
