#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = ["httpx==0.28.1"]
# ///
"""Check SereinGram's release APKs and send them to Telegram.

    verify   fails unless every APK is signed with the release key, and names its signer
    publish  sends the APKs to a Telegram chat through a Bot API server

Run from the repository root, in CI, after Gradle built the release APKs.
"""

import argparse
import base64
import contextlib
import html
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path

import httpx

# Telegram shows the 64-bit APK first, the one most phones take.
ABI_ORDER = ("arm64-v8a", "armeabi-v7a")
CAPTION_LIMIT = 1024


@dataclass(frozen=True)
class Apk:
    path: Path
    abi: str
    version_name: str
    version_code: int


@dataclass(frozen=True)
class Build:
    channel: str
    commit: str
    subject: str
    author: str
    commit_url: str
    run_url: str
    run_number: str
    signer: str
    release_url: str = ""


def apks(directory: Path) -> list[Apk]:
    """The APKs Gradle lists in its output-metadata.json, in ABI_ORDER."""
    metadata = json.loads((directory / "output-metadata.json").read_text(encoding="utf-8"))
    found = []
    for element in metadata.get("elements", []):
        name = element.get("outputFile", "")
        if not name.endswith(".apk") or Path(name).name != name:
            raise SystemExit(f"Unexpected APK in output-metadata.json: {name!r}")
        abis = [f["value"] for f in element.get("filters", []) if f.get("filterType") == "ABI"]
        path = directory / name
        if len(abis) != 1 or not path.is_file():
            raise SystemExit(f"{name} is missing or not built for exactly one ABI")
        found.append(Apk(path, abis[0], element["versionName"], int(element["versionCode"])))
    if not found:
        raise SystemExit(f"No APKs listed in {directory / 'output-metadata.json'}")
    return sorted(
        found, key=lambda apk: ABI_ORDER.index(apk.abi) if apk.abi in ABI_ORDER else len(ABI_ORDER)
    )


def fingerprint(text: str) -> str:
    """A certificate digest such as keytool and apksigner print, as lowercase hex without colons."""
    return text.replace(":", "").strip().lower()


def keystore_certificate(listing: str) -> tuple[str, str, str]:
    """SHA-256, SHA-1 and the common name of the owner, from `keytool -list -v`."""

    def field(pattern: str) -> str:
        match = re.search(pattern, listing, re.MULTILINE)
        if not match:
            raise SystemExit(f"keytool printed no {pattern!r}")
        return match.group(1).strip()

    owner = field(r"^Owner: (.+)$")
    common_name = re.search(r"(?:^|,\s*)CN=([^,]+)", owner)
    return (
        fingerprint(field(r"^\s*SHA256: (\S+)$")),
        fingerprint(field(r"^\s*SHA1: (\S+)$")),
        common_name.group(1).strip() if common_name else owner,
    )


def apk_signer(verification: str) -> str:
    """The single signing certificate's SHA-256, across apksigner output formats."""
    # Depending on the tool and signature scheme, certificates are labelled by
    # signer number, SDK range, or scheme (e.g. "V3.1 Signer:"). Source stamps
    # and public-key digests are not APK signing certificates.
    digests = set()
    for line in verification.splitlines():
        label, separator, value = line.partition(" certificate SHA-256 digest: ")
        if not separator or not re.fullmatch(
            r"Signer (?:#[1-9]\d*|\(.+\))|V\d+(?:\.\d+)? Signer:", label
        ):
            continue
        digest = fingerprint(value.split()[0]) if value.split() else ""
        if not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise SystemExit("apksigner printed an invalid signer SHA-256 digest")
        digests.add(digest)
    if not digests:
        raise SystemExit(
            "apksigner printed no recognized APK signing certificate; "
            f"output was:\n{verification.strip()}"
        )
    if len(digests) != 1:
        raise SystemExit("The APK has multiple signing certificates; expected only the release key")
    return digests.pop()


def caption(build: Build, built: list[Apk]) -> str:
    """The HTML caption of the album, within Telegram's limit; a long commit subject is cut."""
    versions = sorted({f"{apk.version_name} ({apk.version_code})" for apk in built})

    def render(subject: str) -> str:
        lines = [
            f"<b>SereinGram {html.escape(build.channel)}</b>",
            "",
            f"Version: <b>{html.escape(', '.join(versions))}</b>",
            f'Commit: <a href="{html.escape(build.commit_url)}">{html.escape(build.commit[:8])}</a>',
            f"<pre>{html.escape(subject)}</pre>",
            f"Author: <b>{html.escape(build.author)}</b>",
            f'Action: <a href="{html.escape(build.run_url)}">#{html.escape(build.run_number)}</a>',
            f"Signature: <b>verified, {html.escape(build.signer)}</b>",
        ]
        if build.release_url:
            lines.append(f'Release: <a href="{html.escape(build.release_url)}">GitHub</a>')
        return "\n".join(lines)

    text = render(build.subject)
    if len(text) <= CAPTION_LIMIT:
        return text
    room = CAPTION_LIMIT - len(render("")) - 1
    if room < 1:
        raise SystemExit("The caption leaves no room for the commit subject")
    return render(build.subject[:room].rstrip() + "…")


