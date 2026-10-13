# Changelog

All notable changes to Femboy Mod. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions: `<mod>+<mc>-<loader>`, e.g. `1.0.0+26.3-neoforge`. API changes: `docs/api/CHANGELOG.md`.

## [Unreleased]

## [0.1.1] - 2026-10-09
### Fixed
- Night vision from the moonstone pendant no longer flickers: it is kept well above vanilla's flicker threshold and removed as soon as the cosmetic stops applying it
- Fashion Critic no longer piles up around bases: it spawns only on the surface at night, never within `critic_review.spawn_spacing` blocks (128) of another critic, leaves after `idle_despawn_ticks` (6000) without a fight, and its boss bar is shown only to the player it is after

### Changed
- Photo Mode selfies look like a real instant photo: rounded, anti-aliased corners on the card and the photo, a soft inner shade around the shot and a soft shadow under the card (transparent PNG)
- Empty cosmetic slot icons are redrawn in vanilla's style (16x16, one dark grey, 1 px lines), so they match the armor slots and stay sharp at every GUI scale

## [0.1.0] - 2026-09-27
### Added
- 9 cosmetic slots (head accessory, face, neck, back, tail, legs overlay, outfit top, outfit bottom, hands) with a Cosmetics screen and key binding
- Cat ears, tail, programming socks, fishnet tights, pleated skirt, oversized hoodie, UwU choker, 10 hair clips
- Colorways: dyeing in a crafting grid, pride patterns from the Thrifter
- Drip Level, set bonuses (Full Femboy Mode), HUD
- Backpacks (canvas, leather, netherite) with charms; upgrades keep the contents
- "Femboy Level" HUD panel in the top-left corner: level, tier name, bar with tier notches, distance to the next tier, active sets (on by default, client option)
- Ores: rose quartz (mountains and cave biomes, stone + deepslate) and glitter (flower biomes); Block of Rose Quartz
- Rose quartz jewelry: earrings (luck) and bracelet (mining speed), set "Crystal Clear" (Haste)
- Translations: German, French, Brazilian Portuguese, Dutch, Swedish, Japanese and Simplified Chinese (plus English, Russian, Spanish)
- Mod icon and a NeoForge mod list banner
- Glitter colorway: any dyeable cosmetic + 2 glitter gets a slowly shimmering pastel colorway
- Gamer corner: Gamer Chair (sit to slowly regenerate), Pink Monitor (scrolling code, toggles on/off), Clacky Keyboard, LED Strip (4 colors + slow rainbow, server option to disable the rainbow)
- 8 posters (paintings 1x1, 1x2, 2x2), also in the random painting pool
- Setup rating: up to 5 stars for a monitor, keyboard, LED strip, plush and poster around a Gamer Chair; more stars heal faster
- Player profile (stats, collection, addon data), Style Points earned for style and traded for Style Coupons (Thrifter trades), collection counter in the Outfit screen
- Armor no longer covers the outfit: pieces under worn cosmetics are hidden (still protect), per-piece auto/hide/show toggles in the Outfit screen, elytra always visible
- Game rules: `femboymod:allow_hidden_armor`, `femboymod:style_points`, `femboymod:set_bonuses`, `femboymod:keep_cosmetics`, `femboymod:drip_pvp_percent`
- v1.1 "Gamer Room": Phone (Photo Mode selfies as instant prints with the mod watermark), food (bubble tea with 7 surprise flavors from data packs, strawberry milk that cures the caffeine jitters, mochi, onigiri), Terminal (crafting table that says "btw", advancement), Cat-ear Headphones (muffle creepers, RGB near a playing jukebox), Heart Glasses (friends glow), Arm Warmers, Crop Sweater, Belt Chains (new Waist slot), Nail Polish (also in first person), Pride Badges (charms with a flag, crafted from dyes), 20 posters in total
- Dark Shades (chunky black sunglasses; the Fashion Critic is always impressed, `#femboymod:critic_approved`), a fluffier fox-style tail, headphones in pink by default plus black and white presets
- Moonstone (deep deepslate ore that glows faintly): Moonstone Pendant (night vision at night), the shimmering Pearl colorway, Moonstone Night Light
- Neon Quartz (glowing Nether ore): Neon Signs (heart / cat / "btw", click to switch), Cyber Visor (hostiles glow), the shimmering Neon colorway
- Rose quartz geodes: budding rose quartz grows buds into clusters like amethyst
- v1.2 "Friends": Stray Cat (vanilla-style tameable cat in 5 pastel coats that also brings mod gifts in the morning), Wandering Cosplayer (rare trader with exclusive colorways Sakura / Starlight / Cyber Pastel / Witchy, paid with Style Coupons), emotes on G (wave, peace, heart hands; everyone nearby sees them), Vibe Check Scanner (0..100 vibe, server leaderboard, daily Style Points, advancement "Immaculate Vibes")
- Rubber Duck: explain your code to it for a meme answer, Insight (faster digging) and a bit of XP; counts as a setup star
- Advancements: Rubber Duck Debugging, Battlestation, Just Need a Break, Can't Decide What to Wear, Ready to Deploy
- Byte Energy drinks with caffeine and jitter
- UwU choker chat transform (EN, RU, ES)
- Pink Creeper, Thrifter villager, Clothing Rack, Wardrobe with outfit presets, plushies
- Advancements, config screen (Mod Menu / NeoForge mods list)
- Cat Ear Hoodie; fox, bunny, bear and wolf ears; Striped Mittens; ready-made colorways in the creative tab
- Hostile meme mobs: Bugs, Caffeinated Zombie, Hissy Cat and the Fashion Critic mini-boss; Drip reduces the damage they deal
- Compat: JEI info pages; checked with Sodium and Iris
- Public API for addons (`femboymod-api`)
