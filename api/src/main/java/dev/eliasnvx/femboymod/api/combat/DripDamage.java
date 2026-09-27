package dev.eliasnvx.femboymod.api.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Drip in combat: damage that the listed attackers deal to a player is multiplied by the entry for the
 * player's Drip tier ({@code multiplier_by_tier[tier]}, the last entry is used for higher tiers).
 * Data pack registry {@code data/<ns>/femboymod/drip_damage/<name>.json}; server side only.
 *
 * <pre>{@code
 * { "attackers": "#femboymod:bugs", "multiplier_by_tier": [1.0, 0.9, 0.75, 0.6, 0.5, 0.4] }
 * }</pre>
 *
 * @param attackers        entity types (id, list or tag) whose damage is scaled
 * @param multiplierByTier multiplier per Drip tier, starting at tier 0; never empty
 */
@ApiStatus.Experimental
public record DripDamage(HolderSet<EntityType<?>> attackers, List<Float> multiplierByTier) {

    /** Registry key of the {@code femboymod:drip_damage} data pack registry. */
    public static final ResourceKey<Registry<DripDamage>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, "drip_damage"));

    /** JSON codec. */
    public static final Codec<DripDamage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("attackers").forGetter(DripDamage::attackers),
            Codec.floatRange(0.0F, 10.0F).listOf(1, 32).fieldOf("multiplier_by_tier").forGetter(DripDamage::multiplierByTier)
    ).apply(instance, DripDamage::new));

    /** Defensive copy. */
    public DripDamage {
        multiplierByTier = List.copyOf(multiplierByTier);
    }

    /**
     * Returns the multiplier for a Drip tier.
     *
     * @param tier the player's Drip tier (0 = no tier)
     * @return the multiplier; tiers beyond the list use its last entry
     */
    public float multiplierFor(int tier) {
        return multiplierByTier.get(Math.max(0, Math.min(tier, multiplierByTier.size() - 1)));
    }
}
