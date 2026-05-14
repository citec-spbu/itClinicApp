---
title: Networking
---

# Networking

## Main idea

Networking is split by API responsibility rather than hidden inside screen code.

## Typical layers

- API config for endpoint selection;
- API clients for transport-level calls;
- repositories that reshape remote data for UI flows;
- view models that trigger loading and expose UI state.

## What to keep aligned

- endpoint paths;
- auth headers and session behavior;
- local versus non-local base URL switching;
- error handling in user-facing flows.
