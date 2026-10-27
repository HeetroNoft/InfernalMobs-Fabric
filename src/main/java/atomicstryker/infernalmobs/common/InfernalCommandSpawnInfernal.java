package atomicstryker.infernalmobs.common;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class InfernalCommandSpawnInfernal {

    public static final LiteralArgumentBuilder<CommandSourceStack> BUILDER =
            Commands.literal("spawninfernal")
                    .requires((caller) -> caller.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("entClass", IdentifierArgument.id())
                                    .suggests(InfernalCommandSpawnInfernal::suggestEntities)
                                    .then(Commands.argument("modifiers", StringArgumentType.greedyString())
                                            .suggests(InfernalCommandSpawnInfernal::suggestModifiers)
                                            .executes((caller) -> {
                                                execute(caller.getSource(), BlockPosArgument.getSpawnablePos(caller, "pos"), IdentifierArgument.getId(caller, "entClass"), StringArgumentType.getString(caller, "modifiers"));
                                                return 1;
                                            }))));

    private static boolean isSpawnable(EntityType<?> type) {
        return !type.getCategory().isFriendly() && !type.getCategory().isPersistent();
    }

    private static CompletableFuture<Suggestions> suggestEntities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(InfernalCommandSpawnInfernal::isSpawnable)
                .map(BuiltInRegistries.ENTITY_TYPE::getKey), builder);
    }

    /**
     * suggests the modifier for the word being typed, skipping the ones already entered
     */
    private static CompletableFuture<Suggestions> suggestModifiers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        int lastWordStart = typed.lastIndexOf(' ') + 1;
        List<String> alreadyTyped = Arrays.asList(typed.substring(0, lastWordStart).split(" "));
        return SharedSuggestionProvider.suggest(InfernalMobsCore.instance().getModifierNames().stream()
                .filter(name -> !alreadyTyped.contains(name)), builder.createOffset(builder.getStart() + lastWordStart));
    }

    private static void execute(CommandSourceStack source, BlockPos pos, Identifier entClassName, String modifiers) {

        Optional<EntityType<?>> chosenType = BuiltInRegistries.ENTITY_TYPE.getOptional(entClassName);
        if (chosenType.isEmpty() || !isSpawnable(chosenType.get())) {
            source.sendFailure(Component.literal("Invalid SpawnInfernal command, no Entity Resource [" + entClassName + "] known or noncombat entity type"));
            return;
        }

        LivingEntity mob = (LivingEntity) chosenType.get().create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) {
            source.sendFailure(Component.literal("Invalid SpawnInfernal command, failed to create [" + entClassName + "] instance in world"));
        }

        mob.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        source.getLevel().addFreshEntity(mob);

        SidedCache.getInfernalMobs(mob.level()).remove(mob);
        InfernalMobsCore.instance().addEntityModifiersByString(mob, modifiers);
        MobModifier mod = InfernalMobsCore.getMobModifiers(mob);
        if (mod != null) {
            InfernalMobsCore.LOGGER.log(Level.INFO,
                    source.getTextName() + " spawned: " + InfernalMobsCore.getMobModifiers(mob).getLinkedModNameUntranslated() + " at [" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "]");
        } else {
            source.sendFailure(Component.literal("Error adding Infernal Modifier " + modifiers + " to mob " + mob));
        }
    }
}
