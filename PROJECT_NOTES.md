# GracelandMiles — Project Notes

Living notes for picking this project back up after a break. Update this file whenever something significant changes, gets fixed, or gets learned the hard way.

## App overview

**What it does:** Shows the user's live distance to Graceland (and, for Pro users, other Elvis-related sites). Free version shows ads; Pro version ($2.99, one-time purchase) removes ads and unlocks extra locations.

- **Package name:** `com.adamselite.gracelandmiles`
- **Repo:** `graceland` (GitHub)
- **Stack:** Kotlin + Jetpack Compose (Material3), Google Play Billing Library 8.0.0, Google Mobile Ads (AdMob), Google Play Services Location
- **Min build tooling:** compileSdk/targetSdk 36, AGP 8.7.3, Gradle 8.10.2
- **Current versionCode: 10.** The next change should bump to **11**. Always increase this before every Play Console upload — Play rejects a re-used versionCode.

## Architecture / key files

- `app/src/main/java/com/adamselite/gracelandmiles/MainActivity.kt` — all UI (Compose). Main composable is `GracelandScreen`.
- `app/src/main/java/com/adamselite/gracelandmiles/BillingManager.kt` — wraps Google Play Billing. Exposes `isPro: StateFlow<Boolean>`, `connect()`, `loadProduct()`, `restore()`, `launchUpgrade()`.
- `.github/workflows/build.yml` — GitHub Actions CI. Builds a debug APK on every push; builds a *signed* release `.aab` too, if signing secrets are present (see below).
- Repo secrets (GitHub → Settings → Secrets and variables → Actions → **Repository secrets**): `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`gracelandmiles`), `RELEASE_KEY_PASSWORD` (same value as keystore password — PKCS12 requires it).
- The release keystore itself lives only on Randy's machine (`graceland-release.keystore` + `CREDENTIALS_KEEP_SAFE.txt`). **If this is ever lost, the app can never be updated again under the same Play listing.** Keep backed up somewhere safe (not just GitHub).

### Current UI flow (as of versionCode 10)

1. App opens showing time-of-day background photo, two clock lines (Elvis Time / Local Time), and the big "HOW FAR AWAY IS GRACELAND?" button.
2. Tap the button → permission check → location fetch → (every 4th free-tier tap) a full-screen interstitial ad → result appears.
3. **The button disappears** once a result is ready. In its place: "Graceland, Elvis's home, is **XX miles** in this direction" + a live compass dial (uses phone's orientation + location sensors) pointing at the destination. Tapping that panel resets back to the button so the user can measure again.
4. No separate compass icon/button anymore — it was folded into the result panel per tester feedback.
5. Pro users see a horizontal chip picker (Sun Studio, Tupelo birthplace, in addition to Graceland) above the button.
6. Back button (free users only) shows an exit-upsell screen with a 5-second countdown before the app closes; a "Stay in the app" link cancels it. Back is deliberately swallowed while that screen shows, so double-tapping Back can't bypass the countdown.
7. Small "eye" icon (top-right) — hold to fade all UI and see the clean background photo underneath.
8. Debug builds only: a "DEBUG: Pro ON/OFF" toggle (top-left) to flip Pro state locally without a real purchase, for testing both experiences.

## Hard-won gotchas (read before touching CI/Play Console again)

1. **`.github/workflows/` can silently accumulate a duplicate copy of the whole project.** At one point the entire `app/`, `gradle/`, `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts` got duplicated *inside* `.github/workflows/` (stray files from an earlier upload mistake), alongside the real `build.yml`. This caused hours of confusion — edits made to the wrong copy silently did nothing, and a botched cleanup in github.dev once deleted the *real* root-level files by accident (recovered from a backup branch). **Always double-check you're editing `app/build.gradle.kts` at the true repo root, not a nested copy under `.github/workflows/`.** Only `build.yml` should ever live inside `.github/workflows/`.

2. **`billing.connect()` must actually be called, or nothing works.** `BillingManager.connect()` doesn't auto-invoke itself — it needs an explicit call from Compose:
   ```kotlin
   LaunchedEffect(Unit) {
       billing.connect()
   }
   ```
   This must live inside `GracelandScreen`, near the top. Without it, `productDetails` stays `null` forever and "Upgrade to Pro" does nothing (silently) — this was the root cause of a long-running "Pro purchase doesn't work" saga.

3. **License Testing ≠ Internal Testing tester list.** These are two *separate* lists in Play Console:
   - **Internal testing → Testers** just controls who can *install* the app from that track.
   - **Setup → License testing** controls whether a Google account's purchases are flagged as free test orders.
   Being on the Internal testing list does **not** make purchases free. Randy got charged real money once by forgetting this. **Before asking any tester to tap "Upgrade to Pro," make sure their Google account is added under Setup → License testing**, or explicitly tell them not to tap it.

4. **Real Play purchases only work when the app is installed *through* the Play Store** (from an Internal/Closed/Open testing link), not when sideloaded via `adb install`. Use adb for everyday feature testing (fast — see below), but always switch back to a Play-installed build to test the purchase flow specifically.

5. **Browser file downloads can silently give you a stale `.aab`/`.apk`.** If Play Console rejects an upload with "Version code X has already been used" even though the source says otherwise, you likely grabbed an old duplicate download (`app-release (1).aab` etc.) instead of the latest Actions run's artifact. Always download fresh from the *top* run in the Actions list and check the file's modified date before uploading.

6. **Compass N/E/S/W label positioning bug (fixed in v10):** rotating a `Text` directly doesn't move it around a circle — it rotates around its own tiny bounding box near where it started, so all four letters clustered near "N". Fix: wrap each label in its own `Modifier.fillMaxSize().rotate(deg)` box (so the rotation pivots around the full circle's center), with the label itself counter-rotated by `-deg` to stay upright.

## Testing workflow

- **Fast iteration (UI, features, anything not billing-related):** build debug APK via Actions → download → `adb install -r app-debug.apk`. Use `-r` to overwrite in place.
- **Billing testing:** must go through Play — upload signed `.aab` to Internal testing track, install from that testing link on the phone (not adb), and make sure your account is in License Testing first.
- Diagnostic toasts exist in `BillingManager.kt` (`toastEvents: SharedFlow<String>`) but are **not currently wired up** in `MainActivity.kt` (removed after last billing bug was fixed, to keep them out of the shipped app). To re-enable temporarily for debugging, add back in `GracelandScreen`:
  ```kotlin
  LaunchedEffect(billing) {
      billing.toastEvents.collect { msg ->
          android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
      }
  }
  ```
  Remove it again before shipping to testers.

## Open TODOs / not yet done

- [ ] Add testers' Google accounts to Play Console → Setup → License testing (so they can safely test the purchase flow)
- [ ] Seasonal/extra backgrounds as a Pro feature (discussed, no photos supplied yet)
- [ ] "Rate us" / Play Store link on the exit-upsell screen (marked with a `// TODO` in `ExitOverlay`)
- [ ] Data Safety form + privacy policy for the Play Store listing (required before wider release)
- [ ] Closed testing requirement: Play requires 12 testers opted in for 14 continuous days before a first-time developer can publish to Production
- [ ] Eventual Production release

## Change log

- **v1–v2:** Initial build, package rename, signed release CI pipeline set up.
- **v3–v7:** Billing Library 8.0.0 / targetSdk 36 upgrades, various versionCode fixes, stray-duplicate-folder incident + recovery via backup branch, compass/distance-result UI redesign (button replaced by compass + result panel per tester feedback, standalone compass icon removed).
- **v8–v9:** Diagnosed and fixed the real Pro-purchase bug (`billing.connect()` was never called). Confirmed real purchase flow with a real charge (refunded) before realizing License Testing wasn't configured.
- **v10:** Fixed compass N/E/S/W label positions all clustering near "N" (rotation-pivot bug).
