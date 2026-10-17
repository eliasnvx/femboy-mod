# Compatibility

All integrations are soft dependencies in `compat.<modid>`. The mod works without them.
Checked on MC 26.3, 2026-09-25.

| Mod | Status | How it was checked |
|---|---|---|
| Sodium 0.9.2 + Iris 1.11.6 (Fabric) | ✅ cosmetics, colorways, geo models, glow outline and first-person sleeves render correctly (no shader pack) | `./gradlew :fabric:runClientGameTest -Pcompat_render=true`, screenshots compared with vanilla |
| Iris with a shader pack | ⏳ manual | load any shader pack in a dev client (`-Pcompat_render=true`), check cosmetics + tint |
| JEI 31.7 | ✅ plugin loads without errors; all recipes are vanilla types (crafting, dye, transmute, smithing), which JEI shows natively. Info pages (U on an item): cosmetics, dyeing, backpacks, charms | `./gradlew :fabric:runClientGameTest -Pcompat_jei=true` (log). Info page layout: ⏳ manual |
| Mod Menu 21 | ✅ config button (`FemboyModMenu`) | Phase 4 |
| NeoForge mods list | ✅ config button (`IConfigScreenFactory`) | Phase 4 |
| Jade 26.3 | ✅ no integration needed: block names/containers come from vanilla data | ⏳ quick manual look |
| EMI, REI | ⏸ no 26.3 builds yet | re-check on release |
| Curios 9.5 (NeoForge) | ✅ cosmetics fit Curios slots (head: ears, clips and face pieces; necklace; body: tops; hands; belt) and count like ours: Drip, sets, effects, rendering. Curios 9 has no feet slot, so shoes stay in ours. A slot already filled in our outfit wins; backpacks stay in our back slot | `-Pcompat_slots=true`: `CuriosGameTest` with the real mod, 2026-10-13. Rendering in game: ⏳ manual |
| Trinkets 3.10 (Fabric) | ✅ cosmetics fit Trinkets slots (head/hat, head/face, chest/necklace, hand/glove, legs/belt, feet/shoes; players get them from `data/trinkets/entities/femboymod.json`) and count like ours | `-Pcompat_slots=true`: `TrinketsGameTest` with the real mod (+ Cardinal Components), 2026-10-13. Rendering in game: ⏳ manual |
| Accessories (Fabric, NeoForge) | ✅ built against its API; item tags for its default player slots (hat, face, necklace, hand, belt, shoes); it draws nothing for our cosmetics (`registerNoRenderer`), we do | the merge is covered by `ExternalSlotsGameTests` (fake slot mod). NeoForge client starts with Accessories 1.1.0-beta.53 alone and with The Aether 1.5.10 (the 0.2.0 start crash): `./gradlew :neoforge:runClient -Pcompat_aether=true`, 2026-10-16. ⏳ manual in game |
| Figura 0.1.6 (Fabric) | ✅ loads and runs without errors. With no avatar, or an avatar that keeps the vanilla player model, cosmetics show as usual (standing, walking, from behind). Cosmetics hide with the body parts an avatar hides (an avatar with its own model hides the vanilla one, and our cosmetics with it; one that hides only the arms hides what is worn on the arms) | `./gradlew :fabric:runProdCompatClient -Pcompat_figura=true` (production jars: Figura's accessors don't apply in the dev runtime), `CompatHarness` with two local test avatars, 1.21.1, 2026-10-15 |
| 3D Skin Layers, First-person Model, EMF/Fresh Animations | ⏸ checked on 26.3 only (see the 26.3 compat notes) | minimum goal: no crashes |
| Emotecraft | v1.2 | |

## Dev flags
- `-Pcompat_render=true` adds Sodium + Iris to the Fabric dev runtime (Modrinth maven, versions in `gradle.properties`).
- `-Pcompat_jei=true` adds JEI to the Fabric dev runtime.
- `./gradlew :fabric:runProdCompatClient` runs `CompatHarness` (src/gametest; this version has no client GameTest API) in a production-like Fabric client built from the release jars: it makes a flat world, dresses the player, takes screenshots, leaves and loads the world again (fails if the client lost its own outfit) and quits. `-Pcompat_figura=true` adds Figura and two local test avatars. `-Pharness_scene=pets|halloween|mobs|interior|mirror` shoots another scene instead (pet outfits on adults, kittens and puppies plus a real sneak + right-click headpat checked on the server; Halloween costumes; Sock Goblin and Rubber Duck; the furnished room; the mirror and the vanity table). `runCompatClient` is the same in the dev runtime. Screenshots: `fabric/build/run/*CompatClient/screenshots`.
- `-Pcompat_accessories=true` adds Accessories (from maven.wispforest.io, with its libraries) to the NeoForge dev runtime, `-Pcompat_aether=true` adds The Aether too (it ships Accessories). Client runs only.
- `-Pcompat_slots=true` adds Curios to the NeoForge dev runtime and Trinkets (+ Cardinal Components) to the Fabric one. Those runs check only `CuriosGameTest` / `TrinketsGameTest`: the slot mods disconnect or can't sync to the shared tests' mock players, so the other tests are skipped there.

They are never release dependencies. The JEI API is `compileOnly` in `common`, and only `compat.jei` touches it; the slot mods are `compileOnly` in the loader modules, touched only by their `compat.<modid>` package.
