# Changelog

All notable changes to Femboy Mod. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions: `<mod>+<mc>-<loader>`, e.g. `1.0.0+26.3-neoforge`. API changes: `docs/api/CHANGELOG.md`.

## [0.3.0] - 2026-11-10
### Added
- **Reactive ears and tails**: ears turn toward mobs they hear, stand up when alert, flatten at a hissing creeper or when you get hurt, fold underwater and droop at low health or in bed; the tail puffs up when startled, wags near your pet and after a headpat, and twitches after Byte Energy. Every kind of ears hears differently: cat ears notice creepers first and furthest, wolf ears lock onto skeletons, bunny ears hear furthest but react to every mob, fox ears hear better at night, bear ears never flatten. Other players see it too. In first person the ear tips peek in at the top of the screen on the side of what they heard. Client options `reactive_ears_and_tails`, `first_person_ear_tips`; resource packs can tune hearing in `assets/<ns>/femboymod/hearing/`
- **Sock sliding**: in Programming Socks or Fishnet Tights without shoes or boots you slide on polished floors (planks, polished stone, smooth stone, quartz, concrete, glazed terracotta; block tag `#femboymod:sock_slippery`) and keep sliding when you let go. Shoes, carpets and rugs grip, and so does sneaking. A soft swish when you coast, and an advancement: Parquet Drift. Sock Goblins wear socks too, so indoors they slide and are easier to catch (config `mobs.sock_goblin_slide_grip`)
- **Cats knock things off tables**: now and then, while you watch, a cat walks up to a flower pot, candle, plushie, rubber duck or lava lamp at the edge of a table, stares at you, and pushes it off. Nothing breaks: it lands on the floor as items (a potted flower as the pot and the flower). Advancement: Gravity Check. Game rule `femboymod:cat_mischief` (and vanilla mob griefing), config `cats.knock_*`
- **Debug Flytrap**: a swamp plant that catches bugs. It snaps up Bugs, silverfish and endermites that come close, digests with its jaws shut and grows a stage with every meal (three stages, each reaching further). Plant it by your base or put it in a flower pot on your desk. Advancement: Bug Fixed. Entity tag `#femboymod:flytrap_prey`, config `plants.flytrap_*`
- **Touch grass**: after two hours of play (configurable, 0 turns it off) the game gently suggests touching grass, to you only. Right-click grass, or the new Potted Grass, with an empty hand: a short Regeneration, a fresh timer and an advancement. Config `plants.touch_grass_*`
- **Catnip**: grows wild in plains and meadows, can be planted, potted and spread with bone meal. Cats that smell it or eat a sprig get the zoomies, then hunt for things to push off tables, then lie down purring. Feeding catnip to a Stray Cat tames it more easily. Advancement: Zoomies. Config `plants.catnip_*`
- **Headpat consent**: choose who may give you headpats: everyone, teammates (your scoreboard team) or nobody (settings, `headpats_from_players` in `femboymod-client.json`). The server learns your choice when you join and whenever you change it; someone you turn away sees a short note. Pets still love headpats
- **Photo storage limit**: the server keeps at most 64 pictures per player and 2048 in all (config `photos.max_per_player`, `photos.max_total`; 0 = no limit). Past a limit the picture nobody has looked at for the longest fades: the photo item stays, blank, with "The picture has faded" in its tooltip. Photos on display are looked at, so they stay. Pictures printed before this update count toward the total

### Fixed
- Multiplayer hardening (from a multiplayer audit):
  - A client sending photo uploads without paper, or uploads that don't decode, could make the server decode them one after another. Paper and cooldown are now checked first, and a broken upload makes that player wait a second
  - Outfit changes made in one tick (hiding, armor visibility, presets, quick clicks) now go out to the players around as one update at the end of the tick, with one equip sound. Wheel emotes play one at a time
  - Other players no longer receive what's packed inside your worn backpack, only the backpack itself
  - The creative cosmetic slot packet now checks items like vanilla's creative slot packet (no more than a full stack, no negative damage, only items enabled in the world; only in the inventory, not for spectators)
  - Forge: after going through a portal the Lookbook collection and Style Points were empty until something changed; the profile is now resent on dimension change
  - Cats pushing things off tables now respect claim and protection mods on Forge (they ask through its mob griefing event) and report the block as destroyed by the cat
