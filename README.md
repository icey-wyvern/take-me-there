# Take Me There

[![Build](https://github.com/icey-wyvern/take-me-there/actions/workflows/build.yml/badge.svg)](https://github.com/icey-wyvern/take-me-there/actions/workflows/build.yml)

Take Me There is a RuneLite destination picker for the Shortest Path plugin. Browse the destination catalog or search by name, save favourites for quick access, and add your own custom locations.

Take Me There selects a destination; Shortest Path performs the routing:

`Take Me There -> PluginMessage -> Shortest Path`

The catalog has automated integrity tests and was bulk-validated against the Shortest Path pathfinder. Reachability can still depend on account access and transport settings.

## Development

Run tests with `./gradlew test` and build with `./gradlew build` (`.\gradlew.bat` on Windows).

## License

[BSD-2-Clause](LICENSE).
