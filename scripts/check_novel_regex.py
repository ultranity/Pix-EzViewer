#!/usr/bin/env python3
"""Compile the novel reader's production regexes with native ICU.

Desktop JVM tests use java.util.regex, while Android's Pattern implementation
uses ICU.  A pattern can therefore pass the JVM tests and still fail when its
class is initialized on a device.  This check extracts the raw Kotlin regex
literals from the two production novel parser files and asks the native ICU
regex API to compile each one.
"""

from __future__ import annotations

import ctypes
import ctypes.util
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PRODUCTION_FILES = (
    ROOT / "app/src/main/java/com/perol/asdpl/pixivez/ui/novel/NovelMarkup.kt",
    ROOT / "app/src/main/java/com/perol/asdpl/pixivez/ui/novel/NovelViewModel.kt",
)


def load_icu() -> tuple[ctypes.CDLL, str]:
    """Load ICU's common library and return it with its uregex suffix."""

    candidates: list[str] = []
    for logical_name in ("icucore", "icui18n", "icuuc"):
        resolved = ctypes.util.find_library(logical_name)
        if resolved:
            candidates.append(resolved)
    candidates.extend(
        (
            "/usr/lib/libicucore.A.dylib",
            "libicui18n.so",
            "libicui18n.so.76",
            "libicui18n.so.75",
            "libicui18n.so.74",
            "libicui18n.so.73",
            "libicui18n.so.72",
            "libicuuc.so",
            "libicuuc.so.76",
            "libicuuc.so.75",
            "libicuuc.so.74",
            "libicuuc.so.73",
            "libicuuc.so.72",
        )
    )

    loaded: list[ctypes.CDLL] = []
    for candidate in dict.fromkeys(candidates):
        try:
            loaded.append(ctypes.CDLL(candidate))
        except OSError:
            continue
    if not loaded:
        raise RuntimeError("native ICU library not found (tried: %s)" % ", ".join(candidates))

    # Linux ICU exports versioned symbols (uregex_open_72), whereas Apple's
    # libicucore exports the unsuffixed API.  Keep the suffix for uregex_close.
    for library in loaded:
        for suffix in ("",) + tuple(f"_{version}" for version in range(30, 100)):
            try:
                getattr(library, f"uregex_open{suffix}")
                getattr(library, f"uregex_close{suffix}")
                return library, suffix
            except AttributeError:
                continue
    raise RuntimeError("ICU library has no uregex_open/uregex_close symbols")


def compile_with_icu(library: ctypes.CDLL, suffix: str, pattern: str) -> int:
    """Compile one UTF-16 pattern and return ICU's UErrorCode."""

    open_regex = getattr(library, f"uregex_open{suffix}")
    close_regex = getattr(library, f"uregex_close{suffix}")
    open_regex.argtypes = [
        ctypes.POINTER(ctypes.c_uint16),
        ctypes.c_int32,
        ctypes.c_uint32,
        ctypes.c_void_p,
        ctypes.POINTER(ctypes.c_int32),
    ]
    open_regex.restype = ctypes.c_void_p
    close_regex.argtypes = [ctypes.c_void_p]
    close_regex.restype = None

    encoded = pattern.encode("utf-16-le")
    units = (ctypes.c_uint16 * (len(encoded) // 2)).from_buffer_copy(encoded)
    status = ctypes.c_int32(0)  # U_ZERO_ERROR
    expression = open_regex(units, len(units), 0, None, ctypes.byref(status))
    if expression:
        close_regex(expression)
    return status.value


def production_patterns() -> list[tuple[Path, int, str]]:
    """Extract every Kotlin raw Regex literal in the novel code."""

    pattern = re.compile(r'Regex\(\s*"""(.*?)"""', re.DOTALL)
    result: list[tuple[Path, int, str]] = []
    for source in PRODUCTION_FILES:
        text = source.read_text(encoding="utf-8")
        for match in pattern.finditer(text):
            line = text.count("\n", 0, match.start()) + 1
            result.append((source, line, match.group(1)))
    if not result:
        raise RuntimeError("no production novel Regex(\"\"\"...\"\"\") literals found")
    return result


def main() -> int:
    try:
        library, suffix = load_icu()
        patterns = production_patterns()
    except (OSError, RuntimeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2

    failures = 0
    for source, line, pattern in patterns:
        status = compile_with_icu(library, suffix, pattern)
        if status != 0:
            failures += 1
            print(f"FAIL {source.relative_to(ROOT)}:{line}: ICU UErrorCode={status}: {pattern!r}")

    if failures:
        print(f"{failures}/{len(patterns)} production novel regexes failed ICU compilation")
        return 1
    print(f"OK: {len(patterns)} production novel regexes compile with native ICU")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
