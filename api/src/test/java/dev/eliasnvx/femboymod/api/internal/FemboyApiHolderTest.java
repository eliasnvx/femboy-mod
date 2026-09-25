package dev.eliasnvx.femboymod.api.internal;

import dev.eliasnvx.femboymod.api.FemboyApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FemboyApiHolderTest {

    private static final FemboyApi STUB = () -> "0.0.0-test";

    @AfterEach
    void reset() {
        FemboyApiHolder.resetForTests();
    }

    @Test
    void getBeforeInstallThrowsClearIllegalState() {
        IllegalStateException e = assertThrows(IllegalStateException.class, FemboyApi::get);
        assertTrue(e.getMessage().contains("before initialization"));
    }

    @Test
    void getReturnsInstalledInstance() {
        FemboyApiHolder.install(STUB);
        assertSame(STUB, FemboyApi.get());
    }

    @Test
    void secondInstallIsRejected() {
        FemboyApiHolder.install(STUB);
        assertThrows(IllegalStateException.class, () -> FemboyApiHolder.install(STUB));
    }
}
