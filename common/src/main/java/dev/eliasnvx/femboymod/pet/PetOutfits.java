package dev.eliasnvx.femboymod.pet;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Pet outfits (0.2.0): bows, hats, collars and bandanas for tamed cats, wolves and parrots
 * ({@code #femboymod:pet_outfit_wearers}). A piece sits in one of the pet's own equipment slots that these pets
 * don't use ({@link PetOutfitItem.Slot}): vanilla saves it, shows it to players around and drops it when the pet
 * dies. The owner puts a piece on with a right-click (before the pet's own click, so a cat doesn't sit down instead)
 * and takes the pieces off with shears, head first. Server-authoritative; the client only predicts the swing.
 */
public final class PetOutfits {

    private PetOutfits() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(PetOutfits::interact);
    }

    /** A tamed pet of a type that wears outfits. */
    public static boolean canWear(Entity entity) {
        return entity instanceof TamableAnimal pet && pet.isTame() && entity.getType().is(FemboyTags.PET_OUTFIT_WEARERS);
    }

    /** The outfit piece a pet wears in a slot, or empty (anything else another mod put in that slot is ignored). */
    public static ItemStack worn(LivingEntity pet, PetOutfitItem.Slot slot) {
        ItemStack stack = pet.getItemBySlot(slot.equipment());
        return stack.getItem() instanceof PetOutfitItem ? stack : ItemStack.EMPTY;
    }

    public static EventResult interact(Player player, Entity entity, InteractionHand hand) {
        if (!(entity instanceof TamableAnimal pet) || !canWear(pet) || !pet.isOwnedBy(player)) {
            return EventResult.pass();
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof PetOutfitItem outfit) {
            ItemStack inSlot = pet.getItemBySlot(outfit.slot().equipment());
            if (!inSlot.isEmpty() && !(inSlot.getItem() instanceof PetOutfitItem)) {
                return EventResult.pass(); // another mod's item; leave it alone
            }
            if (!player.level().isClientSide()) {
                putOn(player, pet, outfit.slot(), held.consumeAndReturn(1, player));
            }
            return EventResult.interruptTrue();
        }
        if (held.is(Items.SHEARS)) {
            PetOutfitItem.Slot slot = !worn(pet, PetOutfitItem.Slot.HEAD).isEmpty() ? PetOutfitItem.Slot.HEAD
                    : !worn(pet, PetOutfitItem.Slot.NECK).isEmpty() ? PetOutfitItem.Slot.NECK : null;
            if (slot == null) {
                return EventResult.pass();
            }
            if (!player.level().isClientSide()) {
                takeOff(player, pet, slot);
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return EventResult.interruptTrue();
        }
        return EventResult.pass();
    }

    /** Puts a piece on (server); what the pet wore in that slot goes to the player. */
    public static void putOn(Player player, TamableAnimal pet, PetOutfitItem.Slot slot, ItemStack piece) {
        ItemStack old = worn(pet, slot);
        pet.setItemSlot(slot.equipment(), piece);
        pet.setGuaranteedDrop(slot.equipment());
        if (!old.isEmpty()) {
            player.getInventory().placeItemBackInInventory(old);
        }
        pet.level().playSound(null, pet, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** Takes a piece off (server) and gives it to the player. */
    public static void takeOff(Player player, TamableAnimal pet, PetOutfitItem.Slot slot) {
        ItemStack piece = worn(pet, slot);
        if (piece.isEmpty()) {
            return;
        }
        pet.setItemSlot(slot.equipment(), ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(piece);
        pet.level().playSound(null, pet, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.2F);
    }
}
