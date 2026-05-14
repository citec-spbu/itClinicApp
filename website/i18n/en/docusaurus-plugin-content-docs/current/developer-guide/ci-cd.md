---
title: CI/CD
---

# CI/CD

## Scope

CI/CD covers validation, packaging, and release-oriented workflows for the mobile client.

It also includes a dedicated docs workflow:

- `.github/workflows/docs-site-pages.yml`

That workflow:

- builds the Docusaurus site from `website/`;
- publishes `website/build` to GitHub Pages;
- runs on pushes to `main` when `website/**`, `README.md`, or the workflow itself changes.

Production docs URL:

- `https://citec-spbu.github.io/itClinicApp/`

## What belongs here

- build and verification jobs;
- secrets and environment expectations;
- artifact publishing rules;
- update delivery steps;
- operational troubleshooting for broken pipelines.

## Practical rule

If a release or store-ready build depends on a manual step, that step must be described here rather than kept as tribal knowledge.
