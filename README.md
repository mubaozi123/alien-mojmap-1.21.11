# Alien (Mojmap source project)

Reconstructed Fabric mod source project using official Mojang mappings (Mojmap).
The project targets **Minecraft 1.21.11**.

## Requirements

- JDK 21, with `JAVA_HOME` pointing to its installation.
- Internet access on the first build to download Gradle, Minecraft, mappings, and Maven dependencies.
- The two dependency JARs included in `libs/`.

## Build

Run these commands from the directory containing `build.gradle`.

Linux / macOS:

```sh
chmod +x gradlew
./gradlew clean build
```

Windows (PowerShell):

```powershell
.\gradlew.bat clean build
```

The wrapper downloads Gradle automatically; a separate Gradle installation is not required.
Build outputs are written to `build/libs/`. Use the remapped mod JAR
`alien-1.21.11+mojmap-rebuilt.jar` for installation, rather than the sources or development JAR.

## Configured versions

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Java | 21 |
| Gradle wrapper | 9.5.1 |
| Fabric Loom (`net.fabricmc.fabric-loom-remap`) | 1.17.21 |
| Fabric Loader (build dependency) | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Baritone (local dependency) | 1.17.0 |
| Sodium (local dependency) | 0.8.14+mc1.21.11 |

The mappings are configured with `loom.officialMojangMappings()` in `build.gradle`.
Loom remaps the build output for Fabric distribution.

## Project layout

- `src/main/java/`: Java sources, including Mixin classes.
- `src/main/resources/`: mod metadata, Mixin configurations, access widener, fonts, textures, shaders, and resource license.
- `libs/`: local Baritone and Sodium dependencies referenced by `modImplementation files(...)`. Keep both JARs to compile this project.
- `gradle/wrapper/`, `gradlew`, `gradlew.bat`: Gradle wrapper files, including the required wrapper JAR.
- `build.gradle`, `settings.gradle`, `gradle.properties`: build configuration.
- `LICENSE`: project license.

## Running

For a development client, run `./gradlew runClient` (Linux / macOS) or
`.\gradlew.bat runClient` (Windows PowerShell). A graphical environment is required.

For a normal Minecraft installation, use a Fabric profile for Minecraft 1.21.11
with Java 21 and install the remapped mod JAR into its `mods` directory.
Supply compatible runtime dependencies separately; the local dependency JARs are
not bundled by the current build script. The development runtime includes the
Fabric API, Baritone, and Sodium dependencies declared in `build.gradle`.

## Cleanup and verification

This source package omits the previous `verification/` reports, logs, audit tools,
and environment-specific helper, as well as the `.github/` CI/release workflow.
They are not referenced by the Gradle build. Sources, runtime resources, build
configuration, local dependency JARs, and wrapper files are retained.

`build/`, `.gradle/`, and `run/` are generated locally and are excluded by `.gitignore`.
Use the Gradle `clean` task to remove build outputs.

The cleanup was checked against the supplied ZIP: retained files are byte-for-byte
unchanged apart from the executable permission on `gradlew`, and this README is new.
A fresh compilation and an in-game launch were not performed as part of this cleanup.
This reconstructed source project may still have runtime compatibility issues;
compilation alone does not validate Mixin targets or gameplay behavior.

## License

See `LICENSE`. Third-party dependency JARs retain their respective licenses.
