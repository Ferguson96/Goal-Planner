# Boss files

One JSON file per boss in `src/main/resources/bosses/`, named after the `id` (`zulrah.json`,
`general_graardor.json`, ...). Each file is ONE object (not an array).

```json
{
  "id": "zulrah",
  "name": "Zulrah",
  "category": "BOSS",
  "source": "https://oldschool.runescape.wiki/w/Zulrah",
  "retrieved": "2026-10-17",
  "requirements": {
    "skills": [ { "skill": "MAGIC", "level": 75, "boostable": true } ],
    "quests": ["Regicide"],
    "questsStarted": [],
    "other": ["Zul-Andra teleport"]
  },
  "killsPerHour": null,
  "killsPerHourSource": null,
  "drops": [
    { "item": "Tanzanite fang", "rarity": "1/512", "chance": 0.001953125, "notes": "" }
  ],
  "setups": [
    {
      "name": "Mid-level",
      "source": "https://oldschool.runescape.wiki/w/Zulrah/Strategies",
      "equipment": {
        "HEAD": ["Slayer helmet (i)"],
        "WEAPON": ["Trident of the swamp", "Sanguinesti staff"]
      },
      "inventory": [ { "item": "Prayer potion(4)", "quantity": 4 } ],
      "notes": ""
    }
  ],
  "notes": ""
}
```

## Fields

| Field | Meaning |
| --- | --- |
| `id` | Lower snake case, matches the file name. Unique across all files. |
| `name` | Display name as on the wiki. |
| `category` | `BOSS`, `SLAYER_BOSS`, `RAID`, `WILDERNESS_BOSS`, `MINIGAME_BOSS`, `SKILLING_BOSS`. |
| `source` | Wiki page of the boss. Must start with `https://oldschool.runescape.wiki/`. |
| `retrieved` | Date the pages were read, `yyyy-mm-dd`. |
| `requirements` | Same meaning as diary tasks: `skills` (RuneLite skill name in capitals, level 1-99, `boostable` true ONLY if the wiki says so), `quests` that must be completed (wiki names exactly), `questsStarted`, and `other` (plain-text phrases: items, combat level, kill counts, unlocks). Only what the wiki lists as needed to fight or access the boss. |
| `killsPerHour` | A kills-per-hour number ONLY if a wiki page states one (e.g. a money making guide or the boss page). Otherwise `null`. The plugin lets the player enter their own. |
| `killsPerHourSource` | URL of the wiki page that number came from, or `null`. |
| `drops` | Uniques, pets and other rare drops: every drop the wiki lists rarer than 1/50, plus the pet. Skip common drops and "Always" drops. |
| `drops[].item` | Item name as on the wiki. |
| `drops[].rarity` | The wiki's fraction as `n/d`, digits only with no commas (wiki "1/1,500" is written `1/1500`). If the wiki gives several rates for the same item (e.g. different modes), use the base rate and explain in `notes`. |
| `drops[].chance` | `n / d` as a decimal number. Must match `rarity`. |
| `drops[].notes` | Short caveat or empty string. |
| `setups` | 0-3 recommended setups from the wiki's recommended-equipment / strategy section (e.g. budget, mid, best). Empty list if the wiki gives none. |
| `setups[].name` | The wiki's own label (e.g. "Budget", "Mid-level", "Max"), or "Recommended" if it has just one. |
| `setups[].source` | URL of the page the setup came from. |
| `setups[].equipment` | Slot name (`HEAD`, `CAPE`, `NECK`, `AMMO`, `WEAPON`, `BODY`, `SHIELD`, `LEGS`, `HANDS`, `FEET`, `RING`) to a list of item names. First is the wiki's best choice, the rest are its listed alternatives. Leave out slots the wiki leaves empty. |
| `setups[].inventory` | Items with `item` (wiki name) and `quantity` (positive integer). |
| `setups[].notes` | Short caveat or empty string. |
| `notes` | Short boss-level caveats, or empty string. |

## Rules for filling data

1. Everything comes from a wiki page that was actually read. If something is not listed, leave it out
   or use `null`. Never guess or recall rates, items or numbers.
2. Do not copy prose or strategy text; only the fields above.
3. A boss with several modes or variants (e.g. a hard mode, a different wiki page per boss) is one file
   per wiki boss page unless the wiki treats them as one fight.