def media_group(built: list[Apk], text: str) -> list[dict]:
    """sendMediaGroup's documents, with the caption under the last one, below the album."""
    media = [{"type": "document", "media": f"attach://apk{i}"} for i in range(len(built))]
    media[-1].update(caption=text, parse_mode="HTML")
    return media


def verify(args: argparse.Namespace) -> None:
    built = apks(args.apk_dir)
    with tempfile.TemporaryDirectory() as temporary:
        keystore = Path(temporary) / "release.keystore"
        keystore.write_bytes(base64.b64decode(os.environ["KEYSTORE_BASE64"]))
        keytool = ["keytool", "-J-Duser.language=en", "-list", "-v", "-keystore", keystore]
        keytool += ["-storepass:env", "KEYSTORE_PASS", "-alias", os.environ["ALIAS_NAME"]]
        listing = subprocess.run(keytool, capture_output=True, text=True, check=True).stdout
    sha256, sha1, signer = keystore_certificate(listing)
    tools = Path(os.environ["ANDROID_HOME"], "build-tools")
    newest = max(
        tools.iterdir(), key=lambda path: [int(part) for part in re.findall(r"\d+", path.name)]
    )
    for apk in built:
        printed = subprocess.run(
            [newest / "apksigner", "verify", "--print-certs", apk.path],
            capture_output=True,
            text=True,
            check=True,
        ).stdout
        if apk_signer(printed) != sha256:
            raise SystemExit(f"{apk.path.name} is not signed with the release key")
    native = fingerprint(os.environ.get("SEREIN_SIGNING_SHA1", ""))
    if native and native != sha1:
        raise SystemExit(
            f"The native library checks for {native}, but the release key is {sha1}; it would refuse to load."
        )
    if not native:
        print(
            f"::warning::The native library checks no signature; set the SEREIN_SIGNING_SHA1 variable to {sha1}."
        )
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"signer={signer}\n")
    print(f"Every APK is signed by {signer}.")


def git(*arguments: str) -> str:
    return subprocess.run(
        ["git", *arguments], capture_output=True, text=True, check=True
    ).stdout.strip()


def publish(args: argparse.Namespace) -> None:
    built = apks(args.apk_dir)
    server = os.environ["GITHUB_SERVER_URL"] + "/" + os.environ["GITHUB_REPOSITORY"]
    commit = git("rev-parse", "HEAD")
    build = Build(
        channel=args.channel,
        commit=commit,
        subject=git("log", "-1", "--format=%s"),
        author=git("log", "-1", "--format=%an"),
        commit_url=f"{server}/commit/{commit}",
        run_url=f"{server}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
        run_number=os.environ["GITHUB_RUN_NUMBER"],
        signer=args.signer,
        release_url=args.release_url,
    )
    bot = f"{args.bot_api.rstrip('/')}/bot{os.environ['TELEGRAM_BOT_TOKEN']}"
    with httpx.Client(timeout=httpx.Timeout(30, write=600)) as client:
        # The Bot API server starts beside the job and logs the bot in on first use.
        for _ in range(30):
            try:
                if client.get(f"{bot}/getMe").json().get("ok"):
                    break
            except httpx.TransportError:
                pass
            time.sleep(2)
        else:
            raise SystemExit("The Bot API server did not answer")
        with contextlib.ExitStack() as opened:
            files = {
                f"apk{i}": (
                    apk.path.name,
                    opened.enter_context(apk.path.open("rb")),
                    "application/vnd.android.package-archive",
                )
                for i, apk in enumerate(built)
            }
            data = {
                "chat_id": args.chat_id,
                "media": json.dumps(media_group(built, caption(build, built))),
            }
            reply = client.post(f"{bot}/sendMediaGroup", data=data, files=files).json()
    if not reply.get("ok"):
        raise SystemExit(f"Telegram refused the APKs: {reply.get('description')}")
    print(f"Sent {len(built)} APKs to {args.chat_id}.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(required=True)
    check = commands.add_parser("verify")
    check.add_argument("--apk-dir", type=Path, required=True)
    check.set_defaults(run=verify)
    send = commands.add_parser("publish")
    send.add_argument("--apk-dir", type=Path, required=True)
    send.add_argument(
        "--channel", required=True, help="Nightly, or the version of a stable release"
    )
    send.add_argument("--chat-id", required=True)
    send.add_argument("--signer", required=True)
    send.add_argument("--release-url", default="")
    send.add_argument("--bot-api", default="http://localhost:8081")
    send.set_defaults(run=publish)
    args = parser.parse_args()
    args.run(args)


if __name__ == "__main__":
    sys.exit(main())
