import json
from pathlib import Path

import pytest
from release import (
    CAPTION_LIMIT,
    Apk,
    Build,
    apk_signer,
    apks,
    caption,
    keystore_certificate,
    media_group,
)

KEYTOOL = """Alias name: serein
Owner: CN=Eltavine, OU=SereinGram, O=Eltavine, C=CN
Certificate fingerprints:
\t SHA1: AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01
\t SHA256: 01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF
"""

BUILD = Build(
    channel="Nightly",
    commit="0123456789abcdef0123456789abcdef01234567",
    subject="feat(dns): resolve <names> & more",
    author="Eltavine",
    commit_url="https://github.com/eltavine/SereinGram-Android/commit/0123456789abcdef",
    run_url="https://github.com/eltavine/SereinGram-Android/actions/runs/1",
    run_number="7",
    signer="Eltavine",
)


def write_build(directory: Path, abis: list[str]) -> None:
    elements = []
    for abi in abis:
        name = f"SereinGram-v12.10.1(7038)-{abi}.apk"
        (directory / name).write_bytes(b"apk")
        elements.append(
            {
                "filters": [{"filterType": "ABI", "value": abi}],
                "versionCode": 7038,
                "versionName": "12.10.1",
                "outputFile": name,
            }
        )
    (directory / "output-metadata.json").write_text(
        json.dumps({"elements": elements}), encoding="utf-8"
    )


def test_apks_come_from_gradles_metadata_with_the_64_bit_one_first(tmp_path: Path) -> None:
    write_build(tmp_path, ["armeabi-v7a", "arm64-v8a"])
    assert [apk.abi for apk in apks(tmp_path)] == ["arm64-v8a", "armeabi-v7a"]
    assert apks(tmp_path)[0].version_code == 7038


def test_an_apk_outside_the_folder_is_refused(tmp_path: Path) -> None:
    (tmp_path / "output-metadata.json").write_text(
        json.dumps({"elements": [{"outputFile": "../evil.apk"}]}), encoding="utf-8"
    )
    with pytest.raises(SystemExit):
        apks(tmp_path)


def test_the_keystore_names_its_digests_and_owner() -> None:
    sha256, sha1, owner = keystore_certificate(KEYTOOL)
    assert sha256 == "0123456789abcdef" * 4
    assert sha1 == "abcdef0123456789abcdef0123456789abcdef01"
    assert owner == "Eltavine"


def test_apksigner_names_the_first_signer() -> None:
    printed = (
        "Signer #1 certificate DN: CN=Eltavine\nSigner #1 certificate SHA-256 digest: 0123ABCD\n"
    )
    assert apk_signer(printed) == "0123abcd"


def test_the_caption_escapes_and_fits_telegrams_limit() -> None:
    built = [Apk(Path("a.apk"), "arm64-v8a", "12.10.1", 7038)]
    text = caption(BUILD, built)
    assert "resolve &lt;names&gt; &amp; more" in text
    assert "12.10.1 (7038)" in text
    long = caption(Build(**{**BUILD.__dict__, "subject": "x" * 5000}), built)
    assert len(long) <= CAPTION_LIMIT
    assert long.count("…") == 1


def test_the_caption_goes_under_the_last_document() -> None:
    built = [Apk(Path("a.apk"), "arm64-v8a", "1", 1), Apk(Path("b.apk"), "armeabi-v7a", "1", 1)]
    media = media_group(built, "text")
    assert [item["media"] for item in media] == ["attach://apk0", "attach://apk1"]
    assert "caption" not in media[0]
    assert media[1]["caption"] == "text"
    assert media[1]["parse_mode"] == "HTML"
