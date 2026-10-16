package dev.eliasnvx.femboymod.pet;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/** A pet outfit piece (bow, hat, collar, bandana): the owner puts it on a tamed pet, see {@link PetOutfits}. */
public class PetOutfitItem extends Item {

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
