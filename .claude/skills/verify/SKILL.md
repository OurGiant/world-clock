---
name: verify
description: How to build, launch, and drive World Clock to verify a Swing UI change actually works on this dev setup. Use before trusting the generic verify-java-swing skill's screenshot step; read that skill too for the underlying techniques (modal-dialog deadlock, synthetic MouseEvent dispatch, process safety).
---

# Verifying World Clock

This is the project-specific companion to the generic `verify-java-swing`
skill (techniques) and `java-swing-project-setup` (build/structure
standard this project follows). Read those first — this file is what to
actually type for *this* project.

## Build here, run there

Maven only exists in the Docker container, not on the host:

```bash
docker exec festive_bardeen bash -c "cd /projects/world-clock && mvn -q package -DskipTests"
```

If `festive_bardeen` doesn't respond, find the current container:
`docker ps -a --format '{{.Names}} {{.Status}} {{.Image}}'` and
`docker start <name>` if stopped — the name can drift across sessions.
If a build seems to ignore a just-made edit, suspect bind-mount cache
staleness before suspecting your own change — confirm with
`docker exec festive_bardeen grep <marker> /projects/world-clock/<path>`,
and force a sync with `docker cp <file> festive_bardeen:/projects/world-clock/<path>`
if it's actually stale (seen on both `pom.xml` and `.java` edits).

`/projects` is bind-mounted from the host's `~/projects`, so the jar lands
at `target/worldclock-all.jar`, visible on the host. The container is
headless (no `DISPLAY`) — run the jar on the **host**, not inside the
container, or it dies at `JFrame` construction with `HeadlessException`.

```bash
java -jar target/worldclock-all.jar
```

Main class: `com.ourgiant.worldclock.gui.WorldClockApp`.

## Screenshots: Robot works here, but the window may be covered by real windows

Like `doc-scrubber` (and confirmed independently here), this dev host has
a real, working X11 display (`:1`) — `Robot.createScreenCapture(...)`
returns a genuine, non-black screenshot. Confirmed by sampling pixel
values, not just eyeballing the PNG.

This is a **shared desktop** with the user's real windows (VS Code,
terminal, browser, etc.), not an isolated headless session. A full-screen
`Robot` capture can come back showing whatever real window happens to be
on top at that screen location, not the app you just launched — this
happened here (a browser window was on top). There's no `wmctrl`/`xdotool`
on this host to raise a window, and forcing focus changes on a shared
display risks disrupting the user's actual session, so don't try to fix
this by grabbing focus.

What actually works:
- `xwininfo -root -tree` (DISPLAY=:1) lists real windows with title and
  geometry — confirms the JFrame was constructed, its title text (useful
  for verifying dynamic titles, e.g. the version-number suffix), and its
  on-screen bounds, without needing a clean screenshot.
- If you do want pixels, capture the *exact* rectangle `xwininfo` reports
  for the app's window (`Robot.createScreenCapture(new Rectangle(x, y, w,
  h))`) as soon as possible after launch — still not guaranteed to be
  unobstructed, but better odds than a full-screen grab.
- Absent a clean screenshot, "no exceptions/stack traces in the app's
  stdout/stderr across the run" plus `xwininfo` confirming the expected
  window title/geometry is adequate verification for non-visual changes
  (dependency bumps, package moves, wiring a value into an existing
  label/title).

## Process safety

Track the exact PID you launched (`$!` after backgrounding, or `timeout N
java -jar ...` so it self-terminates) and kill only that PID — never a
broad `pkill -f java`, which would hit other real Java processes on this
shared host.

## Nothing else confirmed yet

No other project-specific gotchas (first-run state location, custom
dialog sizing quirks, etc.) have been found and confirmed here yet. Add
them to this file as they turn up.
