# HAIKU release report: Wiz Bill Box 2.0.0 (5)

Date: 2026-11-26. Status: **release saved in Play, NOT sent for review.** Data safety / App content not verified.

## Version and build
- applicationId `com.wizlabs.billbox`, versionName 2.0.0, versionCode 5, minSdk 26, targetSdk 36, compileSdk 36 (`app/build.gradle.kts`).
- Branch `redesign/wiz-bill-box-v2.0.0` @ `f5e7349` (docs-only commits on top of `730a1bb`; the paste said 730a1bb).
- Unit tests (`:app:cleanTestDebugUnitTest :app:testDebugUnitTest`, fresh run): 89 tests, 0 failures, 0 errors (JUnit XML).
- `:app:assembleRelease` (local, unsigned): BUILD SUCCESSFUL, `app-release-unsigned.apk` 1,624,217 bytes.
- Merge: `main` b5cb8b4 -> 91cfaa6 (`--no-ff` merge of the release branch), pushed to origin.
- CI run 45791045171 "Wiz Bill Box - Production APK and AAB": success.

## AAB
- Local copy: `dist/ci/Wiz Bill Box (2.0.0).aab` (4,125,491 bytes; gitignored via `.git/info/exclude`, not in repo `.gitignore`).
- Signer: `CN=Wiz Labs, O=Wiz Labs, OU=Wiz Labs, L=Ambala, ST=Haryana, C=IN` (META-INF signature file is named `WIZFUELD.*`, which is the key alias).
- versionCode 5 / versionName 2.0.0 confirmed by Play Console after upload (bundle table: Version "5 (2.0.0)", Target SDK 36). Local `bundletool` check not done (download blocked by permission policy).

## Signing
- Release signing is read from CI secrets only (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). No secret or keystore printed, copied or changed.

## Compliance checks (from code, not from Play)
- Manifest permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED. AD_ID and ADSERVICES permissions removed.
- Ads: `AdsSetup.ENABLED = false`; no `ads-mobile-sdk` or `user-messaging-platform` in dependencies. Play "Contains ads" stays **No**.
- Privacy policy: `privacy-policy.html` and `app/src/main/assets/privacy-policy.html` identical; URL returns HTTP 200.
- Listing: title 30/30, short 77/80 (en), full 1160/4000. No "best", "top", "free", "#1" or price words found. Note: full description says "No ads are shown in this version."

## Store assets (uploaded to Play, default en-US listing)
- App icon: `store-assets/icon-512.png` (512x512). Verified in Play as icon-512.png.
- Feature graphic: `store-assets/feature-graphic-1024x500.png` (1024x500). Already listed under the same file name; not re-uploaded in this run.
- Phone screenshots: all 8 old screenshots deleted. New 1-8 uploaded and ordered: 1 Every bill safe in Drive, 2 Snap a bill, 3 Reminders before warranties end, 4 Expiring warranties, 5 PDF bills too, 6 Find any bill fast, 7 Restore on any phone, 8 Hindi and dark mode. Order confirmed on the listing page (8/8).
- Not done: no tablet screenshots, no hi-IN graphics (Play falls back to default art).

## Listing text (en-US default)
- Short: "Save bills & warranties to your own Google Drive. Get reminded before expiry." (confirmed on review page)
- Full: replaced with `fastlane/metadata/android/en-US/full_description.txt`.
- hi-IN listing text not pushed in this run (only release notes).
- AI asset declaration: "Don't label assets" selected.

## Release (Closed testing - Alpha track)
- Track: Closed testing - Alpha (id 4698930321457469073). Groups (wiz-apps-dealers / wiz-labs-beta / wiz-labs-reddit) are the tester groups already on this track.
- Release name: "5 (2.0.0)". Release notes en-US and hi-IN from `changelogs/5.txt`.
- Release saved in Play (Save, not sent). Publishing overview lists 4 pending changes: release 5 (2.0.0), short description, full description, phone screenshots. Play quick checks were running.
- Warning shown (non-blocking): no debug symbols uploaded for native code.
- Previous release 4 (1.1.0) remains live on this track until 5 is sent and approved.

## Not done (outstanding)
1. **Data safety / App content not checked.** The paste asks to re-check Data safety (new reminders and PDF bills stored in the user's Drive), Ads = No, the notification rationale and target audience. These were NOT opened in Play Console in this run. Sending for review before that check is risky.
2. **Not sent for review.** Publishing overview "Submit 4 changes for review" is still pending.
3. Production: not applied. Needs an explicit user yes. Play says 12 testers opted in for 10 days; 14 days required.
4. Cold start on emulator 2.1 s vs 1.2 s target (from STATUS.md). Not re-measured.
5. Sonnet untested items (Google sign-in/Drive, notification tap, reboot, PDF picker, CSV in spreadsheet, tablet, RTL, TalkBack, API 26/29) unchanged from SONNET_TO_HAIKU.md.
6. Hindi strings and listing are machine translation (not native-reviewed).

## Next steps
- Open App content > Data safety in Play Console, compare with the code, save any change, then Submit 4 changes for review on Publishing overview.
- Check the Closed testing status after submission. Do not claim it is published until Play shows it.
