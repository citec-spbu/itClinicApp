---
title: Analytics and Observability
---

# Analytics and Observability

## Goal

The client collects product analytics for the main user flows and statistics screens. The goal is meaningful usage visibility, not raw event spam.

## Main pieces

- a shared `AnalyticsTracker` contract in `commonMain`;
- a composite tracker that fans out to multiple providers;
- debug logging for local visibility;
- production integrations such as PostHog and Firebase.

## Key rules

- avoid duplicate screen events from platform auto-tracking;
- avoid sensitive user data in payloads;
- avoid tying analytics to frequent Compose recomposition;
- emit events that can be interpreted product-wise later.
