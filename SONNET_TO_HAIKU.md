# SONNET → HAIKU handoff — Wiz Bill Box 2.0.0 (5)

## Implementation summary
- §9–11 identity: "Paper & Emerald" tokens (light-first, dark), Manrope + IBM Plex Mono (84 KB), own icon set (N8, icons-extended removed).
- §12 every screen redesigned: Welcome (3 pages + inline error/Retry), Home (sync pill, search pill, All/Under warranty/Expiring/Expired segments with counts, stats with rolling numbers, warranty strip, category chips, sticky month headers, extended FAB, pull-to-refresh, empty states, sync-error sheet), Editor (camera/gallery/PDF, required fields, collapsible Details with 4×3 category grid, GSTIN check, warranty chips, notes), Detail (photo pager with dots, PDF tiles, warranty ring card with reminder status, mono details, bottom action bar, viewer with double-tap zoom and swipe-down close), Settings (account, backup with Tally, reminders, window slider, CSV export, appearance, privacy, about), tablet two-pane ≥840 dp.
- §13–14 Tally: code-drawn Compose mascot (8 expressions, 11 actions), shared throttled loop clock, still frame under reduce-motion/Battery Saver/idle/unfocused; pure-Kotlin `TallyBrain` with 16 tests.
- §15 text-free launcher icon, monochrome layer, animated splash.
- §6: N3 reminders, N4 PDF bills, N5 filter, N6 CSV, N7 update popup + ads-ready (ENABLED=false, wiz-ads 0.3 --no-sdk), N8.
- Ads: slots home (native), settings (banner), adfree (rewarded row) render nothing; `AdsSetupTest` green; no AdMob/UMP SDK.

## Branch + last commit SHA
`redesign/wiz-bill-box-v2.0.0` @ `730a1bb` (code and assets). Later commits on the branch are documentation only (this file, the Haiku prompt).

## Modified files (`git diff --stat main...HEAD`, summary)
```
 147 files changed, 5033 insertions(+), 1829 deletions(-)
```
Full list: run `git diff --stat main...HEAD`.

## New assets (replaced, not added to)
- Replaced: `app/src/main/res/drawable/ic_launcher_{foreground,background,monochrome}.xml`, `ic_splash.xml` (animated), new `ic_stat_receipt.xml` (notification), `store-assets/icon-512.png`, `store-assets/feature-graphic-1024x500.png`, `fastlane/.../en-US/images/{icon.png,featureGraphic.png}`.
- Deleted: `store-assets/screenshots/01-home … 07-light.png`, `fastlane/.../images/phoneScreenshots/01…07*.png`, `store-assets/logo-1600x480.png`, `tools/StoreArt.java`, old PNG icon/graphic copies. No old-style image remains (`git ls-files | grep -iE "\.(png|webp|jpg)$"` reviewed: only the new files below).
- New: `store-assets/screenshots/1.png…8.png` (1080×1920, §22 order, real emulator captures in `store-assets/raw/`), copies in `fastlane/.../images/phoneScreenshots/1…8.png`, `store-assets/mascot/tally.svg` + `tally-512.png` + `tally-1024.png`, `store-assets/icon.svg`, `store-assets/icon-concepts/*` (3 concepts + compare.png), `store-assets/CAPTIONS.md`, `store-assets/qa/2026-11-26/notification-shade.png`. Sources: `store-assets/{feature,frame}.html`, `store-assets/render.sh`.
- No tablet or hi-IN graphics exist: Play uses the default art for hi-IN.

## Final architecture
Unchanged: single activity, `AppViewModel`, `BillRepository` sync rules, Drive REST, `drive.file`. New: `engine/{TallyBrain,WarrantyStatus}`, `reminders/{WarrantyScheduler,WarrantyAlarms}` (+ReminderReceiver, BootReceiver), `data/{Files,CsvExport}`, `ui/mascot/*`, `ui/Icons.kt`, `ui/theme` (tokens, fonts), `AdsSetup.kt`, `WizHooks.kt`, shared `com.wizlabs.ads` (0.3 no-sdk) and `com.wizlabs.update` (1.0).

## Application ID / version / SDKs
`com.wizlabs.billbox` · versionName `2.0.0` · versionCode `5` · minSdk 26 · targetSdk 36 · compileSdk 36 (from `app/build.gradle.kts`).

