# Remember

An offline checklist app for Android, with a home screen widget that lists your
checklists and opens one with a single tap. The widget's appearance is yours to
set — font size, text colour, background colour (RGBA, with hex entry) and
corner radius, configured per widget instance.

No accounts, no network, no third-party libraries: everything is stored in the
app's own private preferences as JSON.

## Using the app

- **New checklist** — the button at the bottom of the home screen.
- **Open a checklist** — tap it. The row shows how many items are ticked.
- **Rename or delete a checklist** — long press it.
- **Add an item** — type in the field at the bottom of a checklist and tap
  **Add** (or press the keyboard's done key).
- **Tick an item off** — tap it. Ticked items are struck through and dimmed.
- **Edit or delete an item** — long press it.
- **Uncheck all** — the button beside the checklist's title, for lists you reuse.

## The widget

Add *Remember* from the home screen's widget picker. The widget lists every
checklist by name and scrolls when there are more than fit; tapping a name opens
that checklist. When there are no checklists yet, the widget says so and tapping
it opens the app. The list refreshes itself whenever checklists change.

### Widget settings

The settings screen opens when the widget is placed, and again whenever you
reconfigure it from the home screen. A live preview at the top shows your own
checklist names with the current styling applied.

| Setting | Range | Default |
|---------|-------|---------|
| Font size | 10–30 sp | 15 sp |
| Text colour | RGBA sliders + `#AARRGGBB` hex field | `#FFF2EFEA` |
| Background colour | RGBA sliders + `#AARRGGBB` hex field | `#CC1A1820` |
| Border radius | 0–48 dp | 16 dp |

The colour picker's sliders and hex field are two views of the same value —
editing either updates the other. The hex field accepts `#AARRGGBB`,
`#RRGGBB`, or either without the `#`; six digits are treated as fully opaque.
The swatch is drawn over a light/dark split so alpha is visible. Nothing is
written until **Save**, so backing out leaves an existing widget untouched.

Settings are per widget instance: place the widget twice and each copy can look
different. Removing a widget discards its settings.

## Building

The build is fully containerized and CLI-driven. No JDK, Android SDK, or Gradle
is needed on the host — everything runs inside a podman container defined by the
`Containerfile`. The only host dependency is `podman` (and `adb`, if you want to
install on a device).

Build the toolchain image once:

```sh
make image
```

Build a debug APK (rebuilds the image if needed):

```sh
make debug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Other targets:

```sh
make release            # assembleRelease
make clean              # gradle clean
make gradle ARGS="tasks"   # run any gradle task in the container
make shell              # interactive shell inside the build container
```

The Gradle cache is persisted in a named volume (`android-gradle-cache`) so
incremental builds and the debug keystore survive between runs.

### Install on a device

The build stays containerized; only `adb` runs on the host:

```sh
sudo dnf install android-tools     # Fedora
make install                       # adb install -r the debug APK
```

### Notes

- **Container-only by design.** There is no Gradle wrapper (`gradlew`); the
  pinned Gradle version lives solely in the `Containerfile`. Build through
  `make`, not on the host.
- SDK level, build-tools, and Gradle versions are all `ARG`s at the top of the
  `Containerfile` — change them in one place.
- `minSdk` is 36. The widget relies on API 31+ RemoteViews features
  (`RemoteCollectionItems`, `setViewOutlinePreferredRadius`), which is why it
  needs no AndroidX or Glance dependency.
- The colours in `app/src/main/res/values/colors.xml` — `accent` `#D97757` and
  `background` `#1A1820` — are the shared brand palette. Use `@color/accent` and
  `@color/background` for new UI rather than introducing another palette.
