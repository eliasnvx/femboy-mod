package dev.eliasnvx.femboymod.gametest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Objects;

/** Assertions that 1.21's {@code GameTestHelper} has and 1.20.1's lacks. */
public final class GameTestAsserts {

    private GameTestAsserts() {
    }

    /** Same message as 1.21's {@code GameTestHelper#assertValueEqual}. */
    public static <T> void assertValueEqual(GameTestHelper helper, T actual, T expected, String name) {
        if (!Objects.equals(actual, expected)) {
            throw new GameTestAssertException("Expected " + name + " to be " + expected + ", but was " + actual);
        }
    }
}
