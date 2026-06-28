# Claude Usage

A premium, dark-first Android app that signs in with your Claude subscription and
shows your usage at a glance — the rolling **Session (5h)**, **Weekly**, and
**Sonnet Weekly** limits — with a home-screen widget, reset notifications, and a
one-tap **Ping Claude** button that opens a fresh chat to start a new session.

<p align="center"><em>Built with Kotlin · Jetpack Compose (Material 3) · Glance widgets · WorkManager</em></p>

---

## Features

- **Usage dashboard** — each limit gets its own progress bar in a distinct accent
  colour (coral / periwinkle / teal) so adjacent bars never blend, plus the
  percent used and a live reset countdown.
- **Sign in with Claude** — OAuth (PKCE) login using the same flow as Claude Code.
  Your password never touches the app; tokens are stored encrypted at rest.
- **Home-screen widget** — a resizable Glance widget mirroring the dashboard, with
  its own **Ping** pill.
- **Reset notifications** — a background worker watches your windows and notifies
  you the moment a limit rolls over.
- **Ping Claude** — opens a brand-new conversation (in the Claude app if installed,
  otherwise claude.ai) with “Hi” pre-filled to kick off a fresh 5-hour session.
- **Always looks complete** — bundled sample data renders before the first login
  and as a graceful fallback, so the UI is never empty.

## Getting the APK

Every push is built by **GitHub Actions** (`.github/workflows/android.yml`) and a
ready-to-install APK is attached to the rolling **“Latest build”** release:

1. Open the repository’s **Releases** page → **Latest build**.
2. Download **`ClaudeUsage-release.apk`** onto your phone.
3. Allow “install from unknown sources” when prompted, then open it.

The same APKs are also available under each workflow run’s **Artifacts**.
Tagging a commit `v1.2.3` publishes a full versioned release.

> The build is signed with a committed debug keystore so the APK installs without
> any secrets. Swap in your own upload key for Play Store distribution.

## Building locally

You need the Android SDK (Android Studio or command-line tools):

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

## How sign-in & usage work

The dashboard data comes from signing in with your Claude subscription via OAuth
(PKCE, no client secret), mirroring Claude Code. After you approve access in the
browser you paste the returned code back into the app; tokens are then refreshed
automatically.

> **Heads-up — unofficial endpoint.** The Session/Weekly usage figures are not a
> formally documented public API. The relevant endpoints live in one place,
> [`OAuthConfig.kt`](app/src/main/java/com/claudeusage/app/auth/OAuthConfig.kt),
> and the response is parsed leniently in
> [`UsageResponseParser.kt`](app/src/main/java/com/claudeusage/app/data/UsageResponseParser.kt).
> If Anthropic changes the flow, update those two files — everything else keeps
> working, and the app falls back to the last known / sample data in the meantime.

## Project layout

```
app/src/main/java/com/claudeusage/app/
├── auth/        OAuth (PKCE), encrypted token store, redirect handling
├── data/        Repository, API client, lenient parser, cache, sample data
├── notify/      Reset notifications, WorkManager scheduling, boot receiver
├── ping/        "Ping Claude" deep-link
├── ui/          Compose dashboard, theme, sign-in sheet, ViewModel
├── widget/      Glance home-screen widget
├── ClaudeUsageApp.kt
└── MainActivity.kt
```

## Tech

- **Kotlin 2.0**, **AGP 8.7**, Gradle 8.14 (wrapper committed)
- **Jetpack Compose** + **Material 3**, dark-first warm-neutral theme
- **Glance** for the widget, **WorkManager** for background refresh & alerts
- **OkHttp** + **kotlinx.serialization**, **Jetpack Security** for encrypted tokens
- `minSdk 26`, `targetSdk 35`

## Permissions

`INTERNET`, `ACCESS_NETWORK_STATE` (fetch usage), `POST_NOTIFICATIONS` (reset
alerts), `RECEIVE_BOOT_COMPLETED` + `WAKE_LOCK` (re-arm background refresh).

---

*Not an official Anthropic product. “Claude” is a trademark of Anthropic.*
