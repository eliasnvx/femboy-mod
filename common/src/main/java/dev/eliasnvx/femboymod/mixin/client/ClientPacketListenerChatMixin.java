package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.chat.ChatTransforms;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * UwU choker (SPEC §4.7): rewrite the chat text at the start of sendChat, i.e. before it is signed, so
 * the signature matches and servers show no "modified" marker. Commands use sendCommand and are untouched.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerChatMixin {

    @ModifyVariable(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private String femboymod$transformChat(String content) {
        return ChatTransforms.apply(content);
    }
}
