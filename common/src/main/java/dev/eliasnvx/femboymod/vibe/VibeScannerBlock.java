package dev.eliasnvx.femboymod.vibe;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.profile.VibeCheckEvent;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsCatalog;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.eliasnvx.femboymod.profile.StylePoints;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import dev.eliasnvx.femboymod.block.BlockShapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Vibe Check Scanner (SPEC v1.2): an arch you stand under. Right-click to get your vibe (0..100, from Drip, sets
 * and the collection; config {@code vibe_check}) onto the server leaderboard; sneak + right-click shows the board.
 */
public class VibeScannerBlock extends HorizontalDirectionalBlock {


    /** Score thresholds of the verdict lines (lang {@code message.femboymod.vibe.<verdict>}). */
    public static final int LIGHT = 6;
    private static final int CUTE = 40;
    private static final int IMMACULATE = 80;
    private static final int SCAN_PARTICLES = 12;
    private static final double SCAN_HEIGHT = 2.0;

    /** Arch facing north: floor pad, two posts, top bar. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 5.0, 16.0, 1.0, 11.0),
            Block.box(0.0, 1.0, 6.0, 2.0, 16.0, 10.0),
            Block.box(14.0, 1.0, 6.0, 16.0, 16.0, 10.0),
            Block.box(0.0, 14.0, 6.0, 16.0, 16.0, 10.0));
    /** You walk through the arch: only the posts collide. */
    private static final VoxelShape NORTH_COLLISION = Shapes.or(
            Block.box(0.0, 0.0, 6.0, 2.0, 16.0, 10.0),
            Block.box(14.0, 0.0, 6.0, 16.0, 16.0, 10.0));
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);
    private final Map<Direction, VoxelShape> collisions = BlockShapes.rotateHorizontal(NORTH_COLLISION);

    public VibeScannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisions.get(state.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (player.isSecondaryUseActive()) {
                showLeaderboard(serverPlayer);
            } else {
                int score = scan(serverPlayer);
                for (int i = 0; i <= SCAN_PARTICLES; i++) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + SCAN_HEIGHT * i / SCAN_PARTICLES,
                            player.getZ(), 1, 0.25, 0.0, 0.25, 0.0);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F + score / 125.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Base score before listeners: Drip, sets and the collection, weighted by the config. */
    public static int baseScore(ServerPlayer player) {
        CommonConfig.VibeCheck config = FemboyConfig.common().vibeCheck();
        int drip = FemboyMod.api().getDripLevel(player).level();
        int sets = Math.min(config.maxSets(), FemboyMod.api().getActiveSetBonuses(player).size());
        int collected = FemboyMod.api().getProfile(player).get(FemboyProfileFields.COLLECTION).size();
        float collection = config.collectionWeight() * Math.min(1.0F, collected / (float) Math.max(1, CosmeticsCatalog.size()));
        return Mth.clamp(Math.round(drip * config.dripWeight() + sets * config.perSet() + collection), 0, 100);
    }

    /** Scans a player: posts the event, updates the leaderboard, shows the verdict, pays the daily points. */
    public static int scan(ServerPlayer player) {
        CommonConfig.VibeCheck config = FemboyConfig.common().vibeCheck();
        VibeCheckEvent event = FemboyMod.api().events().post(new VibeCheckEvent(player, baseScore(player)));
        int score = Mth.clamp(event.score(), 0, 100);
        int rank = VibeLeaderboard.get(player.level().getServer())
                .record(player.getUUID(), player.getGameProfile().getName(), score, config.leaderboardSize());
        String verdict = score >= IMMACULATE ? "immaculate" : score >= CUTE ? "cute" : "needs_work";
        player.displayClientMessage(Component.translatable("message.femboymod.vibe." + verdict, score), true);
        if (rank > 0) {
            player.sendSystemMessage(Component.translatable("message.femboymod.vibe.rank", score, rank).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        FemboyTriggers.fire(player, FemboyTriggers.VIBE_CHECK, score);
        long day = player.level().getGameTime() / SharedConstants.TICKS_PER_GAME_DAY;
        var profile = FemboyMod.api().getProfile(player);
        if (profile.get(ProfileHooks.VIBE_LAST_DAY) != day) {
            profile.set(ProfileHooks.VIBE_LAST_DAY, day);
            StylePoints.earn(player, config.dailyPoints(), StylePoints.VIBE_CHECK);
        }
        return score;
    }

    public static void showLeaderboard(ServerPlayer player) {
        List<VibeLeaderboard.Entry> entries = VibeLeaderboard.get(player.level().getServer()).entries();
        player.sendSystemMessage(Component.translatable("message.femboymod.vibe.leaderboard").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.femboymod.vibe.leaderboard.empty").withStyle(ChatFormatting.GRAY));
        }
        for (int i = 0; i < entries.size(); i++) {
            VibeLeaderboard.Entry entry = entries.get(i);
            player.sendSystemMessage(Component.translatable("message.femboymod.vibe.leaderboard.line", i + 1, entry.name(), entry.score())
                    .withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        }
    }
}