- Dedicated servers: players were disconnected right after joining ("Missing tag ... femboymod:bugs"). The client read our synced data before the server sent it any tags. Single player was not affected. Fabric and Forge
- PvP fairness:
  - Armor hidden under an outfit shows for everyone while its wearer is fighting another player, and for 10 seconds after (config `pvp.reveal_hidden_armor_ticks`, 0 = off)
  - Mirrors only reflect what they can see: nobody behind a wall shows up in a mirror anymore
  - Heart Glasses: teammates glow through walls as before; without a team, other players glow only while you can see them
- The server's settings now reach clients: with set bonuses off on the server (config or `femboymod:set_bonuses`), client-side set effects (glowing mobs, sounds, the Drip HUD tier) turn off too; disabled slots and the Style Coupon price shown in the outfit screen are the server's
- Wardrobe and outfit presets follow the same rules as equipping by hand (disabled slots, addons that refuse an item); cosmetics from other mods' slots don't count in a disabled slot
- Shift-clicking a cosmetic off in the outfit menu now tells addons it came off
- Caffeine ear twitch: players who walk up later see it too, and it stops when the caffeine is gone early (milk, death)
- More than 64 photos in view no longer makes the client ask the server for them over and over; the extra ones show as developing
- Singleplayer and LAN hosts: the headpat cooldown no longer carries over to another world
- The settings screen is a scrolling list like vanilla's option screens: on small windows or with a large GUI scale the last options and the Done button were cut off
- Cosmetics now drop together with the rest of the death loot instead of just before it. A death another mod cancels (revive and "downed" mods) no longer drops your outfit. Forge: grave mods collect your cosmetics with the inventory (they read the same drops). Fabric has no shared drops event, so there grave mods only collect them if they support Femboy Mod; otherwise they land next to the grave as before
- 1.20.1: rose quartz, moonstone, glitter and neon quartz ores were black around their gems, the rose quartz buds and cluster stood in black squares, and the neon signs' plate was flat black: the see-through parts of their textures stay see-through now
- 1.20.1 Fabric: a player who walked back into view showed up without their cosmetics until they changed something

## [0.2.1] - 2026-10-17
### Fixed
- Forge: with Accessories installed, it could draw its default item models over our cosmetics worn in its slots (our "draw nothing" registration ran before our items existed)

