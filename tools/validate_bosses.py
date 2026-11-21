#!/usr/bin/env python3
"""Checks every file in src/main/resources/bosses against docs/boss-schema.md.

Usage: python3 -I tools/validate_bosses.py [path/to/bosses_dir_or_file]
Exits 1 and prints every problem if anything is wrong.
"""
import json
import os
import re
import sys

SKILLS = {
    "ATTACK", "DEFENCE", "STRENGTH", "HITPOINTS", "RANGED", "PRAYER", "MAGIC", "COOKING",
    "WOODCUTTING", "FLETCHING", "FISHING", "FIREMAKING", "CRAFTING", "SMITHING", "MINING",
    "HERBLORE", "AGILITY", "THIEVING", "SLAYER", "FARMING", "RUNECRAFT", "HUNTER",
    "CONSTRUCTION", "SAILING",
}
CATEGORIES = {"BOSS", "SLAYER_BOSS", "RAID", "WILDERNESS_BOSS", "MINIGAME_BOSS", "SKILLING_BOSS"}
SLOTS = {"HEAD", "CAPE", "NECK", "AMMO", "WEAPON", "BODY", "SHIELD", "LEGS", "HANDS", "FEET", "RING"}
WIKI = "https://oldschool.runescape.wiki/"
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
RARITY = re.compile(r"^(\d+)/(\d+)$")


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def is_str(v):
    return isinstance(v, str) and v.strip() != ""


def str_list(v):
    return isinstance(v, list) and all(is_str(x) for x in v)


