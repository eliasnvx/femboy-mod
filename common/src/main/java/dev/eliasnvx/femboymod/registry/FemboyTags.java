package dev.eliasnvx.femboymod.registry;

import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class FemboyTags {

    /** Items that fit into backpack charm slots. */
    public static final TagKey<Item> CHARMS = TagKey.create(Registries.ITEM, id("charms"));
    /** Hoodies: first-person sleeves, the Full Femboy Mode set. */
    public static final TagKey<Item> HOODIES = TagKey.create(Registries.ITEM, id("hoodies"));
    /** Wearing any of these, the Fashion Critic is impressed no matter the Drip (dark shades: no arguing with them). */
    public static final TagKey<Item> CRITIC_APPROVED = TagKey.create(Registries.ITEM, id("critic_approved"));
    /** Small animals the Full Femboy Mode "followers" effect may attract (no horses or cows crowding you). */
    public static final TagKey<EntityType<?>> CUTE_FOLLOWERS = TagKey.create(Registries.ENTITY_TYPE, id("cute_followers"));
    /** Damage the netherite backpack (as an item entity) ignores: fire, lava, cactus, explosions. */
    public static final TagKey<DamageType> BACKPACK_IMMUNE_TO = TagKey.create(Registries.DAMAGE_TYPE, id("backpack_immune_to"));

    private FemboyTags() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }
}
