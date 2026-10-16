package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Pet Bed: a round cushion in any dye color. Tamed cats come and lie on it, tamed dogs sit on it
 * ({@link dev.eliasnvx.femboymod.entity.PetBedGoal}). A tamed pet resting on it slowly heals (config
 * {@code furniture.pet_bed_heal_*}).
 */
public class PetBedBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<PetBedBlock> CODEC = simpleCodec(PetBedBlock::new);

    /** Pets stand on the cushion at 9 px, like on a bed (the goal's reach check needs it); the rim is only drawn. */
    private static final VoxelShape COLLISION = Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0);
    private static final VoxelShape OUTLINE = Shapes.or(COLLISION, Block.box(1.0, 9.0, 1.0, 15.0, 12.0, 15.0));

    public PetBedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(DyedFurniture.COLOR, DyedFurniture.DEFAULT_COLOR));
    }

    @Override
    protected MapCodec<PetBedBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DyedFurniture.COLOR);
    }

    /** The low side of the rim faces the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return DyedFurniture.dye(stack, state, level, pos, player);
    }

    /** Whether this pet counts as resting on a bed: tamed, and lying (cats) or sitting. */
    public static boolean isResting(TamableAnimal pet) {
        return pet.isTame() && (pet.isInSittingPose() || pet instanceof Cat cat && cat.isLying());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // once per pet: only for the bed it stands on
        if (level instanceof ServerLevel && entity instanceof TamableAnimal pet && pet.blockPosition().equals(pos) && isResting(pet)) {
            CommonConfig.Furniture config = FemboyConfig.common().furniture();
            if (config.petBedHealInterval() > 0 && pet.tickCount % config.petBedHealInterval() == 0
                    && pet.getHealth() < pet.getMaxHealth()) {
                pet.heal(config.petBedHealAmount());
            }
        }
    }
}
