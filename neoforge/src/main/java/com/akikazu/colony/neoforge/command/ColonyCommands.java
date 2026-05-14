package com.akikazu.colony.neoforge.command;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.colony.ColonyManager;
import com.akikazu.colony.common.colony.ColonyMetadata;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.network.ColonyRegistrationService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side {@code /colony …} root command.
 *
 * <p>
 * Sub-trees and their permission gates:
 * <ul>
 * <li>{@code /colony register <name>} (level 2) — debug pathway exercised by the registration gametest.</li>
 * <li>{@code /colony wand restore} (level 0) — gives the executor a Colony Tool, no-op if they already have one.</li>
 * <li>{@code /colony list} (level 2) — admin listing of every known colony in the current dimension.</li>
 * <li>{@code /colony info <colonyId>} (level 0) — read-only detail dump for the named colony.</li>
 * </ul>
 */
public final class ColonyCommands
{
    private static final int PERMISSION_LEVEL_ANY = 0;

    private static final int PERMISSION_LEVEL_OPERATOR = 2;

    private ColonyCommands()
    {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("colony")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL_ANY))
                .then(Commands.literal("register")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL_OPERATOR))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> runRegister(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("wand")
                        .then(Commands.literal("restore")
                                .executes(ctx -> runWandRestore(ctx.getSource()))))
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL_OPERATOR))
                        .executes(ctx -> runList(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("colonyId", StringArgumentType.string())
                                .suggests(colonyIdSuggestions())
                                .executes(ctx -> runInfo(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "colonyId"))))));
    }

    public static int runWandRestore(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            source.sendFailure(Component.literal("Only players can restore a Colony Tool."));

            return 0;
        }

        if (playerHasColonyTool(player))
        {
            source.sendSuccess(
                    () -> Component.translatable("colony.command.wand_restore.already_have"),
                    false);

            return 0;
        }

        ItemStack tool = new ItemStack(ColonyItems.COLONY_TOOL.get());

        if (!player.getInventory().add(tool))
        {
            player.drop(tool, false);
        }

        source.sendSuccess(
                () -> Component.translatable("colony.command.wand_restore.success"),
                false);

        return 1;
    }

    private static int runRegister(CommandSourceStack source, String name)
    {
        ServerPlayer player = source.getPlayer();
        ServerLevel level = source.getLevel();
        ColonyId id = ColonyId.random();
        BlockPos pos = player != null
                ? player.blockPosition()
                : BlockPos.containing(source.getPosition());

        ColonyRegistrationService.Result result = ColonyRegistrationService.register(level, id, name, pos);

        if (result.accepted())
        {
            source.sendSuccess(() -> Component.literal("Registered colony '" + name + "' (" + id + ") at " + pos),
                    true);

            return 1;
        }

        source.sendFailure(Component.literal("Colony '" + name + "' (" + id + ") was already registered."));

        return 0;
    }

    private static int runList(CommandSourceStack source)
    {
        ServerLevel level = source.getLevel();
        ColonyManager manager = ColonyManager.get(level);
        Map<ColonyId, ColonyMetadata> entries = manager.index().entries();

        if (entries.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("No colonies registered."), false);

            return 0;
        }

        source.sendSuccess(() -> Component.literal("Colonies (" + entries.size() + "):"), false);

        for (Map.Entry<ColonyId, ColonyMetadata> entry : entries.entrySet())
        {
            ColonyId id = entry.getKey();
            ColonyMetadata meta = entry.getValue();
            int citizenCount = manager.loadFull(id)
                    .map(colony -> colony.citizens().size())
                    .orElse(0);
            String summary = id + " @ " + meta.townHallPos()
                    + " in " + meta.dimension().location()
                    + " — citizens=" + citizenCount;

            source.sendSuccess(() -> Component.literal(summary), false);
        }

        return entries.size();
    }

    private static int runInfo(CommandSourceStack source, String rawId)
    {
        ColonyId id = parseColonyId(rawId);

        if (id == null)
        {
            source.sendFailure(Component.literal("Invalid colony id: " + rawId));

            return 0;
        }

        ColonyManager manager = ColonyManager.get(source.getLevel());
        Optional<ColonyMetadata> metaOpt = manager.find(id);

        if (metaOpt.isEmpty())
        {
            source.sendFailure(Component.literal("Unknown colony: " + id));

            return 0;
        }

        ColonyMetadata meta = metaOpt.get();
        int citizenCount = manager.loadFull(id)
                .map(colony -> colony.citizens().size())
                .orElse(0);

        source.sendSuccess(() -> Component.literal("Colony " + id), false);
        source.sendSuccess(() -> Component.literal("  position=" + meta.townHallPos()), false);
        source.sendSuccess(() -> Component.literal("  dimension=" + meta.dimension().location()), false);
        source.sendSuccess(() -> Component.literal("  foundedAtTick=" + meta.foundedAtTick()), false);
        source.sendSuccess(() -> Component.literal("  citizens=" + citizenCount), false);

        return 1;
    }

    private static SuggestionProvider<CommandSourceStack> colonyIdSuggestions()
    {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(knownColonyIds(ctx.getSource().getLevel()), builder);
    }

    private static List<String> knownColonyIds(ServerLevel level)
    {
        return ColonyManager.get(level).index().entries().keySet().stream()
                .map(ColonyId::toString)
                .toList();
    }

    private static @Nullable ColonyId parseColonyId(String raw)
    {
        try
        {
            return new ColonyId(UUID.fromString(raw));
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    public static boolean playerHasColonyTool(ServerPlayer player)
    {
        return player.getInventory().contains(stack -> stack.is(ColonyItems.COLONY_TOOL.get()));
    }
}
