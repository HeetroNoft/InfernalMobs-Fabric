package atomicstryker.infernalmobs.common;

import atomicstryker.infernalmobs.client.ConfigPreviews;
import fr.heetstudio.confee.api.Confee;
import fr.heetstudio.confee.api.ConfeeConfig;
import fr.heetstudio.confee.api.OnFileError;
import fr.heetstudio.confee.api.SuggestionSource;
import fr.heetstudio.confee.api.annotation.AllowDuplicates;
import fr.heetstudio.confee.api.annotation.Comment;
import fr.heetstudio.confee.api.annotation.Dimension;
import fr.heetstudio.confee.api.annotation.Hidden;
import fr.heetstudio.confee.api.annotation.ItemStack;
import fr.heetstudio.confee.api.annotation.Preview;
import fr.heetstudio.confee.api.annotation.Range;
import fr.heetstudio.confee.api.annotation.RequiresRestart;
import fr.heetstudio.confee.api.annotation.Section;
import fr.heetstudio.confee.api.annotation.Suggestions;
import fr.heetstudio.confee.api.annotation.Theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Infernal Mobs config, registered with Confee and stored in config/infernalmobs-common.toml.
 * Confee writes the file (with the comments below) and generates the config screen from these fields.
 * The previews of the screen are drawn by {@link ConfigPreviews} (created on the client only).
 */
// the versions before Confee wrote config/InfernalMobs-common.toml, with a "Default:" line per option
@fr.heetstudio.confee.api.annotation.Config(modId = InfernalMobsCore.MOD_ID, name = "common", writeDefaults = true,
        legacyFiles = "InfernalMobs-common.toml")
// Colors of the logo: ember red accent on neutral gray panels
@Theme(
        accent = 0xFFC8401E,
        panelBg = 0xFF2C2C2E,
        panelBgDeep = 0xFF1A1A1B,
        buttonFace = 0xFFD4D4D6,
        buttonFaceHover = 0xFFE8E8EA,
        buttonShadow = 0xFF5A5A5C,
        outline = 0xFF1A1A1B,
        toggleOffTrack = 0xFF8A8A8D,
        titleBarBg = 0xFFE6E6E8,
        titleBarText = 0xFF1A1A1B)
public class Config {

    @Comment("Determines whether to track the damage mobs take from players and environmental sources (cactus, drowning,\n" +
            "falling, suffocation, lava) to detect mob farms. Other mods can react through the\n" +
            "InfernalMobsEvents.MOB_FARM_DETECTED event.")
    public boolean antiMobFarm = false;

    @Comment("Defines the time in milliseconds between two mob farm checks.")
    @Range(min = 0)
    public long mobFarmCheckIntervals = 0L;

    @Comment("Defines the damage dealt to mobs in one area between two checks above which the area is reported as a mob farm.")
    @Range(min = 0)
    public double mobFarmDamageTrigger = 0D;

    @Comment("Defines the chance for a hostile mob to spawn with modifiers (Rare): 1 in this value.\n" +
            "Set it to 1 to give modifiers to every hostile mob.")
    @Range(min = 1)
    @Preview(renderer = ConfigPreviews.Rarity.class)
    public int eliteRarity = 15;

    @Comment("Defines the chance for a Rare mob to be upgraded to Ultra (3 to 4 more modifiers): 1 in this value.")
    @Range(min = 1)
    @Preview(renderer = ConfigPreviews.Rarity.class)
    public int ultraRarity = 7;

    @Comment("Defines the chance for an Ultra mob to be upgraded to Infernal (3 to 4 more modifiers): 1 in this value.")
    @Range(min = 1)
    @Preview(renderer = ConfigPreviews.Rarity.class)
    public int infernoRarity = 7;

    @Comment("Determines whether entities are identified by their Java class name (e.g. Zombie) instead of their\n" +
            "registry name (e.g. zombie) in permittedentities, entitiesalwaysinfernal and entitybasehealth.")
    @RequiresRestart
    public boolean useSimpleEntityClassNames = true;

    @Comment("Determines whether to hide the boss bar showing the name, modifiers and health of the infernal mob you are looking at.")
    public boolean disableHealthBar = false;

    @Comment("Defines the health multiplier of infernal mobs.\n" +
            "Max health = base health (see entitybasehealth) x number of modifiers x this value.")
    @Range(min = 0)
    @Preview(renderer = ConfigPreviews.Health.class)
    public double modHealthFactor = 1D;

    @Comment("Determines whether infernal mobs keep their vanilla max health instead of getting more health per modifier.")
    @Preview(renderer = ConfigPreviews.Health.class)
    public boolean healthChangesDisabled = false;

    @Comment("Defines the multiplier applied to the cooldown of every modifier ability.\n" +
            "Below 1 abilities are used more often, above 1 less often.")
    @Range(min = 0)
    public double modCooldownFactor = 1D;

    @Comment("Defines the maximum damage dealt by modifiers that deal or reflect damage (Berserk, Ender, Ninja, Vengeance).")
    @Range(min = 0)
    public double maxDamage = 10D;

