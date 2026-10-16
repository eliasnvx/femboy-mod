package dev.eliasnvx.femboymod.registry;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class FemboyTags {

    /** Items that fit into backpack charm slots. */
    public static final TagKey<Item> CHARMS = TagKey.create(Registries.ITEM, id("charms"));
    /** Hoodies: first-person sleeves, the Full Femboy Mode set. */
    public static final TagKey<Item> HOODIES = TagKey.create(Registries.ITEM, id("hoodies"));
    /** Tops that get sleeves in first person (hoodies and the ghost sheet). */
    public static final TagKey<Item> SLEEVED_TOPS = TagKey.create(Registries.ITEM, id("sleeved_tops"));
    /** Wearing any of these counts as a costume for trick-or-treat. */
    public static final TagKey<Item> HALLOWEEN_COSTUME = TagKey.create(Registries.ITEM, id("halloween_costume"));
    /** What a Sock Goblin steals (carried, not worn). */
    public static final TagKey<Item> SOCK_GOBLIN_LOOT = TagKey.create(Registries.ITEM, id("sock_goblin_loot"));
    /** What goes into a Laundry Basket (anything worn in a cosmetic slot, lost socks). */
    public static final TagKey<Item> LAUNDRY = TagKey.create(Registries.ITEM, id("laundry"));
    /** What goes into a Mini Fridge (drinks and snacks). */
    public static final TagKey<Item> FRIDGE_FOOD = TagKey.create(Registries.ITEM, id("fridge_food"));
    /** Fridge items that get chilled after a while (Byte Energy: chilled buffs last longer). */
    public static final TagKey<Item> CHILLABLE = TagKey.create(Registries.ITEM, id("chillable"));
    /** Tames, breeds and tempts rubber ducks. */
    public static final TagKey<Item> RUBBER_DUCK_FOOD = TagKey.create(Registries.ITEM, id("rubber_duck_food"));
    public static final TagKey<net.minecraft.world.level.block.Block> RUBBER_DUCK_SPAWNABLE_ON =
            TagKey.create(Registries.BLOCK, id("rubber_duck_spawnable_on"));
    /** Wearing any of these, the Fashion Critic is impressed no matter the Drip (dark shades: no arguing with them). */
    public static final TagKey<Item> CRITIC_APPROVED = TagKey.create(Registries.ITEM, id("critic_approved"));
    /** Small animals the Full Femboy Mode "followers" effect may attract (no horses or cows crowding you). */
    public static final TagKey<EntityType<?>> CUTE_FOLLOWERS = TagKey.create(Registries.ENTITY_TYPE, id("cute_followers"));
    /** Reflected in mirrors besides players (tameable ones only once tamed). */
    public static final TagKey<EntityType<?>> MIRROR_REFLECTED = TagKey.create(Registries.ENTITY_TYPE, id("mirror_reflected"));
    /** Tamed pets of these types wear pet outfits (their renderers get the outfit layer on the client too). */
    public static final TagKey<EntityType<?>> PET_OUTFIT_WEARERS = TagKey.create(Registries.ENTITY_TYPE, id("pet_outfit_wearers"));
    /** Damage the netherite backpack (as an item entity) ignores: fire, lava, cactus, explosions. */
    public static final TagKey<DamageType> BACKPACK_IMMUNE_TO = TagKey.create(Registries.DAMAGE_TYPE, id("backpack_immune_to"));

    private FemboyTags() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }
}
