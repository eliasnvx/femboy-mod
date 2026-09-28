package dev.eliasnvx.femboymod.backpack;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click opens the backpack; sneak + right-click puts it on (back slot). SPEC §5.3. */
public class BackpackItem extends Item implements DyeableLeatherItem {

    public BackpackItem(Properties properties) {
        super(properties);
    }

    /** No backpacks in shulker boxes or bundles (and our own slots check this too). */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return new InteractionResultHolder<>(CosmeticsEvents.equipFromHand(player, hand, true).asMinecraft(),
                    player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BackpackMenus.openFromHand(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
