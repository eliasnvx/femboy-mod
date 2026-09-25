package dev.eliasnvx.femboymod.backpack;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Right-click opens the backpack; sneak + right-click puts it on (back slot). SPEC §5.3. */
public class BackpackItem extends Item {

    public BackpackItem(Properties properties) {
        super(properties);
    }

    /** No backpacks in shulker boxes or bundles (and our own slots check this too). */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return CosmeticsEvents.equipFromHand(player, hand, true).asMinecraft();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BackpackMenus.openFromHand(serverPlayer, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
