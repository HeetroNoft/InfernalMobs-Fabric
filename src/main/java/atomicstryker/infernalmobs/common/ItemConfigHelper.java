package atomicstryker.infernalmobs.common;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the loot entries. They use the same SNBT as the other versions of the mod, {id:'minecraft:cookie',count:5},
 * so config files can be copied between versions. Item NBT goes in a "tag" compound, as in 1.20.1 item stacks.
 */
public class ItemConfigHelper {
    private final List<ItemStack> itemStackList;

    public ItemConfigHelper(List<? extends String> items, Logger logger, RegistryAccess registryAccess) {
        itemStackList = new ArrayList<>();
        for (String json : items) {
            try {
                ItemStack itemStack = ItemStack.of(toLegacyItemTag(TagParser.parseTag(json)));
                if (!itemStack.isEmpty()) {
                    itemStackList.add(itemStack);
                    logger.info("item config parser identified itemstack {}", itemStack);
                } else {
                    logger.error("item config parser could not create itemStack from {}", json);
                }
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        }
        logger.info("item config parser finished, item count: {}", itemStackList.size());
    }

    /**
     * 1.20.1 item stacks store their count as a byte named "Count"
     */
    private static CompoundTag toLegacyItemTag(CompoundTag nbt) {
        CompoundTag legacy = nbt.copy();
        if (!legacy.contains("Count")) {
            legacy.putByte("Count", (byte) (nbt.contains("count") ? nbt.getInt("count") : 1));
        }
        legacy.remove("count");
        return legacy;
    }

    public static String fromItemStack(ItemStack itemStack, RegistryAccess registryAccess) {
        CompoundTag resultTag = new CompoundTag();
        if (!itemStack.isEmpty()) {
            resultTag.putString("id", BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
            resultTag.putInt("count", itemStack.getCount());
            if (itemStack.getTag() != null) {
                resultTag.put("tag", itemStack.getTag().copy());
            }
        }
        return resultTag.toString();
    }

    public List<ItemStack> getItemStackList() {
        return itemStackList;
    }
}
