# Achievement diary files

One JSON file per diary region in `src/main/resources/diaries/`, named after the `id`
(`fremennik.json`, `kourend_kebos.json`, ...). Each file is ONE object (not an array).

```json
{
  "id": "fremennik",
  "name": "Fremennik Diary",
  "source": "https://oldschool.runescape.wiki/w/Fremennik_Diary",
  "retrieved": "2026-10-17",
  "tiers": {
    "EASY": [
      {
        "id": "fremennik_easy_1",
        "description": "Catch a fish from the Rellekka docks.",
        "skills": [ { "skill": "FISHING", "level": 20, "boostable": true } ],
        "quests": ["The Fremennik Trials"],
        "questsStarted": [],
        "other": ["Fly fishing rod"],
        "notes": ""
      }
    ],
    "MEDIUM": [],
    "HARD": [],
    "ELITE": []
  }
}
```

## Fields

| Field | Meaning |
| --- | --- |
| `id` | Lower snake case region id. The 12 regions: `ardougne`, `desert`, `falador`, `fremennik`, `kandarin`, `karamja`, `kourend_kebos`, `lumbridge_draynor`, `morytania`, `varrock`, `western_provinces`, `wilderness`. |
| `name` | Display name as on the wiki, e.g. "Kourend & Kebos Diary". |
| `source` | URL of the wiki diary page. Must start with `https://oldschool.runescape.wiki/`. |
| `retrieved` | Date the page was read, `yyyy-mm-dd`. |
| `tiers` | Object with exactly the keys `EASY`, `MEDIUM`, `HARD`, `ELITE`, each a list of tasks in wiki order. |
| `tasks[].id` | `<id>_<tier lower case>_<n>`, n counting from 1 in wiki order. Unique across all files. |
| `tasks[].description` | The task text, in the wiki's wording, shortened only if it is very long. |
| `tasks[].skills` | Skill requirements: `skill` (RuneLite name in capitals: ATTACK, DEFENCE, STRENGTH, HITPOINTS, RANGED, PRAYER, MAGIC, COOKING, WOODCUTTING, FLETCHING, FISHING, FIREMAKING, CRAFTING, SMITHING, MINING, HERBLORE, AGILITY, THIEVING, SLAYER, FARMING, RUNECRAFT, HUNTER, CONSTRUCTION), `level` 1-99, `boostable` is true unless the wiki tags the requirement `[nb]` (the wiki says temporary boosts are allowed unless stated otherwise; `[b]` and untagged both mean boostable). |
| `tasks[].quests` | Quests that must be COMPLETED, using the wiki's quest name exactly. |
| `tasks[].questsStarted` | Quests that only need to be STARTED (the wiki says "partial completion" or "started"). |
| `tasks[].other` | Everything else, as short plain-text phrases: items, combat level, kill counts, favour, miniquests, other diaries. |
| `tasks[].notes` | Optional caveat; empty string if none. |

## Rules for filling data

1. Everything comes from the wiki page that was actually read. If a requirement is not listed, leave it out. Never guess or recall.
2. Every task on the wiki page gets an entry, including tasks with no requirements (use empty lists).
3. A task that lists the same skill twice (e.g. a normal and a boosted level) is ONE entry for the higher level, with a note.
4. Miniquests are not quests; put them in `other`.
5. Requirements that are another achievement diary tier go in `other`.
6. Do not copy the page's prose; write only the fields above.
