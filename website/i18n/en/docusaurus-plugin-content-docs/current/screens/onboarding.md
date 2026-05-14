---
title: Onboarding Screen
---

# Onboarding Screen

## Purpose

The onboarding/auth entry screen decides whether the user continues through authenticated GitHub sign-in or enters guest mode.

## Design

- clear primary sign-in action;
- visible guest-mode alternative;
- simple explanatory copy;
- no secondary clutter before the first decision.

## Data and requests

- starts the mobile auth flow;
- consumes auth callback results;
- updates local session state before entering the main shell.
