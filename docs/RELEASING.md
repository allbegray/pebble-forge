# Releasing Pebble Forge

This document is the release checklist. It covers the six published Maven Central modules: core, natural-html, contracts, diagnostics, spring, and the Boot 4 starter. The Gradle plugins publish separately to the Gradle Plugin Portal. `pebble-forge-cli`, `pebble-forge-demo`, `pebble-forge-demo-app`, `pebble-forge-benchmarks`, and `pebble-forge-fuzz` are not published yet.

Version 0.4.0 is the first Maven Central release. Version 0.3.0 is a GitHub source release only.

## Prerequisites

- JDK 21.
- A Maven Central account with the `io.github.allbegray` namespace verified.
- A GPG key for signing artifacts.
- A Gradle Plugin Portal account with an API key.
- Access to push tags to `https://github.com/allbegray/pebble-forge`.
- Access to the `배포` environment in the GitHub repository.

The release workflows read credentials from the `배포` environment. Add these environment secrets.

| Secret | Purpose |
| --- | --- |
| `CENTRAL_USERNAME` | Central Portal token username |
| `CENTRAL_PASSWORD` | Central Portal token password |
| `SIGNING_KEY` | Armored GPG private key |
| `SIGNING_PASSWORD` | GPG key passphrase |
| `GRADLE_PUBLISH_KEY` | Plugin Portal API key |
| `GRADLE_PUBLISH_SECRET` | Plugin Portal API secret |

Do not commit these values.

## 1. Prepare the version

Set the release version in the root `build.gradle.kts`.

```kotlin
allprojects {
    group = "io.github.allbegray.pebbleforge"
    version = "0.4.0"
}
```

Update `CHANGELOG.md`. Move the relevant entries from `Unreleased` to a dated version section.

## 2. Configure signing

The `Release` workflow normalizes `SIGNING_KEY` and signs a probe payload before it builds. A wrong key or passphrase fails the run before the upload, so no version is consumed.

For a local release, put the signing key in `~/.gradle/gradle.properties`.

```properties
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=your-key-passphrase
```

Export an armored secret key with `gpg --armor --export-secret-keys <KEY_ID>`. Keep real newlines in the secret; the workflow also accepts escaped `\n`.

The build signs only when both properties exist. Maven Central rejects unsigned artifacts, so a release without them is incomplete.

## 3. Run local checks

```shell
./gradlew clean build javadoc
./gradlew nmcpCheckAggregationFiles
./gradlew spotlessCheck checkstyleMain checkstyleTest
```

`build` runs the 702-test suite. After `./gradlew publishToMavenLocal`, resolve the core artifact from a separate sample project against `mavenLocal()` and run it. This catches broken POM metadata before the upload.

## 4. Publish to the Central Portal

Run the `Release` workflow from the `main` branch.

```shell
gh workflow run release.yml --repo allbegray/pebble-forge --ref main
```

The workflow builds, signs, and uploads all six modules with `publishingType = USER_MANAGED`. It also publishes both Gradle plugins. The upload leaves the deployment in `VALIDATED`.

The equivalent local commands are:

```shell
export CENTRAL_USERNAME="your-portal-token-username"
export CENTRAL_PASSWORD="your-portal-token-password"
./gradlew nmcpCheckAggregationFiles publishAggregationToCentralPortal
```

If validation fails, fix the POM or signature, bump the version if the deployment already consumed it, and upload again.

## 5. Release the deployment

A `USER_MANAGED` deployment needs a second step. Run the `Release deployment` workflow with the deployment ID from the upload log or the Central Portal URL.

```shell
gh workflow run release-deployment.yml --repo allbegray/pebble-forge --ref main \
  -f deployment_id=<deployment-id>
```

The workflow accepts a deployment in `VALIDATED` or `PUBLISHING`, publishes it, and polls until `PUBLISHED`. Use the Central Portal UI if the API reports an unexpected state.

`./gradlew nmcpPublishDeployment -PnmcpDeploymentId=<id>` can return `HTTP error 400` when the deployment is already publishing. Check the state before retrying. Do not upload the same version again.

## 6. Publish the Gradle plugins

The `Release` workflow runs this step. To publish manually, set `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`, then run:

```shell
./gradlew :pebble-forge-gradle-plugin:publishPlugins
```

A new plugin ID needs manual approval in the Portal the first time. The approval notice can name only one of several new IDs. Check both plugin pages after approval and re-submit any ID that is missing. The root build file excludes this project from the Maven Central aggregation, so the plugins publish only to the Portal.

## 7. Tag and publish the GitHub release

```shell
git tag -a v0.4.0 -m "Pebble Forge 0.4.0"
git push origin v0.4.0
```

Create a GitHub release from the tag. Use the matching `CHANGELOG.md` section as the release notes. State the Maven Central and Plugin Portal status in the release body.

## 8. Verify the published artifacts

Wait for Maven Central to index the release, then check the coordinates.

```text
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-core/0.4.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-natural-html/0.4.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-contracts/0.4.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-diagnostics/0.4.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-spring/0.4.0/
https://repo1.maven.org/maven2/io/github/allbegray/pebbleforge/pebble-forge-spring-boot-4-starter/0.4.0/
```

Resolve the published artifacts in a fresh sample project. Do not rely on the local Maven repository for this check. Verify at least one signature:

```shell
gpg --verify pebble-forge-core-0.4.0.pom.asc pebble-forge-core-0.4.0.pom
```

The Gradle plugins are listed at:

```text
https://plugins.gradle.org/plugin/io.github.allbegray.pebbleforge.contracts
https://plugins.gradle.org/plugin/io.github.allbegray.pebbleforge.diagnostics
```

## 9. Start the next development version

Set the root version to the next development version, for example `0.5.0`, and commit it.

```kotlin
version = "0.5.0"
```

Add a new `Unreleased` section to `CHANGELOG.md`.

## Release history

| Version | Maven Central | Gradle Plugin Portal | GitHub release |
| --- | --- | --- | --- |
| 0.3.0 | Not published | Not published | [v0.3.0](https://github.com/allbegray/pebble-forge/releases/tag/v0.3.0) |
| 0.4.0 | Six modules published on 2026-10-26 | Submitted on 2026-10-26, awaiting first-time approval | v0.4.0 |

## Release checklist

- [ ] Version set in the root build file.
- [ ] `CHANGELOG.md` updated.
- [ ] `./gradlew clean build javadoc` passes.
- [ ] `./gradlew spotlessCheck checkstyleMain checkstyleTest` passes.
- [ ] Aggregate Javadoc site is deployed or the Pages workflow is ready.
- [ ] `./gradlew publishToMavenLocal` passes.
- [ ] Separate local consumer resolves the artifacts.
- [ ] `./gradlew nmcpCheckAggregationFiles` passes.
- [ ] Signing secrets are present and the workflow verifies the passphrase.
- [ ] `Release` workflow succeeds.
- [ ] Deployment reaches `PUBLISHED`.
- [ ] `./gradlew :pebble-forge-gradle-plugin:publishPlugins` succeeds.
- [ ] Both plugin IDs resolve on the Gradle Plugin Portal.
- [ ] Tag pushed.
- [ ] GitHub release published.
- [ ] Maven Central coordinates resolve.
- [ ] Next development version committed.
