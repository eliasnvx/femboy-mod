package dev.eliasnvx.femboymod.api.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;

/**
 * Where to get a cosmetic, shown in the Lookbook. Loaded from the data pack registry {@code femboymod:lookbook},
 * synced to clients. The entry id must equal the item id: {@code mymod:scarf} lives in
 * {@code data/mymod/femboymod/lookbook/scarf.json}.
 * <pre>{@code
 * { "sources": [
 *     { "kind": "crafting", "items": ["minecraft:white_wool", { "id": "minecraft:string", "count": 2 }] },
 *     { "kind": "trade", "from": "femboymod:thrifter", "level": 2, "items": [{ "id": "minecraft:emerald", "count": 7 }] },
 *     { "kind": "drop", "from": "femboymod:fashion_critic" },
 *     { "kind": "note", "note": "lookbook.mymod.scarf.secret" } ] }
 * }</pre>
 * Cosmetics without an entry still show up in the Lookbook, with an empty "where to get it" list.
 *
 * @param sources the ways to get the item, in display order
 */
@ApiStatus.Experimental
@ApiStatus.AvailableSince("0.2.0")
public record LookbookEntry(List<Source> sources) {

    /** Registry key of the {@code femboymod:lookbook} data pack registry. */
    public static final ResourceKey<Registry<LookbookEntry>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, "lookbook"));

    /** Entry of items without one. */
    public static final LookbookEntry NONE = new LookbookEntry(List.of());

    /** JSON codec. */
    public static final Codec<LookbookEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Source.CODEC.listOf().optionalFieldOf("sources", List.of()).forGetter(LookbookEntry::sources)
    ).apply(instance, LookbookEntry::new));

    /** Copies the source list. */
    public LookbookEntry {
        sources = List.copyOf(sources);
    }

    /**
     * Returns the registry key holding the entry of an item.
     *
     * @param item the item
     * @return {@code femboymod:lookbook} key with the item's id
     */
    public static ResourceKey<LookbookEntry> keyOf(Item item) {
        return ResourceKey.create(REGISTRY_KEY, BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * One way to get the item.
     *
     * @param kind  how it is obtained
     * @param from  the entity type or villager profession involved (trader, mob, gift giver), if any
     * @param level the trader level, 0 if it doesn't apply
     * @param items crafting ingredients, smithing inputs or the trade price
     * @param note  an extra line (translation key), e.g. "in an exclusive colorway"
     */
    public record Source(Kind kind, Optional<ResourceLocation> from, int level, List<Count> items, Optional<String> note) {

        /** JSON codec. */
        public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Kind.CODEC.fieldOf("kind").forGetter(Source::kind),
                ResourceLocation.CODEC.optionalFieldOf("from").forGetter(Source::from),
                Codec.intRange(0, 100).optionalFieldOf("level", 0).forGetter(Source::level),
                Count.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Source::items),
                Codec.STRING.optionalFieldOf("note").forGetter(Source::note)
        ).apply(instance, Source::new));

        /** Copies the item list. */
        public Source {
            items = List.copyOf(items);
        }
    }

    /**
     * An item and how many. Not an {@link ItemStack}: on 26.3 stacks can't be built while data pack registries load.
     * 1.21.1 could build them, but the record stays so that the API is the same on both branches.
     *
     * @param item  the item
     * @param count how many, at least 1
     */
    public record Count(Item item, int count) {

        private static final Codec<Count> FULL = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(Count::item),
                Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(Count::count)
        ).apply(instance, Count::new));

        /** An item id, or an object with {@code id} and {@code count}. */
        public static final Codec<Count> CODEC = Codec.withAlternative(FULL,
                BuiltInRegistries.ITEM.byNameCodec().xmap(item -> new Count(item, 1), Count::item));

        /**
         * Builds a stack for display. Call only after the game has loaded (item components are bound).
         *
         * @return a new stack of {@link #item} with {@link #count}
         */
        public ItemStack toStack() {
            return new ItemStack(item, count);
        }
    }

    /** How a cosmetic is obtained. */
    public enum Kind implements StringRepresentable {
        /** Crafting grid; {@code items} are the ingredients. */
        CRAFTING("crafting"),
        /** Smithing table; {@code items} are template, base and addition. */
        SMITHING("smithing"),
        /** Bought from {@code from} (an entity type or villager profession) at {@code level}; {@code items} is the price. */
        TRADE("trade"),
        /** Dropped by the mob {@code from}. */
        DROP("drop"),
        /** Brought as a gift by {@code from}. */
        GIFT("gift"),
        /** Only the {@code note} line. */
        NOTE("note");

        /** JSON codec (lowercase names). */
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
