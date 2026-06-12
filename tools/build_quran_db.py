"""Build the bundled read-only Quran SQLite database from Tanzil source files.

Inputs (downloaded from https://tanzil.net into tools/quran_data/):
    - quran-uthmani.txt: the Uthmani text, one `sura|aya|text` line per ayah.
    - quran-data.xml: surah metadata and page/juz/hizb-quarter start markers.

Output:
    - app/src/main/assets/quran.db, opened read-only by Room via createFromAsset.

The schema (table, column, and index names) must stay in sync with the Room
entities in app/src/main/java/app/alkahf/data/quran/.
"""

from __future__ import annotations

import dataclasses
import pathlib
import sqlite3
import xml.etree.ElementTree as element_tree

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA_DIR = REPO_ROOT / "tools" / "quran_data"
TEXT_FILE = DATA_DIR / "quran-uthmani.txt"
METADATA_FILE = DATA_DIR / "quran-data.xml"
OUTPUT_FILE = REPO_ROOT / "app" / "src" / "main" / "assets" / "quran.db"

EXPECTED_SURAH_COUNT = 114
EXPECTED_AYAH_COUNT = 6236
EXPECTED_PAGE_COUNT = 604
EXPECTED_JUZ_COUNT = 30
EXPECTED_QUARTER_COUNT = 240


class Error(Exception):
    """Base error for this tool."""


class SourceDataError(Error):
    """Raised when the Tanzil source files do not match expectations."""


@dataclasses.dataclass
class SurahInfo:
    number: int
    name_arabic: str
    name_latin: str
    revelation_type: str
    ayah_count: int


@dataclasses.dataclass
class StartMarker:
    """Marks the (surah, ayah) at which a page, juz, or hizb quarter starts."""

    index: int
    surah: int
    ayah: int


def _read_ayah_texts(path: pathlib.Path) -> dict[tuple[int, int], str]:
    """Parse the Tanzil text file into a (surah, ayah) -> text mapping."""
    texts: dict[tuple[int, int], str] = {}
    with path.open(encoding="utf-8") as source:
        for line in source:
            stripped_line = line.strip()
            if len(stripped_line) == 0 or stripped_line.startswith("#") is True:
                continue
            surah_field, ayah_field, text = stripped_line.split("|", maxsplit=2)
            texts[(int(surah_field), int(ayah_field))] = text
    if len(texts) != EXPECTED_AYAH_COUNT:
        raise SourceDataError(f"Expected {EXPECTED_AYAH_COUNT} ayahs, found {len(texts)}")
    return texts


def _read_surahs(root: element_tree.Element) -> list[SurahInfo]:
    surahs = [
        SurahInfo(
            number=int(sura.get("index", "0")),
            name_arabic=sura.get("name", ""),
            name_latin=sura.get("tname", ""),
            revelation_type=sura.get("type", ""),
            ayah_count=int(sura.get("ayas", "0")),
        )
        for sura in root.iter("sura")
    ]
    if len(surahs) != EXPECTED_SURAH_COUNT:
        raise SourceDataError(f"Expected {EXPECTED_SURAH_COUNT} surahs, found {len(surahs)}")
    return surahs


def _read_markers(root: element_tree.Element, tag: str, expected_count: int) -> list[StartMarker]:
    markers = [
        StartMarker(
            index=int(marker.get("index", "0")),
            surah=int(marker.get("sura", "0")),
            ayah=int(marker.get("aya", "0")),
        )
        for marker in root.iter(tag)
    ]
    if len(markers) != expected_count:
        raise SourceDataError(f"Expected {expected_count} <{tag}> markers, found {len(markers)}")
    return markers


def _strip_basmala(surah: int, ayah: int, text: str, basmala: str) -> tuple[str, bool]:
    """Split the basmala prefix off a surah's first ayah.

    Tanzil prefixes the basmala to ayah 1 of every surah except Al-Faatiha
    (where the basmala IS ayah 1) and At-Tawba (which has none). The app
    renders the basmala as a separate centered header, so the stored ayah
    text must not contain it. The basmala string is taken from Al-Faatiha 1:1
    rather than hardcoded, because Tanzil's diacritic codepoint ordering is
    not the normalized form.

    Returns:
        The ayah text without the basmala, and whether a basmala header
        precedes this ayah on the page.
    """
    if ayah != 1 or surah in (1, 9):
        return text, ayah == 1 and surah not in (1, 9)
    words = text.split(" ")
    basmala_words = basmala.split(" ")
    # Suras 95 and 97 carry a basmala variant with a shadda on the ba, so the
    # first word is compared loosely (it must merely start with a ba).
    if words[1:4] != basmala_words[1:4] or words[0].startswith("ب") is False:
        raise SourceDataError(f"Surah {surah} ayah 1 does not start with the basmala")
    return " ".join(words[4:]), True


