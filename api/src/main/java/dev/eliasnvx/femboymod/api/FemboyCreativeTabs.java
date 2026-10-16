package dev.eliasnvx.femboymod.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.ApiStatus;

/**
 * Keys of femboymod's creative tabs, so addons can put their items next to ours (for example with Architectury's
 * {@code Item.Properties#arch$tab(key)} or the loader's tab events) without touching femboymod internals.
 * Items an addon adds are shown after femboymod's own in that tab. The ids sort in menu order: on 1.20.1 Fabric and
 * Forge both order mod tabs by id.
 */
@ApiStatus.AvailableSince("0.2.0")
public final class FemboyCreativeTabs {

    /** Everything worn: cosmetics by body part, backpacks and charms, the Lookbook. In 0.1 the only tab, {@code main}. */
    public static final ResourceKey<CreativeModeTab> OUTFITS = key("clothes");
    /** Furniture and decor: mirrors, the gamer corner, lights, the cozy interior, plushies, posters. */
    public static final ResourceKey<CreativeModeTab> ROOM = key("decor");
    /** Food and drinks, and gadgets: the phone, photos, Style Coupons. */
    public static final ResourceKey<CreativeModeTab> SNACKS = key("food");
    /** Ores, gems and crafting materials. */
    public static final ResourceKey<CreativeModeTab> MATERIALS = key("materials");
    /** Spawn eggs and pet outfits. */
    public static final ResourceKey<CreativeModeTab> MOBS = key("mobs");

    private FemboyCreativeTabs() {
    }

    private static ResourceKey<CreativeModeTab> key(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(FemboyApi.MOD_ID, path));
    }
}
