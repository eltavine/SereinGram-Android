<div align="center">

<img src="Tools/serein/brand/sereingram-logo.svg" alt="SereinGram logo" width="112">

# SereinGram for Android

An unofficial Telegram client for Android, built on Nagram.

[![CI](https://github.com/eltavine/SereinGram-Android/actions/workflows/serein-ci.yml/badge.svg?branch=main)](https://github.com/eltavine/SereinGram-Android/actions/workflows/serein-ci.yml)
[![License](https://img.shields.io/github/license/eltavine/SereinGram-Android)](LICENSE)
![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white)

[Download](https://github.com/eltavine/SereinGram-Android/releases) ·
[SereinGram Desktop](https://github.com/eltavine/SereinGram) ·
[Issues](https://github.com/eltavine/SereinGram-Android/issues) ·
[Branding](BRANDING.md)

</div>

SereinGram builds on [Nagram](https://github.com/NextAlone/Nagram), which builds
on [NekoX](https://web.archive.org/web/20240306062834/https://github.com/NekoX-Dev/NekoX).
It keeps everything both of them offer and adds features of its own, most of
them first seen in other Telegram clients.

> [!TIP]
> On a computer? [SereinGram Desktop](https://github.com/eltavine/SereinGram)
> brings SereinGram to macOS, Windows and Linux.

SereinGram is not affiliated with Telegram, Nagram, NagramX, NagramXF, AyuGram,
Cherrygram, exteraGram, OctoGram or Swiftgram.

## Download

| Build | Where | What |
| --- | --- | --- |
| Stable | [GitHub Releases](https://github.com/eltavine/SereinGram-Android/releases) | Tagged versions |
| Nightly | SereinGram's Telegram channel | Every commit that passes CI on `main` |

The APKs are for `arm64-v8a`, which most phones need, and `armeabi-v7a`, for
older 32-bit ones, and run on Android 5.0 or later. SereinGram has an
application ID of its own, `com.eltavine.sereingram`, so it installs next to
Telegram and Nagram and shares no data with them.

## Features

Everything from Nagram (see [its README](https://github.com/NextAlone/Nagram#readme))
and NekoX (see [README.NekoX.md](README.NekoX.md)) stays. SereinGram adds the
features below, each with its page under **Settings → SereinGram**. Most follow
a feature of [NagramX](https://github.com/risin42/NagramX),
[NagramXF](https://github.com/Keeperorowner/NagramXF),
[AyuGram](https://github.com/AyuGram/AyuGram4A),
[Cherrygram](https://github.com/arsLan4k1390/Cherrygram),
[exteraGram](https://github.com/exteraSquad/exteraGram),
[OctoGram](https://github.com/OctoGramApp/OctoGram) or
[Swiftgram](https://github.com/Swiftgram/Telegram-iOS), or a request in one of
their issue trackers. Each is written anew after that behaviour, not copied
from its code.

### Message history

- **Deleted and edited messages are kept**, per account, in a database of
  their own. Deleted ones come back into their chats with a mark of your
  choice and stay on screen when deleted while the chat is open. Their
  downloaded media is kept too, copied out of Telegram's cache in the kinds of
  chat you choose. What you delete yourself is not kept.<br>
  <sub>After AyuGram</sub>
- **Earlier versions of a message**, and when it was deleted, in its menu.<br>
  <sub>[NagramXF #133](https://github.com/Keeperorowner/NagramXF/issues/133)</sub>
- **Export a chat's kept deleted messages** as text from the chat's menu,
  which also lists and clears them. The History page lists every chat that
  has some.<br>
  <sub>[NagramX #294](https://github.com/risin42/NagramX/issues/294)</sub>
- **Bookmarks** on messages, to jump back to them.<br>
  <sub>[NagramX #190](https://github.com/risin42/NagramX/issues/190)</sub>

### Privacy

- **Ghost mode** goes further than Nagram's: it also holds back reading
  channel comments and secret chats, and channel posts are not counted as
  viewed. Chosen chats still see your read receipts or typing, a chat can be
  marked read on demand, and messages can wait a moment as scheduled ones so
  that sending does not show you online.<br>
  <sub>After AyuGram and NagramX</sub>
- **Ghost mode at hand**: a quick settings tile and a home screen shortcut
  switch it, and a ghost beside the chat list title shows when it is on.<br>
  <sub>[NagramXF #181](https://github.com/Keeperorowner/NagramXF/issues/181) ·
  [NagramX #231](https://github.com/risin42/NagramX/issues/231)</sub>
- **Chat locks**: chats, the archive and secret chats open only after a
  fingerprint, a face or the screen lock, which turning a lock off asks for
  too, and the chat list hides what locked chats show.<br>
  <sub>After Cherrygram and OctoGram ·
  [Swiftgram #56](https://github.com/Swiftgram/Telegram-iOS/issues/56)</sub>
- **DNS of your choice**: the system's resolver, which follows Android's
  Private DNS, or a DNS-over-HTTPS server of your own, for Telegram's host
  names, proxies and the web services the app calls.<br>
  <sub>After NagramX's custom DoH and Nagram's system DNS switch</sub>
- **One check for read messages**: your messages keep a single check after
  they were read.<br>
  <sub>[NagramXF #123](https://github.com/Keeperorowner/NagramXF/issues/123)</sub>
- **Ask before opening any link**, with its full address.<br>
  <sub>After NagramX</sub>
- Crash reports and update checks no longer go to Nagram's services.

### Chats and messages

- **Admin shortcuts** in a chat's menu: recent actions, administrators,
  members, permissions, statistics and invite links.<br>
  <sub>After Cherrygram, exteraGram, OctoGram and NagramX ·
  [Swiftgram #41](https://github.com/Swiftgram/Telegram-iOS/issues/41)</sub>
- **Change a sender's permissions** from their message's menu, also in large
  groups.<br>
  <sub>After Swiftgram</sub>
- **Block a sender** from their message in a group, or **reply to them
  privately**.<br>
  <sub>[NagramX #283](https://github.com/risin42/NagramX/issues/283) ·
  after NagramX and OctoGram</sub>
- **Local names** for people and chats, which only this device shows.<br>
  <sub>[NagramX #254](https://github.com/risin42/NagramX/issues/254)</sub>
- **Pinned reactions** come first above a message's menu.<br>
  <sub>After NagramX and OctoGram</sub>
- **The date beside the time** of older messages.<br>
  <sub>[NagramX #462](https://github.com/risin42/NagramX/issues/462)</sub>
- **Read the QR code in a photo** from its menu, then open or copy what it
  says.<br>
  <sub>After NagramX</sub>
- **Paste as plain text** in messages and captions.<br>
  <sub>[NagramX #415](https://github.com/risin42/NagramX/issues/415)</sub>
- **Ask before sending stickers and GIFs** tapped in their panel.<br>
  <sub>After OctoGram</sub>

### Voice and media

- **Voice transcription with your own service**: voice and video messages
  become text through any service that speaks OpenAI's transcription API,
  without Telegram Premium.<br>
  <sub>After NagramX, Cherrygram and OctoGram ·
  [exteraGram #111](https://github.com/exteraSquad/exteraGram/issues/111) ·
  [Swiftgram #144](https://github.com/Swiftgram/Telegram-iOS/issues/144)</sub>
- **Stop after each voice message** instead of playing the chat's next one.<br>
  <sub>After Cherrygram and NagramX</sub>
- **Set how far a double tap jumps** in a video.<br>
  <sub>After exteraGram, Cherrygram and NagramXF</sub>

### Notifications

- **Message content in notifications while locked**.<br>
  <sub>[NagramX #299](https://github.com/risin42/NagramX/issues/299)</sub>
- **Answer from a notification** without replying to the latest message.<br>
  <sub>[Cherrygram #120](https://github.com/arsLan4k1390/Cherrygram/issues/120)</sub>

### Interface

- **Hide elements**: the Premium and Help sections of the settings, the share
  button beside messages, reactions under messages, and the gift and message
  buttons under channels.<br>
  <sub>After NagramX and OctoGram; reactions after Swiftgram</sub>
- **Hide global search results** in the chat list search, and its **Apps
  tab**.<br>
  <sub>[NagramXF #10](https://github.com/Keeperorowner/NagramXF/issues/10) ·
  [#168](https://github.com/Keeperorowner/NagramXF/issues/168) ·
  [#153](https://github.com/Keeperorowner/NagramXF/issues/153)</sub>

### Tools

- **A proxy tile** in quick settings that turns the chosen proxy on and off.<br>
  <sub>[NagramXF #35](https://github.com/Keeperorowner/NagramXF/issues/35)</sub>
- **Settings backup** to a file and back. API keys and chat locks stay out of
  it, and an account's settings only go back to the same Telegram user.<br>
  <sub>After Cherrygram</sub>

## Building

### Requirements

- Linux based on Debian or Arch Linux, or macOS. On Windows, use WSL2 or a
  Linux virtual machine.
- JDK 21.
- Native tools: `gcc`, `go`, `make`, `cmake`, `ninja`, `yasm` and `meson`.

  ```shell
  # Debian and its derivatives
  sudo apt install gcc golang make cmake ninja-build yasm meson
  # Arch Linux and its derivatives
  sudo pacman -S base-devel go ninja cmake yasm meson
  # macOS
  xcode-select --install
  brew install go cmake ninja yasm meson
  ```

- Android SDK: `build-tools;36.0.0`, `platforms;android-36`,
  `ndk;27.2.12479018` and `cmake;3.22.1`, in `$HOME/Android/SDK` or wherever
  `ANDROID_HOME` points. [Android Studio](https://developer.android.com/studio)
  installs them, and so does `sdkmanager`:

  ```shell
  sdkmanager --sdk_root $HOME/Android/SDK --install "build-tools;36.0.0" "platforms;android-36" "ndk;27.2.12479018" "cmake;3.22.1"
  ```

### Steps

1. Fetch the submodules.

   ```shell
   git submodule update --init --recursive
   ```

2. Build the native dependencies, then the external libraries and the native
   code.

   ```shell
   ./run init libs
   ./run libs native
   ```

3. Optionally, fill in the [build settings](#build-settings).
4. Replace `TMessagesProj/google-services.json` with your own Firebase
   project's if you want push notifications through FCM.
5. Build with Gradle. The APKs land in `TMessagesProj/build/outputs/apk/`.

   ```shell
   ./gradlew assembleRelease   # or assembleDebug
   ```

### Build settings

`local.properties`, at the root of the repository, may hold these settings:

| Setting | Effect |
| --- | --- |
| `TELEGRAM_APP_ID`, `TELEGRAM_APP_HASH` | Your API credentials from [my.telegram.org](https://my.telegram.org/auth). Without them, the build keeps Nagram's. |
| `KEYSTORE_FILE` or `KEYSTORE_BASE64` | The release keystore: its path from the root of the repository, or the keystore itself in base64. |
| `KEYSTORE_PASS`, `ALIAS_NAME`, `ALIAS_PASS` | The keystore's password, the key's alias and the key's password. Without all of the signing settings, the APK is signed with the debug key. |
| `SEREIN_SIGNING_SHA1` | The SHA-1 of the signing certificate, as `keytool -list -v` prints it. The native library then refuses to load in an APK signed with any other key. Leave it unset for development builds. |

Settings missing from the file are read from the environment. The
`LOCAL_PROPERTIES` environment variable, when set, holds the whole file in
base64 and is read in its place.

## Development

SereinGram's code is kept apart from Nagram's, so that Nagram's updates can be
merged. Nagram's files call SereinGram through one line per hook.

| Path | Contents |
| --- | --- |
| `serein/core` | Options, the module registry and fault isolation |
| `serein/hooks` | The only API that Nagram's code calls |
| `serein/ports` | Storage interfaces |
| `serein/features/*` | The logic of each feature, in plain Kotlin |
| `serein/adapters/room` | Room databases |
| `TMessagesProj/src/main/kotlin/com/eltavine/sereingram` | The glue to Telegram: `app/SereinApp` lists every module, `features/*` holds what each feature needs from Telegram, `settings` draws the settings pages |
| `build-logic` | Gradle convention plugins |
| `Tools/serein` | Checks, branding and release scripts |

Each directory below `serein/` with a `build.gradle.kts` is included as a
Gradle module of its own.

### Checks

CI runs all of these. The Python scripts run with
[uv](https://docs.astral.sh/uv/).

| Command | What it checks |
| --- | --- |
| `./gradlew sereinCheck` | The tests of every module, and that the public APIs of `core`, `hooks` and `ports` match their `api/` folders |
| `./gradlew :TMessagesProj:testReleaseUnitTest --tests 'com.eltavine.sereingram.*'` | The tests of the glue, under Robolectric, and the ArchUnit rules that keep the layers apart |
| `Tools/serein/upstream_budget.py` | How much of Nagram's code changes, and that no listed hook call disappears |
| `Tools/serein/lint_commits.sh origin/main..HEAD` | Every commit message, with commitlint, after `npm ci --prefix Tools/serein` |

A change to a public API is made on purpose with `./gradlew apiDump`, run on
its own, and shows up in review. Workflows and scripts are also checked with
actionlint, ShellCheck and Ruff.

Telegram's [API](https://core.telegram.org/api) and
[MTProto](https://core.telegram.org/mtproto) manuals describe the protocol.

## Releases

- **Nightly**: every commit that passes CI on `main` is built, signed with the
  release key and sent to the Telegram channel by `serein-nightly.yml`, unless
  `main` has moved past it by then.
- **Stable**: pushing a `v*` tag on a commit of `main` runs CI on it, builds
  and signs it, publishes it as the latest GitHub release and sends it to the
  channel, by `serein-stable.yml`.

Both read their secrets from the `telegram` environment, which only `main` and
`v*` tags may use:

| Secret | Value |
| --- | --- |
| `TELEGRAM_BOT_TOKEN` | The token of a bot that is an administrator of the channel |
| `KEYSTORE_BASE64` | The release keystore, in base64 |
| `KEYSTORE_PASS` | The keystore's password |
| `ALIAS_NAME` | The key's alias |
| `ALIAS_PASS` | The key's password |

Nightly builds wait for them with a warning; a stable release fails without
them. Setting the `SEREIN_SIGNING_SHA1` repository variable to the key's
SHA-1, which the nightly job prints, turns on the native library's signature
check. The APKs go up through a Bot API server that runs beside the job, since
they are larger than the 50 MB that Telegram's own Bot API server takes. For
now, both the app and that server use Telegram's own published API
credentials.

## Localization

Strings inherited from Telegram and Nagram follow their translations, on
[Telegram's platform](https://translations.telegram.org/en/android/) and
[Nagram's Crowdin](https://xtaolabs.crowdin.com/nagram). SereinGram's own
strings live in `strings_serein.xml`, in English and in Chinese as written in
mainland China, Taiwan and Hong Kong.

## Thanks

- [Nagram](https://github.com/NextAlone/Nagram)
- [NekoX](https://web.archive.org/web/20240306062834/https://github.com/NekoX-Dev/NekoX)
- [Nekogram](https://gitlab.com/Nekogram/Nekogram)
- [Pigeongram](https://gitlab.com/JasonKhew96/Nekogram)
- [Nullgram](https://github.com/qwq233/Nullgram)
- [TeleTux](https://github.com/TeleTux/TeleTux)
- [OwlGram](https://github.com/OwlGramDev/OwlGram)
- The SereinGram logo was designed by [OukaroMF](https://github.com/OukaroMF/).

## License

SereinGram is free software under the
[GNU General Public License v3.0](LICENSE), like Nagram.
[BRANDING.md](BRANDING.md) describes its name, application ID and icons.
