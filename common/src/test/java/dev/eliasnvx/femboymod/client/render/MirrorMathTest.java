package dev.eliasnvx.femboymod.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MirrorMathTest {

    private static final double EPS = 1.0E-4;

    @Test
    void facingTheMirrorMeansTheReflectionFacesYou() {
        // glass facing north (normal -z), player looking south (yaw 0) into it: the reflection looks north (180)
        assertEquals(180.0, Math.abs(MirrorMath.reflectYaw(0.0F, 0, -1)), EPS);
        // glass facing east (+x), player looking west (yaw 90): the reflection looks east (-90)
        assertEquals(-90.0, MirrorMath.reflectYaw(90.0F, 1, 0), EPS);
        // walking along the glass keeps the direction
        assertEquals(90.0, MirrorMath.reflectYaw(90.0F, 0, -1), EPS);
    }

    @Test
    void frontIsTheSignedDistance() {
        assertEquals(2.5, MirrorMath.front(0.5, -2.0, 0.5, 0.5, 0, -1), EPS);
        assertEquals(-1.0, MirrorMath.front(0.5, 1.5, 0.5, 0.5, 0, -1), EPS);
    }

    @Test
    void slidingPutsTheMiddleAtTheWantedDepth() {
        double camera = 3.0;
        double middle = 3.0;
        double depth = 0.08;
        float t = MirrorMath.slideScale(camera, middle, depth);
        // depth behind the glass after scaling about the camera: -camera + t * (middle + camera)
        assertEquals(depth, -camera + t * (middle + camera), EPS);
        // a point on the camera's own ray keeps its direction: only the distance changes
        assertEquals(0.5133, t, 1.0E-3);
    }

    @Test
    void squashOnlyWhenTooDeep() {
        assertEquals(1.0F, MirrorMath.squash(0.1, 0.13), 1.0E-6);
        assertEquals(0.325F, MirrorMath.squash(0.4, 0.13), 1.0E-6);
    }

    @Test
    void crossingTheGlass() {
        assertEquals(0.5, MirrorMath.crossing(3.0, -3.0), EPS);
        assertEquals(-1.0, MirrorMath.crossing(2.0, 2.0), EPS);
    }
}
