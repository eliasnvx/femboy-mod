package dev.eliasnvx.femboymod.compat.jei;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * JEI integration (SPEC §9, soft dependency): recipes are vanilla types JEI already shows; this adds
 * info pages on how to wear, dye and combine our items. Loaded by JEI only (NeoForge: {@link JeiPlugin},
 * Fabric: the {@code jei_mod_plugin} entrypoint).
 */
@JeiPlugin
public final class FemboyJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "jei_plugin");
    private static final TagKey<Item> DYEABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "dyeable_cosmetics"));

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, stack -> stack.has(FemboyComponents.COSMETIC.get()) && !stack.has(FemboyComponents.BACKPACK.get()),
                "cosmetic");
        info(registration, stack -> stack.is(DYEABLE), "dyeable");
        info(registration, stack -> stack.has(FemboyComponents.BACKPACK.get()), "backpack");
        info(registration, stack -> stack.is(FemboyTags.CHARMS), "charm");
    }

    private static void info(IRecipeRegistration registration, Predicate<ItemStack> filter, String key) {
        List<ItemStack> stacks = BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FemboyMod.MOD_ID))
                .map(ItemStack::new)
                .filter(filter)
                .toList();
        if (!stacks.isEmpty()) {
            registration.addItemStackInfo(stacks, Component.translatable("jei." + FemboyMod.MOD_ID + ".info." + key));
        }
    }
}
