package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class FemboyMenus {

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<CosmeticsMenu>> COSMETICS =
            REGISTER.register("cosmetics", () -> new MenuType<>(CosmeticsMenu::new, FeatureFlags.VANILLA_SET));

    private FemboyMenus() {
    }
}
