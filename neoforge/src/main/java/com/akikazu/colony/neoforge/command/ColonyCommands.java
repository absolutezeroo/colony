package com.akikazu.colony.neoforge.command;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.neoforge.network.ColonyRegistrationService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server-side {@code /colony …} root command. The {@code register} sub-command is the testing pathway exercised by
 * {@code ColonyRegistrationGameTest}.
 *
 * <p>
 * The command runs the same {@link ColonyRegistrationService} call the {@code RegisterColonyPayload} handler uses, so
 * the persistence path is exercised identically whether the input arrives via wire or via the integrated console.
 */
public final class ColonyCommands
{
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
                .requires(source -> source.hasPermission(PERMISSION_LEVEL_OPERATOR))
                .then(Commands.literal("register")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> runRegister(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int runRegister(CommandSourceStack source, String name)
    {
        ServerPlayer player = source.getPlayer();
        ServerLevel level = source.getLevel();
        ColonyId id = ColonyId.random();
        net.minecraft.core.BlockPos pos = player != null
                ? player.blockPosition()
                : net.minecraft.core.BlockPos.containing(source.getPosition());

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
}
