package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class Colorways {

    private Colorways() {
    }

    /**
     * The colors an item is shown in: its {@code femboymod:colorway}, or else a solid colorway from
     * vanilla dyeing (1.20.1: {@code display.color} of a {@link DyeableLeatherItem}, set by the armor dye recipe).
     */
    public static Optional<Colorway> effective(ItemStack stack) {
        Colorway colorway = FemboyComponents.COLORWAY.get(stack);
        if (colorway != null) {
            return Optional.of(colorway);
        }
        return stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)
                ? Optional.of(Colorway.solid(dyeable.getColor(stack)))
                : Optional.empty();
    }
}
