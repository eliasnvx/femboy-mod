package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.eliasnvx.femboymod.profile.StylePoints;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rubber duck for rubber duck debugging: right-click to explain your code to it. It answers with a random
 * meme line; once per cooldown it also grants Insight (faster digging) and a little experience
 * (config {@code furniture.duck_*}, {@code furniture.insight_duration}). Counts for the setup rating.
 */
public class RubberDuckBlock extends PlushBlock {

    /** Number of {@code message.femboymod.duck.<n>} lines. */
    public static final int LINES = 10;
    private static final String LINE_KEY = "message.femboymod.duck.";

    private final MapCodec<RubberDuckBlock> codec;

    public RubberDuckBlock(Properties properties, VoxelShape northShape) {
        super(properties, northShape);
        this.codec = simpleCodec(props -> new RubberDuckBlock(props, northShape));
    }

    @Override
    protected MapCodec<RubberDuckBlock> codec() {
        return codec;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult squeak = super.useWithoutItem(state, level, pos, player, hit);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(LINE_KEY + level.getRandom().nextInt(LINES)), true);
            debug(serverPlayer);
        }
        return squeak;
    }

    /** Grants Insight and experience unless the cooldown is running; returns whether it did. */
    public static boolean debug(ServerPlayer player) {
        CommonConfig.Furniture config = FemboyConfig.common().furniture();
        long now = player.level().getGameTime();
        PlayerProfile profile = FemboyMod.api().getProfile(player);
        long last = profile.get(ProfileHooks.DUCK_LAST_INSIGHT);
        if (last >= 0 && now - last < config.duckCooldown()) {
            return false;
        }
        profile.set(ProfileHooks.DUCK_LAST_INSIGHT, now);
        profile.update(FemboyProfileFields.DUCK_SESSIONS, sessions -> sessions + 1);
        StylePoints.earn(player, FemboyConfig.common().stylePoints().rubberDuck(), StylePoints.RUBBER_DUCK);
        player.addEffect(new MobEffectInstance(FemboyEffects.holder(FemboyEffects.INSIGHT), config.insightDuration()));
        player.giveExperiencePoints(config.duckExperience());
        FemboyTriggers.fire(player, FemboyTriggers.DUCK_DEBUGGING);
        return true;
    }
}
