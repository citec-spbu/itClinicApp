---
title: Local Configuration
---

# Local Configuration

## Local files

Developer-specific config is created from templates and kept out of version control.

## Main concerns

- base URLs for client APIs;
- auth-related test configuration where needed;
- mail configuration for feedback flows;
- environment switching between local and non-local endpoints.

## Rule

Do not commit local secrets or developer-only values back into the repository.
