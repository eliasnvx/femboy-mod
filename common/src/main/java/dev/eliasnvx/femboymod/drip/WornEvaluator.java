package dev.eliasnvx.femboymod.drip;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import net.minecraft.server.level.ServerLevel;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.Colors;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.api.effect.ConfiguredEffect;
import dev.eliasnvx.femboymod.api.effect.EffectSource;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.api.backpack.CharmStats;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.item.ItemList;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * Derives Drip Level, completed sets and the effects to apply from what a player wears.
 * Works on both sides (all inputs are synced). Results are cached per player until the worn
 * inventory snapshot or the registries change.
 */
public final class WornEvaluator {

    /** One effect to run, with a stable source id (used e.g. as attribute modifier id). */
    public record PlannedEffect(ConfiguredEffect configured, EffectSource source) {
    }

    public record Evaluation(DripLevel drip, Set<ResourceLocation> activeSets, List<PlannedEffect> effects) {
        public static final Evaluation EMPTY = new Evaluation(DripLevel.NONE, Set.of(), List.of());
    }

    private record Cached(CosmeticInventory inventory, RegistryAccess registries, boolean setsAllowed, Evaluation evaluation) {
    }

    private static final Map<Player, Cached> CACHE = new WeakHashMap<>();

    private WornEvaluator() {
    }

    /** Forget the cached evaluation (e.g. charms changed inside the worn backpack stack). */
    public static synchronized void invalidate(Player player) {
        CACHE.remove(player);
    }

    public static synchronized Evaluation evaluate(Player player) {
        CosmeticInventory inventory = CosmeticsManager.get(player);
        RegistryAccess registries = player.level().registryAccess();
        // Server: the femboymod:set_bonuses game rule can switch sets off per world (clients follow the server's effects)
        boolean setsAllowed = FemboyConfig.common().setBonusesEnabled() && !(player.level() instanceof ServerLevel serverLevel
                && !serverLevel.getGameRules().getBoolean(FemboyGameRules.SET_BONUSES));
        Cached cached = CACHE.get(player);
        if (cached != null && cached.inventory() == inventory && cached.registries() == registries && cached.setsAllowed() == setsAllowed) {
            return cached.evaluation();
        }
        Evaluation evaluation = inventory.isEmpty() ? Evaluation.EMPTY : compute(inventory, registries, setsAllowed);
        CACHE.put(player, new Cached(inventory, registries, setsAllowed, evaluation));
        return evaluation;
    }

    private static Evaluation compute(CosmeticInventory inventory, RegistryAccess registries, boolean setsAllowed) {
        Optional<Registry<CosmeticStats>> statsRegistry = registries.registry(CosmeticStats.REGISTRY_KEY);
        DripRules rules = registries.registry(DripRules.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(DripRules.DEFAULT))
                .orElse(DripRules.FALLBACK);

        List<DripCalculator.Worn> worn = new ArrayList<>();
        List<PlannedEffect> effects = new ArrayList<>();
        inventory.all().forEach((slot, stack) -> {
            CosmeticStats stats = statsRegistry.flatMap(r -> r.getOptional(CosmeticStats.keyOf(stack.getItem())))
                    .orElse(CosmeticStats.NONE);
            worn.add(new DripCalculator.Worn(stats.drip(), colorKey(stack)));
            addEffects(effects, stats.effects(), sourceId("item", slot), 1.0);
            addCharmEffects(effects, stack, slot, registries);
        });

        Set<ResourceLocation> activeSets = new TreeSet<>();
        List<Map.Entry<ResourceKey<SetBonus>, SetBonus>> completed = new ArrayList<>();
        registries.registry(SetBonus.REGISTRY_KEY).filter(sets -> setsAllowed).ifPresent(sets -> {
            for (Map.Entry<ResourceKey<SetBonus>, SetBonus> entry : sets.entrySet()) {
                if (isComplete(entry.getValue(), inventory.all().values())) {
                    activeSets.add(entry.getKey().location());
                    completed.add(entry);
                }
            }
        });

        DripLevel drip = DripCalculator.compute(worn, activeSets.size(), rules);
        for (Map.Entry<ResourceKey<SetBonus>, SetBonus> entry : completed) {
            SetBonus set = entry.getValue();
            double scale = 1.0 + drip.tier() * set.scalingPerTier();
            addEffects(effects, set.effects(), sourceId("set", entry.getKey().location()), scale);
        }
        return new Evaluation(drip, Set.copyOf(activeSets), List.copyOf(effects));
    }

    /** Charms hanging on a worn backpack (SPEC §5.3). */
    private static void addCharmEffects(List<PlannedEffect> out, ItemStack stack, ResourceLocation slot, RegistryAccess registries) {
        if (!FemboyComponents.BACKPACK.has(stack)) {
            return;
        }
        Optional<Registry<CharmStats>> charmRegistry = registries.registry(CharmStats.REGISTRY_KEY);
        if (charmRegistry.isEmpty()) {
            return;
        }
        ItemList charms = FemboyComponents.CHARMS.getOrDefault(stack, ItemList.EMPTY);
        int index = 0;
        for (ItemStack charm : charms.nonEmptyItems()) {
            CharmStats stats = charmRegistry.get().getOptional(CharmStats.keyOf(charm.getItem())).orElse(null);
            if (stats != null) {
                addEffects(out, stats.effects(), sourceId("charm", slot) + "/" + index, 1.0);
            }
            index++;
        }
    }

    static boolean isComplete(SetBonus set, Collection<ItemStack> worn) {
        int matched = 0;
        for (HolderSet<Item> piece : set.pieces()) {
            for (ItemStack stack : worn) {
                if (piece.contains(stack.getItemHolder())) {
                    matched++;
                    break;
                }
            }
        }
        return matched >= set.requiredCount();
    }

    /** Colorway identity for harmony: same pattern (with colors) or same solid color. */
    private static Optional<String> colorKey(ItemStack stack) {
        Optional<Colorway> effective = Colorways.effective(stack);
        if (effective.isEmpty()) {
            return Optional.empty();
        }
        Colorway colorway = effective.get();
        String pattern = colorway.pattern().flatMap(h -> h.unwrapKey()).map(k -> k.location().toString()).orElse("solid");
        return Optional.of(pattern + "/" + Colors.toHex(colorway.baseColor()));
    }

    private static void addEffects(List<PlannedEffect> out, List<ConfiguredEffect> effects, String prefix, double scale) {
        for (int i = 0; i < effects.size(); i++) {
            out.add(new PlannedEffect(effects.get(i),
                    new EffectSource(new ResourceLocation(FemboyMod.MOD_ID, prefix + "/" + i), scale)));
        }
    }

    private static String sourceId(String kind, ResourceLocation id) {
        return kind + "/" + id.getNamespace() + "/" + id.getPath();
    }
}
