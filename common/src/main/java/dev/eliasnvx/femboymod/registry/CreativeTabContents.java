package dev.eliasnvx.femboymod.registry;

import java.util.Comparator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contents of the femboymod creative tab: every item in registration order, each followed by ready-made
 * colorways. Presentation only: players get any colorway by dyeing, these are just a quick pick.
 */
final class CreativeTabContents {

    private static final ResourceKey<ColorwayPattern> STRIPES = ResourceKey.create(ColorwayPattern.REGISTRY_KEY,
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "stripes"));
    private static final int WHITE = 0xFFFFFF;

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

    private CreativeTabContents() {
    }

    static void fill(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        Optional<Holder<ColorwayPattern>> stripes = parameters.holders().lookup(ColorwayPattern.REGISTRY_KEY)
                .flatMap(lookup -> lookup.get(STRIPES))
                .map(holder -> holder);
        for (RegistrySupplier<Item> entry : FemboyItems.TAB_ORDER) {
            Item item = entry.get();
            output.accept(item);
            if (item == FemboyItems.PRIDE_BADGE.get()) {
                // one badge per pride pattern (data-driven: any colorway named pride_*)
                parameters.holders().lookup(ColorwayPattern.REGISTRY_KEY).ifPresent(patterns -> patterns.listElements()
                        .filter(pattern -> pattern.key().identifier().getPath().startsWith("pride_"))
                        .sorted(Comparator.comparing(pattern -> pattern.key().identifier().getPath()))
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
        }
        // Posters: the mod's painting variants (data-driven), as ready-to-hang paintings
        parameters.holders().lookup(Registries.PAINTING_VARIANT).ifPresent(lookup -> lookup.listElements()
                .filter(holder -> holder.key().identifier().getNamespace().equals(FemboyMod.MOD_ID))
                .sorted(Comparator.comparing(holder -> holder.key().identifier().getPath()))
                .forEach(holder -> {
                    ItemStack poster = new ItemStack(Items.PAINTING);
                    poster.set(DataComponents.PAINTING_VARIANT, holder);
                    output.accept(poster);
                }));
    }
}
