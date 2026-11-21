#!/usr/bin/env python3
"""Checks every file in src/main/resources/diaries against docs/diary-schema.md.

Usage: python3 -I tools/validate_diaries.py [path/to/diaries_dir_or_file]
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
    "CONSTRUCTION",
}
REGIONS = {
    "ardougne", "desert", "falador", "fremennik", "kandarin", "karamja", "kourend_kebos",
    "lumbridge_draynor", "morytania", "varrock", "western_provinces", "wilderness",
}
TIERS = ["EASY", "MEDIUM", "HARD", "ELITE"]
WIKI = "https://oldschool.runescape.wiki/"
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def str_list(v):
    return isinstance(v, list) and all(isinstance(x, str) and x.strip() for x in v)


def check_file(data, filename, problems, seen_ids):
    def bad(where, msg):
        problems.append("%s: %s - %s" % (filename, where, msg))

    if not isinstance(data, dict):
        bad("file", "top level must be an object")
        return 0
    allowed = {"id", "name", "source", "retrieved", "tiers"}
    for key in data:
        if key not in allowed:
            bad("file", "unknown field '%s'" % key)
    rid = data.get("id")
    if rid not in REGIONS:
        bad("id", "'%s' is not one of the 12 region ids" % rid)
    elif filename != rid + ".json":
        bad("id", "file must be named %s.json" % rid)
    if not isinstance(data.get("name"), str) or not data["name"].strip():
        bad("name", "missing")
    if not isinstance(data.get("source"), str) or not data["source"].startswith(WIKI):
        bad("source", "must be an %s URL" % WIKI)
    if not isinstance(data.get("retrieved"), str) or not DATE.match(data["retrieved"]):
        bad("retrieved", "must be yyyy-mm-dd")

    tiers = data.get("tiers")
    if not isinstance(tiers, dict) or set(tiers.keys()) != set(TIERS):
        bad("tiers", "must be an object with exactly the keys %s" % TIERS)
        return 0

    count = 0
    for tier in TIERS:
        tasks = tiers[tier]
        if not isinstance(tasks, list) or not tasks:
            bad(tier, "must be a non-empty list of tasks")
            continue
        for i, t in enumerate(tasks, 1):
            count += 1
            where = "%s #%d" % (tier, i)
            if not isinstance(t, dict):
                bad(where, "task is not an object")
                continue
            for key in t:
                if key not in {"id", "description", "skills", "quests", "questsStarted", "other", "notes"}:
                    bad(where, "unknown field '%s'" % key)
            tid = t.get("id")
            expected = "%s_%s_%d" % (rid, tier.lower(), i)
            if tid != expected:
                bad(where, "id should be '%s' (got '%s')" % (expected, tid))
            if tid in seen_ids:
                bad(where, "duplicate id (also in %s)" % seen_ids[tid])
            else:
                seen_ids[tid] = filename
            if not isinstance(t.get("description"), str) or not t["description"].strip():
                bad(where, "description missing")
            skills = t.get("skills")
            if not isinstance(skills, list):
                bad(where, "skills must be a list")
                skills = []
            seen_skills = set()
            for s in skills:
                if not isinstance(s, dict) or set(s.keys()) != {"skill", "level", "boostable"}:
                    bad(where, "each skill needs exactly skill, level, boostable")
                    continue
                if s["skill"] not in SKILLS:
                    bad(where, "unknown skill '%s'" % s["skill"])
                elif s["skill"] in seen_skills:
                    bad(where, "skill %s listed twice (use the higher level once)" % s["skill"])
                seen_skills.add(s["skill"])
                if not is_int(s["level"]) or not 1 <= s["level"] <= 99:
                    bad(where, "level must be an integer 1-99")
                if not isinstance(s["boostable"], bool):
                    bad(where, "boostable must be true or false")
            for key in ("quests", "questsStarted", "other"):
                if not str_list(t.get(key)):
                    bad(where, "%s must be a list of non-empty strings" % key)
            if "notes" in t and not isinstance(t["notes"], str):
                bad(where, "notes must be a string")
    return count


def main():
    default = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "diaries")
    target = sys.argv[1] if len(sys.argv) > 1 else default
    problems = []
    seen_ids = {}
    total = 0
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
        total += check_file(data, filename, problems, seen_ids)
    if problems:
        print("\n".join(problems))
        print("\n%d problem(s) in %d task(s) across %d file(s)" % (len(problems), total, len(files)))
        sys.exit(1)
    print("OK: %d task(s) across %d file(s)" % (total, len(files)))


if __name__ == "__main__":
    main()
