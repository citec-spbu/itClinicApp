---
title: Client Architecture
---

# Client Architecture

## High-level structure

The client is organized around the `composeApp` KMP module:

- `commonMain` for most business logic, models, networking, and Compose UI;
- `androidMain` for Android-specific integration;
- `iosMain` for iOS-specific integration;
- `iosApp` for the Swift host application.

## Main domains

- `core/` for settings, auth, networking, and platform integration;
- `projects/` for projects and project detail flows;
- `rating/` for ranking, statistics, export, and metric detail screens;
- `user/` for profile and user-related API work.
