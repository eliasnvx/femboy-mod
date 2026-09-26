package dev.eliasnvx.femboymod.emote;

import java.util.function.BiConsumer;

/** Set by the client entrypoint, so common network code never touches client classes. */
public final class EmoteClientHooks {

    public static BiConsumer<Integer, Emote> onShow = (id, emote) -> { };

    private EmoteClientHooks() {
    }
}
