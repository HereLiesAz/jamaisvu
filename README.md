# Jamais Vu

Jamais Vu is an Android catalog for the fixed QuarterMuse venue database. The app does not accept user-submitted places and does not use a cloud database, account system, or social backend.

## Canonical content

The only places in the app come from:

`app/src/main/assets/quartermuse_master_v11.csv`

Each row contributes exactly:

- venue name
- latitude
- longitude
- the original semicolon-separated category tags

The app does not invent descriptions, reviews, ratings, neighborhoods, creators, or additional venues.

## What the app does

- Search the catalog by venue name or original tag
- Filter by any tag present in the CSV
- Open the exact catalog coordinates in the user's maps app
- Mark catalog places locally as Saved or Been There
- Show real Google Maps place photos for catalog venues when Google Places is configured
- Show up to five venue-associated photos on the detail screen
- Display Google Maps and third-party/author attribution alongside Google-sourced photos

Saved/Been There state is device-local SharedPreferences data. It does not create or modify catalog content.

## Google Places photos

Jamais Vu uses Places SDK for Android (New) `5.3.0` only to enrich the fixed catalog with real venue photography.

The lookup path is intentionally conservative:

1. Search for the CSV venue name, geographically biased to that row's latitude/longitude.
2. Request only the Google Place ID from Text Search.
3. Persist the Place ID, which Google permits applications to cache indefinitely.
4. Fetch current photo metadata only when a visible venue actually needs a photo.
5. Resolve only the photo URIs that are going to be displayed.
6. Do not persist photo metadata, photo names, or resolved photo URIs.
7. Coil disk caching is disabled for Google photo content.

A list card requests one image. Opening the venue can request up to five images, all associated with that same Google Place.

### API key

Create a Google Maps Platform key with **Places API (New)** enabled, restrict it to the Android application, and add it to the repository as:

`GOOGLE_PLACES_API_KEY`

Android application restriction values:

- package: `com.hereliesaz.jamaisvu`
- signing certificate: use the release certificate SHA-1 that corresponds to this repository's signing keystore

The app still builds and the catalog still works if the key is absent; only Google photos are unavailable.

### Preventing charges

Google Maps Platform requires billing to be enabled for Places SDK. If the goal is a $0 operating bill, use Cloud Console quota limits rather than relying on budget alerts. Quotas stop requests when their configured limit is reached; budget alerts do not stop usage.

The relevant current free usage caps include:

- Text Search Essentials (IDs Only): unlimited
- Place Details Pro: 5,000/month
- Place Details Photos / photo usage: 1,000/month

Set quotas below the applicable free caps, leaving margin for quota/billing accounting differences. The app also resolves photos lazily instead of prefetching the entire catalog.

## Google Maps Platform compliance

Google-sourced images are visually identified as **Google Maps** content. Photo metadata attribution and author attribution are displayed with the image when Google returns them. Place IDs are the only Google Places content persisted by the app.

When publishing the application, provide public Terms of Use and Privacy Policy URLs that satisfy Google Maps Platform requirements.

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

Signing material is written only under the Actions runner's temporary directory and removed after the build.

## Package

`com.hereliesaz.jamaisvu`

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 37.
