import argparse
import base64
import json
import subprocess
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
    verify,
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


@pytest.mark.parametrize(
    "label",
    [
        "Signer #1",
        "Signer (minSdkVersion=33, maxSdkVersion=2147483647)",
        "Signer (minSdkVersion=33 (dev release=true), maxSdkVersion=2147483647)",
        "V1 Signer:",
        "V2 Signer:",
        "V3 Signer:",
        "V3.1 Signer:",
    ],
)
def test_apksigner_accepts_numbered_sdk_range_and_scheme_labels(label: str) -> None:
    printed = f"{label} certificate DN: CN=Eltavine\r\n"
    printed += f"{label} certificate SHA-256 digest: {'0123456789ABCDEF' * 4}\r\n"
    assert apk_signer(printed) == "0123456789abcdef" * 4


def test_apksigner_accepts_the_same_certificate_in_multiple_schemes() -> None:
    digest = "0123456789abcdef" * 4
    printed = f"V2 Signer: certificate SHA-256 digest: {digest}\n"
    printed += f"V3 Signer: certificate SHA-256 digest: {digest} (minSdkVersion=28)\n"
    assert apk_signer(printed) == digest


def test_apksigner_does_not_use_source_stamps_or_public_key_digests() -> None:
    printed = f"Source Stamp Signer certificate SHA-256 digest: {'f' * 64}\n"
    printed += f"Signer #1 public key SHA-256 digest: {'e' * 64}\n"
    printed += f"Signer #1 certificate SHA-256 digest: {'a' * 64}\n"
    assert apk_signer(printed) == "a" * 64


@pytest.mark.parametrize(
    "printed",
    [
        "",
        "Verifies\n",
        f"Source Stamp Signer certificate SHA-256 digest: {'a' * 64}\n",
        f"Signer #1 public key SHA-256 digest: {'a' * 64}\n",
    ],
)
def test_apksigner_refuses_output_without_an_apk_certificate(printed: str) -> None:
    with pytest.raises(SystemExit, match="no recognized APK signing certificate"):
        apk_signer(printed)


@pytest.mark.parametrize("digest", ["", "0123ABCD", "g" * 64, "a" * 65])
def test_apksigner_refuses_invalid_digests(digest: str) -> None:
    with pytest.raises(SystemExit, match="invalid signer SHA-256 digest"):
        apk_signer(f"Signer #1 certificate SHA-256 digest: {digest}\n")


@pytest.mark.parametrize(
    "labels",
    [
        ("Signer #1", "Signer #2"),
        ("V2 Signer:", "V3 Signer:"),
        (
            "Signer (minSdkVersion=33, maxSdkVersion=2147483647)",
            "Signer (minSdkVersion=28, maxSdkVersion=32)",
        ),
    ],
)
def test_apksigner_refuses_different_certificates(labels: tuple[str, str]) -> None:
    printed = f"{labels[0]} certificate SHA-256 digest: {'a' * 64}\n"
    printed += f"{labels[1]} certificate SHA-256 digest: {'b' * 64}\n"
    with pytest.raises(SystemExit, match="multiple signing certificates"):
        apk_signer(printed)


@pytest.mark.parametrize("matches", [True, False])
def test_verify_checks_each_apk_against_the_keystore(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, matches: bool
) -> None:
    apk_dir = tmp_path / "apks"
    apk_dir.mkdir()
    write_build(apk_dir, ["armeabi-v7a", "arm64-v8a"])
    tools = tmp_path / "android" / "build-tools" / "36.0.0"
    tools.mkdir(parents=True)
    output = tmp_path / "output"
    monkeypatch.setenv("ANDROID_HOME", str(tmp_path / "android"))
    monkeypatch.setenv("KEYSTORE_BASE64", base64.b64encode(b"test keystore").decode())
    monkeypatch.setenv("KEYSTORE_PASS", "test password")
    monkeypatch.setenv("ALIAS_NAME", "serein")
    monkeypatch.setenv("SEREIN_SIGNING_SHA1", "abcdef0123456789abcdef0123456789abcdef01")
    monkeypatch.setenv("GITHUB_OUTPUT", str(output))
    checked = []

    def run(command, **kwargs):
        assert kwargs["check"] is True
        if command[0] == "keytool":
            return subprocess.CompletedProcess(command, 0, stdout=KEYTOOL)
        checked.append(Path(command[-1]).name)
        digest = "0123456789abcdef" * 4 if matches else "f" * 64
        return subprocess.CompletedProcess(
            command, 0, stdout=f"V3 Signer: certificate SHA-256 digest: {digest}\n"
        )

    monkeypatch.setattr("release.subprocess.run", run)
    if matches:
        verify(argparse.Namespace(apk_dir=apk_dir))
        assert len(checked) == 2
        assert output.read_text() == "signer=Eltavine\n"
    else:
        with pytest.raises(SystemExit, match="not signed with the release key"):
            verify(argparse.Namespace(apk_dir=apk_dir))
        assert not output.exists()


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
