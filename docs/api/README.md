# Femboy Mod API: addon guide (draft, API 0.1.0)

> Status: `0.x`. The API may still change between minors. The full guide (Gradle snippets for Maven, renderers, set bonuses) comes in Phase 5.
> Working reference: `example-addon/` (built and tested in CI).

## Connecting
The addon is a regular mod that depends on `femboymod`. At compile time, use only `femboymod-api`; everything outside `dev.eliasnvx.femboymod.api` is internal.

## Entry point
```java
@RegisterFemboyAddon                     // NeoForge
public final class MyAddon implements FemboyAddon {
    @Override public void onInitialize(FemboyApi api) { ... }
    @Override public void onInitializeClient(FemboyClientApi api) { ... }
}
```
Fabric: `"entrypoints": { "femboymod": ["com.example.MyAddon"] }` in `fabric.mod.json`.
Register extensions **only** in `onInitialize`. After it, the registries are frozen.

## Your own cosmetic item
```java
new Item(new Item.Properties().setId(key).stacksTo(1)
        .component(api.components().cosmetic().get(), new Cosmetic(FemboySlots.NECK)));
```
Built-in slots: `FemboySlots.*`. Your own slot: `api.cosmeticSlots().register(id, new CosmeticSlotType(sortOrder))`. Its name is the lang key `cosmetic_slot.<ns>.<path>`.

## Your own colorway pattern (no code)
`data/<ns>/femboymod/colorway/<name>.json`:
```json
{ "stripes": ["#FFB3D9", "base", "#B3E5FF", "secondary"] }
```
Name: lang key `colorway.<ns>.<name>`.

## Events
```java
api.events().addListener(CosmeticEquipEvent.class, e -> { if (...) e.cancel(); }); // check, no side effects
api.events().addListener(CosmeticChangedEvent.class, e -> ...);                    // server, after the change
api.events().addListener(CosmeticUnequipEvent.class, e -> ...);
```

## Queries
`api.getCosmetics(entity)` (read-only), `api.getColorway(stack)`.
