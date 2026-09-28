package dev.eliasnvx.femboymod.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Recipe serializers plus the cauldron washing hook (1.21.1 stand-ins for 26.3 vanilla data). */
public final class FemboyRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(FemboyMod.MOD_ID, Registries.RECIPE_SERIALIZER);

    /** {@code femboymod:crafting_transmute}, used by {@code recipe/cat_ear_hoodie.json} and {@code recipe/leather_backpack.json}. */
    public static final RegistrySupplier<RecipeSerializer<TransmuteRecipe>> CRAFTING_TRANSMUTE =
            SERIALIZERS.register("crafting_transmute", TransmuteRecipe.Serializer::new);

    private FemboyRecipes() {
    }

    /** Called from {@code FemboyMod.init()}, after the item register. */
    public static void init() {
        SERIALIZERS.register();
        CauldronWashing.register();
    }
}
