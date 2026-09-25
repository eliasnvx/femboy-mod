package dev.eliasnvx.femboymod.client.chat;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.ChatTransformer;
import dev.eliasnvx.femboymod.api.event.ChatTransformEvent;
import dev.eliasnvx.femboymod.chat.UwuTransformer;
import net.minecraft.util.StringUtil;

/** Runs all chat transformers on an outgoing (pre-signing) chat message. Called from the chat mixin. */
public final class ChatTransforms {

    private ChatTransforms() {
    }

    public static String apply(String content) {
        if (content.isEmpty() || UwuTransformer.isExempt(content)) {
            return content;
        }
        String result = content;
        var registry = FemboyClientApi.get().chatTransformers();
        for (var id : registry.ids()) {
            ChatTransformer transformer = registry.get(id).orElseThrow();
            try {
                result = transformer.transform(result);
            } catch (RuntimeException e) {
                FemboyMod.LOGGER.error("Chat transformer {} failed", id, e);
            }
        }
        if (result.equals(content)) {
            return content;
        }
        ChatTransformEvent event = FemboyMod.api().events().post(new ChatTransformEvent(content, result));
        if (event.isCancelled()) {
            return content;
        }
        // Longer than the packet limit would disconnect the player; never send blank.
        String trimmed = StringUtil.trimChatMessage(event.message());
        return trimmed.isBlank() ? content : trimmed;
    }
}
