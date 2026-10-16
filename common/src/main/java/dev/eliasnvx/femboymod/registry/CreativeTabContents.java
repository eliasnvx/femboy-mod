package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.block.DyedFurniture;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.food.DrinkItem;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;

/**
 * Contents of the femboymod creative tabs ({@link Tab}). Every item lands in exactly one tab, decided by what it is
 * ({@link #tabOf}); outfits go by body part, the room and the rest follow hand-made groupings. Items are followed by
 * ready-made colorways and dye colors: presentation only, players get any of them by dyeing.
 * <p>
 * The order lists name items by path, as in 26.3; paths not registered in this port yet are simply skipped.
 */
final class CreativeTabContents {

    /** The tabs, in creative menu order; ids match {@link dev.eliasnvx.femboymod.api.FemboyCreativeTabs}. */
    enum Tab {
        OUTFITS("clothes"), ROOM("decor"), SNACKS("food"), MATERIALS("materials"), MOBS("mobs");

        final String id;

        Tab(String id) {
            this.id = id;
        }
    }

    /** Room pieces grouped by use: storage, mirrors, seats, the gamer corner, lights, soft decor. */
    private static final List<String> ROOM_ORDER = List.of(
            "wardrobe", "clothing_rack", "laundry_basket", "mini_fridge",
            "mirror", "gilded_mirror", "vanity_table",
            "gamer_chair", "bean_bag", "pet_bed",
            "gamer_monitor", "gamer_keyboard", "terminal", "vibe_scanner",
            "led_strip", "neon_sign", "moonstone_lamp", "fairy_lights", "lava_lamp",
            "fluffy_rug", "shark_plush", "cat_plush", "creeper_plush", "rubber_duck");
    /** Each material next to its ore and blocks. */
    private static final List<String> MATERIALS_ORDER = List.of(
            "rose_quartz_ore", "deepslate_rose_quartz_ore", "rose_quartz", "rose_quartz_block", "budding_rose_quartz",
            "small_rose_quartz_bud", "medium_rose_quartz_bud", "large_rose_quartz_bud", "rose_quartz_cluster",
            "glitter_ore", "glitter", "deepslate_moonstone_ore", "moonstone", "neon_quartz_ore", "neon_quartz",
            "glitch_shard", "lost_sock");
    /** Block items that are materials rather than furniture. */
    private static final Set<String> MATERIAL_BLOCKS = Set.of(
            "rose_quartz_ore", "deepslate_rose_quartz_ore", "rose_quartz_block", "budding_rose_quartz",
            "small_rose_quartz_bud", "medium_rose_quartz_bud", "large_rose_quartz_bud", "rose_quartz_cluster",
            "glitter_ore", "deepslate_moonstone_ore", "neon_quartz_ore");
    /** Drinks first, then food, then gadgets. */
    private static final List<String> SNACKS_ORDER = List.of(
            "byte_energy_pink", "byte_energy_blue", "byte_energy_purple", "byte_energy_rainbow", "empty_energy_can",
            "bubble_tea", "strawberry_milk", "mochi", "onigiri", "candy", "phone", "photo", "style_coupon");
    /** Not food, but at home in the snacks and gadgets tab. */
    private static final Set<String> GADGETS = Set.of("empty_energy_can", "phone", "photo", "style_coupon");

