package dev.eliasnvx.femboymod.network;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import dev.eliasnvx.femboymod.api.backpack.CharmStats;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.LookbookEntry;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

/**
 * The data pack registries the client gets a copy of (tooltips, Lookbook, colorways), registered by each loader with
 * {@link Entry#networkCodec()}.
 *
 * <p>On 1.20.1 the client decodes them from the login packet, before it has any tags. Vanilla's synced registries hold
 * plain tag keys; ours hold holder sets (the programming socks' {@code #femboymod:bugs}, set bonus pieces, addon
 * effect types), and a plain decode fails with "Missing tag", which disconnected every player joining a dedicated
 * server. The network codec decodes through tag-adding lookups instead, the way vanilla loads data pack registries
 * before tags on the server: a tag reference becomes the registry's named set, which the tags packet fills right after.
 * Single player never decodes the packet, so it never showed there.
 */
public final class SyncedRegistries {

    /** A synced data pack registry and its JSON codec. */
    public record Entry<T>(ResourceKey<Registry<T>> key, Codec<T> codec) {

        /** The codec the login packet uses: {@link #codec} with tags resolved once they arrive. */
        public Codec<T> networkCodec() {
            return deferTags(codec);
        }
    }

    public static final List<Entry<?>> ALL = List.of(
            new Entry<>(ColorwayPattern.REGISTRY_KEY, ColorwayPattern.CODEC),
            new Entry<>(CosmeticStats.REGISTRY_KEY, CosmeticStats.CODEC),
            new Entry<>(LookbookEntry.REGISTRY_KEY, LookbookEntry.CODEC),
            new Entry<>(SetBonus.REGISTRY_KEY, SetBonus.CODEC),
            new Entry<>(DripRules.REGISTRY_KEY, DripRules.CODEC),
            new Entry<>(CharmStats.REGISTRY_KEY, CharmStats.CODEC));

    private SyncedRegistries() {
    }

    /** Encodes like {@code codec}; decodes with tag-adding lookups for built-in registries. */
    public static <T> Codec<T> deferTags(Codec<T> codec) {
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
                return codec.decode(ops instanceof RegistryOps<O> registryOps ? tagAdding(registryOps) : ops, input);
            }

            @Override
            public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
                return codec.encode(input, ops, prefix);
            }

            @Override
            public String toString() {
                return "DeferredTags[" + codec + "]";
            }
        };
    }

    /** {@code ops} with each built-in registry's getter swapped for {@link Registry#asTagAddingLookup()}. */
    private static <O> RegistryOps<O> tagAdding(RegistryOps<O> ops) {
        return RegistryOps.create(ops, new RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> key) {
                Optional<HolderOwner<E>> owner = ops.owner(key);
                Optional<HolderGetter<E>> getter = ops.getter(key);
                if (owner.isEmpty() || getter.isEmpty()) {
                    return Optional.empty();
                }
                Registry<E> builtIn = (Registry<E>) BuiltInRegistries.REGISTRY.get(key.location());
                HolderGetter<E> tags = builtIn != null ? builtIn.asTagAddingLookup() : getter.get();
                return Optional.of(new RegistryOps.RegistryInfo<>(owner.get(), tags, Lifecycle.stable()));
            }
        });
    }
}
