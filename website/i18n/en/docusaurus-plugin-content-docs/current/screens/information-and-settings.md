---
title: Information, Settings and Feedback Screens
---

# Information, Settings and Feedback Screens

## Information Screen

This is the entry point into the personal area. It combines profile access, settings, privacy policy, and feedback.

### Design

- title header;
- profile card with avatar, name, email, and project summary;
- list rows for settings, privacy policy, and feedback.

### Requests

- `GET /user/profile`

## Profile Screen

### Purpose

Shows detailed user data, team context, current project, and logout-related actions.

### Requests

- `GET /user/profile`
- `PUT /profile/personal`
- `PUT /profile/account` where account-level editing is separated

## Edit Profile Modal

### Purpose

Allows personal data editing without leaving the profile flow.

### Requests

- `PUT /profile/personal`

## Settings Screen

### Purpose

Controls app-level preferences such as language and theme.

### Data behavior

Theme and language are primarily local client settings. Backend requests are only needed if the screen also edits account data.

## Privacy Policy Screen

### Purpose

Displays policy text as a read-only flow with an emphasis on readability.

## Feedback Screen

### Purpose

Collects free-form user feedback, bug reports, and suggestions.

### Network behavior

This flow uses the configured mail-sending integration instead of the main backend API surface.
