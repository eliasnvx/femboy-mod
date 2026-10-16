package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Sock Goblin: a small humanoid (14 px) with a big head, long pointy ears and a little nose; texture 64x32. Humanoid
 * so the vanilla held-item layer shows the stolen socks in its hand.
 */
public final class SockGoblinModel extends HumanoidModel<SockGoblin> {

    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 32;
    /**
     * The held-item layer puts items 10 px down a 12 px arm; these arms are 5 px. Shifting up 3 px and scaling to
     * 0.7 puts the item at the hand (-3 + 0.7 * 10 = 4 px).
     */
    private static final float HAND_SHIFT = -3.0F / 16.0F;
    private static final float HELD_SCALE = 0.7F;

    public SockGoblinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -5.0F, -2.5F, 6, 5, 5)
                        .texOffs(22, 0).addBox(-0.5F, -2.5F, -3.5F, 1, 2, 1)
                        .texOffs(0, 10).addBox(-7.0F, -4.5F, -0.5F, 4, 2, 1)
                        .texOffs(0, 13).mirror().addBox(3.0F, -4.5F, -0.5F, 4, 2, 1),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        // 1.21.1 HumanoidModel takes the (empty) hat from the root and copies the head's pose onto it (26.3: head child)
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, 0.0F, -1.5F, 5, 5, 3),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 16).addBox(-1.5F, -0.5F, -1.0F, 2, 5, 2),
                PartPose.offset(-3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).addBox(-0.5F, -0.5F, -1.0F, 2, 5, 2),
                PartPose.offset(3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(-1.2F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 24).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(1.2F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void setupAnim(SockGoblin goblin, float walkPos, float walkSpeed, float ageInTicks, float headYaw, float headPitch) {
        super.setupAnim(goblin, walkPos, walkSpeed, ageInTicks, headYaw, headPitch);
        // 1.21.1 HumanoidModel moves the parts to a player's positions (26.3 starts from the layer's own pose): back to ours
        restorePosition(head);
        restorePosition(body);
        restorePosition(rightArm);
        restorePosition(leftArm);
        restorePosition(rightLeg);
        restorePosition(leftLeg);
        hat.copyFrom(head);
    }

    private static void restorePosition(ModelPart part) {
        PartPose pose = part.getInitialPose();
        part.x = pose.x;
        part.y = pose.y;
        part.z = pose.z;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        super.translateToHand(arm, pose);
        pose.translate(0.0F, HAND_SHIFT, 0.0F);
        pose.scale(HELD_SCALE, HELD_SCALE, HELD_SCALE);
    }
}
