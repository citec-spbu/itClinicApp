## Typography

- For Open Sans, do not rely on `fontWeight` when exact visual matching matters.
- Use explicit font families from `AppFonts` instead:
  - `AppFonts.OpenSansLight`
  - `AppFonts.OpenSansRegular`
  - `AppFonts.OpenSansMedium`
  - `AppFonts.OpenSansSemiBold`
  - `AppFonts.OpenSansBold`
- Reason: the weighted `FontFamily` does not always render the same as the direct font file on this project, especially on design-sensitive screens.
- First enforced on the project stats/settings screen. Apply the same rule to other screens when touching typography there.
