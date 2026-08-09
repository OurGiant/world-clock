---
name: ship-issue
description: The standard workflow for shipping a bug fix or feature to World Clock -- file a GitHub issue, branch off main, implement, verify, bump the patch version, and open a PR. Use whenever picking up a bug fix or feature for this repo.
---

# Shipping a change to World Clock

Follow `java-swing-ship-issue` (the generic workflow shared across the
Java Swing project family) with these World Clock specifics:

- **Project path**: `/projects/OHI/world-clock` inside the build container
  (the `OHI/` segment has drifted before — see this repo's own
  `.claude/skills/verify/SKILL.md` for the fallback if `cd` fails).
- **Verify**: use this repo's own `.claude/skills/verify/SKILL.md` for
  build/launch mechanics, including this host's shared-desktop screenshot
  caveat.
- **Preferences/config changes**: anything touching
  `PreferencesManager` must go through the `worldclock.prefsDir` system
  property override (see `pom.xml`'s surefire config and
  `PreferencesManager.prefsDir()`) — never let a test touch the real
  developer's `~/.worldclock`.
- **Outbound API calls**: `WeatherService` (Open-Meteo) and
  `HolidayService` (API Ninjas) make third-party HTTP calls. A change to
  either is a good candidate for documenting the hardening reasoning in
  README's dedicated section (timeouts, retry/backoff, caching, API-key
  handling) alongside the code, not just implementing it silently.
- No repo-specific branch-naming convention beyond the generic workflow
  has been established here yet; follow `java-swing-ship-issue` as-is
  until one is.
