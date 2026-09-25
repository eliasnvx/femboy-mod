package dev.eliasnvx.femboymod.api;

import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticsView;
import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import dev.eliasnvx.femboymod.api.internal.FemboyApiHolder;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Entry point to the Femboy Mod public API.
 *
 * <p>Obtain the instance with {@link #get()}. Addons normally receive it directly in
 * {@link FemboyAddon#onInitialize(FemboyApi)} and do not need to call {@link #get()} at all.
 *
 * <p>Only types in the {@code dev.eliasnvx.femboymod.api} package tree are covered by the API
 * compatibility guarantees (SemVer, see {@link #apiVersion()}). Anything annotated with
 * {@link org.jetbrains.annotations.ApiStatus.Internal} may change without notice.
 */
public interface FemboyApi {

    /** Mod id and resource namespace of Femboy Mod: {@value}. */
    String MOD_ID = "femboymod";

    /**
     * Returns the API instance.
     *
     * <p>Safe to call from any thread once Femboy Mod has finished its common initialization.
     *
     * @return the API instance, never {@code null}
     * @throws IllegalStateException if called before Femboy Mod has initialized
     */
    static FemboyApi get() {
        return FemboyApiHolder.get();
    }

    /**
     * Returns the version of the public API implemented by the running mod, in SemVer format
     * (for example {@code "1.2.0"}). This is versioned independently of the mod itself.
     *
     * @return the API version string, never {@code null}
     */
    String apiVersion();

    /**
     * Returns the event bus for Femboy Mod events ({@code dev.eliasnvx.femboymod.api.event}).
     *
     * @return the event bus
     */
    FemboyEventBus events();

    /**
     * Returns the registry of cosmetic slot types. Built-in slots are listed in
     * {@link dev.eliasnvx.femboymod.api.cosmetic.FemboySlots}.
     *
     * @return the slot type registry
     */
    ApiRegistry<CosmeticSlotType> cosmeticSlots();

    /**
     * Returns the data component types added by Femboy Mod.
     *
     * @return the component types
     */
    FemboyDataComponents components();

    /**
     * Returns the cosmetics an entity is wearing. Only players can wear cosmetics at the moment;
     * other entities return an empty view.
     *
     * @param entity the entity
     * @return a read-only view, never {@code null}
     */
    CosmeticsView getCosmetics(LivingEntity entity);

    /**
     * Returns the colorway of an item.
     *
     * @param stack the item
     * @return the colorway, or empty if the item has none
     */
    Optional<Colorway> getColorway(ItemStack stack);
}
