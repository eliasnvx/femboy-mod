package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Caffeinated Zombie: a zombie that had one Byte Energy too many. Faster, jittery (the renderer shakes it)
 * and always holding a can; never turns into a drowned. Drops cans ({@code loot_table/entities/caffeinated_zombie}).
 */
public class CaffeinatedZombie extends Zombie {

    private static final List<Supplier<Item>> CANS = List.of(FemboyItems.BYTE_ENERGY_PINK::get, FemboyItems.BYTE_ENERGY_BLUE::get,
            FemboyItems.BYTE_ENERGY_PURPLE::get);
    /** The can in hand is decoration; the loot table decides what drops. */
    private static final float HELD_CAN_DROP_CHANCE = 0.0F;
    /** One spark every ~6 ticks on average. */
    private static final int SPARK_CHANCE = 6;
    private static final double SPARK_SPREAD = 0.6;

    public CaffeinatedZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(CommonConfig.Mob config) {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, config.health())
                .add(Attributes.ATTACK_DAMAGE, config.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, config.speed());
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CANS.get(random.nextInt(CANS.size())).get()));
        setDropChance(EquipmentSlot.MAINHAND, HELD_CAN_DROP_CHANCE);
    }

    /** Client only: a few electric sparks crackle around it. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && random.nextInt(SPARK_CHANCE) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(SPARK_SPREAD), getRandomY(), getRandomZ(SPARK_SPREAD),
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
