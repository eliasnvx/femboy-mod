package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
            ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "gameplay/stray_cat_gift"));
    /** Morning = the first in-game hour of a day. */
    private static final long MORNING_END = 1000;
    private static final String COAT_TAG = "femboymod_coat";
    private static final String LAST_GIFT_DAY_TAG = "femboymod_last_gift_day";
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
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        setCoat(level.getRandom().nextInt(COATS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(COAT_TAG, coat());
        tag.putLong(LAST_GIFT_DAY_TAG, lastGiftDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCoat(tag.getInt(COAT_TAG)); // 0 when missing
        lastGiftDay = tag.contains(LAST_GIFT_DAY_TAG) ? tag.getLong(LAST_GIFT_DAY_TAG) : -1;
    }

    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        StrayCat kitten = FemboyEntities.STRAY_CAT.get().create(level);
        if (kitten != null) {
            kitten.setCoat(partner instanceof StrayCat other && random.nextBoolean() ? other.coat() : coat());
            if (isTame() && getOwner() != null) {
                kitten.setOwnerUUID(getOwnerUUID());
                kitten.setTame(true, true);
            }
        }
        return kitten;
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getDayTime();
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
            owner.spawnAtLocation(gift);
        }
        owner.displayClientMessage(Component.translatable("message.femboymod.stray_cat.gift", getDisplayName()), true);
    }
}
