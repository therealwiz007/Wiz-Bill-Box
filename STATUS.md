# STATUS — Wiz Bill Box 2.0.0 (5), branch `redesign/wiz-bill-box-v2.0.0`

## Drift from the handoff (found 2026-11-26)
- `main` was at `b5cb8b4` as expected; `.gitignore` had the user's uncommitted `*.jks`/`*.keystore` lines. Left untouched and uncommitted.
- README said 1.0.1 (2) while the build was 1.1.0 (4): README corrected.
- CI secrets are `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (handoff said `KEYSTORE_FILE`). The workflow already works and was not changed; `app/build.gradle.kts` reads `KEYSTORE_FILE` (a path the workflow sets after decoding).
- No `STATUS.md`, no `hi-IN` listing and no wiz-update/ads module existed. `wiz-ads/sync.sh --no-sdk` and the stubs already existed (0.3), so nothing had to be built there.
- Old art was PNGs from `tools/StoreArt.java` (Java2D, yellow theme): deleted with the tool.

## Decisions taken (no questions asked)
- Kept `WizPalette` field names (accent = emerald, accentInk, warranty, warrantyInk) so existing call sites keep compiling. Light `bad` darkened to `#CC2020` (4.47:1 failed 4.5:1).
- Fonts: Manrope variable + IBM Plex Mono Medium, subset to Latin + ₹ with fontTools (84 KB total), from github.com/google/fonts (OFL). Licences in `store-assets/FONT_LICENSES.txt`.
- Icons: own 40-glyph stroke set in `ui/Icons.kt` with the same `Icons.Rounded.X` call shape; `material-icons-extended` removed.
- Icon concept A (filing box + receipt + shield) chosen from 3 (`store-assets/icon-concepts/`): matches Tally, reads at 48 px, B is a generic receipt like competitors, C is a generic security shield.
- Tally: receipt zig-zag is on the receipt's top edge (visible above the lid) rather than the bottom. Brain returns a `Line` key; UI maps it to strings (translatable).
- Save confirmation = Tally card at the bottom for 2.4 s (replaces the old full-screen receipt print). Old `ui/art/` removed.
- Notification dedupe uses SharedPreferences (`reminders`) so the receiver never writes `bills.json` while the app runs. A bill added with 5 days left gets only the 7-day reminder.
- N4 `bill.json` `files` entries carry `id,name,mime,driveId` (superset of `{name,mime}`); old readers ignore them.
- Editor Save looks muted when invalid but stays tappable, so errors can be shown inline.
- Two-pane (>=840 dp) disables shared-element hero transitions to avoid duplicate keys.
- Hindi strings are machine translation, not native-reviewed.
- Notification permission is asked on the first warranty save (Android 13+) and from Settings › Reminders.
- Fixed all-screen strings into resources (195 strings, 5 plurals, EN + hi).

## Results (2026-11-26)
- Baseline (main): `testDebugUnitTest assembleDebug` PASS.
- Final: `:app:testDebugUnitTest` 89 tests, 0 failures (includes shared wiz-ads / wiz-update tests); `:app:assembleRelease` PASS, unsigned APK 1.55 MB (limit 6 MB).
- Ads check: `grep -rE "com\.google\.android\.(libraries\.ads|ump)" app/src` = 0; releaseRuntimeClasspath has no ads/UMP SDK; merged permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED (no AD_ID).
- Emulator `wiz35` (API 35, 1080x2400): Home, Detail, Editor (photo, PDF), Settings, Expiring filter, search, Hindi + dark verified by screenshots (`store-assets/raw/`, `store-assets/qa/2026-11-26/`).
- N3 verified: with a mixer bill ending in 4 days and a washer in 16 days, firing `ReminderReceiver` posted "Warranty ends in 4 days" and "…in 16 days" (shade screenshot). The 10:00 alarm was armed for next day (`dumpsys alarm`).
- Contrast (ratio): light text/bg 16.4, muted/bg 5.2, accentInk/bg 4.8, warrantyInk/surface 5.5, warn/bg 4.8, bad/bg 5.0 (after fix); white on emerald 3.4 (large bold text only); dark text 17.2, muted 7.1, onPrimary/primary 8.4, warn 8.4, bad 6.9.
- Performance (emulator, NOT a phone): release APK signed with the debug key, speed-profile, empty data: cold start `am start -W` 1.7–2.6 s (median ~2.1 s) — the 1200 ms target is NOT met on this emulator; debug first launch with data was 1.47 s. Debug PSS 78 MB. Jank and idle CPU on release not measured (debug jank on the emulator is meaningless).

## NOT TESTED / NOT DONE
- NOT TESTED: Google sign-in and Drive sync (emulator lacks the registered SHA-1; test on the user's phone with a Play- or upload-signed build). Restore, conflict rule and Drive-bin delete rely on unchanged `BillRepository` rules plus new PDF handling that has only unit-level coverage.
- NOT TESTED: tapping a reminder opens the bill; reboot re-arm; picking a PDF via the system picker (seeded PDFs only); camera capture; tablet two-pane; RTL (`ar`); 200 % font; TalkBack; reduce-motion run; API 26/29 devices; icon on real launchers/themed icons.
- NOT DONE: splash branding wordmark image (needs vector text; the splash shows the animated icon only). `baseline-prof.txt` not regenerated. Release cold-start target (see above). Native review of Hindi.
- Tablet/other-locale screenshots: none made (none existed).
