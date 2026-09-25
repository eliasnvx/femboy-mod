package dev.eliasnvx.femboymod.api.client;

/**
 * Rewrites the local player's outgoing chat message before it is signed and sent (SPEC §4.7).
 * Registered in {@link dev.eliasnvx.femboymod.api.FemboyClientApi#chatTransformers()}; transformers run in
 * registration order. Never called for commands. Must be SFW and must not add control characters or '§'.
 */
@FunctionalInterface
public interface ChatTransformer {

    /**
     * @param message the message so far (never a command)
     * @return the new message, or {@code message} unchanged if this transformer does not apply now
     *         (e.g. its item is not worn)
     */
    String transform(String message);
}
