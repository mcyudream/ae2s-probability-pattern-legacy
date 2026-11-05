#!/usr/bin/env python3
"""Collect verified install, development and source jars for a GitHub Release."""
import argparse
import json
import shutil

from verify_package import ROOT, read_json, require, sha256, verify_package


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", help="A release tag requires the exact runtime-accepted jar")
    parser.add_argument("--require-runtime", action="store_true")
    args = parser.parse_args()
    jar, metadata, tests = verify_package(
        args.tag, require_verified_hash=bool(args.tag), require_runtime=args.require_runtime)
    notes = ROOT / "docs/releases" / (metadata["version"] + ".md")
    require(notes.is_file(), "Release notes are missing: " + str(notes))
    sources = jar.with_name(jar.stem + "-sources.jar")
    development = jar.with_name(jar.stem + "-mcp.jar")
    for asset in (sources, development):
        require(asset.is_file(), "Missing release asset: " + str(asset))

    destination = ROOT / "build/release"
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True)
    assets = []
    for asset in (jar, sources, development):
        target = destination / asset.name
        shutil.copyfile(asset, target)
        assets.append(target)
    summary = read_json(ROOT / "verification/summary.json")
    summary["modid"] = metadata["modid"]
    summary["version"] = metadata["version"]
    summary["unit_tests"] = tests
    summary["jar_audit"] = "PASS: no harness, Minecraft, AE2S, Ponder or HEI classes bundled"
    receipt = destination / "VERIFICATION.json"
    receipt.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    assets.append(receipt)
    checksums = "".join(sha256(asset) + "  " + asset.name + "\n" for asset in assets)
    (destination / "SHA256SUMS").write_text(checksums, encoding="utf-8")
    shutil.copyfile(notes, destination / "RELEASE-NOTES.md")
    print(checksums, end="")


if __name__ == "__main__":
    main()
