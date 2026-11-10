package atomicstryker.infernalmobs.common;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemConfigHelper {
    private final List<ItemStack> itemStackList;

    public ItemConfigHelper(List<? extends String> items, Logger logger, RegistryAccess registryAccess) {
        itemStackList = new ArrayList<>();
        for (String json : items) {
            try {
                CompoundTag nbt = TagParser.parseTag(json);
                Optional<ItemStack> optionalItemStack = ItemStack.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), nbt).result();

                if (optionalItemStack.isPresent()) {
                    ItemStack itemStack = optionalItemStack.get();
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

    public static String fromItemStack(ItemStack itemStack, RegistryAccess registryAccess) {
        CompoundTag resultTag = itemStack.isEmpty() ? new CompoundTag() : (CompoundTag) itemStack.save(registryAccess);
        return resultTag.toString();
    }

    public List<ItemStack> getItemStackList() {
        return itemStackList;
    }
}