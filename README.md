# Jamais Vu

An Android-first hidden-gem discovery app inspired by the interaction model of Pao: positive-only recommendations, city discovery, user gems, location boards, saved places, a personal travel diary, and a giant **GO** button whose sole purpose is to get the phone out of your face.

This is an independent implementation. It does not use Pao branding, private APIs, source code, or assets.

## Included

- Jetpack Compose / Material 3 dark UI
- Home feed of recommended gems
- City-based Discover grid
- Search by place, neighborhood, category, or creator
- Quick category filters and all-time-favorite strip
- Add-a-gem flow with Android document photo picker
- Persistent user-created gems
- Persistent Want to Go / Been There state
- Profile and location boards
- Share and external map/directions intents
- Demo content so the app is immediately usable

## Architecture

The current build is deliberately backend-free: demo data plus local `SharedPreferences` persistence for user gems and travel state. That makes the APK fully usable without API keys while leaving the social/cloud layer as the next clean seam rather than a ceremonial Firebase dependency waiting to become somebody's problem.

Package: `com.hereliesaz.jamaisvu`

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 37.
