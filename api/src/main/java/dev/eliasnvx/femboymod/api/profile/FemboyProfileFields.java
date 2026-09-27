package dev.eliasnvx.femboymod.api.profile;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/** Profile fields of Femboy Mod itself. Read them freely; write Style Points only through the API methods. */
@ApiStatus.AvailableSince("0.1.0")
public final class FemboyProfileFields {

    /** Codec for a set of ids, saved as a sorted list. */
    public static final Codec<Set<ResourceLocation>> ID_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(list -> Set.copyOf(new HashSet<>(list)), set -> set.stream().sorted().toList());

    /** Style Points the player can spend (synced). */
    public static final ProfileField<Integer> STYLE_POINTS = field("style_points", Codec.INT, 0, true);
    /** Style Points earned in total, never decreases: a base for seasonal rankings (synced). */
    public static final ProfileField<Integer> STYLE_POINTS_EARNED = field("style_points_earned", Codec.INT, 0, true);
    /** Highest Drip Level ever reached (synced). */
    public static final ProfileField<Integer> BEST_DRIP_LEVEL = field("best_drip_level", Codec.INT, 0, true);
    /** Cosmetics put on, in total. */
    public static final ProfileField<Integer> EQUIPS = field("equips", Codec.INT, 0, false);
    /** Ids of every cosmetic item the player has worn at least once (synced). */
    public static final ProfileField<Set<ResourceLocation>> COLLECTION = field("collection", ID_SET_CODEC, Set.of(), true);
    /** Fashion Critic reviews received. */
    public static final ProfileField<Integer> CRITIC_REVIEWS = field("critic_reviews", Codec.INT, 0, false);
    /** Fashion Critic reviews passed (the critic was impressed). */
    public static final ProfileField<Integer> CRITIC_PASSED = field("critic_passed", Codec.INT, 0, false);
    /** Byte Energy cans drunk, in total. */
    public static final ProfileField<Integer> ENERGY_DRINKS = field("energy_drinks", Codec.INT, 0, false);
    /** Debugging sessions with the rubber duck that granted Insight. */
    public static final ProfileField<Integer> DUCK_SESSIONS = field("duck_sessions", Codec.INT, 0, false);
    /** Best Gamer Chair setup rating (stars) ever reached. */
    public static final ProfileField<Integer> BEST_SETUP_RATING = field("best_setup_rating", Codec.INT, 0, false);

    /** All fields above, in registration order. */
    public static final List<ProfileField<?>> ALL = List.of(STYLE_POINTS, STYLE_POINTS_EARNED, BEST_DRIP_LEVEL, EQUIPS,
            COLLECTION, CRITIC_REVIEWS, CRITIC_PASSED, ENERGY_DRINKS, DUCK_SESSIONS, BEST_SETUP_RATING);

    private FemboyProfileFields() {
    }

    private static <T> ProfileField<T> field(String name, Codec<T> codec, T defaultValue, boolean synced) {
        return ProfileField.of(ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, name), codec, defaultValue, synced);
    }
}
