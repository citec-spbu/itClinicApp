---
title: Platform Development
---

# Platform Development

## Android

Android work happens mostly through the shared `composeApp` module plus Android-specific integrations in `androidMain`.

## iOS

iOS uses the same shared codebase with a Swift host app and iOS-specific bridges in `iosMain` and `iosApp`.

## Practical expectation

Most feature work should stay in shared code unless the requirement is truly platform-specific.
