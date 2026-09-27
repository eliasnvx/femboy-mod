package dev.eliasnvx.femboymod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Phone (SPEC v1.1 Photo Mode): right-click takes a selfie. Purely client-side; the client entrypoint installs
 * {@link #onClientUse} so this common class never touches client code.
 */
public class PhoneItem extends Item {

    /** Set by the client entrypoint; a no-op on dedicated servers. */
    public static Runnable onClientUse = () -> { };

    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            onClientUse.run();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
