# SereinGram for Android

SereinGram is an unofficial Telegram client for Android. It is based on
[Nagram](https://github.com/NextAlone/Nagram), which builds on
[NekoX](https://web.archive.org/web/20240306062834/https://github.com/NekoX-Dev/NekoX),
and is the Android sibling of [SereinGram Desktop](https://github.com/eltavine/SereinGram).

SereinGram is not affiliated with Telegram, Nagram, NagramX, NagramXF or
AyuGram. The application identity is described in [BRANDING.md](BRANDING.md).

- Source and releases: <https://github.com/eltavine/SereinGram-Android>
- Issues: <https://github.com/eltavine/SereinGram-Android/issues>

## Features

SereinGram keeps the features of Nagram (see
[Nagram's README](https://github.com/NextAlone/Nagram#readme)) and of NekoX
(see [README.NekoX.md](README.NekoX.md)), and ports selected features from
[NagramX](https://github.com/risin42/NagramX),
[NagramXF](https://github.com/Keeperorowner/NagramXF) and
[AyuGram](https://github.com/AyuGram/AyuGram4A).

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