def _build_ayah_rows(
    texts: dict[tuple[int, int], str],
    surahs: list[SurahInfo],
    pages: list[StartMarker],
    juzs: list[StartMarker],
    quarters: list[StartMarker],
) -> list[tuple[int, int, int, str, int, int, int, int]]:
    """Assemble one row per ayah with its page, juz, and hizb-quarter numbers."""
    starts: dict[tuple[int, int], dict[str, int]] = {}
    for name, markers in (("page", pages), ("juz", juzs), ("quarter", quarters)):
        for marker in markers:
            starts.setdefault((marker.surah, marker.ayah), {})[name] = marker.index

    rows: list[tuple[int, int, int, str, int, int, int, int]] = []
    current = {"page": 1, "juz": 1, "quarter": 1}
    basmala = texts[(1, 1)]
    for surah in surahs:
        for ayah in range(1, surah.ayah_count + 1):
            current.update(starts.get((surah.number, ayah), {}))
            text, has_basmala = _strip_basmala(
                surah.number, ayah, texts[(surah.number, ayah)], basmala
            )
            rows.append(
                (
                    surah.number * 1000 + ayah,
                    surah.number,
                    ayah,
                    text,
                    1 if has_basmala is True else 0,
                    current["page"],
                    current["juz"],
                    current["quarter"],
                )
            )
    return rows


def _write_database(
    path: pathlib.Path,
    surahs: list[SurahInfo],
    ayah_rows: list[tuple[int, int, int, str, int, int, int, int]],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.unlink(missing_ok=True)
    with sqlite3.connect(path) as connection:
        connection.executescript(
            """
            CREATE TABLE surahs (
                number INTEGER PRIMARY KEY NOT NULL,
                name_arabic TEXT NOT NULL,
                name_latin TEXT NOT NULL,
                revelation_type TEXT NOT NULL,
                ayah_count INTEGER NOT NULL
            );
            CREATE TABLE ayahs (
                id INTEGER PRIMARY KEY NOT NULL,
                surah INTEGER NOT NULL,
                number INTEGER NOT NULL,
                text TEXT NOT NULL,
                has_basmala INTEGER NOT NULL,
                page INTEGER NOT NULL,
                juz INTEGER NOT NULL,
                hizb_quarter INTEGER NOT NULL
            );
            CREATE INDEX index_ayahs_page ON ayahs (page);
            CREATE INDEX index_ayahs_surah ON ayahs (surah);
            """
        )
        connection.executemany(
            "INSERT INTO surahs VALUES (?, ?, ?, ?, ?)",
            [
                (s.number, s.name_arabic, s.name_latin, s.revelation_type, s.ayah_count)
                for s in surahs
            ],
        )
        connection.executemany("INSERT INTO ayahs VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ayah_rows)


def _verify_database(path: pathlib.Path) -> None:
    """Sanity-check row counts and a known location (Al-Kahf starts on page 293)."""
    with sqlite3.connect(path) as connection:
        ayah_count = connection.execute("SELECT COUNT(*) FROM ayahs").fetchone()[0]
        page_count = connection.execute("SELECT COUNT(DISTINCT page) FROM ayahs").fetchone()[0]
        kahf_page = connection.execute("SELECT page FROM ayahs WHERE id = 18001").fetchone()[0]
    if ayah_count != EXPECTED_AYAH_COUNT:
        raise SourceDataError(f"Database has {ayah_count} ayahs, expected {EXPECTED_AYAH_COUNT}")
    if page_count != EXPECTED_PAGE_COUNT:
        raise SourceDataError(f"Database has {page_count} pages, expected {EXPECTED_PAGE_COUNT}")
    if kahf_page != 293:
        raise SourceDataError(f"Al-Kahf 18:1 is on page {kahf_page}, expected 293")


def main() -> None:
    """Build and verify the bundled Quran database."""
    texts = _read_ayah_texts(TEXT_FILE)
    root = element_tree.parse(METADATA_FILE).getroot()
    surahs = _read_surahs(root)
    pages = _read_markers(root, "page", EXPECTED_PAGE_COUNT)
    juzs = _read_markers(root, "juz", EXPECTED_JUZ_COUNT)
    quarters = _read_markers(root, "quarter", EXPECTED_QUARTER_COUNT)
    ayah_rows = _build_ayah_rows(texts, surahs, pages, juzs, quarters)
    _write_database(OUTPUT_FILE, surahs, ayah_rows)
    _verify_database(OUTPUT_FILE)
    print(f"Wrote {len(ayah_rows)} ayahs across {EXPECTED_PAGE_COUNT} pages to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
