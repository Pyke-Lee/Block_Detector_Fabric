package io.github.pyke.blockdetector.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.pyke.blockdetector.BlockDetector;
import io.github.pyke.blockdetector.config.ConfigAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ReloadCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("detector")
            .requires(src -> src.hasPermission(3))
            .then(Commands.literal("reload").executes(ctx -> {
                try {
                    ConfigAccess.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal(BlockDetector.SYSTEM_PREFIX).append("Successes to reload config!"), false);
                } catch (Exception e) {
                    ctx.getSource().sendFailure(Component.literal(BlockDetector.SYSTEM_PREFIX).append("Failed to reload config!"));
                    ctx.getSource().sendFailure(Component.literal(BlockDetector.SYSTEM_PREFIX).append("Reason: " + e.getMessage()));
                }

                return 1;
            }))
        );
    }
}
