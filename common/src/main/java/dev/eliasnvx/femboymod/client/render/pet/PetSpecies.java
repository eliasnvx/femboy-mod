package dev.eliasnvx.femboymod.client.render.pet;

/**
 * Where outfit pieces sit on each pet model, in the model's pixels relative to its head part (vanilla adult models:
 * cat {@code head}, wolf {@code head/real_head}, parrot {@code head}; the kitten and puppy models of 26.x have their own
 * layout, see {@link #baby()}).
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
 */
public record PetSpecies(String name, String[] headPath, float hatX, float hatY, float hatZ, float hatScale,
                         float bowX, float bowY, float bowZ, float bowTilt,
                         float ringX, float ringY, float ringZ, float ringWidth, float ringDepth, boolean flat) {

    public static final PetSpecies CAT = new PetSpecies("cat", new String[]{"head"},
            0.0F, -2.0F, -1.2F, 0.7F,
            1.9F, -2.5F, 0.4F, -0.35F,
            0.0F, 0.0F, 0.9F, 5.0F, 4.0F, false);
    public static final PetSpecies WOLF = new PetSpecies("wolf", new String[]{"head", "real_head"},
            1.0F, -3.0F, -0.8F, 0.85F,
            3.3F, -3.6F, 0.5F, -0.35F,
            1.0F, 0.0F, 0.2F, 6.0F, 6.0F, false);
    /**
     * Kitten head: the same 5 px width as the adult's, one pixel higher, ears further forward. The body starts right
     * behind the chin, so the strap hangs a little further forward than on the adult.
     */
    public static final PetSpecies CAT_BABY = new PetSpecies("cat_baby", new String[]{"head"},
            0.0F, -3.0F, -1.7F, 0.7F,
            1.9F, -3.5F, -0.5F, -0.35F,
            0.0F, -1.0F, -0.5F, 5.0F, 4.0F, false);
    /** Puppy head: no {@code real_head} part, a 6x5x5 head centered on the part, ears and strap further forward. */
    public static final PetSpecies WOLF_BABY = new PetSpecies("wolf_baby", new String[]{"head"},
            0.0F, -3.25F, -1.5F, 0.85F,
            2.3F, -3.85F, -0.5F, -0.35F,
            0.0F, -0.75F, -0.8F, 6.0F, 5.0F, false);
    public static final PetSpecies PARROT = new PetSpecies("parrot", new String[]{"head"},
            0.0F, -2.5F, -0.4F, 0.35F,
            1.0F, -1.7F, 0.3F, -0.4F,
            0.0F, 1.1F, 0.0F, 2.4F, 2.4F, true);

    /** The anchors for a pet type's (adult) model, or null if the type has no outfit layer. */
    public static @org.jetbrains.annotations.Nullable PetSpecies forType(net.minecraft.world.entity.EntityType<?> type) {
        if (type == net.minecraft.world.entity.EntityTypes.CAT || type == dev.eliasnvx.femboymod.entity.FemboyEntities.STRAY_CAT.get()) {
            return CAT;
        }
        if (type == net.minecraft.world.entity.EntityTypes.WOLF) {
            return WOLF;
        }
        return type == net.minecraft.world.entity.EntityTypes.PARROT ? PARROT : null;
    }

    /** The anchors for this pet's baby model, or null if it has none (parrots don't breed). */
    public @org.jetbrains.annotations.Nullable PetSpecies baby() {
        return this == CAT ? CAT_BABY : this == WOLF ? WOLF_BABY : null;
    }

    /** Every set of anchors, adult and baby (each gets its own neck piece models). */
    public static java.util.List<PetSpecies> all() {
        return java.util.List.of(CAT, CAT_BABY, WOLF, WOLF_BABY, PARROT);
    }

    /** Pet types whose renderers get the outfit layer (#femboymod:pet_outfit_wearers says which wear outfits). */
    public static java.util.List<net.minecraft.world.entity.EntityType<?>> types() {
        return java.util.List.of(net.minecraft.world.entity.EntityTypes.CAT, dev.eliasnvx.femboymod.entity.FemboyEntities.STRAY_CAT.get(),
                net.minecraft.world.entity.EntityTypes.WOLF, net.minecraft.world.entity.EntityTypes.PARROT);
    }

    /** Ring band width and thickness, and the bow's scale relative to the hat's. */
    static final float BAND = 0.55F;
    static final float THICKNESS = 0.9F;
    static final float BOW_SCALE = 0.85F;
}
