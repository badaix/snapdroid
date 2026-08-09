## 1. Inventory and classify branding

- [x] 1.1 Search tracked resources, Java/XML, README/privacy/store metadata, and CI for `snapdroid`, `Snapdroid`, `Snapcast`, and inherited repository URLs; classify each occurrence as product branding or technical/upstream identity.
- [x] 1.2 Confirm the fork's actual repository URL before changing any link that identifies this repository; leave technical Snapcast, native-submodule, and dependency-source URLs accurate.

## 2. Rebrand the Android application

- [x] 2.1 Update the centralized Android application-label resource to `cracklepop`, including any maintained locale resources that define the same label.
- [x] 2.2 Update the main-screen upper-left title source to display lowercase `cracklepop` without changing unrelated Snapcast server/protocol terminology.
- [x] 2.3 Verify that `applicationId`, Java package names, manifest component names, dependency coordinates, and Snapcast connection identifiers remain unchanged.

## 3. Rebrand maintained repository surfaces

- [x] 3.1 Update README and privacy-policy product/repository branding to identify `cracklepop`, using the confirmed fork URL where appropriate.
- [x] 3.2 Update Fastlane/store metadata and CI artifact or path naming that presents `snapdroid` as the current product or repository; preserve upstream technical references and signing behavior.
- [x] 3.3 Review the complete branding search results and remove remaining inherited product identity only where it violates the app-branding specification.

## 4. Validate the rebrand

- [x] 4.1 Run focused source/resource checks and `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew build` (or the configured local JDK 17 equivalent).
- [x] 4.2 Install the resulting debug APK on the authorized Shield at `192.168.0.106:5555`, launch `de.badaix.snapcast/.MainActivity`, and verify both launcher/app labeling and the main-screen upper-left title show `cracklepop`.
- [x] 4.3 Confirm the built package remains `de.badaix.snapcast` and document any intentionally preserved Snapcast/upstream branding in the change review.
