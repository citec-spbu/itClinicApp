---
title: Auth Flow
---

# Auth Flow

## Entry points

Users can either:

- sign in through the GitHub-based flow;
- continue in guest mode.

## Auth responsibilities

The auth layer controls:

- session bootstrap;
- secure token usage in API requests;
- identity reset on logout;
- access restrictions across ranking, statistics, and private project content.
