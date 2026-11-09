package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.entity.CatKnockGoal;
import dev.eliasnvx.femboymod.entity.CatnipGoal;
import dev.eliasnvx.femboymod.entity.PetBedGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tamed cats and dogs rest on Pet Beds. No loader event adds goals to vanilla mobs, and vanilla's cat lie-on-block
 * goal can't be reused: its search skips the cat's own level (it starts two blocks down), so it misses beds on the floor.
 * Cats (Stray Cats included, they inherit the goals) also knock things off tables ({@link CatKnockGoal}) and go
 * wild for catnip ({@link CatnipGoal}).
 */
@Mixin({Cat.class, Wolf.class})
public abstract class PetBedGoalsMixin extends TamableAnimal {

    /** Cats: with their own lie-on-block goal. Dogs: with following the owner, below attacking (5). */
    private static final int CAT_PRIORITY = 5;
    private static final int DOG_PRIORITY = 6;
    /** With sitting on chests and furnaces (7): after following the owner, before strolling around. */
    private static final int MISCHIEF_PRIORITY = 7;
    /** Catnip: with relaxing on the owner (3), above lying on blocks and following; sitting when ordered (2) still wins. */
    private static final int CATNIP_PRIORITY = 3;

    protected PetBedGoalsMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void femboymod$petBed(CallbackInfo ci) {
        goalSelector.addGoal((Object) this instanceof Cat ? CAT_PRIORITY : DOG_PRIORITY, new PetBedGoal(this));
        if ((Object) this instanceof Cat cat) {
            goalSelector.addGoal(MISCHIEF_PRIORITY, new CatKnockGoal(cat));
            goalSelector.addGoal(CATNIP_PRIORITY, new CatnipGoal(cat));
        }
    }
}
