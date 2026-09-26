# femboymod-api changelog

Format: Keep a Changelog. SemVer for the API is independent of the mod version.

## [0.1.0] - unreleased
### Added
- `FemboyApi`, `FemboyAddon`, `FemboyClientApi`, `@RegisterFemboyAddon`
- `FemboyEventBus`, `FemboyEvent`, `CancellableEvent`, `EventPriority`; events `CosmeticEquipEvent`, `CosmeticUnequipEvent`, `CosmeticChangedEvent`
- `ApiRegistry`; `cosmeticSlots()` registry with `CosmeticSlotType`, built-in `FemboySlots`
- Data components `Cosmetic`, `Colorway`; `FemboyDataComponents`
- Datapack registry `ColorwayPattern` (`data/<ns>/femboymod/colorway`), `Colors`
- Queries `getCosmetics`, `getColorway`, `getDripLevel`, `getActiveSetBonuses`, `getCharms`
- Effects: `CosmeticEffect`, `CosmeticCondition`, `ConfiguredEffect`, `EffectSource`; registries `cosmeticEffectTypes()`, `cosmeticConditionTypes()`
- Datapack registries `CosmeticStats` (`cosmetic_stats`), `SetBonus` (`set_bonus`), `DripRules` (`drip_rules`), `CharmStats` (`charm`)
- Events `DripLevelChangedEvent`, `SetBonusEvent`, `BackpackOpenEvent`, `CharmsChangedEvent`, `ChatTransformEvent`, `PinkCreeperBlastEvent`
- Client: `CosmeticRenderer` (+ `Factory`), `CosmeticRenderContext`, `CosmeticMotion`, `ChatTransformer`; registries `cosmeticRenderers()`, `chatTransformers()`
- `CosmeticsView#isHidden(slot)`: worn but hidden by the wearer; renderers must skip it
- Combat: `DripDamage` data pack registry (`drip_damage`): mob damage to players scaled by Drip tier; effect type `femboymod:damage_bonus`
- `ColorwayPattern` chevron (`Chevron`), `Colorway#colorAt`
- Built-in slot `FemboySlots.WAIST` (belts)
- `VibeCheckEvent` (Vibe Check Scanner score, changeable)
- Armor under the outfit: `ArmorVisibility`, `CosmeticSlotType#coversArmor`, `CosmeticsView#armorVisibility`
- Player profile: `PlayerProfile`, `ProfileField`, `FemboyProfileFields`, `FemboyApi#profileFields()`, `FemboyApi#getProfile(player)`
- Style Points: `FemboyApi#addStylePoints(player, amount, reason)`, `StylePointsEvent` (cancellable, amount changeable)
- Events `CollectionUnlockEvent`, `CriticReviewEvent`, `SetupRatedEvent`, `EnergyDrinkEvent` (package `api.event.profile`)
- `ColorwayPattern` shimmer (`Shimmer`, `"shimmer": {"period_ticks": N}`, at least 40 ticks so it can't strobe): stripe colors flow smoothly over time; `ColorwayPattern#stripeColor(index, ticks, base, secondary)`, `Colorway#stripeColor(index, ticks)`, `Colorway#colorAt(u, v, ticks)`, `Colorway#hasShimmer()`
