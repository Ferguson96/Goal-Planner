package com.osrsgoals.data;

import com.osrsgoals.core.Boss;
import com.osrsgoals.core.Names;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bosses bundled in resources/bosses. The list of files is in bosses/_index.txt
 * (one id per line); run `python3 tools/build_index.py` after adding or removing a boss file.
 */
public class BossRepository
{
	private final Map<String, Boss> byId = new LinkedHashMap<>();

	public void load()
	{
		byId.clear();
		for (String id : JsonResources.lines("/bosses/_index.txt"))
		{
			Boss boss = JsonResources.read("/bosses/" + id + ".json", Boss.class);
			if (boss != null && boss.id != null)
			{
				byId.put(boss.id, boss);
			}
		}
	}

	/** All loaded bosses, sorted by name. */
	public List<Boss> all()
	{
		List<Boss> list = new ArrayList<>(byId.values());
		list.sort(Comparator.comparing((Boss b) -> b.name == null ? b.id : b.name));
		return list;
	}

	public Boss get(String id)
	{
		return byId.get(id);
	}

	/** Finds a boss by display name, ignoring case, punctuation and a leading "The". */
	public Boss byName(String name)
	{
		String key = Names.bossKey(name);
		for (Boss boss : byId.values())
		{
			if (Names.bossKey(boss.name).equals(key))
			{
				return boss;
			}
		}
		return null;
	}
}
