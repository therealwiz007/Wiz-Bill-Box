# Wiz Bill Box

Wiz Bill Box is a Wiz Labs Android app for saving bills. Take a photo of a bill, add the store, the product and the invoice date, and the app stores it in **your own Google Drive**. Sign in with the same Google account on any phone, or after reinstalling, and every bill comes back.

- **Package:** `com.wizlabs.billbox`
- **Launcher name:** Wiz Bill Box
- **Play title:** Wiz Bill Box: Bills & Warranty
- **Tech:** Kotlin, Jetpack Compose, Google Identity Services, Drive REST v3
- **Version:** 1.0.1 (versionCode 2)

## Features

- **Add a bill:** take photos with the camera or pick several from the gallery. Photos are kept upright and resized to 2400 px so they stay readable but upload quickly.
- **Required fields:** store name, product / items, invoice date (date picker). Store names you've used before are suggested as you type.
- **Optional fields:** amount (₹), invoice number, category, payment mode, warranty (6 months to 5 years or a custom date), GSTIN, notes.
- **Google Drive storage and restore:** each bill is saved to `My Drive › Wiz Bill Box › <date> · <store> · <product>/`. That folder holds the photos plus a `bill.json` with the details.
- **Two-way sync:** adds, edits and deletes are synced between phones. Deleted bills go to the Drive bin, not hard-deleted.
- **Offline first:** everything is saved on the phone straight away. Uploads happen when you're online (when the app opens, after each save, or on pull-to-refresh).
- **Find bills fast:** search by store, product, invoice number, notes or GSTIN; filter by category; sort by date, recently added, amount or store. Bills are grouped by month.
- **At a glance:** total bills, amount spent this year, active warranties, and a "warranty ending soon" card (7–90 days, set in Settings).
- **Bill details:** swipe through photos, view full screen with pinch-zoom, share photos and details, open the bill in Google Drive, edit, delete.
- **Settings:** account, sync status, open the Drive folder, sign out or switch account, theme (system/dark/light), privacy policy.

## How the Google Drive storage works

| | |
|---|---|
| Scope | `drive.file` only. The app can see the files it creates and nothing else in the user's Drive. It's a non-sensitive scope, so there's no Google security assessment. |
| Sign-in | `Identity.getAuthorizationClient(...).authorize()` from play-services-auth 21.4.0. Google shows its own account picker and consent screen. |
| Restore | After sign-in, the app lists every `bill.json` it created (by `appProperties`), downloads the details, and fetches photos when they're first shown. |
| Conflicts | A change made on this phone always wins until it has been uploaded. Otherwise the newer `updatedAt` wins. |
| Sign out | Revokes the app's Drive access and clears the phone's copy. Bills stay in Drive. |

The code is in `data/BillRepository.kt` (sync), `drive/DriveApi.kt` (REST) and `drive/DriveAuth.kt` (sign-in).

## ⚠️ One-time Google Cloud setup (required before sign-in works)

Google only lets an app use Drive if the app's package name and signing certificate are registered in a Google Cloud project. Until you do this, tapping **Sign in** shows *"Google sign-in isn't set up for this build yet"*. Everything else in the app works without it.

1. Go to <https://console.cloud.google.com/> and create a project, e.g. **Wiz Bill Box**.
2. **APIs & Services › Library** → enable **Google Drive API**.
3. **APIs & Services › OAuth consent screen** (Google Auth Platform):
   - User type **External**, app name **Wiz Bill Box**, your support email, and the Wiz Labs logo.
   - Add the scope `.../auth/drive.file` (and `email` and `profile`).
   - Add your privacy-policy URL (host `privacy-policy.html`, e.g. on GitHub Pages).
   - While testing, add your own Gmail under **Test users**. Click **Publish app** before the Play release. `drive.file` doesn't need verification.
4. **Credentials › Create credentials › OAuth client ID › Android**, once for **each** signing key:
   - Package name: `com.wizlabs.billbox`
   - SHA-1, from the list below:

   | Build | Where to get the SHA-1 |
   |---|---|
   | Debug (this Mac) | `C8:BF:06:F2:51:54:19:44:49:F2:4F:7C:20:E0:3E:FF:26:EB:8D:EE` |
   | Your release keystore | `keytool -list -v -keystore <your.jks> -alias <alias>` |
   | Google Play | Play Console › Test and release › App integrity › **App signing key certificate** |

   You don't need to put a client ID in the code. Google matches the app by package name and SHA-1.

## Build

The Gradle wrapper download is blocked on this Mac, so use the local Gradle:

```bash
~/.local/opt/gradle-8.11.1/bin/gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Note that play-services-auth comes from Google Maven, so the first build needs internet. After that, `--offline` works.

Signed release builds come from GitHub Actions (`.github/workflows/android.yml`), using the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` secrets. It uploads two artifacts: `Wiz Bill Box (<version>) APK` and `Wiz Bill Box (<version>) AAB`.

## Play Store

All listing assets are ready: icon, feature graphic and 7 screenshots in `store-assets/`, listing text and release notes in `fastlane/metadata/android/en-US/`, and step-by-step Play Console answers (including the Data safety form) in [`store-assets/PLAY_CONSOLE_GUIDE.md`](store-assets/PLAY_CONSOLE_GUIDE.md). Regenerate art with `tools/StoreArt.java`. Screenshots use a debug-only demo mode (a `demo-mode` file in the app's storage turns off sync).


- **Data safety:** photos and the bill details the user enters are stored in the user's own Google Drive (encrypted in transit). No data is collected by or shared with Wiz Labs. No ads and no analytics.
- **Permissions:** `INTERNET` and `ACCESS_NETWORK_STATE` (for Drive). The camera is used through the system camera app, so the app doesn't request the camera permission. The gallery uses the system photo picker, so it doesn't request storage permission either.
- **Privacy policy:** `privacy-policy.html` (also bundled at `app/src/main/assets/` and shown in the app).

## Project layout

```
app/src/main/java/com/wizlabs/bills/
  MainActivity.kt          navigation, Google consent launcher
  AppViewModel.kt          sign-in, sync scheduling, error messages
  model/Models.kt          Bill, BillPhoto, Category, PaymentMode, AppData
  data/BillRepository.kt   local edits + Drive sync (push, pull, deletes)
  data/Codec.kt            JSON for the local store and Drive bill.json
  data/Store.kt            atomic local JSON file
  data/Photos.kt           photo import (EXIF rotation, resize) and cached decoding
  drive/DriveApi.kt        minimal Drive v3 REST client
  drive/DriveAuth.kt       Google Identity AuthorizationClient wrapper
  ui/screens/              Welcome, Home, BillEditor, BillDetail, Settings/Privacy
```
