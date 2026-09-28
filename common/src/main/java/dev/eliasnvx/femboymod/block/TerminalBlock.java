package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Terminal (SPEC v1.1): a retro computer that works as a crafting table. Now and then it reminds its user,
 * in chat, that they use Terminal btw (cooldown {@code furniture.terminal_btw_cooldown}). Counts as a screen for
 * the setup rating.
 */
public class TerminalBlock extends HorizontalDirectionalBlock {


    /** Number of {@code message.femboymod.terminal.<n>} lines. */
    public static final int LINES = 6;
    private static final Component TITLE = Component.translatable("container.femboymod.terminal");
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 3.0, 14.0, 11.0, 13.0),
            Block.box(1.0, 0.0, 0.5, 15.0, 1.5, 3.0));
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public TerminalBlock(Properties properties) {
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new Menu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE));
            btw(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sends a meme line to the user's chat unless one was sent recently; returns whether it did. */
    public static boolean btw(ServerPlayer player) {
        long now = player.level().getGameTime();
        var profile = FemboyMod.api().getProfile(player);
        long last = profile.get(ProfileHooks.TERMINAL_LAST_BTW);
        if (last >= 0 && now - last < FemboyConfig.common().furniture().terminalBtwCooldown()) {
            return false;
        }
        profile.set(ProfileHooks.TERMINAL_LAST_BTW, now);
        player.sendSystemMessage(Component.translatable("message.femboymod.terminal." + player.getRandom().nextInt(LINES))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Vanilla crafting that stays open while the player is near a Terminal (not a crafting table). */
    public static final class Menu extends CraftingMenu {
        private final ContainerLevelAccess access;

        public Menu(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
            this.access = access;
        }

        @Override
        public boolean stillValid(Player player) {
            return stillValid(access, player, FemboyBlocks.TERMINAL.get());
        }
    }
}