def check_boss(b, filename, problems, seen_ids):
    where = "%s" % filename

    def bad(msg):
        problems.append("%s - %s" % (where, msg))

    if not isinstance(b, dict):
        bad("file must hold one object")
        return
    allowed = {"id", "name", "category", "source", "retrieved", "requirements", "killsPerHour",
               "killsPerHourSource", "drops", "setups", "notes"}
    for key in b:
        if key not in allowed:
            bad("unknown field '%s'" % key)

    bid = b.get("id")
    if not isinstance(bid, str) or not re.match(r"^[a-z0-9_]+$", bid or ""):
        bad("id must be lower snake case")
    else:
        if filename != bid + ".json":
            bad("file name must be %s.json" % bid)
        if bid in seen_ids:
            bad("duplicate id (also in %s)" % seen_ids[bid])
        seen_ids[bid] = filename
    if not is_str(b.get("name")):
        bad("name missing")
    if b.get("category") not in CATEGORIES:
        bad("category must be one of %s" % sorted(CATEGORIES))
    if not isinstance(b.get("source"), str) or not b["source"].startswith(WIKI):
        bad("source must be an %s URL" % WIKI)
    if not isinstance(b.get("retrieved"), str) or not DATE.match(b["retrieved"]):
        bad("retrieved must be yyyy-mm-dd")

    req = b.get("requirements")
    if not isinstance(req, dict):
        bad("requirements must be an object")
    else:
        for key in req:
            if key not in {"skills", "quests", "questsStarted", "other"}:
                bad("requirements: unknown field '%s'" % key)
        skills = req.get("skills")
        if not isinstance(skills, list):
            bad("requirements.skills must be a list")
        else:
            for s in skills:
                if not isinstance(s, dict) or set(s) - {"skill", "level", "boostable"}:
                    bad("requirements.skills entry must have only skill, level, boostable")
                    continue
                if s.get("skill") not in SKILLS:
                    bad("requirement skill '%s' is not a RuneLite skill name" % s.get("skill"))
                if not is_int(s.get("level")) or not 1 <= s["level"] <= 99:
                    bad("requirement level must be 1-99")
                if not isinstance(s.get("boostable"), bool):
                    bad("requirement boostable must be true or false")
        for key in ("quests", "questsStarted", "other"):
            if not str_list(req.get(key)):
                bad("requirements.%s must be a list of non-empty strings" % key)

    kph = b.get("killsPerHour")
    if kph is not None and (isinstance(kph, bool) or not isinstance(kph, (int, float)) or kph <= 0):
        bad("killsPerHour must be a positive number or null")
    ksrc = b.get("killsPerHourSource")
    if kph is not None and not (isinstance(ksrc, str) and ksrc.startswith(WIKI)):
        bad("killsPerHour needs killsPerHourSource (a wiki URL)")
    if kph is None and ksrc is not None:
        bad("killsPerHourSource must be null when killsPerHour is null")

    drops = b.get("drops")
    if not isinstance(drops, list):
        bad("drops must be a list")
        drops = []
    seen_items = set()
    for d in drops:
        if not isinstance(d, dict) or set(d) - {"item", "rarity", "chance", "notes"}:
            bad("drop entries may only have item, rarity, chance, notes")
            continue
        if not is_str(d.get("item")):
            bad("drop item missing")
            continue
        label = d["item"]
        if label in seen_items:
            bad("drop '%s' listed twice" % label)
        seen_items.add(label)
        m = RARITY.match(d.get("rarity") if isinstance(d.get("rarity"), str) else "")
        if not m:
            bad("drop '%s': rarity must look like 1/512 (digits only)" % label)
            continue
        num, den = int(m.group(1)), int(m.group(2))
        if den == 0 or num == 0 or num > den:
            bad("drop '%s': rarity %s is not a valid chance" % (label, d["rarity"]))
            continue
        chance = d.get("chance")
        if isinstance(chance, bool) or not isinstance(chance, (int, float)):
            bad("drop '%s': chance must be a number" % label)
        elif abs(chance - num / den) > 1e-9 + 1e-6 * (num / den):
            bad("drop '%s': chance %s does not match rarity %s" % (label, chance, d["rarity"]))
        if "notes" in d and not isinstance(d["notes"], str):
            bad("drop '%s': notes must be a string" % label)

    setups = b.get("setups")
    if not isinstance(setups, list) or len(setups) > 3:
        bad("setups must be a list of at most 3")
        setups = []
    for s in setups:
        if not isinstance(s, dict) or set(s) - {"name", "source", "equipment", "inventory", "notes"}:
            bad("setup entries may only have name, source, equipment, inventory, notes")
            continue
        if not is_str(s.get("name")):
            bad("setup name missing")
        if not isinstance(s.get("source"), str) or not s["source"].startswith(WIKI):
            bad("setup source must be an %s URL" % WIKI)
        eq = s.get("equipment")
        if not isinstance(eq, dict):
            bad("setup equipment must be an object")
        else:
            for slot, items in eq.items():
                if slot not in SLOTS:
                    bad("setup equipment: unknown slot '%s'" % slot)
                if not str_list(items) or not items:
                    bad("setup equipment %s must be a non-empty list of item names" % slot)
        inv = s.get("inventory")
        if not isinstance(inv, list):
            bad("setup inventory must be a list")
        else:
            for it in inv:
                if (not isinstance(it, dict) or set(it) - {"item", "quantity"} or not is_str(it.get("item"))
                        or not is_int(it.get("quantity")) or it["quantity"] < 1):
                    bad("inventory entries need item (text) and quantity (positive integer)")
        if "notes" in s and not isinstance(s["notes"], str):
            bad("setup notes must be a string")
    if "notes" in b and not isinstance(b["notes"], str):
        bad("notes must be a string")


def main():
    default = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "bosses")
    target = sys.argv[1] if len(sys.argv) > 1 else default
    if os.path.isdir(target):
        files = sorted(os.path.join(target, f) for f in os.listdir(target) if f.endswith(".json"))
    else:
        files = [target]
    problems = []
    seen_ids = {}
    for path in files:
        name = os.path.basename(path)
        try:
            with open(path, encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError) as exc:
            problems.append("%s - cannot read as JSON: %s" % (name, exc))
            continue
        check_boss(data, name, problems, seen_ids)
    if problems:
        print("\n".join(problems))
        print("%d problem(s)" % len(problems))
        sys.exit(1)
    print("OK: %d boss file(s)" % len(files))


if __name__ == "__main__":
    main()
