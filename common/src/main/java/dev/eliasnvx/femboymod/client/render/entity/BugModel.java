package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.entity.Bug;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Bug: beetle shell, small head with antennae, six legs that scuttle while walking (texture 32x32). */
public final class BugModel extends HierarchicalModel<Bug> {

    private static final int TEXTURE_SIZE = 32;
    private static final int LEGS_PER_SIDE = 3;
    private static final float LEG_DOWN_TILT = 0.55F;
    private static final float LEG_SWING = 0.6F;
    private static final float LEG_SPEED = 1.8F;
    private static final float ANTENNA_WIGGLE = 0.12F;
    private static final float ANTENNA_SPEED = 0.25F;

    private final ModelPart root;
    private final ModelPart[] rightLegs = new ModelPart[LEGS_PER_SIDE];
    private final ModelPart[] leftLegs = new ModelPart[LEGS_PER_SIDE];
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;

    public BugModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            rightLegs[i] = root.getChild("right_leg" + i);
            leftLegs[i] = root.getChild("left_leg" + i);
        }
        ModelPart head = root.getChild("head");
        leftAntenna = head.getChild("left_antenna");
        rightAntenna = head.getChild("right_antenna");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -4.0F, 6, 4, 8),
                PartPose.offset(0.0F, 21.0F, 0.5F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -1.5F, -3.0F, 4, 3, 3),
                PartPose.offset(0.0F, 21.5F, -3.5F));
        head.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(14, 12).addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1),
                PartPose.offsetAndRotation(1.2F, -1.2F, -2.0F, -0.5F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(14, 12).mirror().addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1),
                PartPose.offsetAndRotation(-1.2F, -1.2F, -2.0F, -0.5F, 0.0F, -0.35F));
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            float z = -2.5F + i * 2.5F;
            root.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(20, 12).addBox(-3.0F, -0.5F, -0.5F, 3, 1, 1),
                    PartPose.offsetAndRotation(-2.5F, 22.0F, z, 0.0F, 0.0F, -LEG_DOWN_TILT));
            root.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(20, 12).mirror().addBox(0.0F, -0.5F, -0.5F, 3, 1, 1),
                    PartPose.offsetAndRotation(2.5F, 22.0F, z, 0.0F, 0.0F, LEG_DOWN_TILT));
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Bug bug, float walkPos, float walkSpeed, float ageInTicks, float headYaw, float headPitch) {
        float walk = walkPos * LEG_SPEED;
        float amount = Math.min(1.0F, walkSpeed) * LEG_SWING;
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            // alternating tripod gait: legs 0 and 2 of one side move with leg 1 of the other
            float phase = (i % 2 == 0 ? 0.0F : Mth.PI);
            rightLegs[i].yRot = Mth.sin(walk + phase) * amount;
            leftLegs[i].yRot = Mth.sin(walk + phase + Mth.PI) * amount;
        }
        float wiggle = Mth.sin(ageInTicks * ANTENNA_SPEED) * ANTENNA_WIGGLE;
        leftAntenna.zRot = 0.35F + wiggle;
        rightAntenna.zRot = -0.35F - wiggle;
    }
}
