# femboymod-api changelog

Format: Keep a Changelog. SemVer for the API is independent of the mod version.

## [0.1.0] - unreleased
### Added
- `FemboyApi`, `FemboyAddon`, `FemboyClientApi`, `@RegisterFemboyAddon`
- `FemboyEventBus`, `FemboyEvent`, `CancellableEvent`, `EventPriority`; events `CosmeticEquipEvent`, `CosmeticUnequipEvent`, `CosmeticChangedEvent`
- `ApiRegistry`; `cosmeticSlots()` registry with `CosmeticSlotType`, built-in `FemboySlots`
- Data components `Cosmetic`, `Colorway`; `FemboyDataComponents`
- Datapack registry `ColorwayPattern` (`data/<ns>/femboymod/colorway`), `Colors`
- Queries `getCosmetics`, `getColorway`
