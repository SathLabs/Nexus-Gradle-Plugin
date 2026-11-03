# Nexus Gradle plugin

A Gradle plugin to go along the nexus minecraft library.

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

In `build.gradle`:

```groovy
plugins {
    id 'net.neoforged.moddev' version '2.0.147'
    id 'dev.satherov.nexus.gradle-plugin' version '1.1.0'
}
```

## Configuration

Example config
```groovy
nexus {
    // The players that will be automatically opped when joining a dev server.
    ops = ['Dev']

    gametest {
        // The mod that the gametests belong to. Needed only when more than one mod is declared to mod dev.
        modId = 'examplemod'

        // The source set the gametests live in.
        sourceSet = sourceSets.gametest

        // The directory -Precord writes golden images into.
        goldens = file('src/gametest/resources')

        // The harness dependency. Defaults to the project's own nexus dependency with the gametest capability.
        harness = dependencies.create('dev.satherov.nexus:nexus:1.0.0') {
            capabilities {
                requireCapability('dev.satherov.nexus:nexus-gametest')
            }
        }

        // Extra client runs, one per entry. The entry shaders adds the task runGameTestClientShaders.
        clients {
            shaders {
                // The mods on the classpath of this run only.
                mods libs.sodium

                // The system properties of this run, beside the shared ones.
                systemProperties.put('key', 'value')

                // The directory the goldens of this run are read from and recorded into.
                goldens = file('src/gametest/goldens/shaders')
            }
        }
    }
}
```
### Ops

The ops section will simply op any player with the given name joining a dev server. By default, the list will only contain `Dev`.
Exists for convenience, so you don't have to manually op yourself when testing on a dev server.

### Runs

All mod dev runs will use `runs/<name>` as their default directory, unless explicitly overwritten.
Exists so not all runs will be mushed into the same `run` directory.

### Gametests

The plugin will automatically configure two gametest runs for you, `runGameTestServer` and `runGameTestClient`.

Additionally, it will setup the `gametest` source set for your gametest to run in.

The gametest harness itself has no version and takes that of the project's `dev.satherov.nexus:nexus` dependency.


**Properties:**

- `modId`: 
  - The identifier that the mod that wants to use the gametests belongs to
  - Defaults to the first and only mod that mod dev knows.. By default this will be the first and only mod that moddev knows. 
  - Is required if there are multiple mods declared.
- `sourceSet`:
  - The source set that the gametests live in.
  - Defaults to the `gametest` source set that the plugin creates.
  - Compiles against the `main` source set and then adds itself to the mod.
- `goldens`:
  - The directory that `-Precord` writes the golden images in.
  - Defaults to the resource directory of the gametest source set.
- `harness`: 
  - The harness dependency.
  - Defaults to `dev.satherov.nexus:nexus` with the `dev.satherov.nexus:nexus-gametest` capability.

#### Client runs

The `clients` block holds extra client runs, one per entry. Every run gets the same `-P` properties as
`runGameTestClient`, the players to op and the virtual display under `-Pxvfb`. An entry `<name>` gets:

- The task `runGameTestClient<Name>`, with the first letter upper-cased.
- The run directory `runs/gametest-client-<name>`.
- The report side `client-<name>`, with its report at `build/reports/gametest/client-<name>/client-<name>.xml`.
- The goldens root `<name>` inside the `-Pgoldens` path, or inside `goldens` if the property is not given.
- The source set `gameTestClient<Name>`, whose runtime classpath is the gametest source set's plus the mods, so the
  mods reach that run only.

**Properties:**

- `mods`:
  - The mods on the runtime classpath of the run, as a coordinate string, a version catalog alias or a dependency.
  - Empty by default.
- `systemProperties`:
  - The system properties the run receives beside the shared ones. A key the plugin already set on the run is replaced.
  - Empty by default.
- `goldens`:
  - The directory the goldens of the run are read from and recorded into.
  - Defaults to the goldens root of the entry.

## Gametests

Run the server or client tests via one of the two gradle commands.
```
./gradlew runGameTestServer
./gradlew runGameTestClient
```

The following arguments are available:

| Property             | Description                                                                                             |
|----------------------|---------------------------------------------------------------------------------------------------------|
| `-Ptests=<selector>` | Specifies the identifiers of the tests to run. `*` can be used for wildcard matching.                   |
| `-Prealtime`         | Runs the tests in real time, where usually the game will sprint as fast as it can.                      |
| `-Pshow`             | Shows the client window, which is not rendered by default.                                              |
| `-Pcompare=<path>`   | Compares the performance measurements against the file in the given path.                               |
| `-Pgoldens=<dir>`    | The directory that the golden images are written into. Overrides the `nexus.gametest.goldens` property. |
| `-Precord`           | Records the golden images instead of checking them.                                                     |
| `-Pxvfb`             | Runs the client on a virtual X screen. Needs Xvfb installed. Will not work on a non linux system.       |

All paths are relative to the project directory.

The server report is written into `build/reports/gametest/server/server.xml`.
The client report is written into `build/reports/gametest/client/client.xml`.
