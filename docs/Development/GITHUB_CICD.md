# GitHub Actions CI/CD

## Scope

This workflow is configured only for the mobile application in this repository.

- no backend is required
- CI validates the Android/Compose application layer
- CD publishes a documentation/showcase image instead of a backend container

Primary workflow: `.github/workflows/mobile-app-ci-cd.yml`

## What the workflow does

### CI

The workflow runs two jobs:

- `Android Lint`
  - command: `./gradlew :composeApp:lintDebug --stacktrace`
- `Android Unit Tests`
  - command: `./gradlew :composeApp:testDebugUnitTest --stacktrace`

Before Gradle starts, the workflow generates temporary config files:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.kt`

They are created from:

- `BuildConfig.example.kt`
- `MailConfig.example.kt`

This is required because real local config files are intentionally not committed to Git.

### CD

On `push` to the default branch or on a git tag push, the workflow publishes a Docker image:

- `ghcr.io/<owner>/itclinicapp-showcase`

The image contains:

- a static entry page
- the `docs/` folder

The container runs on top of `nginx`.
The publish job builds a multi-architecture image for:

- `linux/amd64`
- `linux/arm64`

## Image tags

Each publish creates immutable tags:

- short commit SHA
- branch tag
- git tag, when the publish is triggered by a release tag

Additionally:

- `latest` is updated only for the default branch

The idea is simple: `latest` is just a pointer, not the only source of truth.

## Rollback strategy

Rollback is performed without rebuilding the image.

1. Open `Actions`
2. Select the `Mobile App CI/CD` workflow
3. Click `Run workflow`
4. Provide `rollback_tag`
5. Run the workflow manually

The rollback job promotes an existing image:

- from `ghcr.io/<owner>/itclinicapp-showcase:<rollback_tag>`
- to `ghcr.io/<owner>/itclinicapp-showcase:latest`

So rollback changes only the `latest` pointer.

## Why `docker pull ghcr.io/...:latest` may return `unauthorized`

There are usually two possible reasons:

1. the image has not been published yet
2. the GHCR package is private and the client is not authenticated

### Scenario 1: the image was not published

If `Android Lint` or `Android Unit Tests` fail, the publish job does not start.

In that case:

- `latest` is not created
- `docker pull` cannot fetch the image

The first step is to get a green workflow run.

### Scenario 2: the GHCR package is private

By default, a package in GitHub Container Registry is often private.

In that case, you need to log in before pulling:

```bash
echo <PAT> | docker login ghcr.io -u <github_username> --password-stdin
docker pull ghcr.io/<owner>/itclinicapp-showcase:latest
```

The `PAT` must include:

- `read:packages`

If you want anonymous pulls without `docker login`:

1. open the package page in GitHub
2. switch package visibility to `public`

## Why `no matching manifest for linux/arm64/v8` may happen

This means the image exists, but the published manifest does not include an ARM64 variant.

This usually happens when the image was built only for:

- `linux/amd64`

On Apple Silicon, Docker tries to pull:

- `linux/arm64/v8`

If that platform is missing, pull fails with:

- `no matching manifest for linux/arm64/v8`

Temporary workaround:

```bash
docker pull --platform linux/amd64 ghcr.io/<owner>/itclinicapp-showcase:<tag>
```

Proper fix:

- publish the image as multi-arch
- include both `linux/amd64` and `linux/arm64`

## What to verify after push

1. The `Mobile App CI/CD` workflow appears in the `Actions` tab
2. These jobs pass:
   - `Android Lint`
   - `Android Unit Tests`
3. On the default branch or on a tag, this job also runs:
   - `Publish App Showcase Image`
4. A package named `itclinicapp-showcase` appears in `Packages`

## Short summary

This repository uses an app-only GitHub Actions workflow.

- CI runs `lintDebug` and `testDebugUnitTest`
- CD publishes `ghcr.io/<owner>/itclinicapp-showcase`
- rollback is performed by promoting an existing immutable tag back to `latest`
- `unauthorized` on `docker pull` usually means either the image was never published or the GHCR package is private
