---
title: Repository Map
---

# Repository Map

## Main areas

- `composeApp/` — shared KMP client code, platform integrations, UI, repositories, and view models;
- `iosApp/` — Swift host application for iOS;
- `website/` — Docusaurus documentation site;
- root Gradle files — build configuration and project-level setup.

## Important boundaries

- backend code is not part of this repository;
- secrets and developer-specific config live outside committed source files;
- the docs site documents behavior, contracts, and operations rather than mirroring the file tree.
