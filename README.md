# BingeMode

BingeMode is an Android app for discovering movies and TV shows, keeping a watchlist, and tracking episode progress.

> **Development status:** BingeMode is actively being developed. The app and its features may be incomplete, change over time, or contain bugs.

## Features

- Discover movies and TV shows with curated feeds.
- Search for movies and series using TMDB.
- Keep a personal watchlist.
- Track watched episodes and season progress.
- Configure scheduled auto-checks for shows.
- Receive notifications for supported reminders and automation.
- Customize the app's appearance and display preferences.
- Export and import backups of your show list and supported settings.

## TMDB setup

A TMDB API key is optional for exploring the app's demo feed, but it is needed for live search and for retrieving cover art, episode names, and overview metadata.

1. Create an account at [The Movie Database](https://www.themoviedb.org/).
2. Generate a developer API key from your account settings.
3. Open BingeMode and go to **Settings**.
4. Expand the integrations section, enter your TMDB API key, and tap **Save Key**.

Keep your API key private. Do not commit it to the repository.

## Build

BingeMode is an Android project built with Kotlin and Jetpack Compose.

1. Clone this repository.
2. Open the project in Android Studio.
3. Allow Gradle to sync and install any required Android SDK components.
4. Select the **app** run configuration and run it on an Android device or emulator.

### Project requirements

- Android minimum SDK: **24**
- Compile SDK: **36**
- Target SDK: **36**
- Application ID: `com.jbuilds.bingemode`

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Navigation
- Room for local database storage
- Retrofit and OkHttp for network requests
- Moshi for JSON serialization
- Coil for image loading
- Kotlin Coroutines and Flow

## Data and backups

Show and episode-tracking data is stored locally using Room. BingeMode also provides backup export and import in Settings. Keep your own backup copies when moving between devices or reinstalling the app.

## Contributing

BingeMode is under active development. Bug reports, feedback, and contributions are welcome. Please check the current implementation before assuming a feature or behavior is finalized.

## Third-party services

Movie and television metadata is provided through [The Movie Database (TMDB)](https://www.themoviedb.org/). BingeMode is not endorsed or certified by TMDB.
