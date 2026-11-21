package atomicstryker.infernalmobs.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * An item as offered in the loot suggestions, searchable by its in game name and its id.
 */
record LootItemChoice(Item item, ItemStack stack, String name, String id, String searchKey) {

    static List<LootItemChoice> all() {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .map(LootItemChoice::of)
                .sorted(Comparator.comparing(LootItemChoice::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static LootItemChoice of(Item item) {
        ItemStack stack = new ItemStack(item);
        String name = stack != null ? stack.getHoverName().getString() : Component.translatable(item.getDescriptionId()).getString();
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        return new LootItemChoice(item, stack, name, id, (name + " " + id).toLowerCase(Locale.ROOT));
    }

    /**
     * @param text an item id, or an in game name
     */
    @Nullable
    static LootItemChoice find(List<LootItemChoice> choices, String text) {
        ResourceLocation identifier = ResourceLocation.tryParse(text);
        String id = identifier != null ? identifier.toString() : text;
        for (LootItemChoice choice : choices) {
            if (choice.id().equals(id) || choice.name().equalsIgnoreCase(text)) {
                return choice;
            }
        }
        return null;
    }
}
