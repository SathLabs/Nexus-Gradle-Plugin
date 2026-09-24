# Nexus Gradle plugin

A Gradle plugin that sets up gametests for a NeoForge mod built with moddev and tested with the Nexus gametest harness.

It adds to the project:

- A `gametest` source set, compiled against `main` and the harness and added to the mod.
- Two moddev runs, `gameTestServer` and `gameTestClient`, started with `runGameTestServer` and `runGameTestClient`.

## Setup

In `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven {
            url = 'https://maven.satherov.dev/releases'
        }
        gradlePluginPortal()
    }
}
```

The plugin comes from `maven.satherov.dev`, and `gradlePluginPortal()` is there for moddev.

In `build.gradle`, together with moddev:

```groovy
plugins {
    id 'net.neoforged.moddev' version '2.0.147'
    id 'dev.satherov.nexus.gradle-plugin' version '1.0.0'
}
```

The harness has no version of its own and takes the one of the project's `dev.satherov.nexus:nexus` dependency.

## Configuration

Everything is configured in the `nexus` block:

```groovy
nexus {
    ops = ['Dev', 'Satherov']
    gametest {
        modId = 'examplemod'
        goldens = file('src/gametest/goldens')
    }
}
```

`ops` holds the names of the players every dev server makes an operator when they log in. Defaults to `Dev`. An empty
list ops nobody. Every moddev run gets the names as the system property `nexus.dev.ops`, comma separated.

Every moddev run uses `runs/<run name>` as its game directory unless the build script sets one, so the `client` run
runs in `runs/client`. The gametest runs use `runs/gametest` and `runs/gametest-client`.

The `gametest` block has four properties:

- `modId`: The identifier of the mod the gametests belong to. Defaults to the only mod moddev knows, and is required if
  moddev knows several.
- `sourceSet`: The source set the gametests live in. Defaults to the `gametest` source set the plugin creates. The
  plugin compiles it against `main` and the harness and adds it to the mod, so the build script must not add it to a
  mod itself.
- `goldens`: The directory `-Precord` writes golden images into. Defaults to the resources directory of the source set.
- `harness`: The harness dependency. Defaults to `dev.satherov.nexus:nexus` with the `dev.satherov.nexus:nexus-gametest`
  capability and no version.

## Running

```
./gradlew runGameTestServer
./gradlew runGameTestClient
```

The runs take these project properties:

| Property             | Effect                                                                                        |
|----------------------|-----------------------------------------------------------------------------------------------|
| `-Ptests=<selector>` | Runs only the tests the selector matches. A selector without a namespace gets `*:` in front.  |
| `-Prealtime`         | Runs the tests in real time.                                                                  |
| `-Pshow`             | Shows the client window, which is hidden by default.                                          |
| `-Pcompare=<path>`   | Compares the measurements against the file at the given path.                                 |
| `-Pgoldens=<dir>`    | The directory golden images are written into. Overrides `nexus.gametest.goldens` for the run. |
| `-Precord`           | Records golden images instead of comparing against them.                                      |
| `-Pxvfb`             | Runs the client on a virtual X screen. Needs Xvfb installed.                                  |

Paths are relative to the project directory.

The server run writes its report to `build/reports/gametest/server/server.xml`, the client run to
`build/reports/gametest/client/client.xml`.
