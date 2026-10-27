package atomicstryker.infernalmobs.common;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Infernal Mobs config, stored in config/InfernalMobs-common.toml.
 * Declared like a NeoForge ModConfigSpec, on top of night-config which ModConfigSpec itself uses.
 */
public class Config {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Builder BUILDER = new Builder();

    private static final Value<Boolean> ANTI_MOB_FARM = BUILDER
            .comment("Determines whether to track the damage mobs take from players and environmental sources (cactus, drowning,\n" +
                    "falling, suffocation, lava) to detect mob farms. Other mods can react through the\n" +
                    "InfernalMobsEvents.MOB_FARM_DETECTED event.")
            .define("antiMobFarm", false);

    private static final Value<Long> MOB_FARM_CHECK_INTERVALS = BUILDER
            .comment("Defines the time in milliseconds between two mob farm checks.")
            .defineInRange("mobFarmCheckIntervals", 0L, 0L, Long.MAX_VALUE);

    private static final Value<Double> MOB_FARM_DAMAGE_TRIGGER = BUILDER
            .comment("Defines the damage dealt to mobs in one area between two checks above which the area is reported as a mob farm.")
            .defineInRange("mobFarmDamageTrigger", 0D, 0D, Double.MAX_VALUE);

    private static final Value<Integer> ELITE_RARITY = BUILDER
            .comment("Defines the chance for a hostile mob to spawn with modifiers (Rare): 1 in this value.\n" +
                    "Set it to 1 to give modifiers to every hostile mob.")
            .defineInRange("eliteRarity", 15, 1, Integer.MAX_VALUE);

    private static final Value<Integer> ULTRA_RARITY = BUILDER
            .comment("Defines the chance for a Rare mob to be upgraded to Ultra (3 to 4 more modifiers): 1 in this value.")
            .defineInRange("ultraRarity", 7, 1, Integer.MAX_VALUE);

    private static final Value<Integer> INFERNO_RARITY = BUILDER
            .comment("Defines the chance for an Ultra mob to be upgraded to Infernal (3 to 4 more modifiers): 1 in this value.")
            .defineInRange("infernoRarity", 7, 1, Integer.MAX_VALUE);

    private static final Value<Boolean> USE_SIMPLE_ENTITY_CLASS_NAMES = BUILDER
            .comment("Determines whether entities are identified by their Java class name (e.g. Zombie) instead of their\n" +
                    "registry name (e.g. zombie) in permittedentities, entitiesalwaysinfernal and entitybasehealth.")
            .define("useSimpleEntityClassNames", true);

    private static final Value<Boolean> DISABLE_HEALTH_BAR = BUILDER
            .comment("Determines whether to hide the boss bar showing the name, modifiers and health of the infernal mob you are looking at.")
            .define("disableHealthBar", false);

    private static final Value<Double> MOD_HEALTH_FACTOR = BUILDER
            .comment("Defines the health multiplier of infernal mobs.\n" +
                    "Max health = base health (see entitybasehealth) x number of modifiers x this value.")
            .defineInRange("modHealthFactor", 1D, 0D, Double.MAX_VALUE);

    private static final Value<Boolean> HEALTH_CHANGES_DISABLED = BUILDER
            .comment("Determines whether infernal mobs keep their vanilla max health instead of getting more health per modifier.")
            .define("healthChangesDisabled", false);

    private static final Value<Double> MOD_COOLDOWN_FACTOR = BUILDER
            .comment("Defines the multiplier applied to the cooldown of every modifier ability.\n" +
                    "Below 1 abilities are used more often, above 1 less often.")
            .defineInRange("modCooldownFactor", 1D, 0D, Double.MAX_VALUE);

    private static final Value<Double> MAX_DAMAGE = BUILDER
            .comment("Defines the maximum damage dealt by modifiers that deal or reflect damage (Berserk, Ender, Ninja, Vengeance).")
            .defineInRange("maxDamage", 10D, 0D, Double.MAX_VALUE);

    private static final Value<List<String>> DROPPED_ITEMS_ELITE = BUILDER
            .comment("Defines the loot dropped by Rare mobs, one item stack per entry in SNBT format.\n" +
                    "Example: \"{id:'minecraft:cookie',count:5}\"")
            .defineList("droppedItemIDsElite", List.of(
                    item("iron_shovel"), item("iron_pickaxe"), item("iron_axe"), item("iron_sword"), item("iron_hoe"),
                    item("chainmail_helmet"), item("chainmail_boots"), item("chainmail_chestplate"), item("chainmail_leggings"),
                    item("iron_helmet"), item("iron_boots"), item("iron_chestplate"), item("iron_leggings"),
                    item("cookie", 5)));