    private static final ResourceKey<ColorwayPattern> STRIPES = ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "stripes"));
    private static final int WHITE = 0xFFFFFF;
    private static final String ENTITY_ID_KEY = "id";
    private static final String PAINTING_ENTITY_ID = "minecraft:painting";

    /** A solid colorway, or two-color stripes (base + secondary) when {@code striped}. */
    private record Preset(int base, boolean striped, int secondary) {
        static Preset solid(int color) {
            return new Preset(color, false, WHITE);
        }

        static Preset stripes(int base, int secondary) {
            return new Preset(base, true, secondary);
        }
    }

    private static final List<Preset> BACKPACK_COLORS = List.of(
            Preset.solid(0x6FBF73),  // green
            Preset.solid(0xF4A6C8),  // pink
            Preset.solid(0xAE8BE0)); // purple

    /** Black, light blue and red stripes on white (the plain item is pink-white). */
    private static final List<Preset> STRIPED = List.of(
            Preset.stripes(0x2A2A33, WHITE),
            Preset.stripes(0x8FD3F4, WHITE),
            Preset.stripes(0xE0343F, WHITE));

    private static final List<Preset> HOODIE_COLORS = List.of(
            Preset.solid(0xF7B8D2),  // pastel pink
            Preset.solid(0xA8E6CF),  // mint
            Preset.solid(0xA7D3F2),  // baby blue
            Preset.solid(0xF2F0F2),  // white
            Preset.solid(0x2B2A33)); // black

    /** Headphones come in pink (the plain item), black and white. */
    private static final List<Preset> HEADPHONE_COLORS = List.of(
            Preset.solid(0x2B2A33),  // black
            Preset.solid(0xF2F0F2)); // white

    /** Item path -> extra variants shown after the plain item (the plain socks are pink-white already). */
    private static final Map<String, List<Preset>> PRESETS = Map.of(
            "programming_socks", STRIPED,
            "striped_mittens", STRIPED,
            "oversized_hoodie", HOODIE_COLORS,
            "cat_ear_hoodie", HOODIE_COLORS,
            "canvas_backpack", BACKPACK_COLORS,
            "cat_ear_headphones", HEADPHONE_COLORS);

    /** Dyeable furniture (pink by default) also shows up in a few other dye colors. */
    private static final List<DyeColor> FURNITURE_PRESETS = List.of(DyeColor.PURPLE, DyeColor.LIGHT_BLUE, DyeColor.WHITE);
    private static final Map<String, List<DyeColor>> FURNITURE_COLORS = Map.of(
            "bean_bag", FURNITURE_PRESETS,
            "fluffy_rug", FURNITURE_PRESETS,
            "pet_bed", FURNITURE_PRESETS,
            "lava_lamp", FURNITURE_PRESETS);

    private CreativeTabContents() {
    }

    /** Which tab an item of ours belongs in. */
    static Tab tabOf(Item item, String path) {
        if (item == FemboyItems.LOOKBOOK.get()) {
            return Tab.OUTFITS; // the guide to the outfits
        }
        if (item instanceof SpawnEggItem || item instanceof PetOutfitItem) {
            return Tab.MOBS;
        }
        ItemStack stack = item.getDefaultInstance();
        if (stack.has(FemboyComponents.COSMETIC.get()) || stack.is(FemboyTags.CHARMS)) {
            return Tab.OUTFITS;
        }
        if (GADGETS.contains(path)) {
            return Tab.SNACKS; // the empty can is also a block you can place
        }
        if (item instanceof BlockItem) {
            return MATERIAL_BLOCKS.contains(path) ? Tab.MATERIALS : Tab.ROOM;
        }
        // 1.21.1 has no consumable component: drinks without food (Byte Energy) are DrinkItems
        if (stack.has(DataComponents.FOOD) || item instanceof DrinkItem) {
            return Tab.SNACKS;
        }
        return Tab.MATERIALS;
    }

    static void fill(Tab tab, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        Optional<Holder<ColorwayPattern>> stripes = parameters.holders().lookup(ColorwayPattern.REGISTRY_KEY)
                .flatMap(lookup -> lookup.get(STRIPES))
                .map(holder -> holder);
        List<RegistrySupplier<Item>> items = new ArrayList<>();
        for (RegistrySupplier<Item> entry : FemboyItems.TAB_ORDER) {
            if (!items.contains(entry) && tabOf(entry.get(), entry.getId().getPath()) == tab) {
                items.add(entry);
            }
        }
        items.sort(order(tab)); // stable: registration order breaks ties
        for (RegistrySupplier<Item> entry : items) {
            acceptWithVariants(entry, parameters, output, stripes);
        }
        if (tab == Tab.ROOM) {
            posters(parameters, output);
        }
    }

    /** Outfits by body part (slot order; charms after everything worn), the rest by the lists above. */
    private static Comparator<RegistrySupplier<Item>> order(Tab tab) {
        return switch (tab) {
            case OUTFITS -> Comparator.comparingInt(entry -> outfitRank(entry.get()));
            case ROOM -> byList(ROOM_ORDER);
            case SNACKS -> byList(SNACKS_ORDER);
            case MATERIALS -> byList(MATERIALS_ORDER);
            case MOBS -> Comparator.comparingInt(entry -> entry.get() instanceof SpawnEggItem ? 0 : 1);
        };
    }

    private static int outfitRank(Item item) {
        if (item == FemboyItems.LOOKBOOK.get()) {
            return -1;
        }
        Cosmetic cosmetic = item.getDefaultInstance().get(FemboyComponents.COSMETIC.get());
        if (cosmetic == null) {
            return Integer.MAX_VALUE; // charms
        }
        int slot = CosmeticsManager.orderedSlots().indexOf(cosmetic.slot());
        return slot < 0 ? Integer.MAX_VALUE - 1 : slot;
    }

    private static Comparator<RegistrySupplier<Item>> byList(List<String> order) {
        return Comparator.comparingInt(entry -> {
            int index = order.indexOf(entry.getId().getPath());
            return index < 0 ? Integer.MAX_VALUE : index;
        });
    }

    private static void acceptWithVariants(RegistrySupplier<Item> entry, CreativeModeTab.ItemDisplayParameters parameters,
                                           CreativeModeTab.Output output, Optional<Holder<ColorwayPattern>> stripes) {
        Item item = entry.get();
        output.accept(item);
        if (item == FemboyItems.PRIDE_BADGE.get()) {
            // one badge per pride pattern (data-driven: any colorway named pride_*)
            parameters.holders().lookup(ColorwayPattern.REGISTRY_KEY).ifPresent(patterns -> patterns.listElements()
                    .filter(pattern -> pattern.key().location().getPath().startsWith("pride_"))
                    .sorted(Comparator.comparing(pattern -> pattern.key().location().getPath()))
                    .forEach(pattern -> {
                        ItemStack badge = new ItemStack(item);
                        badge.set(FemboyComponents.COLORWAY.get(), new Colorway(WHITE, Optional.of(pattern), Optional.empty()));
                        output.accept(badge);
                    }));
        }
        for (Preset preset : PRESETS.getOrDefault(entry.getId().getPath(), List.of())) {
            if (preset.striped() && stripes.isEmpty()) {
                continue; // data pack removed the pattern
            }
            ItemStack stack = new ItemStack(item);
            stack.set(FemboyComponents.COLORWAY.get(), preset.striped()
                    ? new Colorway(preset.base(), stripes, Optional.of(preset.secondary()))
                    : Colorway.solid(preset.base()));
            output.accept(stack);
        }
        for (DyeColor color : FURNITURE_COLORS.getOrDefault(entry.getId().getPath(), List.of())) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(DyedFurniture.COLOR, color));
            output.accept(stack);
        }
    }

    private static void posters(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        // Posters: the mod's painting variants (data-driven), as ready-to-hang paintings
        RegistryOps<Tag> ops = parameters.holders().createSerializationContext(NbtOps.INSTANCE);
        parameters.holders().lookup(Registries.PAINTING_VARIANT).ifPresent(lookup -> lookup.listElements()
                .filter(holder -> holder.key().location().getNamespace().equals(FemboyMod.MOD_ID))
                .sorted(Comparator.comparing(holder -> holder.key().location().getPath()))
                .forEach(holder -> {
                    // 1.21.1 keeps the variant in the painting's entity data (like vanilla's preset paintings)
                    CustomData entityData = CustomData.EMPTY.update(ops, Painting.VARIANT_MAP_CODEC, holder).getOrThrow()
                            .update(tag -> tag.putString(ENTITY_ID_KEY, PAINTING_ENTITY_ID));
                    ItemStack poster = new ItemStack(Items.PAINTING);
                    poster.set(DataComponents.ENTITY_DATA, entityData);
                    output.accept(poster);
                }));
    }
}
