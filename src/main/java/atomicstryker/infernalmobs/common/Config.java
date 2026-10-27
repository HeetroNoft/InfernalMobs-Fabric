package atomicstryker.infernalmobs.common;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Infernal Mobs config, registered with AutoConfig (Cloth Config) and stored in config/InfernalMobs-common.toml.
 * The config screen is generated from these fields, the file is written by {@link CommentedTomlSerializer}.
 */
@me.shedaniel.autoconfig.annotation.Config(name = "InfernalMobs-common")
public class Config implements ConfigData {

    private static final Logger LOGGER = LogManager.getLogger();

    @Comment("Determines whether to track the damage mobs take from players and environmental sources (cactus, drowning,\n" +
            "falling, suffocation, lava) to detect mob farms. Other mods can react through the\n" +
            "InfernalMobsEvents.MOB_FARM_DETECTED event.")
    @ConfigEntry.Gui.Tooltip
    public boolean antiMobFarm = false;

    @Comment("Defines the time in milliseconds between two mob farm checks.\n Range: > 0")
    @ConfigEntry.Gui.Tooltip
    public long mobFarmCheckIntervals = 0L;

    @Comment("Defines the damage dealt to mobs in one area between two checks above which the area is reported as a mob farm.\n Range: > 0.0")
    @ConfigEntry.Gui.Tooltip
    public double mobFarmDamageTrigger = 0D;

    @Comment("Defines the chance for a hostile mob to spawn with modifiers (Rare): 1 in this value.\n" +
            "Set it to 1 to give modifiers to every hostile mob.\n Range: > 1")
    @ConfigEntry.Gui.Tooltip
    public int eliteRarity = 15;

    @Comment("Defines the chance for a Rare mob to be upgraded to Ultra (3 to 4 more modifiers): 1 in this value.\n Range: > 1")
    @ConfigEntry.Gui.Tooltip
    public int ultraRarity = 7;

    @Comment("Defines the chance for an Ultra mob to be upgraded to Infernal (3 to 4 more modifiers): 1 in this value.\n Range: > 1")
    @ConfigEntry.Gui.Tooltip
    public int infernoRarity = 7;

    @Comment("Determines whether entities are identified by their Java class name (e.g. Zombie) instead of their\n" +
            "registry name (e.g. zombie) in permittedentities, entitiesalwaysinfernal and entitybasehealth.")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.RequiresRestart
    public boolean useSimpleEntityClassNames = true;

    @Comment("Determines whether to hide the boss bar showing the name, modifiers and health of the infernal mob you are looking at.")
    @ConfigEntry.Gui.Tooltip
    public boolean disableHealthBar = false;

    @Comment("Defines the health multiplier of infernal mobs.\n" +
            "Max health = base health (see entitybasehealth) x number of modifiers x this value.\n Range: > 0.0")
    @ConfigEntry.Gui.Tooltip
    public double modHealthFactor = 1D;

    @Comment("Determines whether infernal mobs keep their vanilla max health instead of getting more health per modifier.")
    @ConfigEntry.Gui.Tooltip
    public boolean healthChangesDisabled = false;

    @Comment("Defines the multiplier applied to the cooldown of every modifier ability.\n" +
            "Below 1 abilities are used more often, above 1 less often.\n Range: > 0.0")
    @ConfigEntry.Gui.Tooltip
    public double modCooldownFactor = 1D;

    @Comment("Defines the maximum damage dealt by modifiers that deal or reflect damage (Berserk, Ender, Ninja, Vengeance).\n Range: > 0.0")
    @ConfigEntry.Gui.Tooltip
    public double maxDamage = 10D;

    @Comment("Defines the loot dropped by Rare mobs, one item stack per entry in SNBT format.\n" +
            "Example: \"{id:'minecraft:cookie',count:5}\"")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.RequiresRestart
    public List<String> droppedItemIDsElite = items(
            "iron_shovel", "iron_pickaxe", "iron_axe", "iron_sword", "iron_hoe",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "iron_helmet", "iron_boots", "iron_chestplate", "iron_leggings",
            "cookie:5");

