# Compatibility

All integrations are soft dependencies in `compat.<modid>`. The mod works without them.
Checked on MC 26.3, 2026-09-25.

| Mod | Status | How it was checked |
|---|---|---|
| Sodium 0.9.2 + Iris 1.11.6 (Fabric) | ✅ cosmetics, colorways, geo models, glow outline and first-person sleeves render correctly (no shader pack) | `./gradlew :fabric:runClientGameTest -Pcompat_render=true`, screenshots compared with vanilla |
| JEI 31.7 | ✅ plugin loads without errors; all recipes are vanilla types (crafting, dye, transmute, smithing), which JEI shows natively. Info pages (U on an item): cosmetics, dyeing, backpacks, charms | `./gradlew :fabric:runClientGameTest -Pcompat_jei=true` (log). Info page layout: ⏳ manual |
| Mod Menu 21 | ✅ config button (`FemboyModMenu`) | Phase 4 |
| NeoForge mods list | ✅ config button (`IConfigScreenFactory`) | Phase 4 |
| Jade 26.3 | ✅ no integration needed: block names/containers come from vanilla data | ⏳ quick manual look |
| EMI, REI | ⏸ no 26.3 builds yet | re-check on release |
| Curios 17.0.0-beta (NeoForge) | ✅ cosmetics fit Curios slots (head: ears, clips and face pieces; necklace; body: tops; hands; belt; feet: shoes) and count like ours: Drip, sets, effects, rendering. A slot already filled in our outfit wins; backpacks stay in our back slot | `-Pcompat_slots=true`: `CuriosGameTest` with the real mod (beta.3 on NeoForge 26.3.0.76; beta.2 on 26.3.0.16-beta), 2026-10-13. Rendering in game: ⏳ manual |
| Accessories / Trinkets | ⏸ no 26.3 builds yet (the 1.21.1 and 1.20.1 builds support them) | re-check on release |
| 3D Skin Layers 1.11.3 | ✅ after a fix (2026-10-14): it puts the skin's 3D outer layers into every `PlayerModel` in `setupAnim`, our cosmetic models included, so the outer skin was drawn with the cosmetic's texture (reported by a player). Cosmetic models now keep the outer layer parts hidden. 1.21.1 / 1.20.1 aren't affected (our cosmetic models are `HumanoidModel`s there; its mixin targets `PlayerModel`) | `-Pcompat_models=skinlayers`, `CompatModelsClientTest` screenshots (bisected against ETF and EMF) |
| Entity Model Features 3.3.11 + Entity Texture Features 7.2.5 + Fresh Animations 1.10.5 | ✅ cosmetics render on the animated player, standing, walking, from behind | `-Pcompat_models=true` with `FEMBOYMOD_COMPAT_PACKS` (Fresh Animations), 2026-10-14 |
| Iris 1.11.6 + Sodium + Complementary Reimagined r5.9.3 | ✅ cosmetics and their tint render with the shader pack (together with the model mods above) | `-Pcompat_models=true -Pcompat_render=true` with `FEMBOYMOD_COMPAT_SHADER`, 2026-10-14 |
| First-person Model 2.7.3 + Not Enough Animations 1.12.6 | ✅ loads without errors; cosmetics show on the body it draws in first person (socks and shoes when you look down while walking) | `-Pcompat_models=firstperson`, `CompatModelsClientTest` first-person shots, 2026-10-15 |
| Emotecraft 3.5.0-a.171 + Player Animation Library 1.2.8 | ✅ our emotes and theirs both play; cosmetics follow Emotecraft's animations: socks, shoes and the skirt move with the legs (Kazotsky Kick), mittens with the arms (Clap), the whole outfit, backpack and tail turn with the body (Backflip) | `-Pcompat_models=emotecraft`, `CompatModelsClientTest` plays three bundled emotes through Emotecraft's client API, 2026-10-15 |
| Figura | ⏸ no 26.x build (0.1.6 supports up to 1.21.4). On the 1.21.1 and 1.20.1 versions: ✅ no errors, cosmetics show with avatars that keep the vanilla model and hide with the body parts an avatar hides | those versions' compat notes (`runProdCompatClient -Pcompat_figura=true`), 2026-10-15 |
| Xaero's World Map 1.47.0 + Minimap 26.6.0 (Fabric) | ✅ our blocks show on the minimap and the world map, no errors. The Femboy Level panel moves to the bottom right when Xaero's Minimap is installed (it sits in the top left corner by default); players can pick any corner in the settings | `-Pcompat_maps=true` (MapModsClientTest), 2026-10-13 |
| Xaero's World Map, mod removal | ⚠️ upstream bug: after removing **any** mod with blocks, singleplayer (world save) map regions turn black. Xaero reads chunk palettes strictly and drops the region. Reported to Xaero. Workaround: Full Resave before removing the mod, then use the "Converted World Save" map ([#1](https://github.com/eliasnvx/femboy-mod/issues/1)) | bytecode of 1.46.4 / 1.47.0, repro in a client test |
| JourneyMap 6.0.10 (Fabric) | ✅ our blocks show on the minimap and the full-screen map, no errors | `-Pcompat_maps=true` (MapModsClientTest), 2026-10-13 |

## Dev flags
- `-Pcompat_render=true` adds Sodium + Iris to the Fabric dev runtime (Modrinth maven, versions in `gradle.properties`).
- `-Pcompat_jei=true` adds JEI to the Fabric dev runtime.
- `-Pcompat_maps=true` adds Xaero's World Map + Minimap and JourneyMap; `MapModsClientTest` places every block of the mod and screenshots the maps (`compat_maps_*.png`). It uses a dedicated test server: with Xaero's map loaded, closing a singleplayer world deadlocks Fabric's client test framework, so the singleplayer client tests skip themselves in that run.
- `-Pcompat_models=true` adds Entity Model Features, Entity Texture Features, 3D Skin Layers, First-person Model (+ Not Enough Animations) and Emotecraft (+ Player Animation Library), or a comma list of `etf`, `emf`, `skinlayers`, `firstperson`, `emotecraft`. `CompatModelsClientTest` (with `FEMBOYMOD_COMPAT_SHOTS=1`) screenshots the player bare and in cosmetics; `FEMBOYMOD_COMPAT_PACKS=<folder of resource pack zips>` turns packs on (Fresh Animations), `FEMBOYMOD_COMPAT_SHADER=<shader pack zip>` with `-Pcompat_render=true` turns an Iris shader pack on. The client test run dir is wiped before every run, so packs are copied in at run time.
- `-Pcompat_slots=true` adds Curios to the NeoForge dev runtime. The NeoForge GameTest server then runs only `CuriosGameTest`: Curios can't sync its slots to the shared tests' mock players.

They are never release dependencies. The JEI API is `compileOnly` in `common`, and only `compat.jei` touches it; the slot mods are `compileOnly` in the loader modules, touched only by their `compat.<modid>` package.
