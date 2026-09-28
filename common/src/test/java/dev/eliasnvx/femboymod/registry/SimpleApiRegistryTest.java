package dev.eliasnvx.femboymod.registry;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleApiRegistryTest {

    private static final ResourceLocation A = new ResourceLocation("test", "a");

    private final SimpleApiRegistry<String> registry = new SimpleApiRegistry<>(new ResourceLocation("test", "reg"));

    @Test
    void registerAndLookup() {
        String entry = registry.register(A, "value");
        assertEquals("value", registry.get(A).orElseThrow());
        assertEquals(A, registry.getId(entry).orElseThrow());
    }

    @Test
    void duplicatesAndFrozenAreRejected() {
        registry.register(A, "value");
        assertThrows(IllegalArgumentException.class, () -> registry.register(A, "other"));
        registry.freeze();
        assertThrows(IllegalStateException.class,
                () -> registry.register(new ResourceLocation("test", "b"), "late"));
    }

    @Test
    void codecRoundTripAndUnknownId() {
        String entry = registry.register(A, "value");
        var encoded = registry.byIdCodec().encodeStart(JsonOps.INSTANCE, entry).getOrThrow(false, error -> { });
        assertEquals(new JsonPrimitive("test:a"), encoded);
        assertEquals(entry, registry.byIdCodec().parse(JsonOps.INSTANCE, encoded).getOrThrow(false, error -> { }));
        assertTrue(registry.byIdCodec().parse(JsonOps.INSTANCE, new JsonPrimitive("test:missing")).error().isPresent());
    }
}
