#!/usr/bin/env python3
"""Writes src/main/resources/bosses/_index.txt, the list of boss files the plugin loads.

Usage: python3 -I tools/build_index.py
Run it again whenever a boss file is added or removed.
"""
import os

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "bosses")
ids = sorted(name[:-5] for name in os.listdir(root) if name.endswith(".json"))
with open(os.path.join(root, "_index.txt"), "w", encoding="utf-8") as out:
    out.write("\n".join(ids) + ("\n" if ids else ""))
print("indexed %d boss file(s)" % len(ids))
