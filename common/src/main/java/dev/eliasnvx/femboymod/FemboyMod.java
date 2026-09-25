package dev.eliasnvx.femboymod;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.internal.FemboyApiHolder;
import dev.eliasnvx.femboymod.command.FemboyCommands;
import dev.eliasnvx.femboymod.cosmetic.BuiltinSlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.network.FemboyNetwork;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry point, called by each loader's main initializer. */
public final class FemboyMod {

    public static final String MOD_ID = FemboyApi.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FemboyApiImpl api;

    private FemboyMod() {
    }

    public static synchronized void init() {
        if (api != null) {
            throw new IllegalStateException("FemboyMod.init() called twice");
        }
        api = new FemboyApiImpl();
        FemboyApiHolder.install(api);

        BuiltinSlots.register(api.cosmeticSlots());

        FemboyComponents.REGISTER.register();
        FemboyItems.TABS.register();
        FemboyItems.REGISTER.register();
        FemboyMenus.REGISTER.register();

        FemboyNetwork.register();
        CosmeticsEvents.register();
        CommandRegistrationEvent.EVENT.register(FemboyCommands::register);

        AddonLoader.initCommon(api);

        // Addons may only register API extensions during onInitialize.
        api.freezeRegistries();
        CosmeticsManager.bake(api.cosmeticSlotRegistry());

        LOGGER.info("femboymod initialized (api {}, {} addon(s), {} cosmetic slots)",
                api.apiVersion(), AddonLoader.addonCount(), CosmeticsManager.orderedSlots().size());
    }

    /** The API implementation; available after {@link #init()}. */
    public static FemboyApiImpl api() {
        return api;
    }
}
