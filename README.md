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
- Compressed photo upload
- Add-a-gem flow with Android document photo picker
- Profile and personal location boards
- Share and external map/directions intents
- Local/demo fallback when no backend is configured

## Canonical place catalog

The initial New Orleans catalog is the supplied QuarterMuse database:

`cloudflare/seed/quartermuse_master_v11.csv`

It contains 143 venues with exact latitude/longitude and the original semicolon-separated category tags. The import deliberately preserves those fields. A broad Jamais Vu display category is derived only for the existing top-level filters; the original tags remain intact and searchable by the backend.

Generate an idempotent D1 seed SQL file with:

```bash
cd cloudflare
npm run seed:sql > .quartermuse-seed.sql
```

## Backend: Cloudflare Free, fail-closed

Jamais Vu no longer depends on Supabase or Firebase. The social backend is implemented as a Cloudflare Worker plus two D1 databases:

- `DB` — accounts, profiles, gems, follows, saves, visits, sessions, coordinates, and tags
- `MEDIA` — aggressively compressed JPEG image blobs

R2 is intentionally **not** used. The design is meant for Cloudflare's free Workers/D1 tier and fails when free capacity is exhausted rather than relying on paid storage overages.

The media database has additional application limits:

- maximum image upload: 750,000 bytes
- Android targets 700,000 bytes or less before upload
- application-wide media stop: 400,000,000 bytes, leaving headroom below the D1 Free per-database storage ceiling

The Worker/API lives under `cloudflare/` and provides:

- account creation, sign-in, refreshable sessions
- public profiles and gems
- follows
- Want to Go / Been There state
- gem publishing
- photo upload and serving
- public snapshots for discovery

Raw passwords are never sent to the Worker. Android derives a PBKDF2-HMAC-SHA256 password proof locally; the Worker salts and hashes that proof again before storing it. Bearer and refresh tokens are stored only as hashes.

### Deploying the free backend

A Cloudflare account is required, but no paid backend product is required by this architecture.

```bash
cd cloudflare
npm install
npx wrangler login
npx wrangler deploy
npm run migrate:remote
npm run seed:sql > .quartermuse-seed.sql
npx wrangler d1 execute DB --remote --file=.quartermuse-seed.sql
rm .quartermuse-seed.sql
```

`wrangler.jsonc` declares the `DB` and `MEDIA` bindings. Current Wrangler versions can provision missing D1 bindings during deployment; if your account or Wrangler version asks you to create them explicitly, create `jamaisvu` and `jamaisvu-media` and put their IDs in the corresponding entries in `wrangler.jsonc`.

After deployment, add the Worker's public origin as this GitHub Actions secret:

- `CLOUDFLARE_API_URL`

Example value: `https://jamaisvu-api.<your-subdomain>.workers.dev`

Release builds embed only that public API origin. No Cloudflare API token or account credential is placed in the APK.

If `CLOUDFLARE_API_URL` is absent, the Android app still builds and uses its local/demo fallback.

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
