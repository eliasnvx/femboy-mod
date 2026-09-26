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
| Curios / Accessories / Trinkets | ⏸ not available for 26.3; our own slots are used | re-check on release |
| 3D Skin Layers, First-person Model, EMF/Fresh Animations, Figura | ⏸ not checked yet (26.3 builds pending) | minimum goal: no crashes |
| Emotecraft | v1.2 | |

## Dev flags
- `-Pcompat_render=true` adds Sodium + Iris to the Fabric dev runtime (Modrinth maven, versions in `gradle.properties`).
- `-Pcompat_jei=true` adds JEI to the Fabric dev runtime.

They are never compile or release dependencies. The JEI API is `compileOnly` in `common`, and only `compat.jei` touches it.
