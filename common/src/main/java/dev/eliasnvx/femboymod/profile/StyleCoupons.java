package dev.eliasnvx.femboymod.profile;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

/** Style Points → Style Coupon (the Thrifter's second currency). */
public final class StyleCoupons {

    private StyleCoupons() {
    }

    /** Spends {@code style_points.coupon_cost} points for one coupon; false if the player can't afford it. */
    public static boolean redeem(ServerPlayer player) {
        int cost = FemboyConfig.common().stylePoints().couponCost();
        if (!StylePoints.add(player, -cost, StylePoints.COUPON)) {
            return false;
        }
        ItemStack coupon = new ItemStack(FemboyItems.STYLE_COUPON.get());
        player.getInventory().placeItemBackInInventory(coupon, Prediction.SERVER_ONLY); // drops it if the inventory is full
        return true;
    }
}
