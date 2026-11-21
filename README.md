# Infernal Mobs (Fabric)

Fabric port of AtomicStryker's Infernal Mobs: hostile mobs randomly spawn with Diablo-style modifiers and drop loot rewards when defeated.

- Minecraft 1.21.11
- Fabric Loader 0.20.1+
- Fabric API (required)
- Confee (required)
- Mod Menu (optional, shows the config screen button)

If you want to use this mod in modpacks, see the license at the bottom of <https://atomicstryker.github.io/>

## Building

Confee is not on Modrinth yet: build it from its sources, then copy its Fabric jar to `.SourcesMods/` (see `confee_jar` in `gradle.properties`).

Loom requires Gradle itself to run on Java 25. Point `JAVA_HOME` to a Java 25 JDK, then (the mod itself is compiled for Java 21):

```sh
./gradlew build
```

The jar ends up in `build/libs/`.

To test in game, use `./gradlew runClient` or `./gradlew runServer`.

## Configuration

The config file is created on first launch at `config/infernalmobs-common.toml`. Every setting is documented in the file with its description, default value and allowed range. The config uses Confee, so it can also be edited in game from the Mod Menu config screen, with live previews of the rarities, mob health, outline and loot.

A `config/InfernalMobs-common.toml` file from an older version is renamed to `infernalmobs-common.toml` on first launch, its values are kept.

## For mod developers

Infernal Mobs fires two Fabric events, both in `atomicstryker.infernalmobs.common.InfernalMobsEvents`:

- `MOB_FARM_DETECTED`: the AntiMobFarm check found a chunk that went over the configured damage trigger.
- `ENTITY_TELEPORT`: a modifier is about to teleport a mob. A listener can return a different target, or `null` to cancel the teleport.

## Credits

Original mod by AtomicStryker: <https://github.com/AtomicStryker/atomicstrykers-minecraft-mods>
