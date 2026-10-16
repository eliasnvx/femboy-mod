package dev.eliasnvx.femboymod.client.render.pet;

import dev.eliasnvx.femboymod.entity.FemboyEntities;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Where outfit pieces sit on each pet model, in the model's pixels relative to its head part (vanilla models: cat
 * {@code head}, wolf {@code head/real_head}, parrot {@code head}). Kittens and puppies use the same anchors (except
 * {@code babyRingZ}): on 1.21.1 they are drawn with the adult model, the head scaled and moved by the model, and
 * {@link PetOutfitLayer} repeats that transform (26.x has separate baby models with their own anchors instead).
 *
 * @param headPath  child names from the model root to the head part
 * @param hatX      hat brim center on top of the head
 * @param hatScale  hats and bows are modeled for a 8 px head and scaled down
 * @param bowX      bow knot, next to the right ear
 * @param bowTilt   bow roll (radians)
 * @param ringX     middle of the head where the neck piece hangs (cat, wolf: the strap goes under the chin, in front of
 *                  the body or the wolf's mane); for the parrot, the center of its ring
 * @param ringWidth width across the neck (the strap and bandana are made from it; the parrot's ring's inner width)
 * @param ringDepth head height (cat, wolf: the chin is half of it below the anchor) or the inner depth of the
 *                  parrot's ring ({@code flat})
 * @param flat      the ring lies flat (x-z plane) instead of standing (x-y plane)
 * @param babyRingZ {@code ringZ} on a young model: a kitten's head overlaps the front of its half-size body, so the
 *                  strap hangs further forward, or the bell and bandana would sit inside the body (26.3 moved its
 *                  kitten's strap forward for the same reason)
 */
public record PetSpecies(String name, String[] headPath, float hatX, float hatY, float hatZ, float hatScale,
                         float bowX, float bowY, float bowZ, float bowTilt,
                         float ringX, float ringY, float ringZ, float ringWidth, float ringDepth, boolean flat,
                         float babyRingZ) {

    public static final PetSpecies CAT = new PetSpecies("cat", new String[]{"head"},
            0.0F, -2.0F, -1.2F, 0.7F,
            1.9F, -2.5F, 0.4F, -0.35F,
            0.0F, 0.0F, 0.9F, 5.0F, 4.0F, false,
            -0.5F);
    /** A puppy's head keeps the adult size and sits in front of its half-size body: the adult anchors fit. */
    public static final PetSpecies WOLF = new PetSpecies("wolf", new String[]{"head", "real_head"},
            1.0F, -3.0F, -0.8F, 0.85F,
            3.3F, -3.6F, 0.5F, -0.35F,
            1.0F, 0.0F, 0.2F, 6.0F, 6.0F, false,
            0.2F);
    public static final PetSpecies PARROT = new PetSpecies("parrot", new String[]{"head"},
            0.0F, -2.5F, -0.4F, 0.35F,
            1.0F, -1.7F, 0.3F, -0.4F,
            0.0F, 1.1F, 0.0F, 2.4F, 2.4F, true,
            0.0F);

    /** The anchors for a pet type's model, or null if the type has no outfit layer. */
    public static @Nullable PetSpecies forType(EntityType<?> type) {
        if (type == EntityType.CAT || type == FemboyEntities.STRAY_CAT.get()) {
            return CAT;
        }
        if (type == EntityType.WOLF) {
            return WOLF;
        }
        return type == EntityType.PARROT ? PARROT : null;
    }

    /** Every set of anchors (each gets its own neck piece models). */
    public static List<PetSpecies> all() {
        return List.of(CAT, WOLF, PARROT);
    }

    /** Pet types whose renderers get the outfit layer (#femboymod:pet_outfit_wearers says which wear outfits). */
    public static List<EntityType<?>> types() {
        return List.of(EntityType.CAT, FemboyEntities.STRAY_CAT.get(), EntityType.WOLF, EntityType.PARROT);
    }

    /** Ring band width and thickness, and the bow's scale relative to the hat's. */
    static final float BAND = 0.55F;
    static final float THICKNESS = 0.9F;
    static final float BOW_SCALE = 0.85F;
}
