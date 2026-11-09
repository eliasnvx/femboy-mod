package dev.eliasnvx.femboymod.example;

import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticReactionSource;
import net.minecraft.world.entity.player.Player;

/**
 * A cosmetic reaction source: while the friendship pin is worn, the wearer's reactive ears perk up and their tail
 * wags when another player is close by. Uses only what the client knows (worn cosmetics are synced, positions too).
 */
public final class ExampleReactions {

    private static final double FRIEND_RANGE = 4.0;
    private static final float FRIEND_WAG = 0.7F;
    private static final float FRIEND_PERK = 0.5F;

    private ExampleReactions() {
    }

    static void register(FemboyClientApi api) {
        CosmeticReactionSource friends = (wearer, reactions) -> {
            if (api.common().getCosmetics(wearer).get(ExampleAddon.PIN_SLOT).isEmpty()) {
                return;
            }
            for (Player other : wearer.level().players()) {
                if (other != wearer && !other.isSpectator() && other.distanceToSqr(wearer) <= FRIEND_RANGE * FRIEND_RANGE) {
                    reactions.wag(FRIEND_WAG);
                    reactions.perk(FRIEND_PERK);
                    return;
                }
            }
        };
        api.reactionSources().register(ExampleAddon.PIN_RENDERER, friends);
    }
}
