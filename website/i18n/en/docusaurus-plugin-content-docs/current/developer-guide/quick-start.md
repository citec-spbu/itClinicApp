---
title: Quick Start
---

# Quick Start

## Prerequisites

- JDK 17+
- Android Studio
- Xcode for iOS work
- Node.js for the docs site

## Basic path

1. Create local `BuildConfig.kt` and `MailConfig.kt` from templates.
2. Decide whether you need the external Registry backend stack.
3. Build the Android debug client.
4. Compile the iOS target if needed.

## Minimum commands

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```
