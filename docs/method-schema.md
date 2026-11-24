# Training method files

One JSON file per skill in `src/main/resources/methods/`, named after the skill in lower case
(`fishing.json`, `runecraft.json`, ...). Each file is an array of methods.

```json
{
  "id": "fishing_leechfin",
  "skill": "FISHING",
  "name": "Leechfin fishing",
  "category": "TRAINING",
  "minLevel": 72,
  "requirements": ["The Blood Moon Rises", "Big fishing net"],
  "rates": [
    { "level": 78, "afk": null, "active": 109000, "gpPerHour": null },
    { "level": 99, "afk": null, "active": 130000, "gpPerHour": null }
  ],
  "source": "https://oldschool.runescape.wiki/w/Pay-to-play_Fishing_training",
  "retrieved": "2026-10-16",
  "notes": "Angler's outfit assumed."
}
```

## Fields

| Field | Meaning |
| --- | --- |
| `id` | Unique across all files, `<skill>_<method>` in lower snake case. |
| `skill` | RuneLite `Skill` enum name in capitals: ATTACK, DEFENCE, STRENGTH, HITPOINTS, RANGED, PRAYER, MAGIC, COOKING, WOODCUTTING, FLETCHING, FISHING, FIREMAKING, CRAFTING, SMITHING, MINING, HERBLORE, AGILITY, THIEVING, SLAYER, FARMING, RUNECRAFT, HUNTER, CONSTRUCTION, SAILING. |
| `name` | Short display name. Put the variant in the name (e.g. "(dragon harpoon)"). |
| `category` | `TRAINING`, `MINIGAME`, `QUEST`, `MONEY_MAKING`. |
| `minLevel` | The lowest level in this skill at which the method can be used. Must not be below the first `rates` level unless the wiki says it can be used earlier at that rate. |
| `maxLevel` | Optional. For methods the wiki only lists up to a certain level (e.g. a ladder of items for levels 1-40, `maxLevel` 40): the method is not used for levels at or above this number. Leave it out if the method works to 99. |
| `requirements` | Plain-text list: quests, other skill levels, items. Short phrases. |
| `rates` | XP per hour in THIS skill, at specific levels, in ascending level order. The calculator interpolates between them and never extrapolates past the last. |
| `rates[].afk` | XP/hr when played low-attention. `null` if the wiki doesn't list it. |
| `rates[].active` | XP/hr when played actively / optimally (e.g. 3-tick). `null` if not listed. |
| `rates[].gpPerHour` | GP gained (positive) or spent (negative) per hour, from the wiki figure. `null` if not listed. |
| `source` | URL of the wiki page the numbers came from. Must start with `https://oldschool.runescape.wiki/`. |
| `retrieved` | Date the page was read, `yyyy-mm-dd`. |
| `xpEach` | Optional. XP in this skill for one action (one arrow, one lap, one potion), from the wiki's item or course page. Leave it out when the XP per action changes with level or the method mixes items. |
| `unit` | Required with `xpEach`: what one action is called, in the plural, e.g. `broad arrows`, `laps`, `potions`. |
| `notes` | Short caveats: XP in other skills, assumptions, ranges where a lower bound was used. |

## Rules for filling data

1. Numbers come only from a wiki page that was actually read. Never estimate or recall a rate. If the
   wiki doesn't list a number, use `null`; do not leave out the method if its existence is certain.
2. A table with several columns (e.g. AFK vs 3-tick) maps to `afk` and `active`.
   A single unlabelled figure goes in `active` only, unless the page calls the method AFK.
3. If the wiki gives a range (e.g. 34,000-38,000), use the lower bound and say so in `notes`.
4. XP given in other skills (Hunter, Agility, Cooking) is noted in `notes`; only this skill's XP goes in `rates`.
5. Variants that change the rate (different tool, outfit, boost) are separate methods.
6. GP figures are snapshots at the wiki's prices on `retrieved`; do not combine them with other prices.
