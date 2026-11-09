package dev.eliasnvx.femboymod.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * How a reactive cosmetic "hears" (client resources, {@code assets/<ns>/femboymod/hearing/*.json}): which worn
 * items it covers, how far they notice mobs, what they notice first, whether they startle at harmless mobs and
 * how far they flatten. Visual only, so resource packs and addons can tune it.
 *
 * <pre>{@code
 * {
 *   "items": ["femboymod:cat_ears"],
 *   "radius": 16,
 *   "notices_first": ["minecraft:creeper", "#minecraft:skeletons"],
 *   "notices_first_radius": 24,
 *   "night_radius": 20,
 *   "startles": false,
 *   "flatten": 1.0
 * }
 * }</pre>
 */
public final class EarHearing {

    public static final Codec<EarHearing> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().fieldOf("items").forGetter(h -> h.items),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("radius").forGetter(h -> h.radius),
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("night_radius").forGetter(h -> h.nightRadius),
            ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().optionalFieldOf("notices_first", List.of()).forGetter(h -> h.noticesFirst),
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("notices_first_radius").forGetter(h -> h.noticesFirstRadius),
            Codec.BOOL.optionalFieldOf("startles", false).forGetter(h -> h.startles),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flatten", 1.0F).forGetter(h -> h.flatten)
    ).apply(i, EarHearing::new));

    private static final String DIRECTORY = "femboymod/hearing";
    /** Sorted by file id, so the first match is stable across reloads. */
    private static List<EarHearing> loaded = List.of();
    /** Bumped on every reload; render data re-resolves its hearing when it changes. */
    private static int generation;

    private final List<ExtraCodecs.TagOrElementLocation> items;
    private final float radius;
    private final Optional<Float> nightRadius;
    private final List<ExtraCodecs.TagOrElementLocation> noticesFirst;
    private final Optional<Float> noticesFirstRadius;
    private final boolean startles;
    private final float flatten;
    private final Matcher<Item> itemMatcher;
    private final Matcher<EntityType<?>> firstMatcher;

    public EarHearing(List<ExtraCodecs.TagOrElementLocation> items, float radius, Optional<Float> nightRadius,
                      List<ExtraCodecs.TagOrElementLocation> noticesFirst, Optional<Float> noticesFirstRadius,
                      boolean startles, float flatten) {
        this.items = List.copyOf(items);
        this.radius = radius;
        this.nightRadius = nightRadius;
        this.noticesFirst = List.copyOf(noticesFirst);
        this.noticesFirstRadius = noticesFirstRadius;
        this.startles = startles;
        this.flatten = flatten;
        this.itemMatcher = Matcher.of(Registries.ITEM, items);
        this.firstMatcher = Matcher.of(Registries.ENTITY_TYPE, noticesFirst);
    }

    /** How far mobs are noticed; fox-like ears hear further in the dark. */
    public float radius(boolean dark) {
        return dark ? nightRadius.orElse(radius) : radius;
    }

    /** How far the mobs in {@code notices_first} are noticed (at least the normal radius). */
    public float firstRadius(boolean dark) {
        return Math.max(radius(dark), noticesFirstRadius.orElse(0.0F));
    }

    public boolean noticesFirst(Entity entity) {
        return firstMatcher.matches(entity.getType().builtInRegistryHolder());
    }

    /** Notices every mob, not only hostile ones (bunny ears). */
    public boolean startles() {
        return startles;
    }

    /** 0..1 how far these ears flatten (bear ears never do). */
    public float flatten() {
        return flatten;
    }

    public boolean covers(ItemStack stack) {
        return itemMatcher.matches(stack.getItemHolder());
    }

    public static int generation() {
        return generation;
    }

    /**
     * The hearing of the worn item in the earliest slot (by slot sort order) that has one, so ears on the head
     * decide over a tail worn at the same time. Called when the worn list changes, not per frame.
     */
    public static @Nullable EarHearing find(List<CosmeticRenderData.Worn> worn) {
        List<EarHearing> all = loaded;
        EarHearing best = null;
        int bestOrder = Integer.MAX_VALUE;
        for (CosmeticRenderData.Worn w : worn) {
            int order = FemboyMod.api().cosmeticSlots().get(w.slot()).map(CosmeticSlotType::sortOrder).orElse(Integer.MAX_VALUE);
            if (best != null && order >= bestOrder) {
                continue;
            }
            for (EarHearing hearing : all) {
                if (hearing.covers(w.stack())) {
                    best = hearing;
                    bestOrder = order;
                    break;
                }
            }
        }
        return best;
    }

    /** Ids and tags of one registry; tags are checked against the holder, so they work after tag reloads. */
    private record Matcher<T>(List<TagKey<T>> tags, List<ResourceLocation> ids) {

        static <T> Matcher<T> of(ResourceKey<? extends net.minecraft.core.Registry<T>> registry,
                                 List<ExtraCodecs.TagOrElementLocation> entries) {
            List<TagKey<T>> tags = new ArrayList<>();
            List<ResourceLocation> ids = new ArrayList<>();
            for (ExtraCodecs.TagOrElementLocation entry : entries) {
                if (entry.tag()) {
                    tags.add(TagKey.create(registry, entry.id()));
                } else {
                    ids.add(entry.id());
                }
            }
            return new Matcher<>(List.copyOf(tags), List.copyOf(ids));
        }

        boolean matches(Holder<T> holder) {
            for (ResourceLocation id : ids) {
                if (holder.is(id)) {
                    return true;
                }
            }
            for (TagKey<T> tag : tags) {
                if (holder.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 1.21.1's JSON reload listener hands over raw JSON; each file is decoded with {@link #CODEC} here. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        public Loader() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            TreeMap<ResourceLocation, EarHearing> sorted = new TreeMap<>();
            map.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> FemboyMod.LOGGER.warn("Bad hearing profile {}: {}", id, error))
                    .ifPresent(hearing -> sorted.put(id, hearing)));
            loaded = List.copyOf(sorted.values());
            generation++;
            FemboyMod.LOGGER.debug("Loaded {} hearing profiles for reactive cosmetics", loaded.size());
        }
    }
}
