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
- `ColorwayPattern` chevron (`Chevron`), `Colorway#colorAt`
