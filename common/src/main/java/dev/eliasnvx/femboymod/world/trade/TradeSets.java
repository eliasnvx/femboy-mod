package dev.eliasnvx.femboymod.world.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Data-driven trades on Minecraft 1.20.1. 26.3 has them in vanilla; 1.20.1 does not, so this class reads the
 * same files: {@code data/<ns>/villager_trade/**} (one trade each), {@code data/<ns>/tags/villager_trade/**}
 * (lists of trades) and {@code data/<ns>/trade_set/**} ({@code amount} + {@code trades}: a tag, an id or a list).
 * The {@code random_sequence} field is ignored: offers use the trader's own random. Items are 1.20.1 item stacks
 * ({@code {"id", "Count", "tag"}}); a wanted item matches by item and count, like 26.3's item cost.
 */
public final class TradeSets {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter TRADES = FileToIdConverter.json("villager_trade");
    private static final FileToIdConverter TRADE_TAGS = FileToIdConverter.json("tags/villager_trade");
    private static final FileToIdConverter SETS = FileToIdConverter.json("trade_set");
    private static final String TAG_PREFIX = "#";

    /** Trades stay JSON until an offer is made: their item components may point into data pack registries. */
    private static volatile Data data = Data.EMPTY;

    private TradeSets() {
    }

    /** Called from FemboyMod.init. */
    public static void init() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new Loader(),
                new ResourceLocation(FemboyMod.MOD_ID, "trade_sets"));
    }

    /**
     * Adds up to the set's {@code amount} random trades from trade set {@code setId} that {@code offers} does not
     * have yet (what 26.3's {@code addOffersFromTradeSet} does). Returns how many were added.
     */
    public static int addOffers(Entity trader, MerchantOffers offers, ResourceLocation setId, RandomSource random) {
        TradeSet set = data.sets().get(setId);
        if (set == null) {
            return 0;
        }
        List<VillagerTrade> trades = resolve(set, trader.level().registryAccess());
        int added = 0;
        while (added < set.amount()) {
            MerchantOffer offer = pickNew(trades, offers, random);
            if (offer == null) {
                break;
            }
            offers.add(offer);
            added++;
        }
        return added;
    }

    /**
     * A vanilla trade listing that draws from trade set {@code setId}. Villagers take two listings per level, so
     * register two of these per level; each one offers a trade the villager does not have yet, and none once the
     * villager has {@code amount} trades from the set (so {@code amount} works up to two).
     */
    public static VillagerTrades.ItemListing listing(ResourceLocation setId) {
        return (trader, random) -> {
            TradeSet set = data.sets().get(setId);
            if (set == null || !(trader instanceof Merchant merchant)) {
                return null;
            }
            List<VillagerTrade> trades = resolve(set, trader.level().registryAccess());
            MerchantOffers offers = merchant.getOffers();
            long fromSet = offers.stream().filter(offer -> trades.stream().anyMatch(trade -> trade.sameAs(offer))).count();
            return fromSet >= set.amount() ? null : pickNew(trades, offers, random);
        };
    }

    private static @Nullable MerchantOffer pickNew(List<VillagerTrade> trades, MerchantOffers offers, RandomSource random) {
        List<VillagerTrade> fresh = trades.stream()
                .filter(trade -> offers.stream().noneMatch(trade::sameAs))
                .toList();
        return fresh.isEmpty() ? null : fresh.get(random.nextInt(fresh.size())).toOffer();
    }

    private static List<VillagerTrade> resolve(TradeSet set, RegistryAccess registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        List<VillagerTrade> trades = new ArrayList<>();
        for (ResourceLocation id : set.trades()) {
            JsonElement json = data.trades().get(id);
            if (json == null) {
                LOGGER.warn("Unknown villager trade {}", id);
                continue;
            }
            VillagerTrade.CODEC.parse(ops, json)
                    .resultOrPartial(error -> LOGGER.error("Invalid villager trade {}: {}", id, error))
                    .ifPresent(trades::add);
        }
        return trades;
    }

    /** One trade, the same format as 26.3's {@code villager_trade} files. */
    record VillagerTrade(ItemStack wants, Optional<ItemStack> additionalWants, ItemStack gives, int maxUses, int xp,
                         float reputationDiscount) {
        static final Codec<VillagerTrade> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("wants").forGetter(VillagerTrade::wants),
                ItemStack.CODEC.optionalFieldOf("additional_wants").forGetter(VillagerTrade::additionalWants),
                ItemStack.CODEC.fieldOf("gives").forGetter(VillagerTrade::gives),
                ExtraCodecs.POSITIVE_INT.fieldOf("max_uses").forGetter(VillagerTrade::maxUses),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("xp").forGetter(VillagerTrade::xp),
                Codec.FLOAT.fieldOf("reputation_discount").forGetter(VillagerTrade::reputationDiscount)
        ).apply(i, VillagerTrade::new));

        MerchantOffer toOffer() {
            return new MerchantOffer(wants.copy(), additionalWants.map(ItemStack::copy).orElse(ItemStack.EMPTY), gives.copy(),
                    maxUses, xp, reputationDiscount);
        }

        boolean sameAs(MerchantOffer offer) {
            return ItemStack.isSameItemSameTags(offer.getResult(), gives)
                    && ItemStack.isSameItem(offer.getBaseCostA(), wants);
        }
    }

    /** A trade set with its trade list already resolved (tags expanded). */
    record TradeSet(int amount, List<ResourceLocation> trades) {
    }

    record Data(Map<ResourceLocation, JsonElement> trades, Map<ResourceLocation, TradeSet> sets) {
        static final Data EMPTY = new Data(Map.of(), Map.of());
    }

    private static final class Loader extends SimplePreparableReloadListener<Data> {

        @Override
        protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, JsonElement> trades = new HashMap<>();
            TRADES.listMatchingResources(manager).forEach((file, resource) ->
                    read(file, resource).ifPresent(json -> trades.put(TRADES.fileToId(file), json)));

            Map<ResourceLocation, List<String>> tags = new HashMap<>();
            TRADE_TAGS.listMatchingResourceStacks(manager).forEach((file, stack) -> {
                List<String> entries = new ArrayList<>();
                for (Resource resource : stack) { // bottom pack first, like vanilla tags
                    read(file, resource).map(JsonElement::getAsJsonObject).ifPresent(tag -> {
                        if (tag.has("replace") && tag.get("replace").getAsBoolean()) {
                            entries.clear();
                        }
                        for (JsonElement entry : tag.getAsJsonArray("values")) {
                            entries.add(entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString());
                        }
                    });
                }
                tags.put(TRADE_TAGS.fileToId(file), entries);
            });

            Map<ResourceLocation, TradeSet> sets = new HashMap<>();
            SETS.listMatchingResources(manager).forEach((file, resource) -> read(file, resource).ifPresent(json -> {
                ResourceLocation id = SETS.fileToId(file);
                try {
                    JsonObject set = json.getAsJsonObject();
                    Set<ResourceLocation> ids = new LinkedHashSet<>();
                    JsonElement ref = set.get("trades");
                    if (ref.isJsonArray()) {
                        for (JsonElement entry : (JsonArray) ref) {
                            expand(entry.getAsString(), tags, ids, new LinkedHashSet<>());
                        }
                    } else {
                        expand(ref.getAsString(), tags, ids, new LinkedHashSet<>());
                    }
                    sets.put(id, new TradeSet(set.get("amount").getAsInt(), List.copyOf(ids)));
                } catch (RuntimeException e) {
                    LOGGER.error("Invalid trade set {}", id, e);
                }
            }));
            return new Data(Map.copyOf(trades), Map.copyOf(sets));
        }

        @Override
        protected void apply(Data prepared, ResourceManager manager, ProfilerFiller profiler) {
            data = prepared;
        }

        private static void expand(String entry, Map<ResourceLocation, List<String>> tags, Set<ResourceLocation> out,
                                   Set<ResourceLocation> visiting) {
            if (!entry.startsWith(TAG_PREFIX)) {
                out.add(new ResourceLocation(entry));
                return;
            }
            ResourceLocation tag = new ResourceLocation(entry.substring(TAG_PREFIX.length()));
            if (!visiting.add(tag)) {
                return; // a tag that includes itself
            }
            for (String nested : tags.getOrDefault(tag, List.of())) {
                expand(nested, tags, out, visiting);
            }
        }

        private static Optional<JsonElement> read(ResourceLocation file, Resource resource) {
            try (Reader reader = resource.openAsReader()) {
                return Optional.of(JsonParser.parseReader(reader));
            } catch (Exception e) {
                LOGGER.error("Could not read {} from {}", file, resource.sourcePackId(), e);
                return Optional.empty();
            }
        }
    }
}
