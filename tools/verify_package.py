#!/usr/bin/env python3
"""Verify packaged identity, isolation, tests and official CR 0.4.0 acceptance."""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
VARIANTS = {
    "server-without-ponder": ("server", False),
    "client-without-ponder": ("client", False),
    "server-with-ponder": ("server", True),
    "client-with-ponder": ("client", True),
}


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def verify_package(tag=None, require_verified_hash=False, require_runtime=False):
    identity = read_json(ROOT / "legacy-identity.json")
    jars = [
        jar for jar in (ROOT / "build/libs").glob(identity["modid"] + "-*.jar")
        if not jar.stem.endswith(("-mcp", "-sources", "-javadoc"))
    ]
    require(len(jars) == 1, "Expected exactly one production jar")
    jar = jars[0]
    with ZipFile(jar) as archive:
        names = archive.namelist()
        metadata = json.loads(archive.read("mcmod.info"))[0]
        require(metadata["modid"] == identity["modid"], "Packaged Mod ID mismatch")
        require(metadata["name"] == identity["name"], "Packaged display name mismatch")
        require(metadata["mcversion"] == "1.12.2", "Wrong Minecraft version")
        require(metadata["authorList"] == ["yudream", "TaoLe-si (original)"],
                "Missing maintainer or upstream attribution")
        require(metadata["logoFile"] in names, "Missing packaged logo")
        require(json.loads(archive.read("pack.mcmeta"))["pack"]["pack_format"] == 3,
                "Minecraft 1.12.2 lowercase language files need pack format 3")
        main_class = archive.read("com/tz/statpatterns/StatPatternsMod.class")
        for value in (identity["modid"], identity["name"], "required-after:cleanroom@[0.4.0,)",
                      "required-after:ae2@[1.1.0,1.2.0)", "after:ponder-legacy"):
            require(value.encode() in main_class, "Missing loader annotation value: " + value)
        require(b"required-after:ponder-legacy" not in main_class, "Ponder must be optional")
        for name in ("LICENSE", "NOTICE.md", "upstream-lock.json", "dependency-lock.json"):
            require(archive.read(name) == (ROOT / name).read_bytes(), "Packaged provenance mismatch: " + name)
        forbidden = ("net/minecraft/", "net/minecraftforge/", "ae2/", "net/createmod/",
                     "crafttweaker/", "mezz/jei/", "com/tz/statpatterns/verification/")
        require(not any(name.endswith(".class") and name.startswith(forbidden) for name in names),
                "A dependency or verification harness was bundled in the production jar")
    expected_tag = "v" + metadata["version"]
    require(identity["release_tag"] == expected_tag, "Identity release tag differs from jar version")
    require(jar.name == f"{identity['modid']}-{metadata['version']}.jar", "Unexpected jar filename")
    if tag:
        require(tag == expected_tag, "Tag must match packaged version: " + expected_tag)

    reports = sorted((ROOT / "build/test-results/test").glob("TEST-*.xml"))
    require(bool(reports), "JUnit reports are missing; run build before packaging")
    tests = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
    for report in reports:
        suite = ET.parse(report).getroot()
        for key in tests:
            tests[key] += int(suite.get(key, "0"))
    require(tests["tests"] >= 16 and tests["failures"] == tests["errors"] == tests["skipped"] == 0,
            "Unit tests did not all pass: " + repr(tests))

    actual = sha256(jar)
    if require_verified_hash:
        require(identity["cleanroom_baseline"] == "0.4.0"
                and identity["status"] == "verified-cleanroom-0.4.0",
                "Official Cleanroom 0.4.0 acceptance is incomplete")
        require(actual == identity["verified_jar_sha256"],
                "Runtime-accepted SHA-256 differs from built jar: " + actual)

    if require_runtime or require_verified_hash:
        summary = read_json(ROOT / "verification/summary.json")
        lock = read_json(ROOT / "dependency-lock.json")
        require(summary["cleanroom"] == "0.4.0" and summary["artifact_sha256"] == actual,
                "Runtime summary does not describe this jar on official Cleanroom 0.4.0")
        require(set(summary["scenarios"]) == set(VARIANTS), "Incomplete runtime scenario set")
        for variant, (side, ponder) in VARIANTS.items():
            evidence = ROOT / "verification/cleanroom-0.4.0" / variant
            result = read_json(evidence / "result.json")
            runtime = read_json(evidence / "runtime.json")
            require(summary["scenarios"][variant] == result["result"] == "FUNCTIONAL_PASS",
                    "Runtime scenario failed: " + variant)
            require(result["cleanroom"] == runtime["cleanroom"] == "0.4.0"
                    and result["side"] == runtime["side"] == side
                    and runtime["userdev"] is False
                    and result["exit"] == 0 and result["timed_out"] is False
                    and result["loader_version_seen"] is True,
                    "Invalid official runtime receipt: " + variant)
            gameplay = read_json(evidence / "gameplay/report.json")
            require(gameplay["result"] == result["gameplay_report"]["result"] == "PASS",
                    "Missing gameplay acceptance: " + variant)
            installed = runtime["jars"]
            require(installed.get(jar.name) == actual, "Runtime tested a different jar: " + variant)
            ae2s = lock["ae2s"]["runtime"]
            require(installed.get(ae2s["file"]) == ae2s["sha256"], "Wrong AE2S jar: " + variant)
            for optional in ("ponder", "crafttweaker"):
                dependency = lock[optional]
                if ponder:
                    require(installed.get(dependency["file"]) == dependency["sha256"],
                            "Wrong optional dependency: " + variant + "/" + optional)
                else:
                    require(dependency["file"] not in installed,
                            "Optional dependency present in absent-Ponder scenario: " + variant)
            if side == "client":
                require(installed.get(lock["hei"]["file"]) == lock["hei"]["sha256"],
                        "HEI client acceptance is missing: " + variant)
    print(f"PASS: {jar.name}, Mod ID {metadata['modid']}, SHA-256 {actual}")
    return jar, metadata, tests


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag")
    parser.add_argument("--require-verified-hash", action="store_true")
    parser.add_argument("--require-runtime", action="store_true")
    args = parser.parse_args()
    verify_package(args.tag, args.require_verified_hash, args.require_runtime)


if __name__ == "__main__":
    main()