    @Comment("Defines the loot dropped by Ultra mobs, one item stack per entry in SNBT format.")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.RequiresRestart
    public List<String> droppedItemIDsUltra = items(
            "iron_hoe", "bow",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "iron_helmet", "iron_boots", "iron_chestplate", "iron_leggings",
            "golden_helmet", "golden_boots", "golden_chestplate", "golden_leggings",
            "golden_apple:3", "blaze_powder:5", "enchanted_book");

    @Comment("Defines the loot dropped by Infernal mobs, one item stack per entry in SNBT format.")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.RequiresRestart
    public List<String> droppedItemIDsInfernal = items(
            "enchanted_book", "diamond:3",
            "diamond_sword", "diamond_axe", "diamond_hoe", "diamond_pickaxe", "diamond_shovel",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "diamond_helmet", "diamond_boots", "diamond_chestplate", "diamond_leggings",
            "ender_pearl:3");

    @Comment("Defines the dimensions where mobs never get modifiers.\n" +
            "Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]")
    @ConfigEntry.Gui.Tooltip
    public List<String> dimensionIDBlackList = new ArrayList<>();

    @Comment("Mobs having one of these NBT keys or scoreboard tags never get modifiers.\n" +
            "Example: [\"affixes\"] to leave the mobs of the Champions mod alone.")
    @ConfigEntry.Gui.Tooltip
    public List<String> entityTagBlackList = new ArrayList<>();

    // the tables below are filled in at runtime, the config screen has no editor for maps so they are only in the file

    @Comment("Determines which modifiers are enabled.")
    @ConfigEntry.Gui.Excluded
    public Map<String, Boolean> modsEnabled = new LinkedHashMap<>();

    @Comment("Determines which mobs can get modifiers.\n" +
            "Every mob is added here with true the first time it spawns, set it to false to exclude it.")
    @ConfigEntry.Gui.Excluded
    public Map<String, Boolean> permittedentities = new LinkedHashMap<>();

    @Comment("Determines which mobs always spawn with modifiers.\n" +
            "Every mob is added here with false the first time it spawns.")
    @ConfigEntry.Gui.Excluded
    public Map<String, Boolean> entitiesalwaysinfernal = new LinkedHashMap<>();

    @Comment("Defines the base health used to compute the health of infernal mobs.\n" +
            "Every mob is added here with its vanilla max health the first time it spawns.")
    @ConfigEntry.Gui.Excluded
    public Map<String, Double> entitybasehealth = new LinkedHashMap<>();

    private static ConfigHolder<Config> holder;

    /**
     * Loads the file and rewrites it, so missing values and comments are added
     */
    public static void register() {
        holder = AutoConfig.register(Config.class, CommentedTomlSerializer::new);
        holder.save();
    }

    public static Config get() {
        return holder.getConfig();
    }

    public static void save() {
        holder.save();
    }

    /**
     * Values out of range are reset to their default, a rarity of 0 would crash the spawn roll
     */
    @Override
    public void validatePostLoad() {
        Config defaults = new Config();
        mobFarmCheckIntervals = (long) checkRange("mobFarmCheckIntervals", mobFarmCheckIntervals, 0, defaults.mobFarmCheckIntervals);
        mobFarmDamageTrigger = checkRange("mobFarmDamageTrigger", mobFarmDamageTrigger, 0, defaults.mobFarmDamageTrigger);
        eliteRarity = (int) checkRange("eliteRarity", eliteRarity, 1, defaults.eliteRarity);
        ultraRarity = (int) checkRange("ultraRarity", ultraRarity, 1, defaults.ultraRarity);
        infernoRarity = (int) checkRange("infernoRarity", infernoRarity, 1, defaults.infernoRarity);
        modHealthFactor = checkRange("modHealthFactor", modHealthFactor, 0, defaults.modHealthFactor);
        modCooldownFactor = checkRange("modCooldownFactor", modCooldownFactor, 0, defaults.modCooldownFactor);
        maxDamage = checkRange("maxDamage", maxDamage, 0, defaults.maxDamage);
    }

    private static double checkRange(String name, double value, double min, double defaultValue) {
        if (value < min) {
            LOGGER.warn("Config value {} = {} is below {}, using the default {}", name, value, min, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * @param entries item ids, optionally followed by :count
     */
    private static List<String> items(String... entries) {
        List<String> items = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            items.add("{id:'minecraft:" + parts[0] + "',count:" + (parts.length > 1 ? parts[1] : "1") + "}");
        }
        return items;
    }
}
