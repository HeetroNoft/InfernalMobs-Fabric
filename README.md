# Infernal Mobs (Fabric)

Fabric port of AtomicStryker's Infernal Mobs: hostile mobs randomly spawn with Diablo-style modifiers and drop loot rewards when defeated.

- Minecraft 26.2
- Fabric Loader 0.20.1+
- Fabric API (required)

If you want to use this mod in modpacks, see the license at the bottom of <https://atomicstryker.github.io/>

## Building

Loom requires Gradle itself to run on Java 25. Point `JAVA_HOME` to a Java 25 JDK, then:

```sh
./gradlew build
```

The jar ends up in `build/libs/`.

To test in game, use `./gradlew runClient` or `./gradlew runServer`.

## Configuration

The config file is created on first launch at `config/infernalmobs.toml` (TOML). An older `config/infernalmobs.cfg` (JSON) is converted automatically and kept as `infernalmobs.cfg.bak`.

## For mod developers

Infernal Mobs fires two Fabric events, both in `atomicstryker.infernalmobs.common.InfernalMobsEvents`:

- `MOB_FARM_DETECTED`: the AntiMobFarm check found a chunk that went over the configured damage trigger.
- `ENTITY_TELEPORT`: a modifier is about to teleport a mob. A listener can return a different target, or `null` to cancel the teleport.

## Credits

Original mod by AtomicStryker: <https://github.com/AtomicStryker/atomicstrykers-minecraft-mods>
