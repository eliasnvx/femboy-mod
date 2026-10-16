package dev.eliasnvx.femboymod.client.render;

/**
 * Geometry of mirror reflections, in world units. The mirror is a vertical plane through {@code (qx, qz)} with a
 * horizontal unit normal {@code (nx, nz)} pointing out of the glass. "Front" is the signed distance in front of it.
 * Pure math, covered by JUnit.
 */
public final class MirrorMath {

    private MirrorMath() {
    }

    /** Signed horizontal distance of a point in front of the glass (negative behind it). */
    public static double front(double x, double z, double qx, double qz, double nx, double nz) {
        return (x - qx) * nx + (z - qz) * nz;
    }

    /** Yaw (degrees, Minecraft convention: 0 = +z, 90 = -x) of a direction mirrored in the glass. */
    public static float reflectYaw(float yaw, double nx, double nz) {
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double dot = dx * nx + dz * nz;
        double rx = dx - 2 * dot * nx;
        double rz = dz - 2 * dot * nz;
        return (float) Math.toDegrees(Math.atan2(-rx, rz));
    }

    /**
     * Scale about the camera that slides a reflection along its view rays until its middle is {@code depth} behind
     * the glass. On screen it looks exactly like the real reflection; in the world it now sits inside the mirror,
     * where blocks behind the mirror can't cover it.
     *
     * @param camera camera distance in front of the glass, &gt; 0
     * @param middle how far behind the glass the middle of the true reflection is (= the player's distance in front)
     * @param depth  where its middle should end up behind the glass
     * @return the scale, in (0, 1] for reflections farther than {@code depth}
     */
    public static float slideScale(double camera, double middle, double depth) {
        return (float) ((camera + depth) / (middle + camera));
    }

    /**
     * Squash along the glass normal that makes a reflection {@code thickness} deep (after {@link #slideScale}) fit
     * in {@code room}: 1 when it already fits. Squashing is not along the view rays, so it shifts the picture a
     * little at steep angles; for a few pixels of depth that is not noticeable.
     */
    public static float squash(double thickness, double room) {
        return thickness <= room ? 1.0F : (float) (room / thickness);
    }

    /**
     * Where the ray from the camera to a point crosses the glass plane, as the parameter along that ray (0 at the
     * camera, 1 at the point); negative or above 1 when it doesn't cross between them.
     */
    public static double crossing(double cameraFront, double pointFront) {
        double denominator = cameraFront - pointFront;
        return denominator == 0 ? -1 : cameraFront / denominator;
    }
}
