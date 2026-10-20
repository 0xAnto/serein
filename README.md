<div align="center">
    <img src="assets/Serein-Logo.png" alt="Serein logo" width="96" height="96">
    <h1>Serein</h1>
    <p>A quiet space for sleep, focus, and unwinding.</p>
    <p>Android 12+ · Fully offline · No account, ads, or analytics · MIT licensed</p>

[Download APK](https://github.com/0xAnto/serein/releases/latest/download/Serein.apk) · [All releases](https://github.com/0xAnto/serein/releases) · [Report a bug](https://github.com/0xAnto/serein/issues)
</div>

Serein is a native Android ambient sound mixer. Layer sounds, save your favorite mixes, set a sleep timer, or take a moment with guided breathing. All twelve soundscapes are bundled, so playback works without a connection.

![Serein interface previews showing Home with saved mixes, the sound mixer, guided breathing, and night mode.](assets/serein-preview-board.png)

*Promotional interface previews. Actual app appearance may vary.*

## Download

Requires **Android 12 or newer**.

1. Open the [latest release](https://github.com/0xAnto/serein/releases/latest) and download **Serein.apk** under **Assets**.
2. Open the APK on your phone. If prompted, allow your browser or file manager to install apps from this source, then tap **Install**.

Releases include `SHA256SUMS` to verify the download. Updates signed with the same release key retain saved mixes and preferences. Switching between release and development builds signed with different keys requires uninstalling first, which removes local data.

## Features

- **Mix your own soundscape.** Combine sounds, adjust individual layers and master volume, and save favorites. Sleep, Focus, and Unwind presets offer a starting point.
- **Set a sleep timer.** Choose a preset or custom duration with a fade included in the selected time. The countdown pauses when playback pauses.
- **Keep listening in the background.** Control playback from notifications, the lock screen, or home-screen widgets.
- **Take a breathing break.** Follow a guided cycle: breathe in for 4 seconds, hold for 4, and breathe out for 6.
- **Make it comfortable.** Light and night themes, support for larger text, and player and sleep widgets.

**Bundled sounds:** rain, brown noise, ocean, white noise, forest, fireplace, pink noise, wind, stream, singing bowl, night, and soft fan.

### Privacy

Audio and saved mixes stay on your device. Serein does not request internet access, require an account, or include ads or analytics. Notification permission supports playback controls.

## Getting started

- **Home:** start a preset or saved mix. Long-press a saved mix to edit or delete it.
- **Mixes:** tap sounds to combine them. Tap the compact player's artwork or name to adjust volumes, save the mix, or set a sleep timer. Tap an active countdown to change it.
- **Breathe:** follow the breathing guide. Use the top-right options menu to change the theme or add a widget.

The player widget offers playback and timer controls. The sleep widget starts a 30-minute session when no timer is selected. Playback responds to Android audio focus and continues when the activity is swiped away; force-stopping the app stops playback.

## Build from source

Serein uses Java and native Android views, with `MediaPlayer`, `MediaSession`, and `RemoteViews`. It has no third-party app library dependencies.

Install **JDK 17**, **Android SDK Platform 36**, and **Android SDK Build Tools 36.0.0**. Android Studio can install the SDK components.

```sh
git clone https://github.com/0xAnto/serein.git
cd serein
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. You can also open the project in Android Studio. The first build downloads Gradle and the Android build plugin.

Output: `app/build/outputs/apk/debug/app-debug.apk`.

<details>
<summary>Build directly with the Android SDK, without Gradle</summary>

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build-apk.sh
```

Output: `build/Serein-Android.apk`. The script creates a local test key in the ignored `test-signing/` directory. Set `SEREIN_BUILD_TOOLS` to select another installed build-tools directory.

</details>

## Contributing

Bug reports, documentation improvements, and pull requests are welcome.

For bugs, [open an issue](https://github.com/0xAnto/serein/issues) with your Android version, device model, app version, steps to reproduce, and expected and actual behavior. For a substantial change, open an issue first to discuss the approach.

Before submitting a pull request:

1. Run the checks below and build a debug APK.
2. Follow [the device checklist](DEVICE-CHECK.md) for playback, widget, or interface changes.
3. Describe the change and include test results and any device testing. Include screenshots for visual changes.

### Checks

Requires JDK 17, Python 3, and FFprobe (included with FFmpeg).

```sh
./tests/run.sh
```

Checks cover timer formatting, gain bounds, fade math, manifest and widget contracts, offline packaging, and all twelve audio assets. [GitHub Actions](https://github.com/0xAnto/serein/actions/workflows/ci.yml) runs these checks and builds a debug APK on pushes to `main` and pull requests. Playback and device behavior also need manual verification.

## Project layout

| Path | Contents |
| --- | --- |
| [`app/src/main/java/com/serein/app/`](app/src/main/java/com/serein/app/) | UI, playback service, session state, and widgets |
| [`app/src/main/assets/audio/`](app/src/main/assets/audio/) | Bundled soundscapes |
| [`app/src/main/res/`](app/src/main/res/) | Android resources and widget layouts |
| [`assets/`](assets/) | Logo and promotional artwork |
| [`tests/`](tests/) | Java and package checks |

## Releases

See [RELEASING.md](RELEASING.md) for signing, release builds, and publishing APKs. Version notes live in [`docs/releases/`](docs/releases/).

## License

[MIT](LICENSE), including the bundled original synthesized soundscapes.
