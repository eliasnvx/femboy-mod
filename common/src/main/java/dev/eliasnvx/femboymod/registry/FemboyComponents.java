package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyDataComponents;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import dev.eliasnvx.femboymod.backpack.BackpackSpec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.function.Supplier;

public final class FemboyComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(FemboyMod.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Cosmetic>> COSMETIC = REGISTER.register("cosmetic",
            () -> DataComponentType.<Cosmetic>builder()
                    .persistent(Cosmetic.CODEC)
                    .networkSynchronized(Cosmetic.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static final RegistrySupplier<DataComponentType<Colorway>> COLORWAY = REGISTER.register("colorway",
            () -> DataComponentType.<Colorway>builder()
                    .persistent(Colorway.CODEC)
                    .networkSynchronized(Colorway.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static final RegistrySupplier<DataComponentType<BackpackSpec>> BACKPACK = REGISTER.register("backpack",
            () -> DataComponentType.<BackpackSpec>builder()
                    .persistent(BackpackSpec.CODEC)
                    .networkSynchronized(BackpackSpec.STREAM_CODEC)
                    .build());

    /** Printed photo: picture id and author. */
    public static final RegistrySupplier<DataComponentType<dev.eliasnvx.femboymod.photo.PhotoData>> PHOTO = REGISTER.register("photo",
            () -> DataComponentType.<dev.eliasnvx.femboymod.photo.PhotoData>builder()
                    .persistent(dev.eliasnvx.femboymod.photo.PhotoData.CODEC)
                    .networkSynchronized(dev.eliasnvx.femboymod.photo.PhotoData.STREAM_CODEC)
                    .build());
    /** Charms on a backpack; same format as vanilla container contents. */
    public static final RegistrySupplier<DataComponentType<ItemContainerContents>> CHARMS = REGISTER.register("charms",
            () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    /** A drink cooled in a Mini Fridge: Byte Energy buffs last longer (config {@code furniture.chilled_bonus}). */
    public static final RegistrySupplier<DataComponentType<Unit>> CHILLED = REGISTER.register("chilled",
            () -> DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build());

    public static final FemboyDataComponents API = new FemboyDataComponents() {
        @Override
        public Supplier<DataComponentType<Cosmetic>> cosmetic() {
            return COSMETIC;
        }

        @Override
        public Supplier<DataComponentType<Colorway>> colorway() {
            return COLORWAY;
        }
    };

    private FemboyComponents() {
    }
}
