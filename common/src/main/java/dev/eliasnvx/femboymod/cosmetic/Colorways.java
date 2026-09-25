package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Optional;

public final class Colorways {

    private Colorways() {
    }

    /**
     * The colors an item is shown in: its {@code femboymod:colorway}, or else a solid colorway from
     * vanilla dyeing ({@code minecraft:dyed_color}, set by the {@code crafting_dye} recipes).
     */
    public static Optional<Colorway> effective(ItemStack stack) {
        Colorway colorway = stack.get(FemboyComponents.COLORWAY.get());
        if (colorway != null) {
            return Optional.of(colorway);
        }
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        return dyed == null ? Optional.empty() : Optional.of(Colorway.solid(dyed.rgb()));
    }
}
