package com.osrsgoals.data;

import com.google.gson.Gson;
import com.osrsgoals.core.Diary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads the achievement diaries bundled in resources/diaries (one file per region). */
public class DiaryRepository
{
	/** The 12 regions; a file named <id>.json is loaded for each if it exists. */
	public static final String[] IDS = {
		"ardougne", "desert", "falador", "fremennik", "kandarin", "karamja", "kourend_kebos",
		"lumbridge_draynor", "morytania", "varrock", "western_provinces", "wilderness"
	};

	private final Map<String, Diary> byId = new LinkedHashMap<>();

	public void load(Gson gson)
	{
		byId.clear();
		for (String id : IDS)
		{
			Diary diary = JsonResources.read(gson, "/diaries/" + id + ".json", Diary.class);
			if (diary != null && diary.id != null)
			{
				byId.put(diary.id, diary);
			}
		}
	}

	/** All loaded diaries, sorted by name. */
	public List<Diary> all()
	{
		List<Diary> list = new ArrayList<>(byId.values());
		list.sort(Comparator.comparing((Diary d) -> d.name == null ? d.id : d.name));
		return list;
	}

	public Diary get(String id)
	{
		return byId.get(id);
	}
}
