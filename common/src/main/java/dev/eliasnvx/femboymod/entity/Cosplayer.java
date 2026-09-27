package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;

/**
 * Wandering Cosplayer (SPEC v1.2): a rare travelling trader in costume. Sells cosmetics in colorways found nowhere
 * else (trade set {@code femboymod:cosplayer/exclusive}) plus a few everyday goodies ({@code cosplayer/common}),
 * mostly for Style Coupons. Behaves like the wandering trader: wanders around, leaves after a while.
 */
public class Cosplayer extends WanderingTrader {

    public static final ResourceKey<TradeSet> EXCLUSIVE = tradeSet("cosplayer/exclusive");
    public static final ResourceKey<TradeSet> COMMON = tradeSet("cosplayer/common");

    public Cosplayer(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    private static ResourceKey<TradeSet> tradeSet(String path) {
        return ResourceKey.create(Registries.TRADE_SET, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = getOffers();
        addOffersFromTradeSet(level, offers, EXCLUSIVE);
        addOffersFromTradeSet(level, offers, COMMON);
    }
}