## Signing status
Release signing is configured only by CI env/secrets `KEYSTORE_BASE64` (decoded by the workflow to a file passed as `KEYSTORE_FILE`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in `.github/workflows/android.yml`. No keystore or secret touched. The release build made here is **unsigned** (local only); one copy was signed with the Android debug key on this Mac for emulator timing only and is not shipped.

## Build results
- `~/.local/opt/gradle-8.11.1/bin/gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleDebug` — PASS (baseline on main and final).
- `... :app:assembleRelease` — PASS, unsigned APK 1,624,217 bytes (1.55 MB).

## Test results
- Unit tests: 89 run, 0 failed (own: TallyBrain 16, WarrantyScheduler 15, CsvExport 6, CodecCompat 6, WarrantyStatus/filters 6, AdsSetup 7, CoreTest 6; rest are the shared ads/update suites).
- Emulator `wiz35` (API 35): PASS by screenshot — Home, segments/Expiring, search, Editor with photo and PDF tile, Detail with warranty ring, Settings with reminders/backup, Hindi + dark Home, reminder notifications posted for 4-day and 16-day warranties (`store-assets/qa/2026-11-26/`).
- NOT TESTED: see "Known issues".

## Acceptance checklist (§25)
- F1–F13: F1,F2,F3(code unchanged),F8,F9,F10,F11,F12 PASS on emulator by screenshot/code; F4–F7,F13 (Google sign-in, Drive save/sync/restore, skip) **NOT TESTED: needs the user's phone with Play/upload-signed build**.
- Old/new `bill.json` compatibility: PASS (CodecCompatTest).
- N3 notifications fire for 30/7/1 (logic unit-tested incl. once-only, DST, leap year); receiver posted real notifications on the emulator; survive reboot / open the bill: NOT TESTED.
- N4 PDF: preview/open code and tile PASS with a seeded PDF; picking from the system picker and Drive upload NOT TESTED.
- N5 counts PASS (test + screenshot). N6 CSV: unit-tested; opening in a spreadsheet app NOT TESTED.
- N7: no ad SDK (grep 0, dependency list clean), slots zero-height, `AdsSetupTest` green; `--ei wiz_fake_update 99` popup NOT TESTED.
- `material-icons-extended` removed PASS; release APK 1.55 MB (≤6 MB) PASS.
- Tally states unit-tested PASS; icon text-free PASS; light + dark complete PASS; Hindi complete (machine) PASS.
- §19: cold start **FAIL on emulator** (median ~2.1 s vs 1.2 s; not measured on a phone), contrast PASS (STATUS.md), others NOT TESTED.

## Permissions diff (merged manifest, release)
Before (1.1.0): INTERNET, ACCESS_NETWORK_STATE. Now: INTERNET, ACCESS_NETWORK_STATE, **POST_NOTIFICATIONS**, **RECEIVE_BOOT_COMPLETED** (plus the internal DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION). AD_ID and ADSERVICES permissions are removed.

## Data collected/shared (Data safety)
Google account email + name (shown in app; used only for Drive sign-in) and the user's bills/photos/PDFs stored in the **user's own Google Drive**; reminders are created on the phone. No data collected by or shared with Wiz Labs; no ads, no analytics, no advertising ID. Play services contact Google for sign-in and update checks. Changed since 1.1.0: reminders (local) and PDF bills (stored in the user's Drive). Code: `drive/DriveAuth.kt`, `drive/DriveApi.kt`, `reminders/*`, `data/Files.kt`.

## Privacy policy
https://therealwiz007.github.io/Wiz-Bill-Box/privacy-policy.html — both copies (`privacy-policy.html`, `app/src/main/assets/privacy-policy.html`) updated and identical (reminders, PDFs, Play update check, export, "no ads in this version; AdMob may come later"). Pages must redeploy after merge to main.

## ASO content
`fastlane/metadata/android/en-US/{title,short_description,full_description}.txt` and `hi-IN/...`; counts in `store-assets/LISTING_COUNTS.md` (en title 30/30, short 77/80, full 1160/4000, notes 356/500; hi title 27, short 74, full 1090, notes 243). Release notes: `changelogs/5.txt` per locale. hi-IN is machine translation.

## Known issues / blockers
- Release cold start on the emulator ~2.1 s (target 1.2 s); measure on a phone. Baseline profile not regenerated.
- Google sign-in/Drive untested on emulator (SHA-1). Test on the phone first.
- Splash wordmark branding image not made. Hindi not native-reviewed.
- Doze can delay reminders by hours (documented in the app).
- Previous Play state: 1.0.2 (3) published 2026-11-11; 1.1.0 (4) upload status unknown — verify in Play Console.

## Release instructions
Track: Closed testing (groups wiz-apps-dealers / wiz-labs-beta / wiz-labs-reddit). Release name `5 (2.0.0)`. Notes: `fastlane/metadata/android/en-US/changelogs/5.txt` (and hi-IN). Re-check declarations: Data safety (Google account email + user files in the user's own Drive; reminders and PDF bills are new), Ads = No, Notifications permission rationale (warranty reminders), target audience. Graphics: replace icon (`store-assets/icon-512.png`), feature graphic, delete old phone screenshots, upload `screenshots/1–8.png` in order. After the workflow builds, check the AAB versionCode 5 and CN=Wiz Labs signer.

## Decisions taken
See `STATUS.md` ("Decisions taken"). Nothing was asked of the user.
