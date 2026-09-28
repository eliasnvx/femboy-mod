package dev.eliasnvx.femboymod.item;

import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Where item data finds registries when it decodes holders (e.g. a colorway pattern) from NBT. Minecraft 1.20.1
 * item stacks have no registry context of their own. The server sets it while running; the client sets a supplier
 * for its current level.
 */
public final class RegistryAccessContext {

    private static volatile @Nullable RegistryAccess server;
    private static volatile Supplier<@Nullable RegistryAccess> client = () -> null;

    private RegistryAccessContext() {
    }

    public static void setServer(@Nullable RegistryAccess access) {
        server = access;
    }

    public static void setClient(Supplier<@Nullable RegistryAccess> access) {
        client = access;
    }

    /** @return the registries of the running server, else of the client's level, else null */
    public static @Nullable RegistryAccess current() {
        RegistryAccess access = server;
        return access != null ? access : client.get();
    }
}
