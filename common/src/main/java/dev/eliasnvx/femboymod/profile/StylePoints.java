package dev.eliasnvx.femboymod.profile;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.profile.StylePointsEvent;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Style Points: earned for style (worn sets, critic, collection, duck), spent on Style Coupons. */
public final class StylePoints {

    public static final ResourceLocation SET_WORN = id("set_worn");
    public static final ResourceLocation CRITIC_PASSED = id("critic_passed");
    public static final ResourceLocation COLLECTION = id("collection");
    public static final ResourceLocation RUBBER_DUCK = id("rubber_duck");
    public static final ResourceLocation COUPON = id("coupon");
    public static final ResourceLocation VIBE_CHECK = id("vibe_check");

    private StylePoints() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    /** See {@link dev.eliasnvx.femboymod.api.FemboyApi#addStylePoints}. */
    public static boolean add(ServerPlayer player, int amount, ResourceLocation reason) {
        if (amount == 0) {
            return false;
        }
        if (amount > 0 && !((ServerLevel) player.level()).getGameRules().getBoolean(FemboyGameRules.STYLE_POINTS)) {
            return false;
        }
        StylePointsEvent event = new StylePointsEvent(player, reason, amount);
        FemboyMod.api().events().post(event);
        int change = event.amount();
        if (event.isCancelled() || change == 0) {
            return false;
        }
        PlayerProfile profile = FemboyMod.api().getProfile(player);
        int balance = profile.get(FemboyProfileFields.STYLE_POINTS);
        if (balance + change < 0) {
            return false;
        }
        profile.set(FemboyProfileFields.STYLE_POINTS, balance + change);
        if (change > 0) {
            profile.update(FemboyProfileFields.STYLE_POINTS_EARNED, total -> total + change);
            player.displayClientMessage(Component.translatable("message.femboymod.style_points", change), true);
        }
        return true;
    }

    /** Earning with a config value that may be 0 (then nothing happens). */
    public static void earn(ServerPlayer player, int amount, ResourceLocation reason) {
        if (amount > 0) {
            add(player, amount, reason);
        }
    }
}
