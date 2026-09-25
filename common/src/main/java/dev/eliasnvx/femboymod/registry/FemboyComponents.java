package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyDataComponents;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

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