## [0.2.0] - 2026-10-16
### Added
- 5 more languages: Polski, Türkçe, 한국어, Italiano, Українська (15 in total)
- **Halloween**: Witch Hat, Pumpkin Hoodie (with a carved face), Ghost Sheet and Candy Pail; the hat, a Halloween top and the pail make the Spooky Season set (hostile mobs nearby glow at night). Candy gives a short sugar rush. **Trick-or-treat**: in the Halloween season (October 20 to November 3; config `halloween.season`: `auto`, `always` or `never`), sneak and right-click a villager with an empty hand while in costume to get candy, once per villager per day
- **Pet outfits**: Pet Bow, Mini Witch Hat, Bell Collar and Pet Bandana for your tamed cats, wolves, parrots and Stray Cats, kittens and puppies included (fitted to their smaller heads). Right-click your pet to put one on, shears take it off; dyeable; a pet drops what it wears when it dies
- **Sock Goblin**: a small night prowler that steals socks you carry (never the ones you wear) and runs. Catch it to get them back, plus a Lost Sock; two Lost Socks make Programming Socks. It never attacks and never despawns while it holds your socks. Game rule `femboymod:sock_goblin_theft`
- **Rubber Duck** mob: waddles on river banks and beaches and floats high on water like the bath toy. Tame it with seeds; it follows you, sneak + right-click makes it sit. Explain your code to it like to the Rubber Duck block (Insight, shared cooldown), and it also fixes one bug: a harmful effect goes away. Config `rubber_ducks`
- Lookbook: a guide book every player gets once on first join (also a button in the Outfit screen, or craft it from a book and pink dye). Every cosmetic by slot, what you have collected, and for each piece its Drip, sets and where to get it: recipe, Thrifter level and price, Wandering Cosplayer, mob drops, Stray Cat gifts. Config `give_lookbook_on_first_join`
- New **feet** slot (over socks, hides boots): Sneakers (faster sprinting), Canvas High-Tops (no slowdown on soul sand and the like), Platform Sneakers (step up full blocks), Bunny Slippers (faster sneaking); all dyeable, crafted or bought from the Thrifter, sakura slippers from the Wandering Cosplayer. Each pair has its own footsteps (squeaky sneakers, clunky platforms, soft slippers)
- Cat Paw Gloves (hands): fluffy paws with pink toe beans, also in first person; cats land on their feet (softer falls)
- **Printed photos**: a Phone selfie with paper in your inventory also prints a Photo item (1 paper, 5 s cooldown). It shows the picture in your hand, in item frames and in inventories, right-click to look at it up close, give it to friends; everyone sees it. Pictures are 128x128, stored with the world (`femboymod/photos`). Config `photos`
- **Mirror** and **Gilded Mirror** (two blocks tall, hang on a wall or stand free): live reflections of nearby players and their tamed pets, outfits and animations included, cut exactly at the glass, over a reflection of the floor; right-click opens the Outfit screen. The room itself is not reflected. Sounds like glass
- **Cozy interior**: Bean Bag (sit to slowly regenerate, like the Gamer Chair), Fluffy Rug (square, round or heart: right-click to switch), Fairy Lights (hang on a wall, warm white, pink or pastel bulbs), Lava Lamp (slowly rising wax, switch on and off), Pet Bed (tamed cats lie and dogs sit on it and slowly heal), Laundry Basket (9 slots for clothes; Sock Goblins raid it before they rob anyone), Mini Fridge (9 slots for drinks and snacks; Byte Energy inside gets chilled and its buffs last 50% longer), Vanity Table (a dressing table with a real mirror: live reflections, opens the Outfit screen). Bean bags, rugs, pet beds and lava lamps come in all 16 dye colors: right-click with a dye, the color stays when you break it. Bean bags, rugs and pet beds count as comfort and the lamps as lighting for the setup rating. Config `furniture.pet_bed_heal_*`, `furniture.fridge_chill_ticks`, `furniture.chilled_bonus`
- **Headpats**: sneak and right-click a player or your tamed pet with an empty hand. Hearts, a soft sound and a short pat animation; the one you pat gets Cozy (slow regeneration for 10 s) and pets heal a little right away. Two advancements. Config `friends.headpat_cooldown_ticks`, `friends.cozy_*`, `friends.pet_headpat_heal`
- The cosmetics panel is 2x6 and the Outfit screen has a taller doll for the 11th slot
- API 0.2.0: `LookbookEntry` data pack registry (`data/<ns>/femboymod/lookbook/<item>.json`) so addon cosmetics can say where to find them
- Femboy Level panel position: auto, or any corner (settings or `femboy_level_panel_corner` in `femboymod-client.json`)
- Curios (Forge), Trinkets (Fabric) and Accessories support: wear cosmetics in their slots; they count for Drip, sets and effects and render as usual. Backpacks stay in the back slot

### Changed
- Figura: cosmetics hide together with the body parts an avatar hides, so avatars with their own model don't get our clothes floating on them
- The creative tab is split in five: Outfits (everything worn, by body part, with ready-made colorways, plus the Lookbook), Cozy Room (furniture and decor, posters), Snacks & Gadgets, Materials, Mobs & Pets (spawn eggs, pet outfits)
- Spawn eggs of the Stray Cat and the Wandering Cosplayer are drawn in the same style as the other mod eggs
- The Bug has its own sounds instead of the silverfish's: it chitters and blips like buggy code, beeps an error when hit and crashes when it dies

### Fixed
- Fabric: the Femboy Level panel now hides with the rest of the HUD (F1)
- Fabric: after joining a world your own cosmetics were invisible to you until you changed something (others always saw them): the join sync reached the client before its player existed. Addon profile values synced to the owner were lost the same way
- The Femboy Level panel no longer covers Xaero's Minimap: with it installed, the panel moves to the bottom right by default
- Fox Ears make you sneak faster on 1.20.1 too: the sneaking speed bonus was ignored, it is now applied like the other newer attributes

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
