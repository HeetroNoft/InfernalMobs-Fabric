package atomicstryker.infernalmobs.client;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Config screen entry for the loot lists (droppedItemIDs*): every entry is a {@link LootStackEntry},
 * editing the item by its in game name with suggestions, the additional SNBT and the count separately.
 */
public class LootListGuiProvider implements GuiProvider {

    @Override
    public List<AbstractConfigListEntry> get(String i18n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        List<LootStackEntry.ItemChoice> choices = LootStackEntry.ItemChoice.all();
        List<String> value = Utils.getUnsafely(field, config);
        return List.of(new NestedListListEntry<String, LootStackEntry>(
                Component.translatable(i18n),
                new ArrayList<>(value),
                false,
                Optional::empty,
                newValue -> Utils.setUnsafely(field, config, newValue),
                () -> Utils.getUnsafely(field, defaults),
                ConfigEntryBuilder.create().getResetButtonKey(),
                true,
                false,
                (entry, list) -> new LootStackEntry(entry == null ? "" : entry, choices)));
    }
}
