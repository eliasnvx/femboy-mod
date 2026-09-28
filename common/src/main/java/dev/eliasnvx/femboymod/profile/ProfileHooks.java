package dev.eliasnvx.femboymod.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.DripLevelChangedEvent;
import dev.eliasnvx.femboymod.api.event.profile.CollectionUnlockEvent;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.api.profile.ProfileField;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Fills the player profile from game events and hands out Style Points (config {@code style_points}). */
public final class ProfileHooks {

    /** Equips within one game day, for "Can't Decide What to Wear". */
    public record DayCount(long day, int count) {
        public static final Codec<DayCount> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("day").forGetter(DayCount::day),
                Codec.INT.fieldOf("count").forGetter(DayCount::count)
        ).apply(i, DayCount::new));
    }

    /** Internal fields (not part of the API). */
    public static final ProfileField<DayCount> EQUIPS_TODAY = ProfileField.of(id("equips_today"), DayCount.CODEC, new DayCount(-1, 0), false);
    /** Game time of the last Insight from a rubber duck; -1 = never. */
    public static final ProfileField<Long> DUCK_LAST_INSIGHT = ProfileField.of(id("duck_last_insight"), Codec.LONG, -1L, false);
    /** Game time of the last "btw" from a Terminal; -1 = never. */
    /** Game day of the last Vibe Check that paid Style Points; -1 = never. */
    public static final ProfileField<Long> VIBE_LAST_DAY = ProfileField.of(id("vibe_last_day"), Codec.LONG, -1L, false);
    public static final ProfileField<Long> TERMINAL_LAST_BTW = ProfileField.of(id("terminal_last_btw"), Codec.LONG, -1L, false);

    private ProfileHooks() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(FemboyMod.MOD_ID, path);
    }

    public static void registerFields(ApiRegistry<ProfileField<?>> registry) {
        for (ProfileField<?> field : FemboyProfileFields.ALL) {
            registry.register(field.id(), field);
        }
        registry.register(EQUIPS_TODAY.id(), EQUIPS_TODAY);
        registry.register(DUCK_LAST_INSIGHT.id(), DUCK_LAST_INSIGHT);
        registry.register(TERMINAL_LAST_BTW.id(), TERMINAL_LAST_BTW);
        registry.register(VIBE_LAST_DAY.id(), VIBE_LAST_DAY);
    }

    public static void register(FemboyEventBus events) {
        events.addListener(CosmeticChangedEvent.class, e -> {
            if (e.entity() instanceof ServerPlayer player && !e.current().isEmpty()) {
                onEquip(player, BuiltInRegistries.ITEM.getKey(e.current().getItem()));
            }
        });
        events.addListener(DripLevelChangedEvent.class, e -> {
            if (e.player() instanceof ServerPlayer player) {
                FemboyMod.api().getProfile(player).update(FemboyProfileFields.BEST_DRIP_LEVEL, best -> Math.max(best, e.current().level()));
            }
        });
    }

    private static void onEquip(ServerPlayer player, ResourceLocation item) {
        PlayerProfile profile = FemboyMod.api().getProfile(player);
        profile.update(FemboyProfileFields.EQUIPS, count -> count + 1);
        Set<ResourceLocation> collection = profile.get(FemboyProfileFields.COLLECTION);
        if (!collection.contains(item)) {
            Set<ResourceLocation> updated = new HashSet<>(collection);
            updated.add(item);
            profile.set(FemboyProfileFields.COLLECTION, Set.copyOf(updated));
            FemboyMod.api().events().post(new CollectionUnlockEvent(player, item, updated.size()));
            StylePoints.earn(player, FemboyConfig.common().stylePoints().collectionUnlock(), StylePoints.COLLECTION);
        }
    }

    /** Counts an equip for today and returns today's total. */
    public static int countEquipToday(ServerPlayer player) {
        long day = player.level().getGameTime() / SharedConstants.TICKS_PER_GAME_DAY;
        return FemboyMod.api().getProfile(player).update(EQUIPS_TODAY,
                previous -> new DayCount(day, previous.day() == day ? previous.count() + 1 : 1)).count();
    }

    /** Server player tick: Style Points for wearing set bonuses. */
    public static void tick(ServerPlayer player) {
        CommonConfig.StylePoints config = FemboyConfig.common().stylePoints();
        if (config.setWornInterval() <= 0 || player.tickCount % config.setWornInterval() != 0) {
            return;
        }
        int sets = FemboyMod.api().getActiveSetBonuses(player).size();
        StylePoints.earn(player, sets * config.perActiveSet(), StylePoints.SET_WORN);
    }
}
