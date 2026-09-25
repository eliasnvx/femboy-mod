package dev.eliasnvx.femboymod.api;

import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import net.minecraft.core.component.DataComponentType;

import java.util.function.Supplier;

/**
 * Data component types added by Femboy Mod. Obtain via {@link FemboyApi#components()}.
 *
 * <p>The suppliers resolve once Minecraft's registries are populated (they are safe to call
 * inside item suppliers passed to a deferred register), not during {@code onInitialize}.
 */
public interface FemboyDataComponents {

    /**
     * {@code femboymod:cosmetic} — see {@link Cosmetic}.
     *
     * @return supplier of the component type
     */
    Supplier<DataComponentType<Cosmetic>> cosmetic();

    /**
     * {@code femboymod:colorway} — see {@link Colorway}.
     *
     * @return supplier of the component type
     */
    Supplier<DataComponentType<Colorway>> colorway();
}
