package atomicstryker.infernalmobs.common;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Moves the config file of the versions before Confee to Confee's file, before Confee reads it.
 * <ul>
 *     <li>config/InfernalMobs-common.toml is renamed to config/infernalmobs-common.toml (Confee names the file from
 *     the mod id, which is lower case; the names differ on case sensitive file systems).</li>
 *     <li>The comments the old serializer wrote ("# ...") are removed. Confee writes its own ("#: ...") and keeps any
 *     other comment as written by hand, so they would be in the file twice.</li>
 * </ul>
 */
final class ConfigMigration {

    private static final Logger LOGGER = LogManager.getLogger();

    static final String OLD_FILE = "InfernalMobs-common.toml";
    static final String NEW_FILE = InfernalMobsCore.MOD_ID + "-common.toml";

    private ConfigMigration() {
    }

    static void migrate() {
        Path dir = FabricLoader.getInstance().getConfigDir();
        try {
            Path file = renameOldFile(dir);
            if (file != null) {
                stripOldComments(file);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not migrate the config file {} to {}, Confee may write the defaults", OLD_FILE, NEW_FILE, e);
        }
    }

    /**
     * @return the file to clean, or null when there is no old file to migrate
     */
    private static Path renameOldFile(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        // compare the exact names: on Windows and macOS, Files.exists ignores the case
        boolean hasOld;
        boolean hasNew;
        try (Stream<Path> files = Files.list(dir)) {
            List<String> names = files.map(path -> path.getFileName().toString()).toList();
            hasOld = names.contains(OLD_FILE);
            hasNew = names.contains(NEW_FILE);
        }
        if (hasNew) {
            if (hasOld) {
                LOGGER.warn("Both {} and {} exist, {} is used and {} is ignored", OLD_FILE, NEW_FILE, NEW_FILE, OLD_FILE);
            }
            return null;
        }
        if (!hasOld) {
            return null;
        }
        // through a temporary name, a case only rename is a no-op on case insensitive file systems
        Path temporary = dir.resolve(NEW_FILE + ".migrating");
        Files.move(dir.resolve(OLD_FILE), temporary, StandardCopyOption.REPLACE_EXISTING);
        Path target = dir.resolve(NEW_FILE);
        Files.move(temporary, target);
        LOGGER.info("Renamed the config file {} to {}", OLD_FILE, NEW_FILE);
        return target;
    }

    /**
     * The old serializer rewrote the whole file on every save, so every comment line in it is one it generated
     */
    private static void stripOldComments(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> kept = lines.stream().filter(line -> !line.stripLeading().startsWith("#")).toList();
        if (kept.size() != lines.size()) {
            Files.write(file, kept, StandardCharsets.UTF_8);
            LOGGER.info("Removed {} comment lines of the previous version from {}", lines.size() - kept.size(), NEW_FILE);
        }
    }
}