    private static final Value<List<String>> DROPPED_ITEMS_ULTRA = BUILDER
            .comment("Defines the loot dropped by Ultra mobs, one item stack per entry in SNBT format.")
            .defineList("droppedItemIDsUltra", List.of(
                    item("iron_hoe"), item("bow"),
                    item("chainmail_helmet"), item("chainmail_boots"), item("chainmail_chestplate"), item("chainmail_leggings"),
                    item("iron_helmet"), item("iron_boots"), item("iron_chestplate"), item("iron_leggings"),
                    item("golden_helmet"), item("golden_boots"), item("golden_chestplate"), item("golden_leggings"),
                    item("golden_apple", 3), item("blaze_powder", 5), item("enchanted_book")));

    private static final Value<List<String>> DROPPED_ITEMS_INFERNAL = BUILDER
            .comment("Defines the loot dropped by Infernal mobs, one item stack per entry in SNBT format.")
            .defineList("droppedItemIDsInfernal", List.of(
                    item("enchanted_book"), item("diamond", 3),
                    item("diamond_sword"), item("diamond_axe"), item("diamond_hoe"), item("diamond_pickaxe"), item("diamond_shovel"),
                    item("chainmail_helmet"), item("chainmail_boots"), item("chainmail_chestplate"), item("chainmail_leggings"),
                    item("diamond_helmet"), item("diamond_boots"), item("diamond_chestplate"), item("diamond_leggings"),
                    item("ender_pearl", 3)));

    private static final Value<List<String>> DIMENSION_BLACKLIST = BUILDER
            .comment("Defines the dimensions where mobs never get modifiers.\n" +
                    "Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]")
            .defineList("dimensionIDBlackList", List.of());

    private static final Value<List<String>> ENTITY_TAG_BLACKLIST = BUILDER
            .comment("Mobs having one of these NBT keys or scoreboard tags never get modifiers.\n" +
                    "Example: [\"affixes\"] to leave the mobs of the Champions mod alone.")
            .defineList("entityTagBlackList", List.of());

    public static final Table<Boolean> MODS_ENABLED = BUILDER
            .comment("Determines which modifiers are enabled.")
            .defineTable("modsEnabled", value -> value instanceof Boolean bool ? bool : null);

    public static final Table<Boolean> PERMITTED_ENTITIES = BUILDER
            .comment("Determines which mobs can get modifiers.\n" +
                    "Every mob is added here with true the first time it spawns, set it to false to exclude it.")
            .defineTable("permittedentities", value -> value instanceof Boolean bool ? bool : null);

    public static final Table<Boolean> ENTITIES_ALWAYS_INFERNAL = BUILDER
            .comment("Determines which mobs always spawn with modifiers.\n" +
                    "Every mob is added here with false the first time it spawns.")
            .defineTable("entitiesalwaysinfernal", value -> value instanceof Boolean bool ? bool : null);

    public static final Table<Double> ENTITY_BASE_HEALTH = BUILDER
            .comment("Defines the base health used to compute the health of infernal mobs.\n" +
                    "Every mob is added here with its vanilla max health the first time it spawns.")
            .defineTable("entitybasehealth", value -> value instanceof Number number ? number.doubleValue() : null);

    public static boolean antiMobFarm;
    public static long mobFarmCheckIntervals;
    public static double mobFarmDamageTrigger;
    public static int eliteRarity;
    public static int ultraRarity;
    public static int infernoRarity;
    public static boolean useSimpleEntityClassNames;
    public static boolean disableHealthBar;
    public static double modHealthFactor;
    public static boolean healthChangesDisabled;
    public static double modCooldownFactor;
    public static double maxDamage;
    public static List<String> droppedItemIDsElite;
    public static List<String> droppedItemIDsUltra;
    public static List<String> droppedItemIDsInfernal;
    public static List<String> dimensionIDBlackList;
    public static List<String> entityTagBlackList;

    private static CommentedFileConfig config;
    private static boolean dirty;

    /**
     * Reads the file, adds missing values, resets invalid ones to their default and rewrites the comments
     */
    public static void load(Path path) {
        config = CommentedFileConfig.builder(path, TomlFormat.instance())
                .sync()
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build();
        config.load();
        BUILDER.spec.correct(config, (action, valuePath, incorrectValue, correctedValue) ->
                LOGGER.warn("Config value {} was {}, {} to {}", String.join(".", valuePath), incorrectValue, action, correctedValue));
        BUILDER.comments.forEach(config::setComment);
        config.save();
        onLoad();
    }

    public static boolean isLoaded() {
        return config != null;
    }

    /**
     * Writes the entries added by {@link Table#getOrAdd} since the last save
     */
    public static void save() {
        if (dirty) {
            config.save();
            dirty = false;
        }
    }

