# Femboy Mod API: addon guide (API 0.1.0)

> The API is `0.x`, so minor versions may still break things. Changes are listed in [CHANGELOG.md](CHANGELOG.md).
> Working reference: [`example-addon/`](../../example-addon). CI builds and tests it, and every snippet below comes from it.

- [Setup](#setup)
- [Entry point](#entry-point)
- [A cosmetic item](#a-cosmetic-item)
- [A cosmetic slot](#a-cosmetic-slot)
- [Stats: Drip and effects](#stats-drip-and-effects)
- [A set bonus](#a-set-bonus)
- [A charm](#a-charm)
- [A colorway pattern](#a-colorway-pattern)
- [Custom effect and condition types](#custom-effect-and-condition-types)
- [A renderer](#a-renderer)
- [A Blockbench model instead of code](#a-blockbench-model-instead-of-code)
- [Chat transformers](#chat-transformers)
- [Events](#events)
- [Queries](#queries)
- [Rules](#rules)

## Setup
Compile against the API jar only. Everything outside `dev.eliasnvx.femboymod.api` is internal and can change at any time. At runtime you need the full mod.

```groovy
repositories {
    maven { url "https://api.modrinth.com/maven" } // full mod for dev runs
    // + the Maven repository the API is published to (see the mod page)
}

dependencies {
    // Architectury common module
    compileOnly "dev.eliasnvx:femboymod-api:0.1.0+26.3"

    // Fabric module: API at compile time, full mod at runtime
    compileOnly "dev.eliasnvx:femboymod-api:0.1.0+26.3"
    runtimeOnly "maven.modrinth:femboy-mod:0.1.0+26.3-fabric"

    // NeoForge module
    compileOnly "dev.eliasnvx:femboymod-api:0.1.0+26.3"
    runtimeOnly "maven.modrinth:femboy-mod:0.1.0+26.3-neoforge"
}
```

The mod also needs Architectury API and GeckoLib at runtime. Declare the dependency in your metadata:
- Fabric `fabric.mod.json`: `"depends": { "femboymod": ">=0.1.0" }`
- NeoForge `neoforge.mods.toml`: `[[dependencies.<your_mod>]] modId = "femboymod"`, `type = "required"`, `ordering = "AFTER"`

Check the API version at runtime with `FemboyApi#apiVersion()`.

## Entry point
```java
@RegisterFemboyAddon                         // NeoForge: found by annotation scan
public final class MyAddon implements FemboyAddon {
    @Override public void onInitialize(FemboyApi api) { /* registries, listeners */ }
    @Override public void onInitializeClient(FemboyClientApi api) { /* renderers, chat transformers */ }
}
```
On Fabric, add the entrypoint to `fabric.mod.json`: `"entrypoints": { "femboymod": ["com.example.MyAddon"] }`.

Register extensions **only** inside these methods. Every `ApiRegistry` is frozen once all addons have run, and a late `register` throws. Outside the callbacks, use `FemboyApi.get()` / `FemboyClientApi.get()`.

## A cosmetic item
A cosmetic is a normal item that carries the `femboymod:cosmetic` data component:
```java
ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("friendship_pin"));
new Item(new Item.Properties().setId(key).stacksTo(1)
        .component(api.components().cosmetic().get(), new Cosmetic(PIN_SLOT, Optional.of(PIN_RENDERER))));
```
- `Cosmetic(slot)` picks the renderer by convention (below). `Cosmetic(slot, Optional.of(rendererId))` names a renderer explicitly.
- Players wear it by right-clicking it or through the Cosmetics screen. The server checks everything, and `CosmeticEquipEvent` can cancel it.
- Dyeable: add the item to `#femboymod:dyeable_cosmetics` and write a `crafting_dye` recipe (see `data/femboymod/recipe/*_dyed.json`). The colorway ends up in the `femboymod:colorway` component.

## A cosmetic slot
```java
api.cosmeticSlots().register(id("pin"), new CosmeticSlotType(1000)); // sort order; built-ins use multiples of 100
```
The slot name is the lang key `cosmetic_slot.<ns>.<path>`. The built-in slots are listed in `FemboySlots`.
Give it a silhouette for when it's empty with `new CosmeticSlotType(order, Optional.of(spriteId))`: a 16x16 GUI sprite at `assets/<ns>/textures/gui/sprites/<path>.png` (built-ins use `femboymod:container/slot/cosmetic/<slot>`). It shows in the inventory panel and in the outfit screen, where addon slots go in a row under the doll.

## Stats: Drip and effects
`data/<ns>/femboymod/cosmetic_stats/<item path>.json`. The file name is the item id, so the file for `femboymod_example:friendship_pin` is `data/femboymod_example/femboymod/cosmetic_stats/friendship_pin.json`.
```json
{
  "drip": 4,
  "effects": [ { "effect": { "type": "femboymod:glow_hostiles", "radius": 16 } } ]
}
```
Drip adds up to the player's Drip Level; see `drip_rules/default.json` for tiers and bonuses. `effects` apply while the item is worn.

Built-in effect types:

| Type | Fields |
|---|---|
| `femboymod:attribute` | `attribute`, `amount`, `operation` (`add_value`, `add_multiplied_base`, `add_multiplied_total`) |
| `femboymod:mob_effect` | `effect`, `amplifier` (0), `duration` in ticks (60, refreshed), `show_icon` (true) |
| `femboymod:particles` | `particle`, `interval`, `count`, `spread` |
| `femboymod:step_sound` | `sound`, `distance` (blocks walked between sounds) |
| `femboymod:follow_passive` | `radius`, `speed`, `interval`, `stop_distance` |
| `femboymod:glow_hostiles` | `radius` (client-side outline only) |
| `femboymod:damage_bonus` | `targets` (entity id, list or tag), `multiplier`: the wearer hits those mobs harder |

Built-in conditions (`"when"`): `femboymod:cold_biome`, `femboymod:crouching`, `femboymod:sprinting`.

## A set bonus
`data/<ns>/femboymod/set_bonus/<name>.json`, lang key `set_bonus.<ns>.<name>`:
```json
{
  "pieces": ["femboymod_example:friendship_pin", "femboymod:cat_ears"],
  "effects": [
    {
      "effect": { "type": "femboymod_example:xp_trickle", "interval": 200 },
      "when": { "type": "femboymod_example:daytime" }
    }
  ]
}
```
- A piece is an item id, a list, or a tag (`"#femboymod:legwear"`).
- `required` (optional) is how many pieces you need; the default is all of them.
- `scaling_per_tier` (optional) multiplies the effect strength per Drip tier (`EffectSource#scale`).
- `SetBonusEvent.Activate` / `SetBonusEvent.Deactivate` fire when a bonus turns on or off.

## Drip in combat
`data/<ns>/femboymod/drip_damage/<name>.json` (`DripDamage`): damage that the listed mobs deal to a player is multiplied by the entry for the player's Drip tier.
```json
{ "attackers": "#femboymod:bugs", "multiplier_by_tier": [1.0, 0.9, 0.75, 0.6, 0.5, 0.4] }
```
Tiers past the end of the list use the last value. Rules stack if several match. The other direction is the `femboymod:damage_bonus` effect (see the table above).

## A charm
Charms go into the charm slots of a backpack and work while the backpack is worn.
1. Add the item to the tag `data/femboymod/tags/item/charms.json`: `{"values": ["femboymod_example:friendship_pin"]}`.
2. Add stats in `data/<ns>/femboymod/charm/<item path>.json`:
```json
{ "effects": [ { "effect": { "type": "femboymod:attribute", "attribute": "minecraft:luck", "amount": 1.0, "operation": "add_value" } } ] }
```
`CharmsChangedEvent` fires when the charms change. Read the current charms with `api.getCharms(backpackStack)`.

## A colorway pattern
No code needed: `data/<ns>/femboymod/colorway/<name>.json`, lang key `colorway.<ns>.<name>`.
```json
{ "stripes": ["#FFB3D9", "base", "#B3E5FF", "secondary"] }
```
- `base` / `secondary` are the item's dye colors.
- An optional `"chevron"` block adds a Progress-style chevron (see `ColorwayPattern.Chevron` and `data/femboymod/femboymod/colorway/pride_progress.json`).
- Patterns only become colors on screen. Keep them free of text and slogans (SPEC §1.1).

## Custom effect and condition types
```java
public record XpTrickle(int interval, int amount) implements CosmeticEffect {
    public static final MapCodec<XpTrickle> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(XpTrickle::interval),
            Codec.intRange(1, 100).optionalFieldOf("amount", 1).forGetter(XpTrickle::amount)
    ).apply(i, XpTrickle::new));

    @Override public MapCodec<XpTrickle> codec() { return CODEC; }

    @Override public void tick(ServerPlayer player, EffectSource source) {
        if (player.tickCount % interval == 0) player.giveExperiencePoints(amount);
    }
}

api.cosmeticEffectTypes().register(id("xp_trickle"), XpTrickle.CODEC);
api.cosmeticConditionTypes().register(id("daytime"), Daytime.CODEC); // CosmeticCondition#test(Player)
```
- Effects run on the server only.
- `onActivate` and `onDeactivate` must undo each other. Remove whatever you added, such as attribute modifiers.
- `tick` runs every tick while the effect is active, so keep it cheap.
- `EffectSource` tells you which item or bonus caused the effect and its tier scale.

## A renderer
Client side, in `onInitializeClient`:
```java
EntityModelLayerRegistry.register(LAYER, ExamplePinRenderer::layer); // your LayerDefinition
api.cosmeticRenderers().register(id("pin"), models -> new ExamplePinRenderer(models.bakeLayer(LAYER)));
```
```java
@Override
public void submit(CosmeticRenderContext ctx) {
    PoseStack pose = ctx.poseStack();
    pose.pushPose();
    ctx.parentModel().body.translateAndRotate(pose); // follow a body part
    ctx.collector().submitModelPart(pin, pose, RenderTypes.entityCutout(TEXTURE), ctx.light(), ctx.overlay(), null, 0xFFFFFFFF);
    pose.popPose();
}
```
- The factory runs once per resource reload. `submit` runs every frame for every player who wears the item, so don't allocate in it.
- `ctx.stack()` is the worn item: read the colorway with `api.common().getColorway(stack)`.
- `ctx.state()` is the player's `AvatarRenderState`.
- `ctx.motion()` gives smoothed movement values (walk amount, turn sway, phase) for procedural animation (`CosmeticMotion`).

## A Blockbench model instead of code
If your item has no explicit renderer, you can draw it without code. Put the files at
`assets/<ns>/geckolib/models/cosmetic/<item path>.geo.json` and `assets/<ns>/textures/cosmetic/<item path>.png`
(an optional `_dyeable.png` is multiplied by the colorway; an optional `geckolib/animations/cosmetic/<item path>.animation.json` plays a looping `idle`).
Details and bone names: [docs/art/blockbench.md](../art/blockbench.md).

## Chat transformers
```java
api.chatTransformers().register(id("my_filter"), message -> wearsMyItem() ? message.replace("hello", "hewwo") : message);
```
- Transformers run in registration order on the sender's client, before the message is signed, so the signature stays valid.
- They are never called for commands. Return the message unchanged when your item isn't worn.
- A transformer that throws is logged and skipped.
- `ChatTransformEvent` fires after all transformers. It can change the final text or cancel, in which case the original is sent.
- The UwU choker is `femboymod:uwu`.

## Events
Subscribe with `api.events().addListener(EventClass.class, handler)`. You can pass a priority: `addListener(Class, EventPriority, handler)`.

| Event | Side | Cancellable | When |
|---|---|---|---|
| `CosmeticEquipEvent` | server | yes | before an item is put on; check only, no side effects |
| `CosmeticUnequipEvent` | server | no | an item was taken off |
| `CosmeticChangedEvent` | server | no | after a slot changed |
| `DripLevelChangedEvent` | server | no | Drip Level or tier changed |
| `SetBonusEvent.Activate` / `.Deactivate` | server | no | a set bonus turned on or off |
| `BackpackOpenEvent` | server | yes | a backpack is about to open |
| `CharmsChangedEvent` | server | no | a backpack's charms changed |
| `ChatTransformEvent` | client | yes | after chat transformers ran, before signing (cancel = send the original) |
| `PinkCreeperBlastEvent` | server | yes | a Pink Creeper is about to explode |

Handlers of cancellable events should only look and decide. Do the actual work in an event that fires after the change, such as `CosmeticChangedEvent`.

## Queries
| Method | Returns |
|---|---|
| `getCosmetics(entity)` | read-only `CosmeticsView` of what is worn, slot by slot |
| `getColorway(stack)` | the item's colorway, if dyed |
| `getDripLevel(player)` | `DripLevel(level, tier)` |
| `getActiveSetBonuses(player)` | ids of active set bonuses |
| `getCharms(backpack)` | charms in a backpack stack |

## Rules
- Never touch classes outside `dev.eliasnvx.femboymod.api`.
- Content must be SFW and free of third-party brands (SPEC §1.1). Addons that break this are not listed or supported.
- Report API gaps as issues instead of using mixins into femboymod.
