#!/usr/bin/env python3
"""Checks every file in src/main/resources/methods against docs/method-schema.md.

Usage: python3 -I tools/validate_methods.py [path/to/methods_dir]
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
CATEGORIES = {"TRAINING", "MINIGAME", "QUEST", "MONEY_MAKING"}
WIKI = "https://oldschool.runescape.wiki/"
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def is_num(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def check_method(m, filename, problems, seen_ids):
    where = "%s: %s" % (filename, m.get("id", "<no id>") if isinstance(m, dict) else "<not an object>")

    def bad(msg):
        problems.append("%s - %s" % (where, msg))

    if not isinstance(m, dict):
        bad("method is not an object")
        return
    allowed = {"id", "skill", "name", "category", "minLevel", "maxLevel", "requirements", "rates",
               "source", "retrieved", "notes"}
    for key in m:
        if key not in allowed:
            bad("unknown field '%s'" % key)

    mid = m.get("id")
    if not isinstance(mid, str) or not re.match(r"^[a-z0-9_]+$", mid or ""):
        bad("id must be lower snake case")
    elif mid in seen_ids:
        bad("duplicate id (also in %s)" % seen_ids[mid])
    else:
        seen_ids[mid] = filename

    skill = m.get("skill")
    if skill not in SKILLS:
        bad("skill '%s' is not a RuneLite skill name" % skill)
    elif filename != skill.lower() + ".json":
        bad("skill %s does not belong in %s" % (skill, filename))
    elif isinstance(mid, str) and not mid.startswith(skill.lower() + "_"):
        bad("id should start with '%s_'" % skill.lower())

    if not isinstance(m.get("name"), str) or not m["name"].strip():
        bad("name missing")
    if m.get("category") not in CATEGORIES:
        bad("category must be one of %s" % sorted(CATEGORIES))

    min_level = m.get("minLevel")
    if not isinstance(min_level, int) or isinstance(min_level, bool) or not 1 <= min_level <= 99:
        bad("minLevel must be an integer 1-99")
        min_level = None

    if "maxLevel" in m:
        mx = m["maxLevel"]
        if not isinstance(mx, int) or isinstance(mx, bool) or not 2 <= mx <= 99:
            bad("maxLevel must be an integer 2-99")
        elif min_level is not None and mx <= min_level:
            bad("maxLevel must be above minLevel")

    reqs = m.get("requirements")
    if not isinstance(reqs, list) or not all(isinstance(r, str) and r.strip() for r in reqs):
        bad("requirements must be a list of non-empty strings")

    rates = m.get("rates")
    if not isinstance(rates, list):
        bad("rates must be a list")
        rates = []
    prev = 0
    has_xp = False
    for p in rates:
        if not isinstance(p, dict):
            bad("rate entry is not an object")
            continue
        for key in p:
            if key not in {"level", "afk", "active", "gpPerHour"}:
                bad("unknown rate field '%s'" % key)
        lvl = p.get("level")
        if not isinstance(lvl, int) or isinstance(lvl, bool) or not 1 <= lvl <= 99:
            bad("rate level must be an integer 1-99")
            continue
        if lvl <= prev:
            bad("rate levels must be strictly ascending (level %d)" % lvl)
        prev = lvl
        for key in ("afk", "active"):
            v = p.get(key)
            if v is not None:
                if not is_num(v) or v <= 0:
                    bad("level %d: %s must be a positive number or null" % (lvl, key))
                else:
                    has_xp = True
        g = p.get("gpPerHour")
        if g is not None and not is_num(g):
            bad("level %d: gpPerHour must be a number or null" % lvl)
    if min_level is not None and rates and isinstance(rates[0], dict) and isinstance(rates[0].get("level"), int):
        if rates[0]["level"] < min_level:
            bad("first rate level %d is below minLevel %d" % (rates[0]["level"], min_level))
    if not has_xp:
        # Allowed (method known but rate not found) but must be explained.
        if not (isinstance(m.get("notes"), str) and m["notes"].strip()):
            bad("no XP rates listed, so notes must explain why")

    src = m.get("source")
    if not isinstance(src, str) or not src.startswith(WIKI):
        bad("source must be an %s URL" % WIKI)
    if not isinstance(m.get("retrieved"), str) or not DATE.match(m["retrieved"]):
        bad("retrieved must be yyyy-mm-dd")
    if "notes" in m and not isinstance(m["notes"], str):
        bad("notes must be a string")


def main():
    default = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "methods")
    target = sys.argv[1] if len(sys.argv) > 1 else default
    problems = []
    seen_ids = {}
    total = 0
    # The argument can be a folder (all files in it) or a single .json file.
    if os.path.isfile(target):
        folder = os.path.dirname(os.path.abspath(target))
        files = [os.path.basename(target)]
    else:
        folder = target
        files = sorted(f for f in os.listdir(folder) if f.endswith(".json"))
    for filename in files:
        try:
            with open(os.path.join(folder, filename), encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError) as exc:
            problems.append("%s: cannot read as JSON (%s)" % (filename, exc))
            continue
        if not isinstance(data, list):
            problems.append("%s: top level must be an array" % filename)
            continue
        for m in data:
            total += 1
            check_method(m, filename, problems, seen_ids)
    if problems:
        print("\n".join(problems))
        print("\n%d problem(s) in %d method(s) across %d file(s)" % (len(problems), total, len(files)))
        sys.exit(1)
    print("OK: %d method(s) across %d file(s)" % (total, len(files)))


if __name__ == "__main__":
    main()
