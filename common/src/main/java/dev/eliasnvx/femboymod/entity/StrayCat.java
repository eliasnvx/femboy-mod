package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * Stray Cat (SPEC v1.2): a vanilla cat at heart (tame it with fish, it sits, sleeps on your bed and brings the usual
 * morning gifts) with a pastel coat, that also brings a little mod gift each morning when its owner is near
 * (loot table {@code femboymod:gameplay/stray_cat_gift}, config {@code friends.stray_cat_*}).
 */
public class StrayCat extends Cat {

    public static final int COATS = 5;
    public static final ResourceKey<LootTable> GIFT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "gameplay/stray_cat_gift"));
    /** Morning = the first in-game hour of a day. */
    private static final long MORNING_END = 1000;
    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(StrayCat.class, EntityDataSerializers.INT);

    private long lastGiftDay = -1;

    public StrayCat(EntityType<? extends Cat> type, Level level) {
        super(type, level);
    }

    public int coat() {
        return entityData.get(COAT);
    }

    public void setCoat(int coat) {
        entityData.set(COAT, Math.floorMod(coat, COATS));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COAT, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData data) {
        setCoat(level.getRandom().nextInt(COATS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("femboymod_coat", coat());
        output.putLong("femboymod_last_gift_day", lastGiftDay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCoat(input.getIntOr("femboymod_coat", 0));
        lastGiftDay = input.getLongOr("femboymod_last_gift_day", -1);
    }

    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        StrayCat kitten = FemboyEntities.STRAY_CAT.get().create(level, EntitySpawnReason.BREEDING);
        if (kitten != null) {
            kitten.setCoat(partner instanceof StrayCat other && random.nextBoolean() ? other.coat() : coat());
            if (isTame() && getOwner() != null) {
                kitten.setOwnerReference(getOwnerReference());
                kitten.setTame(true, true);
            }
        }
        return kitten;
    }

    @Override
    public void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        long time = level.getOverworldClockTime();
        long day = time / SharedConstants.TICKS_PER_GAME_DAY;
        if (!isTame() || day == lastGiftDay || time % SharedConstants.TICKS_PER_GAME_DAY >= MORNING_END) {
            return;
        }
        lastGiftDay = day;
        CommonConfig.Friends config = FemboyConfig.common().friends();
        if (getOwner() instanceof ServerPlayer owner && distanceTo(owner) <= config.strayCatGiftRange()
                && random.nextFloat() < config.strayCatGiftChance()) {
            giveGift(level, owner);
        }
    }

    /** Drops a gift from the loot table at the owner's feet (also used by GameTests). */
    public void giveGift(ServerLevel level, ServerPlayer owner) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT);
        for (ItemStack gift : level.getServer().reloadableRegistries().getLootTable(GIFT).getRandomItems(params)) {
            owner.spawnAtLocation(level, gift);
        }
        owner.sendOverlayMessage(Component.translatable("message.femboymod.stray_cat.gift", getDisplayName()));
    }
}
