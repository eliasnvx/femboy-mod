package dev.eliasnvx.femboymod.pet;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

/**
 * A pet outfit piece (bow, hat, collar, bandana): the owner puts it on a tamed pet, see {@link PetOutfits}.
 * Dyeable like the cosmetics: 1.21.1 lists the pieces in {@code #femboymod:dyeable_cosmetics} (so
 * {@code #minecraft:dyeable}); on 1.20.1 the armor dye recipe and the cauldron wash only accept
 * {@link DyeableLeatherItem}s (color in {@code display.color}).
 */
public class PetOutfitItem extends Item implements DyeableLeatherItem {

    /** Where a piece is worn, and the pet's equipment slot that holds it (one these pets don't use themselves). */
    public enum Slot {
        HEAD(EquipmentSlot.HEAD),
        NECK(EquipmentSlot.CHEST);

        private final EquipmentSlot equipment;

        Slot(EquipmentSlot equipment) {
            this.equipment = equipment;
        }

        public EquipmentSlot equipment() {
            return equipment;
        }
    }

    private final Slot slot;

    public PetOutfitItem(Properties properties, Slot slot) {
        super(properties.stacksTo(1));
        this.slot = slot;
    }

    public Slot slot() {
        return slot;
    }
}
