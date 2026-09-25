package dev.eliasnvx.femboymod.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/** Client-only options ({@code config/femboymod-client.json}, SPEC §10). */
public record ClientConfig(boolean showOthersCosmetics, boolean dripHud, boolean nyaSound, boolean rgbAnimations,
                           Physics physics, boolean uwuChat) {

    public static final ClientConfig DEFAULTS = new ClientConfig(true, false, false, true, Physics.FULL, true);

    public static final Codec<ClientConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("show_others_cosmetics").orElse(DEFAULTS.showOthersCosmetics).forGetter(ClientConfig::showOthersCosmetics),
            Codec.BOOL.fieldOf("drip_hud").orElse(DEFAULTS.dripHud).forGetter(ClientConfig::dripHud),
            Codec.BOOL.fieldOf("nya_sound").orElse(DEFAULTS.nyaSound).forGetter(ClientConfig::nyaSound),
            Codec.BOOL.fieldOf("rgb_animations").orElse(DEFAULTS.rgbAnimations).forGetter(ClientConfig::rgbAnimations),
            Physics.CODEC.fieldOf("tail_skirt_physics").orElse(DEFAULTS.physics).forGetter(ClientConfig::physics),
            Codec.BOOL.fieldOf("uwu_chat").orElse(DEFAULTS.uwuChat).forGetter(ClientConfig::uwuChat)
    ).apply(i, ClientConfig::new));

    public ClientConfig withShowOthersCosmetics(boolean v) { return new ClientConfig(v, dripHud, nyaSound, rgbAnimations, physics, uwuChat); }
    public ClientConfig withDripHud(boolean v) { return new ClientConfig(showOthersCosmetics, v, nyaSound, rgbAnimations, physics, uwuChat); }
    public ClientConfig withNyaSound(boolean v) { return new ClientConfig(showOthersCosmetics, dripHud, v, rgbAnimations, physics, uwuChat); }
    public ClientConfig withRgbAnimations(boolean v) { return new ClientConfig(showOthersCosmetics, dripHud, nyaSound, v, physics, uwuChat); }
    public ClientConfig withPhysics(Physics v) { return new ClientConfig(showOthersCosmetics, dripHud, nyaSound, rgbAnimations, v, uwuChat); }
    public ClientConfig withUwuChat(boolean v) { return new ClientConfig(showOthersCosmetics, dripHud, nyaSound, rgbAnimations, physics, v); }

    /** Tail/skirt procedural animation quality. */
    public enum Physics implements StringRepresentable {
        FULL("full"), SIMPLE("simple"), OFF("off");

        public static final Codec<Physics> CODEC = StringRepresentable.fromEnum(Physics::values);
        private final String name;

        Physics(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
