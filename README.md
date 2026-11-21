# Goal Planner (RuneLite plugin)

A RuneLite sidebar plugin for Old School RuneScape that turns goals into a plan:

- **Skills**: pick a skill and target level (e.g. Fishing 78 -> 82) and see every training method
  bundled from the OSRS Wiki, ranked by time, with XP/hr, total hours and GP gained or lost. Choose
  Best, AFK or Active playstyle and sort by fastest or most profit. "Plan" chains the fastest method
  at each level.
- **Diaries**: all 12 Achievement Diaries, every tier. Tick the tasks you've done and get a merged
  list of the levels and quests you still need, with a button to open each skill's plan.
- **Bosses**: 62 bosses, raids and minigames with requirements, rare drops and pets, drop-chance maths
  (chance within N kills, kills needed for 90%), and the Wiki's recommended gear and inventory with a
  checklist of what you own and what's missing (toggle "Show recommended gear and inventory").
- **Goals and wheel**: save skill, diary, kill-count and drop goals, see progress and estimated time,
  set priorities 1-5, and "Spin the wheel" to pick what to do next (equal chance, by priority, or
  favour quickest).

## First run (step by step)

1. Install **IntelliJ IDEA Community** and a **JDK 11** (IntelliJ can download one:
   File > Project Structure > SDK > Add SDK > Download JDK > version 11).
2. Unzip this project and open the folder in IntelliJ (File > Open). Click **Trust Project**.
3. Wait for the Gradle sync to finish (progress bar bottom right). It downloads RuneLite itself.
4. Open `src/test/java/com/osrsgoals/GoalPlannerPluginTest.java` and click the green arrow next to
   `main`. A developer copy of RuneLite opens with the plugin loaded.
5. Log in, open Goal Planner in the right sidebar, and press **Refresh from game**.

Your normal RuneLite install is not touched. Close that RuneLite window to stop.

## What it reads from the game

- Your XP in every skill and your quest states (on login, on level-ups and about once a minute).
- Your inventory, worn gear and bank (open the bank once so it can see it) for the gear checklist.
- Kill counts from chat messages like "Your Zulrah kill count is: 12." They update from then on.
- Grand Exchange prices from RuneLite, for the cost of missing gear.

Diary tasks are ticked by you in the Diaries view (RuneLite doesn't expose per-task completion).
Everything you save is stored in your RuneLite profile.

## Where the numbers come from

All data is bundled from the [OSRS Wiki](https://oldschool.runescape.wiki/) (CC BY-NC-SA 3.0),
read in October 2026, in `src/main/resources/`:

| Folder | Contents |
| --- | --- |
| `methods/` | 504 training methods across all 24 skills (XP/hr at listed levels, GP/hr where given) |
| `diaries/` | 489 tasks in the 12 diaries, with skill, quest and item requirements |
| `bosses/` | 62 bosses: requirements, rare drops and pets; 26 have 1-3 Wiki gear setups |

Each file names the Wiki page it came from. Where the Wiki gives a range (e.g. 50,000-56,000 XP/hr)
the lower number is used and the notes say so. The calculator interpolates between listed levels
and never extrapolates. `docs/` describes each file format and `tools/validate_*.py` checks them
(run `python3 tools/build_index.py` after adding a boss file).

## Known limits

- **Not yet compiled against real RuneLite.** Logic and data pass 23 unit tests, but the
  RuneLite-facing code was only checked against stand-ins. If step 4 shows errors, send them back.
- **Wiki text was read through a summariser**, so task wording is paraphrased, and some long pages
  were cut off. Gaps are written in each entry's notes. The main ones:
  - Every skill has hourly rates to 99 except Hitpoints, Slayer, Farming and Magic (left as-is on
    purpose). Three skills start with a short quest-only stretch the calculator can't time:
    Herblore 1-3 (Druidic Ritual), Hunter 1-9 (Natural History Quiz), Sailing 1-15 (early Sailing quests).
  - Some methods are listed without hourly rates (Guardians of the Rift, Farming runs, minnows), so
    they show no time estimate; other methods cover those levels.
  - A few rates are missing (Vorki, the Hueycoatl pet, Duke's Eye and Virtus).
  - Raid uniques depend on points: Chambers of Xeric uses the Wiki's 26,025-point example, Theatre
    of Blood is per team raid, and Tombs of Amascut figures are "given a unique" only.
- Diary requirements count as boostable unless the Wiki marks them non-boostable (the Wiki's rule).
- Kills per hour is mostly blank (the Wiki rarely states one); enter your own in the Bosses view.

## Project layout

- `core/` - pure logic: XP table, calculator, planner, requirement gaps, drop maths, gear checklist, wheel.
- `data/` - loads the JSON, saves your goals (`GoalStore`) and works out goal progress (`GoalEvaluator`).
- `ui/` - the sidebar panel and its four views.
- `GoalPlannerPlugin.java` - the bridge to RuneLite (XP, quests, items, prices, chat kill counts).
