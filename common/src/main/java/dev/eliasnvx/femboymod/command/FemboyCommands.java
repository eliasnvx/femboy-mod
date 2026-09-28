package dev.eliasnvx.femboymod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.colorway.Colors;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** {@code /femboymod colorway ...}: debug/creative helper to apply colorways until dyeing lands (Phase 2). */
public final class FemboyCommands {

    private static final SimpleCommandExceptionType EMPTY_HAND =
            new SimpleCommandExceptionType(Component.translatable("commands.femboymod.colorway.empty_hand"));
    private static final SimpleCommandExceptionType BAD_COLOR =
            new SimpleCommandExceptionType(Component.translatable("commands.femboymod.colorway.bad_color"));

    private FemboyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal(FemboyMod.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("colorway")
                        .then(Commands.literal("clear").executes(FemboyCommands::clear))
                        .then(Commands.literal("solid")
                                .then(Commands.argument("base", StringArgumentType.word())
                                        .executes(ctx -> apply(ctx, Optional.empty(), false))))
                        .then(Commands.literal("pattern")
                                .then(Commands.argument("pattern", ResourceArgument.resource(context, ColorwayPattern.REGISTRY_KEY))
                                        .executes(ctx -> apply(ctx, Optional.of(pattern(ctx)), false))
                                        .then(Commands.argument("base", StringArgumentType.word())
                                                .executes(ctx -> apply(ctx, Optional.of(pattern(ctx)), false))
                                                .then(Commands.argument("secondary", StringArgumentType.word())
                                                        .executes(ctx -> apply(ctx, Optional.of(pattern(ctx)), true))))))));
    }

    private static Holder<ColorwayPattern> pattern(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ResourceArgument.getResource(ctx, "pattern", ColorwayPattern.REGISTRY_KEY);
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, Optional<Holder<ColorwayPattern>> pattern, boolean withSecondary)
            throws CommandSyntaxException {
        ItemStack stack = heldItem(ctx);
        int base = hasArgument(ctx, "base") ? color(StringArgumentType.getString(ctx, "base")) : Colorway.DEFAULT_SECONDARY;
        Optional<Integer> secondary = withSecondary
                ? Optional.of(color(StringArgumentType.getString(ctx, "secondary")))
                : Optional.empty();
        FemboyComponents.COLORWAY.set(stack, new Colorway(base, pattern, secondary));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.femboymod.colorway.set", stack.getDisplayName()), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldItem(ctx);
        FemboyComponents.COLORWAY.remove(stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.femboymod.colorway.cleared", stack.getDisplayName()), false);
        return 1;
    }

    private static ItemStack heldItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            throw EMPTY_HAND.create();
        }
        return stack;
    }

    /** Accepts "FFB6D9" or "#FFB6D9" (brigadier words cannot contain '#', so it is optional). */
    private static int color(String raw) throws CommandSyntaxException {
        return Colors.parseHex(raw.startsWith("#") ? raw : "#" + raw).result().orElseThrow(BAD_COLOR::create);
    }

    private static boolean hasArgument(CommandContext<CommandSourceStack> ctx, String name) {
        return ctx.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(name));
    }
}