    private static void onLoad() {
        antiMobFarm = ANTI_MOB_FARM.get();
        mobFarmCheckIntervals = MOB_FARM_CHECK_INTERVALS.get();
        mobFarmDamageTrigger = MOB_FARM_DAMAGE_TRIGGER.get();
        eliteRarity = ELITE_RARITY.get();
        ultraRarity = ULTRA_RARITY.get();
        infernoRarity = INFERNO_RARITY.get();
        useSimpleEntityClassNames = USE_SIMPLE_ENTITY_CLASS_NAMES.get();
        disableHealthBar = DISABLE_HEALTH_BAR.get();
        modHealthFactor = MOD_HEALTH_FACTOR.get();
        healthChangesDisabled = HEALTH_CHANGES_DISABLED.get();
        modCooldownFactor = MOD_COOLDOWN_FACTOR.get();
        maxDamage = MAX_DAMAGE.get();
        droppedItemIDsElite = DROPPED_ITEMS_ELITE.get();
        droppedItemIDsUltra = DROPPED_ITEMS_ULTRA.get();
        droppedItemIDsInfernal = DROPPED_ITEMS_INFERNAL.get();
        dimensionIDBlackList = DIMENSION_BLACKLIST.get();
        entityTagBlackList = ENTITY_TAG_BLACKLIST.get();
    }

    private static String item(String id) {
        return item(id, 1);
    }

    private static String item(String id, int count) {
        return "{id:'minecraft:" + id + "',count:" + count + "}";
    }

    /**
     * A single config value, read from the loaded file
     */
    public static final class Value<T> {
        private final String key;
        private final Function<Object, T> converter;

        private Value(String key, Function<Object, T> converter) {
            this.key = key;
            this.converter = converter;
        }

        public T get() {
            return converter.apply(config.get(key));
        }
    }

    /**
     * A table whose entries are added at runtime, like the entity lists filled in as mobs spawn
     */
    public static final class Table<V> {
        private final String key;
        private final Function<Object, V> converter;

        private Table(String key, Function<Object, V> converter) {
            this.key = key;
            this.converter = converter;
        }

        /**
         * @return the configured value of entry, or defaultValue after adding it to the table if it is missing or invalid
         */
        public V getOrAdd(String entry, V defaultValue) {
            CommentedConfig table = config.get(key);
            // valueMap instead of get(), entries like "minecraft:zombie" must not be split into paths
            Map<String, Object> entries = table.valueMap();
            V value = converter.apply(entries.get(entry));
            if (value == null) {
                entries.put(entry, defaultValue);
                dirty = true;
                return defaultValue;
            }
            return value;
        }
    }

    /**
     * Same API as ModConfigSpec.Builder: comment() applies to the next defined value.
     * Comments get the Forge layout, with the default and the range on their own lines.
     */
    private static final class Builder {
        private final ConfigSpec spec = new ConfigSpec(TomlFormat.newConfig(LinkedHashMap::new));
        private final Map<String, String> comments = new LinkedHashMap<>();
        private String comment;

        Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        Value<Boolean> define(String key, boolean defaultValue) {
            spec.define(key, defaultValue, value -> value instanceof Boolean);
            addComment(key, "Default: " + defaultValue);
            return new Value<>(key, value -> (Boolean) value);
        }

        Value<Integer> defineInRange(String key, int defaultValue, int min, int max) {
            return defineNumber(key, defaultValue, min, max, Number::intValue);
        }

        Value<Long> defineInRange(String key, long defaultValue, long min, long max) {
            return defineNumber(key, defaultValue, min, max, Number::longValue);
        }

        Value<Double> defineInRange(String key, double defaultValue, double min, double max) {
            return defineNumber(key, defaultValue, min, max, Number::doubleValue);
        }

        /**
         * any number type is accepted, so writing 2 instead of 2.0 in the file does not reset the value
         */
        private <T extends Number> Value<T> defineNumber(String key, T defaultValue, T min, T max, Function<Number, T> converter) {
            Predicate<Object> validator = value -> value instanceof Number number
                    && number.doubleValue() >= min.doubleValue() && number.doubleValue() <= max.doubleValue();
            spec.define(key, defaultValue, validator);
            addComment(key, "Default: " + defaultValue + "\n Range: " + formatRange(min, max));
            return new Value<>(key, value -> converter.apply((Number) value));
        }

        @SuppressWarnings("unchecked")
        Value<List<String>> defineList(String key, List<String> defaultValue) {
            spec.defineList(key, defaultValue, element -> element instanceof String);
            addComment(key, null);
            return new Value<>(key, value -> List.copyOf((List<String>) value));
        }

        <V> Table<V> defineTable(String key, Function<Object, V> converter) {
            spec.define(key, () -> TomlFormat.newConfig(LinkedHashMap::new), value -> value instanceof UnmodifiableConfig);
            addComment(key, null);
            return new Table<>(key, converter);
        }

        private void addComment(String key, String details) {
            String text = comment == null ? "" : comment;
            if (details != null) {
                text = text + (text.isEmpty() ? " " : "\n ") + details;
            }
            if (!text.isEmpty()) {
                comments.put(key, text);
            }
            comment = null;
        }

        private static String formatRange(Number min, Number max) {
            boolean unbounded = max.doubleValue() == Integer.MAX_VALUE || max.doubleValue() == Long.MAX_VALUE || max.doubleValue() == Double.MAX_VALUE;
            return unbounded ? "> " + min : min + " ~ " + max;
        }
    }
}
