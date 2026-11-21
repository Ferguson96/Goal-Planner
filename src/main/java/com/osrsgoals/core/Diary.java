package com.osrsgoals.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One achievement diary region (all four tiers). Loaded from src/main/resources/diaries/&lt;id&gt;.json. */
public class Diary
{
	public String id;
	public String name;
	public String source;
	public String retrieved;
	/** Keys are DiaryTier names: EASY, MEDIUM, HARD, ELITE. */
	public Map<String, List<DiaryTask>> tiers = new LinkedHashMap<>();

	public List<DiaryTask> tasks(DiaryTier tier)
	{
		List<DiaryTask> list = tiers == null ? null : tiers.get(tier.name());
		return list == null ? Collections.<DiaryTask>emptyList() : list;
	}
}
