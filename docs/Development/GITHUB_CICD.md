# GitHub Actions CI/CD

## Overview

This repository is treated as a frontend-only Kotlin Multiplatform project.

- The only Gradle module in the repository is `:composeApp`.
- CI validates the Android application and the shared KMP code.
- The repository does not require a backend checkout to run CI.
- CI creates temporary stub config files so the project can compile without local secrets or backend-specific files.

Primary workflow:

- `.github/workflows/mobile-app-ci-cd.yml`

## CI/CD workflows in this repository

The repository currently uses one workflow:

- `Mobile App CI/CD`

The workflow contains four jobs:

- `Android CI`
- `iOS Kotlin Validation`
- `Publish App Showcase Image`
- `Release Android Artifact`

## What runs on pull requests

Pull requests run the `Android CI` job on `ubuntu-latest`.

That job performs:

- `./gradlew :composeApp:lintDebug --stacktrace`
- `./gradlew :composeApp:testDebugUnitTest --stacktrace`
- `./gradlew :composeApp:clean :composeApp:assembleDebug`

It uploads:

- Android lint reports
- Android unit test reports
- the debug APK artifact

## What runs on main/develop

Pushes to the default branch keep running `Android CI`.

Pushes to the default branch and `develop` also run:

- `iOS Kotlin Validation`

That job performs:

- `./gradlew :composeApp:compileKotlinIosSimulatorArm64 --stacktrace`

This keeps the KMP iOS target compiling without requiring release signing, Xcode project automation, or backend services.

Pushes to the default branch also run:

- `Publish App Showcase Image`

That job publishes:

- `ghcr.io/<owner>/itclinicapp-showcase`

The image contains:

- a lightweight static landing page
- the debug APK produced by CI
- this CI/CD documentation

## What runs on tags/releases

Version tags matching `v*` run:

- `Android CI`
- `iOS Kotlin Validation`
- `Publish App Showcase Image`
- `Release Android Artifact`

The release job creates or updates the GitHub Release for the tag and uploads:

- `release-artifacts/itclinicapp-debug.apk`

This is a debug artifact, not a signed production binary.

## Required GitHub Secrets / Variables

No custom GitHub Secrets or Variables are required for the current workflow.

The workflow only relies on the built-in:

- `GITHUB_TOKEN`

`GITHUB_TOKEN` is used for:

- publishing the GHCR image
- creating or updating GitHub Releases
- uploading the APK release asset

Optional manual GitHub setup outside the workflow:

- make the GHCR package public if you want anonymous `docker pull`

## How to work with CI/CD as a developer

Use these commands locally to mirror the main CI stages:

```bash
./gradlew :composeApp:lintDebug
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:clean :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

The workflow generates temporary CI stubs through:

- `scripts/prepare_ci_stubs.sh`

APK staging for CD is handled by:

- `scripts/prepare_mobile_showcase.sh`

## How to run the Docker image

Pull the published image:

```bash
docker pull ghcr.io/<owner>/itclinicapp-showcase:latest
```

Run the container locally:

```bash
docker run --rm -p 8080:80 ghcr.io/<owner>/itclinicapp-showcase:latest
```

If you want to run a specific published tag instead of `latest`:

```bash
docker run --rm -p 8080:80 ghcr.io/<owner>/itclinicapp-showcase:<tag>
```

After the container starts, open:

- `http://localhost:8080/` for the landing page
- `http://localhost:8080/release-artifacts/itclinicapp-debug.apk` for the APK
- `http://localhost:8080/docs/Development/GITHUB_CICD.md` for the CI/CD documentation

If the package is private, authenticate first:

```bash
echo <PAT> | docker login ghcr.io -u <github_username> --password-stdin
```

The personal access token needs:

- `read:packages`

## How to troubleshoot common failures

### Missing `BuildConfig` or `MailConfig`

CI should not depend on local untracked config files.

If compilation starts failing with unresolved references to `BuildConfig` or `MailConfig`, verify:

- `scripts/prepare_ci_stubs.sh`
- the `Prepare CI stubs` step in `.github/workflows/mobile-app-ci-cd.yml`

### Duplicate dex classes during `assembleDebug`

If Android packaging fails with duplicate dex entries such as `*.dex` and `* 2.dex`, the local build directory is stale.

Use:

```bash
./gradlew :composeApp:clean :composeApp:assembleDebug
```

The CI artifact helper already does this clean build automatically.

### GHCR `unauthorized`

The most common reasons are:

- the image has not been published yet
- the GHCR package is private

For private packages, log in before pulling:

```bash
echo <PAT> | docker login ghcr.io -u <github_username> --password-stdin
docker pull ghcr.io/<owner>/itclinicapp-showcase:latest
```

The personal access token needs:

- `read:packages`

### GHCR `no matching manifest for linux/arm64/v8`

The workflow publishes both:

- `linux/amd64`
- `linux/arm64`

If an old tag still fails on Apple Silicon, it was likely published before multi-arch support was added. Pull a newer tag instead.

## Notes about frontend-only repository limitations

- The workflow does not clone, build, or test any backend repository.
- No backend containers are required for CI.
- The APK produced by CI is built with CI-safe stub config, not developer secrets.
- The release artifact is a debug APK and should be treated as a validation artifact, not a store-ready build.
- The workflow validates the Kotlin iOS target compilation, but it does not produce signed iOS app bundles or TestFlight-ready artifacts.

## What still requires manual setup

The following items remain manual by design:

- Android release signing
- Play Store publishing
- iOS code signing and distribution
- making the GHCR package public, if desired
- replacing CI stub config with real runtime configuration for local developer environments
