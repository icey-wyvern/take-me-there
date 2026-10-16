# Take Me There

[![Build](https://github.com/icey-wyvern/take-me-there/actions/workflows/build.yml/badge.svg)](https://github.com/icey-wyvern/take-me-there/actions/workflows/build.yml)

Take Me There is a RuneLite destination picker for the Shortest Path plugin. Browse the destination catalog or search by name, save favourites for quick access, and add your own custom locations.

Take Me There selects a destination; Shortest Path performs the routing:

`Take Me There -> PluginMessage -> Shortest Path`

## Search palette

Press **Ctrl+T** to open the destination search palette. The shortcut is configurable in RuneLite's Take Me There settings. The search field is focused immediately: type a destination, use **Up/Down** to change the highlighted result, and press **Enter** to set the Shortest Path target. **Escape** or the configured shortcut closes the palette without choosing a destination. You can also single-click to highlight a result and double-click to choose it.

Search covers destination names, category terms, common RuneScape aliases, and reasonable misspellings. For example, try `cox`, `red chins`, `sawmill`, `rune shop`, or `magic shop`. The sidebar search uses the same matching while keeping its category layout.

With an empty search, **Enter** repeats the last selected destination if one exists. Use **Up/Down** to choose from recent or frequent destinations. With no previous destination, immediate Enter does nothing.

Take Me There stores selections locally in RuneLite configuration. Repeated choices can move relevant destinations higher for the same search, while stronger name matches keep priority. Take Me There sends no search telemetry.

The catalog has automated integrity tests and was bulk-validated against the Shortest Path pathfinder. Reachability can still depend on account access and transport settings.

## Development

Run tests with `./gradlew test` and build with `./gradlew build` (`.\gradlew.bat` on Windows).

## License

[BSD-2-Clause](LICENSE).
