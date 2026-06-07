# Image to Directions

Kotlin Android app that reads JPEG EXIF metadata. Select a photo to view its timestamp and GPS coordinates, then open the location in a maps app.

## Features

- Title screen with a **Browse** button
- JPEG image picker
- Kotlin EXIF parsing (timestamp and GPS coordinates via AndroidX ExifInterface)
- Shows latitude, longitude, and **Open in Maps** when GPS data is present
- Shows a no-GPS message when coordinates are missing
- Browse button stays available to pick another image

## Requirements

- Android SDK (API 35 recommended)
- JDK 17+

Set `ANDROID_HOME` to your Android SDK path, for example:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
```

## Build

From the project root:

```bash
./gradlew assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

## Project structure

```
app/src/
  main/kotlin/com/imagetodirections/app/
    ui/MainActivity.kt              # Screen, file picker, maps intent
    exif/ExifMetadataReader.kt      # EXIF timestamp and GPS extraction
    exif/GpsCoordinateParser.kt     # GPS rational-to-decimal conversion
    exif/ImageMetadata.kt           # Parsed metadata model
  main/res/                         # Layouts, drawables, strings, themes
  test/kotlin/com/imagetodirections/app/exif/
    ExifMetadataReaderTest.kt       # EXIF file-reading integration tests
    GpsCoordinateParserTest.kt      # GPS coordinate parsing unit tests
  test/resources/                   # Sample JPEG fixtures with/without EXIF
```

## How it works

1. The user taps **Browse** and selects a JPEG image.
2. The app copies the original file to cache via a file descriptor.
3. `ExifMetadataReader` reads `DateTimeOriginal`/`DateTime` and GPS tags with AndroidX ExifInterface.
4. Results are shown on screen. If GPS coordinates exist, **Open in Maps** launches a `geo:` intent.

## Testing

Use JPEG photos taken with a phone camera (they usually include EXIF timestamps; GPS only if location was enabled when the photo was taken).

Unit tests cover EXIF parsing using sample images in `app/src/test/resources/`.
