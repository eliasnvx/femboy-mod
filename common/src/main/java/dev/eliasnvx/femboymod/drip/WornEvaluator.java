package dev.eliasnvx.femboymod.drip;

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
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
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

    public record Evaluation(DripLevel drip, Set<Identifier> activeSets, List<PlannedEffect> effects) {
        public static final Evaluation EMPTY = new Evaluation(DripLevel.NONE, Set.of(), List.of());
    }

    private record Cached(CosmeticInventory inventory, RegistryAccess registries, Evaluation evaluation) {
    }

    private static final Map<Player, Cached> CACHE = new WeakHashMap<>();

    private WornEvaluator() {
    }

    public static synchronized Evaluation evaluate(Player player) {
        CosmeticInventory inventory = CosmeticsManager.get(player);
        RegistryAccess registries = player.level().registryAccess();
        Cached cached = CACHE.get(player);
        if (cached != null && cached.inventory() == inventory && cached.registries() == registries) {
            return cached.evaluation();
        }
        Evaluation evaluation = inventory.isEmpty() ? Evaluation.EMPTY : compute(inventory, registries);
        CACHE.put(player, new Cached(inventory, registries, evaluation));
        return evaluation;
    }

    private static Evaluation compute(CosmeticInventory inventory, RegistryAccess registries) {
        Optional<Registry<CosmeticStats>> statsRegistry = registries.lookup(CosmeticStats.REGISTRY_KEY);
        DripRules rules = registries.lookup(DripRules.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(DripRules.DEFAULT))
                .orElse(DripRules.FALLBACK);

        List<DripCalculator.Worn> worn = new ArrayList<>();
        List<PlannedEffect> effects = new ArrayList<>();
        inventory.all().forEach((slot, stack) -> {
            CosmeticStats stats = statsRegistry.flatMap(r -> r.getOptional(CosmeticStats.keyOf(stack.getItem())))
                    .orElse(CosmeticStats.NONE);
            worn.add(new DripCalculator.Worn(stats.drip(), colorKey(stack)));
            addEffects(effects, stats.effects(), sourceId("item", slot), 1.0);
        });

        Set<Identifier> activeSets = new TreeSet<>();
        List<Map.Entry<ResourceKey<SetBonus>, SetBonus>> completed = new ArrayList<>();
        registries.lookup(SetBonus.REGISTRY_KEY).ifPresent(sets -> {
            for (Map.Entry<ResourceKey<SetBonus>, SetBonus> entry : sets.entrySet()) {
                if (isComplete(entry.getValue(), inventory.all().values())) {
                    activeSets.add(entry.getKey().identifier());
                    completed.add(entry);
                }
            }
        });

        DripLevel drip = DripCalculator.compute(worn, activeSets.size(), rules);
        for (Map.Entry<ResourceKey<SetBonus>, SetBonus> entry : completed) {
            SetBonus set = entry.getValue();
            double scale = 1.0 + drip.tier() * set.scalingPerTier();
            addEffects(effects, set.effects(), sourceId("set", entry.getKey().identifier()), scale);
        }
        return new Evaluation(drip, Set.copyOf(activeSets), List.copyOf(effects));
    }

    static boolean isComplete(SetBonus set, Collection<ItemStack> worn) {
        int matched = 0;
        for (HolderSet<Item> piece : set.pieces()) {
            for (ItemStack stack : worn) {
                if (piece.contains(stack.typeHolder())) {
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
        String pattern = colorway.pattern().flatMap(h -> h.unwrapKey()).map(k -> k.identifier().toString()).orElse("solid");
        return Optional.of(pattern + "/" + Colors.toHex(colorway.baseColor()));
    }

    private static void addEffects(List<PlannedEffect> out, List<ConfiguredEffect> effects, String prefix, double scale) {
        for (int i = 0; i < effects.size(); i++) {
            out.add(new PlannedEffect(effects.get(i),
                    new EffectSource(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, prefix + "/" + i), scale)));
        }
    }

    private static String sourceId(String kind, Identifier id) {
        return kind + "/" + id.getNamespace() + "/" + id.getPath();
    }
}
