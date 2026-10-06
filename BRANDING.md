# SereinGram branding

SereinGram for Android is an independent Telegram client based on
[Nagram](https://github.com/NextAlone/Nagram). Upstream source, history,
license and copyright notices remain with their respective authors.
SereinGram is not affiliated with Telegram, Nagram, NagramX, NagramXF or
AyuGram.

Application identity:

- Display name: SereinGram, also used for the APK file names, the folders
  it saves pictures and downloads to, and the UnifiedPush registration.
- Application ID: `com.eltavine.sereingram`, so it installs next to Nagram
  and shares no data with it. The `serein.android.application` convention
  plugin in `build-logic` sets it over the upstream value in
  `TMessagesProj/build.gradle`; the system account and contact sync use the
  same ID through the generated `serein_application_id` string.
- Firebase: `TMessagesProj/google-services.json` is a placeholder for that
  ID and points at no real project, so nothing is sent to Nagram's Firebase
  project. FCM push needs your own Firebase project's file.

Icons:

- The SereinGram logo was designed by
  [OukaroMF](https://github.com/OukaroMF/). Its master file,
  `Tools/serein/brand/sereingram-logo.svg`, is kept exactly as delivered and
  is the same file [SereinGram Desktop](https://github.com/eltavine/SereinGram)
  ships.
- `Tools/serein/brand/generate_icons.py` renders it with resvg
  (`uv run Tools/serein/brand/generate_icons.py`): the adaptive launcher icon
  with a light tile and a monochrome layer for themed icons, the legacy and
  round mipmaps, the green variant, the store icons, the call avatar and the
  status bar glyph, which reuses the dark paths of the logo and leaves its
  lighter folds open. The wordmark is the name set in the Roboto Medium the
  app bundles for its titles.
- Nagram's brand policy reserves the Nagram name, logos and icon artwork
  (© MaitungTM) for official Nagram releases. No Nagram or NekoX artwork is
  used for the application identity, and their icon assets are removed.

Text:

- Upstream strings are not edited in place, because their translations are
  merged from Nagram's Crowdin project. `Tools/serein/brand/generate_strings.py`
  writes every string that names Nagram, renamed, to
  `TMessagesProj/src/serein/res`, which each build type merges over
  `src/main/res`. Rerun it after merging upstream translations.
