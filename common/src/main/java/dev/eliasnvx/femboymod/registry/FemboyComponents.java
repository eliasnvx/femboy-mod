package dev.eliasnvx.femboymod.registry;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyDataComponents;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.item.ItemData;
import dev.eliasnvx.femboymod.backpack.BackpackSpec;
import dev.eliasnvx.femboymod.item.ItemList;
import dev.eliasnvx.femboymod.item.NbtItemData;

/**
 * Item data of the mod. Minecraft 1.20.1 has no data components, so each value is stored in the stack's NBT
 * under {@code femboymod:<name>} (see {@link NbtItemData}). Read with {@code X.get(stack)}, write with
 * {@code X.set(stack, value)}.
 */
public final class FemboyComponents {

    public static final NbtItemData<Cosmetic> COSMETIC = data("cosmetic", Cosmetic.CODEC);
    public static final NbtItemData<Colorway> COLORWAY = data("colorway", Colorway.CODEC);
    public static final NbtItemData<BackpackSpec> BACKPACK = data("backpack", BackpackSpec.CODEC);
    /** Charms on a backpack. */
    public static final NbtItemData<ItemList> CHARMS = data("charms", ItemList.CODEC);
    /** Items inside a backpack (1.21 used the vanilla container component). */
    public static final NbtItemData<ItemList> CONTENTS = data("contents", ItemList.CODEC);

    public static final FemboyDataComponents API = new FemboyDataComponents() {
        @Override
        public ItemData<Cosmetic> cosmetic() {
            return COSMETIC;
        }

        @Override
        public ItemData<Colorway> colorway() {
            return COLORWAY;
        }
    };

    private FemboyComponents() {
    }

    private static <T> NbtItemData<T> data(String name, com.mojang.serialization.Codec<T> codec) {
        return new NbtItemData<>(FemboyMod.MOD_ID + ":" + name, codec);
    }
}
