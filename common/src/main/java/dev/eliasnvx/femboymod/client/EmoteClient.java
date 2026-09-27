package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.emote.Emote;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;

/**
 * Plays emotes on the client: which player does what since when, and the arm poses (applied after vanilla's
 * {@code setupAnim}; sleeves are children of the arms, and cosmetic models run the same setupAnim). Moving cancels an emote.
 */
public final class EmoteClient {

    private record Active(Emote emote, long startMillis, double x, double z) {
    }

    private static final long MILLIS_PER_TICK = 50L;
    /** Moving further than this (blocks) from where the emote started cancels it. */
    private static final double CANCEL_DISTANCE_SQ = 0.25 * 0.25;
    private static final float WAVE_SPEED = 0.012F;
    private static final float WAVE_SWING = 0.35F;
    private static final float BOUNCE_SPEED = 0.008F;
    private static final float BOUNCE = 0.08F;
    /** Arms ease into the pose over this many milliseconds. */
    private static final float BLEND_MILLIS = 200.0F;

    private static final Int2ObjectMap<Active> ACTIVE = new Int2ObjectOpenHashMap<>();

    private EmoteClient() {
    }

    public static void show(int entityId, Emote emote) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (entity != null) {
            ACTIVE.put(entityId, new Active(emote, Util.getMillis(), entity.getX(), entity.getZ()));
        }
    }

    public static void tick(Minecraft minecraft) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = Util.getMillis();
        ACTIVE.int2ObjectEntrySet().removeIf(entry -> {
            Active active = entry.getValue();
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entry.getIntKey());
            return entity == null || now - active.startMillis() > active.emote().durationTicks() * MILLIS_PER_TICK
                    || Mth.lengthSquared(entity.getX() - active.x(), entity.getZ() - active.z()) > CANCEL_DISTANCE_SQ;
        });
    }

    /**
     * Called at the end of {@code PlayerModel#setupAnim} with the id of the entity being posed (1.21.1 has no
     * render states, so the mixin passes the entity's id). Does not allocate.
     */
    public static void pose(HumanoidModel<?> model, int entityId) {
        Active active = ACTIVE.isEmpty() ? null : ACTIVE.get(entityId);
        if (active == null) {
            return;
        }
        float elapsed = Util.getMillis() - active.startMillis();
        float blend = Mth.clamp(elapsed / BLEND_MILLIS, 0.0F, 1.0F);
        switch (active.emote()) {
            case WAVE -> {
                set(model.rightArm, -2.9F, 0.0F, 0.2F + Mth.sin(elapsed * WAVE_SPEED) * WAVE_SWING, blend);
            }
            case PEACE -> {
                float bounce = Mth.sin(elapsed * BOUNCE_SPEED) * BOUNCE;
                set(model.rightArm, -2.2F + bounce, -0.35F, 0.55F, blend);
                set(model.leftArm, -0.3F, 0.0F, -0.1F, blend);
            }
            case HEART_HANDS -> {
                float bounce = Mth.sin(elapsed * BOUNCE_SPEED) * BOUNCE;
                // both arms up, leaning inward so the hands meet above the head (+zRot on the right arm = outward)
                set(model.rightArm, -2.9F + bounce, 0.0F, -0.35F, blend);
                set(model.leftArm, -2.9F + bounce, 0.0F, 0.35F, blend);
            }
        }
    }

    private static void set(ModelPart arm, float xRot, float yRot, float zRot, float blend) {
        arm.xRot = Mth.lerp(blend, arm.xRot, xRot);
        arm.yRot = Mth.lerp(blend, arm.yRot, yRot);
        arm.zRot = Mth.lerp(blend, arm.zRot, zRot);
    }
}