    // the same stack listed twice drops twice as often. A stack of the file that cannot be read is kept as written:
    // ItemConfigHelper skips it with a log line, the screen shows its error until it is fixed
    @Comment("Defines the loot dropped by Rare mobs, one item stack per entry in SNBT format.\n" +
            "Example: \"{id:'minecraft:cookie',count:5}\"")
    @ItemStack(onFileError = OnFileError.KEEP)
    @RequiresRestart
    @AllowDuplicates
    @Preview(renderer = ConfigPreviews.Loot.class)
    public List<String> droppedItemIDsElite = items(
            "iron_shovel", "iron_pickaxe", "iron_axe", "iron_sword", "iron_hoe",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "iron_helmet", "iron_boots", "iron_chestplate", "iron_leggings",
            "cookie:5");

    @Comment("Defines the loot dropped by Ultra mobs, one item stack per entry in SNBT format.")
    @ItemStack(onFileError = OnFileError.KEEP)
    @RequiresRestart
    @AllowDuplicates
    @Preview(renderer = ConfigPreviews.Loot.class)
    public List<String> droppedItemIDsUltra = items(
            "iron_hoe", "bow",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "iron_helmet", "iron_boots", "iron_chestplate", "iron_leggings",
            "golden_helmet", "golden_boots", "golden_chestplate", "golden_leggings",
            "golden_apple:3", "blaze_powder:5", "enchanted_book");

    @Comment("Defines the loot dropped by Infernal mobs, one item stack per entry in SNBT format.")
    @ItemStack(onFileError = OnFileError.KEEP)
    @RequiresRestart
    @AllowDuplicates
    @Preview(renderer = ConfigPreviews.Loot.class)
    public List<String> droppedItemIDsInfernal = items(
            "enchanted_book", "diamond:3",
            "diamond_sword", "diamond_axe", "diamond_hoe", "diamond_pickaxe", "diamond_shovel",
            "chainmail_helmet", "chainmail_boots", "chainmail_chestplate", "chainmail_leggings",
            "diamond_helmet", "diamond_boots", "diamond_chestplate", "diamond_leggings",
            "ender_pearl:3");

    @Comment("Defines the dimensions where mobs never get modifiers.\n" +
            "Example: [\"minecraft:the_nether\", \"minecraft:the_end\"]")
    @Dimension
    public List<String> dimensionIDBlackList = new ArrayList<>();

    @Comment("Mobs having one of these NBT keys or scoreboard tags never get modifiers.\n" +
            "Example: [\"affixes\"] to leave the mobs of the Champions mod alone.")
    // the Champions mod marks its mobs with "affixes"; the other suggestions are the scoreboard tags of the world
    @Suggestions(value = "affixes", sources = SuggestionSource.SCOREBOARD_TAGS)
    public List<String> entityTagBlackList = new ArrayList<>();

    @Comment("Settings of the colored outline drawn around infernal mobs, only used on the client.\n" +
            "Rare mobs are outlined in green, Ultra mobs in yellow and Infernal mobs in red.")
    public Outline outline = new Outline();

    // the tables below are filled in at runtime, they are only in the file

    @Comment("Determines which modifiers are enabled.")
    @Hidden
    public Map<String, Boolean> modsEnabled = new LinkedHashMap<>();

    @Comment("Determines which mobs can get modifiers.\n" +
            "Every mob is added here with true the first time it spawns, set it to false to exclude it.")
    @Hidden
    public Map<String, Boolean> permittedentities = new LinkedHashMap<>();

    @Comment("Determines which mobs always spawn with modifiers.\n" +
            "Every mob is added here with false the first time it spawns.")
    @Hidden
    public Map<String, Boolean> entitiesalwaysinfernal = new LinkedHashMap<>();

    @Comment("Defines the base health used to compute the health of infernal mobs.\n" +
            "Every mob is added here with its vanilla max health the first time it spawns.")
    @Hidden
    public Map<String, Double> entitybasehealth = new LinkedHashMap<>();

    // one preview for the whole section, it shows the scene of the hovered option
    @Section
    @Preview(renderer = ConfigPreviews.OutlinePreview.class)
    public static class Outline {

        @Comment("Determines whether infernal mobs get a colored outline.")
        public boolean enabled = true;

        @Comment("Determines whether the outline is visible through blocks, like the Glowing effect.\n" +
                "When false, the outline is only drawn while the mob is in your line of sight.")
        public boolean throughWalls = true;

        @Comment("Defines the distance in blocks beyond which the outline is not drawn.")
        @Range(min = 1)
        public int maxDistance = 32;

        @Comment("Determines whether Rare mobs get the outline.")
        public boolean rare = true;

        @Comment("Determines whether Ultra mobs get the outline.")
        public boolean ultra = true;

        @Comment("Determines whether Infernal mobs get the outline.")
        public boolean infernal = true;
    }

    private static ConfeeConfig<Config> config;

    /**
     * Loads the file. Confee moves the file of the previous versions to its name, creates the file with every key and
     * comment, and clamps the values out of range to the nearest bound.
     */
    public static void register() {
        config = Confee.register(Config.class);
    }

    public static Config get() {
        return config.get();
    }

    public static void save() {
        config.save();
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
