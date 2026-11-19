#!/usr/bin/env python3
"""Validate the immutable TypeScript-generated Panchanga reference corpus."""

from __future__ import annotations

import json
import sys
from pathlib import Path

EXPECTED_COUNT = 41
EXPECTED_SOURCE_REPO = "creativeamantech/mahavtaarpanchanga"
EXPECTED_SOURCE_COMMIT = "e65f81caa7fedad0bcbfaf59335d70d8729d39a4"
EXPECTED_FIXTURE_COMMIT = "088035ddea6c9757f09e77fb4f30bd2657ef2d32"


def fail(message: str) -> None:
    raise SystemExit(f"REFERENCE CORPUS INVALID: {message}")


def main() -> None:
    corpus_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "reference-source/reference/panchanga/v1")
    manifest_path = corpus_dir / "manifest.json"
    if not manifest_path.is_file():
        fail(f"missing {manifest_path}")

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    source = manifest.get("source", {})
    if manifest.get("schemaVersion") != 1:
        fail("unsupported manifest schemaVersion")
    if source.get("repository") != EXPECTED_SOURCE_REPO:
        fail("unexpected source repository")
    if source.get("commit") != EXPECTED_SOURCE_COMMIT:
        fail("unexpected TypeScript source commit")
    if manifest.get("fixtureCount") != EXPECTED_COUNT:
        fail(f"expected {EXPECTED_COUNT} fixtures, found {manifest.get('fixtureCount')}")

    entries = manifest.get("fixtures", [])
    if len(entries) != EXPECTED_COUNT:
        fail(f"manifest lists {len(entries)} fixtures, expected {EXPECTED_COUNT}")

    listed_files: set[str] = set()
    for entry in entries:
        filename = entry.get("file")
        if not isinstance(filename, str) or Path(filename).name != filename:
            fail(f"unsafe or missing fixture filename: {filename!r}")
        if filename in listed_files:
            fail(f"duplicate fixture filename: {filename}")
        listed_files.add(filename)

        path = corpus_dir / filename
        if not path.is_file():
            fail(f"missing fixture: {filename}")
        fixture = json.loads(path.read_text(encoding="utf-8"))
        fixture_source = fixture.get("source", {})
        if fixture.get("schemaVersion") != 1:
            fail(f"{filename}: unsupported schemaVersion")
        if fixture_source.get("repository") != EXPECTED_SOURCE_REPO:
            fail(f"{filename}: unexpected source repository")
        if fixture_source.get("commit") != EXPECTED_SOURCE_COMMIT:
            fail(f"{filename}: unexpected source commit")
        if fixture.get("scenario", {}).get("id") != entry.get("id"):
            fail(f"{filename}: scenario id does not match manifest")
        if not isinstance(fixture.get("expected"), dict):
            fail(f"{filename}: expected output object missing")

    actual_files = {p.name for p in corpus_dir.glob("*.json") if p.name != "manifest.json"}
    if actual_files != listed_files:
        fail(f"fixture file set differs from manifest (extra={sorted(actual_files-listed_files)}, missing={sorted(listed_files-actual_files)})")

    print(
        f"Validated {len(listed_files)} immutable reference fixtures "
        f"from {EXPECTED_SOURCE_REPO}@{EXPECTED_FIXTURE_COMMIT} "
        f"(engine source {EXPECTED_SOURCE_COMMIT})."
    )


if __name__ == "__main__":
    main()
