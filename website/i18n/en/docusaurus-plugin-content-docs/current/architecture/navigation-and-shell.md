---
title: Navigation and Shell
---

# Navigation and Shell

## Main model

The client is built around three primary tab flows:

- Projects;
- Ranking / Statistics;
- Information.

Detail screens, filters, dialogs, and nested settings/profile flows open on top of the tab shell.

## Root shell responsibilities

`MainScreen` owns:

- current tab selection;
- bottom tab bar visibility;
- overlay navigation for nested screens;
- guest-mode restrictions.

## Custom tab bar

The bottom bar is a custom Compose component with its own active indicator, icons, and transitions. The important contract is behavioral:

- the active tab must stay visually obvious;
- switching tabs must not destroy internal screen state unnecessarily;
- overlay flows must not reset the selected tab.
