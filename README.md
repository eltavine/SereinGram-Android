# SereinGram for Android

SereinGram is an unofficial Telegram client for Android. It is based on
[Nagram](https://github.com/NextAlone/Nagram), which builds on
[NekoX](https://web.archive.org/web/20240306062834/https://github.com/NekoX-Dev/NekoX),
and is the Android sibling of [SereinGram Desktop](https://github.com/eltavine/SereinGram).

SereinGram is not affiliated with Telegram, Nagram, NagramX, NagramXF,
AyuGram, Cherrygram, exteraGram, OctoGram or Swiftgram. The application
identity is described in [BRANDING.md](BRANDING.md).

- Source and releases: <https://github.com/eltavine/SereinGram-Android>
- Issues: <https://github.com/eltavine/SereinGram-Android/issues>

## Features

SereinGram keeps the features of Nagram (see
[Nagram's README](https://github.com/NextAlone/Nagram#readme)) and of NekoX
(see [README.NekoX.md](README.NekoX.md)), and ports selected features from
[NagramX](https://github.com/risin42/NagramX),
[NagramXF](https://github.com/Keeperorowner/NagramXF),
[AyuGram](https://github.com/AyuGram/AyuGram4A),
[Cherrygram](https://github.com/arsLan4k1390/Cherrygram),
[exteraGram](https://github.com/exteraSquad/exteraGram),
[OctoGram](https://github.com/OctoGramApp/OctoGram) and
[Swiftgram](https://github.com/Swiftgram/Telegram-iOS), along with feature
requests from their issue trackers. Each is written anew after the behaviour
of the project or request named, not copied from its code, and each has its
page under Settings → SereinGram.

- **Message history**, after AyuGram: messages others delete and earlier
  versions of edited messages are kept per account in a database of their
  own. Kept deleted messages come back into their chats, stay on screen when
  deleted while the chat is open, carry a "deleted" mark that can be any text,
  and keep their downloaded media, of which copies outlive Telegram's cache in
  the kinds of chat chosen. A message's menu shows its earlier versions and
  when it was deleted ([NagramXF #133](https://github.com/Keeperorowner/NagramXF/issues/133)),
  and a chat's menu lists, exports as text
  ([NagramX #294](https://github.com/risin42/NagramX/issues/294)) and clears
  its kept deleted messages; the History page lists every chat that has some.
  What the user deletes is not kept.
- **Ghost mode**, rounding out Nagram's after AyuGram and NagramX: it also
  holds back reading channel comments and secret chats, and channel posts are
  not counted as viewed. Chosen chats can see read receipts or typing, a chat
  can be marked read on demand, messages can wait a moment as scheduled ones
  so that sending does not show one online, after AyuGram, a quick settings
  tile and a home screen shortcut switch it
  ([NagramXF #181](https://github.com/Keeperorowner/NagramXF/issues/181)),
  and a ghost beside the chat list title shows it is on
  ([NagramX #231](https://github.com/risin42/NagramX/issues/231)).
- **Chat locks**, after Cherrygram and OctoGram: chats, the archive and
  secret chats open only after a fingerprint, face or the screen lock, which
  turning a lock off asks for as well, and the chat list hides what locked
  chats show ([Swiftgram #56](https://github.com/Swiftgram/Telegram-iOS/issues/56)).
- **Admin shortcuts** in a chat's menu, after Cherrygram, exteraGram,
  OctoGram and NagramX: recent actions, administrators, members,
  permissions, statistics and invite links
  ([Swiftgram #41](https://github.com/Swiftgram/Telegram-iOS/issues/41)),
  and restricting the sender of a message from its menu, after Swiftgram.
- **Voice transcription with one's own service**, after NagramX, Cherrygram
  and OctoGram: voice and video messages are turned into text by any service
  that speaks OpenAI's transcription API, without Telegram Premium
  ([exteraGram #111](https://github.com/exteraSquad/exteraGram/issues/111),
  [Swiftgram #144](https://github.com/Swiftgram/Telegram-iOS/issues/144)).
- **Bookmarks** on messages to jump back to
  ([NagramX #190](https://github.com/risin42/NagramX/issues/190)).
- **Local names** for people and chats that only this device shows
  ([NagramX #254](https://github.com/risin42/NagramX/issues/254)).
- **Block a sender** from their message in a group
  ([NagramX #283](https://github.com/risin42/NagramX/issues/283)), or
  **reply to them privately**, after NagramX and OctoGram.
- **Stop after each voice message** instead of playing the chat's next one,
  after Cherrygram and NagramX.
- **Ask before sending stickers and GIFs** tapped in their panel, after
  OctoGram.
- **Read the QR code in a photo** from its menu, then open or copy what it
  says, after NagramX.
- **The date beside the time** of older messages
  ([NagramX #462](https://github.com/risin42/NagramX/issues/462)), and **one
  check** on one's messages after they were read
  ([NagramXF #123](https://github.com/Keeperorowner/NagramXF/issues/123)).
- **Ask before opening any link**, after NagramX.
- **Hide global search results** in the chat list search
  ([NagramXF #10](https://github.com/Keeperorowner/NagramXF/issues/10),
  [#168](https://github.com/Keeperorowner/NagramXF/issues/168)), and its
  **Apps tab** ([NagramXF #153](https://github.com/Keeperorowner/NagramXF/issues/153)).
- **Paste as plain text** in messages and captions
  ([NagramX #415](https://github.com/risin42/NagramX/issues/415)).
- **Message content in notifications while locked**
  ([NagramX #299](https://github.com/risin42/NagramX/issues/299)).
- **Hide elements**, after NagramX and OctoGram: the Premium and Help
  sections of the settings, the share button beside messages, and the gift
  and message buttons under channels.
- **A backup of SereinGram's settings** to keep as a file and restore, after
  Cherrygram; API keys and chat locks stay out of it.
- Crash reports and update checks no longer go to Nagram's services.

## Architecture

SereinGram's code is kept apart from Nagram's so that Nagram's updates can be
merged:

- `serein/` holds Gradle modules that are included on their own: `core`
  (options, the module registry, fault isolation), `hooks` (the only API that
  Nagram's code calls), `ports` (storage interfaces), `features/*` (the logic
  of each feature in plain Kotlin) and `adapters/room` (Room databases).
- `TMessagesProj/src/main/kotlin/com/eltavine/sereingram` connects them to
  Telegram: `app/SereinApp` lists every module, `features/*` holds what each
  feature needs from Telegram, `settings` draws the settings pages.
- Nagram's files call SereinGram through one line per hook.
  `Tools/serein/upstream_budget.py` caps how much of Nagram's code changes and
  fails when a listed hook call disappears, ArchUnit tests keep the layers
  apart, and commitlint checks every commit; all of them run in CI.
  `./gradlew sereinCheck` runs the tests of every module.

## API and protocol documentation

Telegram API manuals: <https://core.telegram.org/api>

MTProto protocol manuals: <https://core.telegram.org/mtproto>

## Compilation guide

**NOTE: For Windows users, please consider using a Linux VM (such as WSL2) or dual booting.**

Environment:

- Linux distribution based on Debian or Arch Linux, or macOS

- Native tools: `gcc` `go` `make` `cmake` `ninja` `yasm` `meson`

  ```shell
  # for Debian based distribution
  sudo apt install gcc golang make cmake ninja-build yasm meson
  # for Arch Linux based distribution
  sudo pacman -S base-devel go ninja cmake yasm meson
  # for macOS
  xcode-select --install # install developer tools (will open confirm dialog)
  brew install go cmake ninja yasm meson # install other tools by homebrew
  ```

- Android SDK: `build-tools;36.0.0` `platforms;android-36` `ndk;27.2.12479018` `cmake;3.22.1` (the default location is **$HOME/Android/SDK**, otherwise you need to specify **$ANDROID_HOME** for it)

  It is recommended to use [Android Studio](https://developer.android.com/studio) to install, but you can also use `sdkmanager`:

  ```shell
  sdkmanager --sdk_root $HOME/Android/SDK --install "build-tools;36.0.0" "platforms;android-36" "ndk;27.2.12479018" "cmake;3.22.1"
  ```

Build:

1. Checkout submodules

   ```shell
   git submodule update --init --recursive
   ```

2. Optionally set `SEREIN_SIGNING_SHA1` in **local.properties** to the SHA-1
   of the certificate you sign the APK with (as printed by
   `keytool -list -v`). The native library then refuses to load in an APK
   signed with any other key. Leave it unset for development builds, which
   skips the check.

3. Build native dependencies:

   ```shell
   ./run init libs
   ```

4. Build external libraries and native code:

   ```shell
   ./run libs native
   ```

5. Fill out `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` in **local.properties** (from [Telegram Developer](https://my.telegram.org/auth))

6. Replace **TMessagesProj/google-services.json** if you want FCM to work.

7. Replace **release.keystore** with yours and fill out `ALIAS_NAME`, `KEYSTORE_PASS` and `ALIAS_PASS` in **local.properties**.

8. Build with Gradle:

   ```shell
   ./gradlew assemble<Release/Debug>
   ```

## Localization

Strings inherited from Telegram and Nagram follow their translations
([Telegram](https://translations.telegram.org/en/android/),
[Nagram on Crowdin](https://xtaolabs.crowdin.com/nagram)). SereinGram's own
strings live in `strings_serein.xml`.

## Thanks

- [Nagram](https://github.com/NextAlone/Nagram)
- [NekoX](https://web.archive.org/web/20240306062834/https://github.com/NekoX-Dev/NekoX)
- [Nekogram](https://gitlab.com/Nekogram/Nekogram)
- [Pigeongram](https://gitlab.com/JasonKhew96/Nekogram)
- [Nullgram](https://github.com/qwq233/Nullgram)
- [TeleTux](https://github.com/TeleTux/TeleTux)
- [OwlGram](https://github.com/OwlGramDev/OwlGram)
- The SereinGram logo was designed by [OukaroMF](https://github.com/OukaroMF/).
