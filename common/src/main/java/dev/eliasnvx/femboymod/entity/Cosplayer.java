package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.world.trade.TradeSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * Wandering Cosplayer (SPEC v1.2): a rare travelling trader in costume. Sells cosmetics in colorways found nowhere
 * else (trade set {@code cosplayer/exclusive}) plus a few everyday goodies ({@code cosplayer/common}), mostly for
 * Style Coupons. Behaves like the wandering trader: wanders around, leaves after a while.
 */
public class Cosplayer extends WanderingTrader {

    private static final ResourceLocation EXCLUSIVE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosplayer/exclusive");
    private static final ResourceLocation COMMON = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosplayer/common");

    public Cosplayer(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        TradeSets.addOffers(this, offers, EXCLUSIVE, random);
        TradeSets.addOffers(this, offers, COMMON, random);
    }
}
