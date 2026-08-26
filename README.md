# Jamais Vu

An Android-first hidden-gem discovery app inspired by the interaction model of Pao: positive-only recommendations, city discovery, social profiles, follows, user gems, location boards, saved places, a personal travel diary, and a giant **GO** button whose sole purpose is to get the phone out of your face.

This is an independent implementation. It does not use Pao branding, private APIs, source code, or assets.

## Included

- Jetpack Compose / Material 3 dark UI
- Home feed of recommended gems
- City-based Discover grid
- Search by place, neighborhood, category, or creator
- Quick category filters and all-time-favorite strip
- Email/password accounts
- Public creator profiles and follow/unfollow
- Cloud-synced gems, Want to Go, Been There, and follows
- Photo upload to cloud storage
- Add-a-gem flow with Android document photo picker
- Profile and personal location boards
- Share and external map/directions intents
- Local/demo fallback when no backend is configured

## Backend

The social layer uses Supabase Auth, Postgres/PostgREST, Row Level Security, and Storage. The complete schema is in:

`supabase/migrations/001_social.sql`

Apply that migration to a Supabase project and add these Actions secrets to the repository:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

The client deliberately uses only the anonymous/public project key plus each signed-in user's JWT. The service-role key does not belong in an APK unless you enjoy giving strangers the keys to the municipal sewage plant.

Without those two values the app still builds and runs using its local/demo fallback.

## Release signing

Push/workflow-dispatch release builds reconstruct a temporary PKCS#12 keystore from the repository's split signing material and verify the resulting signing certificate before Gradle signs anything.

Consumed repository secrets:

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEYSTORE_OWNER`
- `KEYSTORE_SHA1`
- `KEYSTORE_SHA256`
- `KEYSTORE_PRIVATE`
- `KEYSTORE_PUBLIC`
- `KEYSTORE_CHAIN`
- `KEYSTORE_RSA`

`KEYSTORE_PUBLIC` may be a raw public key; the workflow then obtains the X.509 signing certificate from `KEYSTORE_CHAIN`. SHA-1 and SHA-256 are treated as authoritative identity checks. Signing material is written only under the Actions runner's temporary directory and removed after the build.

Pull requests build unsigned debug APKs because repository secrets are intentionally unavailable to untrusted PR contexts.

## Package

`com.hereliesaz.jamaisvu`

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 37.
