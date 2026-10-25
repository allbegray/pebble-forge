# Releasing Pebble Forge

This document is the release checklist. It covers the six published modules: core, natural-html, contracts, diagnostics, spring, and the Boot 4 starter. `pebble-forge-demo`, `pebble-forge-demo-app`, `pebble-forge-gradle-plugin`, `pebble-forge-benchmarks`, and `pebble-forge-fuzz` are not published yet.

## Prerequisites

- JDK 21.
- A Maven Central account with the `io.github.allbegray` namespace verified.
- A GPG key for signing artifacts.
- Access to push tags to `https://github.com/allbegray/pebble-forge`.

## 1. Prepare the version

Set the release version in the root `build.gradle.kts`.

```kotlin
allprojects {
    group = "io.github.allbegray.pebbleforge"
    version = "0.1.0"
}
```

Update `CHANGELOG.md`. Move the relevant entries from `Unreleased` to a dated version section.

## 2. Configure signing

Put the signing key in `~/.gradle/gradle.properties`. Do not commit this file.

```properties
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=your-key-passphrase
```

Export an armored secret key with `gpg --armor --export-secret-keys <KEY_ID>` and replace real newlines with `\n`.

The build signs only when both properties exist. Maven Central rejects unsigned artifacts, so a release without them is incomplete.

## 3. Run local checks

```shell
./gradlew clean build
./gradlew publishToMavenLocal
./gradlew nmcpCheckAggregationFiles
```

`publishToMavenLocal` must produce all six POMs, jars, sources jars, and javadoc jars.

Resolve the core artifact from a separate sample project against `mavenLocal()` and run it. This catches broken POM metadata before the upload.

## 4. Upload to the Central Portal

The settings plugin reads the portal token from environment variables.

```shell
export CENTRAL_USERNAME="your-portal-token-username"
export CENTRAL_PASSWORD="your-portal-token-password"
./gradlew publishAggregationToCentralPortal
```

The project uses `publishingType = USER_MANAGED`. The upload creates a deployment in the Central Portal. Open `https://central.sonatype.com/`, wait for validation, review the coordinates, and release the deployment manually.

If validation fails, fix the POM or signature, bump the version if the deployment already consumed it, and upload again.

## 5. Tag and publish the GitHub release

```shell
git tag -a v0.1.0 -m "Pebble Forge 0.1.0"
git push origin v0.1.0
```

Create a GitHub release from the tag. Use the matching `CHANGELOG.md` section as the release notes.

## 6. Verify the published artifacts

Wait for Maven Central to index the release, then check the coordinates.

```text
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-core/0.1.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-natural-html/0.1.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-contracts/0.1.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-diagnostics/0.1.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-spring/0.1.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-spring-boot-4-starter/0.1.0/
```

Resolve the published artifacts in a fresh sample project. Do not rely on the local Maven repository for this check.

## 7. Start the next development version

Set the root version to the next snapshot, for example `0.2.0-SNAPSHOT`, and commit it.

```kotlin
version = "0.2.0-SNAPSHOT"
```

Add a new `Unreleased` section to `CHANGELOG.md`.

## Release checklist

- [ ] Version set in the root build file.
- [ ] `CHANGELOG.md` updated.
- [ ] `./gradlew clean build javadoc javadocAll` passes.
- [ ] `./gradlew spotlessCheck checkstyleMain checkstyleTest` passes.
- [ ] Aggregate Javadoc site is deployed or the Pages workflow is ready.
- [ ] `./gradlew publishToMavenLocal` passes.
- [ ] Separate local consumer resolves the artifacts.
- [ ] `./gradlew nmcpCheckAggregationFiles` passes.
- [ ] Signing properties are present.
- [ ] Central Portal upload succeeds.
- [ ] Deployment released in the portal.
- [ ] Tag pushed.
- [ ] GitHub release published.
- [ ] Maven Central coordinates resolve.
- [ ] Next snapshot version committed.
