# Wiz Bill Box — Google Play Console guide

Everything needed to publish, in the order Play Console asks for it.

## 0. Before you start (Google sign-in)
Drive sign-in only works once the Google Cloud project is set up (see the README):
1. Enable the **Google Drive API**.
2. OAuth consent screen: app name **Wiz Bill Box**, support email, privacy-policy URL (below), scopes `drive.file`, `email`, `profile`. Click **Publish app** (no verification is needed for `drive.file`; leave the logo empty so Google doesn't ask for brand verification).
3. Create **Android** OAuth clients for `com.wizlabs.billbox` with these SHA-1s:
   - Upload key (shared Wiz keystore, alias `wizfueldensity`): `F1:81:C6:7C:2D:2B:C5:A2:41:CC:25:28:5D:D9:5D:37:E0:22:50:02`
   - Play **App signing key**: Play Console › Test and release › App integrity (available after the first upload)
   - Debug (testing on this Mac): `C8:BF:06:F2:51:54:19:44:49:F2:4F:7C:20:E0:3E:FF:26:EB:8D:EE`

## 1. App details
| Field | Value |
|---|---|
| App name | Wiz Bill Box: Bills & Warranty |
| Package | `com.wizlabs.billbox` |
| Default language | English (United States) — en-US |
| App or game | App |
| Free or paid | Free |
| Category | Productivity (Finance also fits) |
| Tags (up to 5) | Receipt organizer, Productivity, Document manager, Tools, Personal finance |
| Contact email | *your Wiz Labs support email* |
| Privacy policy URL | https://therealwiz007.github.io/Wiz-Bill-Box/privacy-policy.html |

## 2. Store listing (text is in `fastlane/metadata/android/en-US/`)
- **Short description** → `short_description.txt` (79/80 characters)
- **Full description** → `full_description.txt`
- **Release notes** → `changelogs/<versionCode>.txt` (`2.txt` for 1.0.1)

## 3. Graphics (all in `store-assets/`)
| Asset | File | Play requirement |
|---|---|---|
| App icon | `icon-512.png` | 512×512 PNG, no transparency ✓ |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 PNG ✓ |
| Phone screenshots (7) | `screenshots/01-home.png` … `07-light.png` | 2–8 images, 9:16, 1080×1920 ✓ |
| Logo (website/docs) | `logo-1600x480.png` | not uploaded to Play |

Upload order: Home → Add → Detail → Warranty → Sign in → Drive → Light.
The shop names, bills and the account in the screenshots are made up.

## 4. App content (Policy → App content)
| Section | Answer |
|---|---|
| Privacy policy | URL above |
| Ads | **No**, my app does not contain ads |
| App access | **All functionality is available without special access**. Every feature works with "Use on this phone only"; Drive backup uses any personal Google account, no Wiz Labs login |
| Content rating | IARC questionnaire, category **Utility, Productivity, Communication or other**; answer **No** to everything. Expected: **Everyone / 3+** |
| Target audience | **18 and over** (13+ is also fine) |
| News app | No |
| Government app | No |
| Financial features | My app doesn't provide any financial features |
| Health | No health features |
| Advertising ID | **No** |

## 5. Data safety form
Bills are sent from the phone **to the user's own Google Drive** (Google's servers), never to Wiz Labs. Declare that transfer to be safe and accurate:

**Data collection and security**
- Does your app collect or share any of the required user data types? → **Yes**
- Is all of the user data collected by your app encrypted in transit? → **Yes** (HTTPS to Google APIs)
- Do you provide a way for users to request that their data is deleted? → **Yes** (delete bills in the app, delete the Drive folder, or revoke access in the Google Account)

**Data types** (each: *Collected* ✓, *Shared* ✗, *Processed ephemerally* ✗, *Optional* — users can use the app without signing in, *Purpose:* **App functionality**)
| Category | Type | Why |
|---|---|---|
| Photos and videos | **Photos** | bill photos saved to the user's Drive |
| App activity | **Other user-generated content** | store, product, date, amount, notes saved to Drive |
| Personal info | **Email address** | shown in Settings to identify the signed-in Google account |

Nothing is shared with third parties, no analytics, no ads.

## 6. Release
1. Production (or **Internal testing** first, recommended) → Create new release.
2. Use **Play App Signing** (default). Upload `Wiz Bill Box (<versionName>).aab` from the GitHub Actions artifact **Wiz Bill Box (<versionName>) AAB** (the phone APK is in the separate **… APK** artifact).
3. Release name: `<versionName> (<versionCode>)`, e.g. `1.0.1 (2)`; notes from `changelogs/<versionCode>.txt`.
4. After the first upload, copy the **App signing key SHA-1** into a new Android OAuth client (step 0), then install from Play on your phone and test sign-in, upload and restore (sign in on a second phone or after reinstalling).
5. Countries: India (and any others you want).

## 7. Before the next update
- Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
- Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
- Regenerate art if the design changes:
  `java tools/StoreArt.java icon store-assets/icon-512.png 512` (also `feature`, `logo`, `frame`; see the header of `tools/StoreArt.java`).
