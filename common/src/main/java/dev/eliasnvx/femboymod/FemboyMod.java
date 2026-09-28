package dev.eliasnvx.femboymod;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.world.FemboyWorldgen;
import dev.eliasnvx.femboymod.recipe.FemboyRecipes;
import dev.eliasnvx.femboymod.world.trade.TradeSets;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.entity.AdvancementHooks;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.internal.FemboyApiHolder;
import dev.eliasnvx.femboymod.command.FemboyCommands;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.BuiltinSlots;
import dev.eliasnvx.femboymod.effect.BuiltinConditions;
import dev.eliasnvx.femboymod.effect.BuiltinEffects;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.network.FemboyNetwork;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
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
        FemboyConfig.loadCommon();
        api = new FemboyApiImpl();
        FemboyApiHolder.install(api);

        BuiltinSlots.register(api.cosmeticSlots());
        BuiltinEffects.register(api.cosmeticEffectTypes());
        BuiltinConditions.register(api.cosmeticConditionTypes());
        ProfileHooks.registerFields(api.profileFields());

        // 1.20.1: item data lives in NBT; decoding holders from it needs the server's registries
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STARTING.register(server ->
                dev.eliasnvx.femboymod.item.RegistryAccessContext.setServer(server.registryAccess()));
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STOPPED.register(server ->
                dev.eliasnvx.femboymod.item.RegistryAccessContext.setServer(null));
        FemboyEffects.REGISTER.register();
        FemboyTriggers.REGISTER.register();
        FemboyEntities.init();
        FemboySounds.REGISTER.register();
        dev.eliasnvx.femboymod.registry.FemboyPaintings.init();
        FemboyBlocks.CLOTHING_RACK.getId(); // class-init FemboyBlocks so its blocks and block items are queued first
        FemboyItems.BLOCKS.register();
        FemboyItems.TABS.register();
        FemboyItems.REGISTER.register();
        FemboyRecipes.init();
        FemboyMenus.REGISTER.register();
        FemboyBlocks.init();
        TradeSets.init();
        FemboyWorldgen.init();
        FemboyGameRules.init();
        PlatformHelper.registerPoi();

        FemboyNetwork.register();
        CosmeticsEvents.register();
        CommandRegistrationEvent.EVENT.register(FemboyCommands::register);

        AdvancementHooks.register(api.events());
        ProfileHooks.register(api.events());
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
