package dev.eliasnvx.femboymod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Lookbook: right-click opens the guide to every cosmetic. The screen is client-only; the client entrypoint installs
 * {@link #onClientUse} so this common class never touches client code.
 */
public class LookbookItem extends Item {

    /** Set by the client entrypoint; a no-op on dedicated servers. */
    public static Runnable onClientUse = () -> { };

    public LookbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            onClientUse.run();
        }
        return InteractionResult.SUCCESS;
    }
}
