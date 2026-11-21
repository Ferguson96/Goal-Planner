package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Merges the requirements of several tasks (diary tasks, a boss) into one list of what you still need:
 * for each skill the highest level asked for, plus the quests and other items.
 */
public final class GapSummary
{
	/** Looks up your current level in a skill; negative if unknown (not logged in). */
	public interface Levels
	{
		int level(String skill);
	}

	/** Looks up a quest's state by name. */
	public interface Quests
	{
		QuestProgress state(String quest);
	}

	public static final class SkillGap
	{
		public final String skill;
		public final int required;
		public final int current;
		/** True only if every task that asks for this level says it can be boosted. */
		public final boolean boostable;
		/** The first task that needs this level. */
		public final String driver;
		/** How many other tasks need the same level. */
		public final int alsoNeededBy;

		SkillGap(String skill, int required, int current, boolean boostable, String driver, int alsoNeededBy)
		{
			this.skill = skill;
			this.required = required;
			this.current = current;
			this.boostable = boostable;
			this.driver = driver;
			this.alsoNeededBy = alsoNeededBy;
		}

		public boolean known()
		{
			return current >= 0;
		}

		public boolean met()
		{
			return known() && current >= required;
		}
	}

	public static final class QuestGap
	{
		public final String name;
		public final QuestProgress state;
		/** True if the quest has to be finished, false if starting it is enough. */
		public final boolean mustFinish;
		public final String driver;

		QuestGap(String name, QuestProgress state, boolean mustFinish, String driver)
		{
			this.name = name;
			this.state = state;
			this.mustFinish = mustFinish;
			this.driver = driver;
		}
	}

	private static final class SkillNeed
	{
		int level;
		boolean boostable;
		String driver;
		int others;
	}

	private static final class QuestNeed
	{
		boolean mustFinish;
		String driver;
	}

	private final Map<String, SkillNeed> skillNeeds = new TreeMap<>();
	private final Map<String, QuestNeed> questNeeds = new TreeMap<>();
	private final Set<String> other = new LinkedHashSet<>();

	public void add(String driver, RequirementSet requirements)
	{
		if (requirements != null)
		{
			add(driver, requirements.skills, requirements.quests, requirements.questsStarted, requirements.other);
		}
	}

	public void add(String driver, List<SkillRequirement> skills, List<String> quests, List<String> started,
		List<String> otherRequirements)
	{
		String who = driver == null ? "" : driver;
		if (skills != null)
		{
			for (SkillRequirement s : skills)
			{
				if (s == null || s.skill == null)
				{
					continue;
				}
				SkillNeed need = skillNeeds.get(s.skill);
				if (need == null || s.level > need.level)
				{
					need = new SkillNeed();
					need.level = s.level;
					need.boostable = s.boostable;
					need.driver = who;
					skillNeeds.put(s.skill, need);
				}
				else if (s.level == need.level)
				{
					need.boostable = need.boostable && s.boostable;
					if (!who.equals(need.driver))
					{
						need.others++;
					}
				}
			}
		}
		if (quests != null)
		{
			for (String quest : quests)
			{
				if (quest == null || quest.isEmpty())
				{
					continue;
				}
				QuestNeed need = questNeeds.computeIfAbsent(quest, k -> new QuestNeed());
				need.mustFinish = true;
				if (need.driver == null)
				{
					need.driver = who;
				}
			}
		}
		if (started != null)
		{
			for (String quest : started)
			{
				if (quest == null || quest.isEmpty())
				{
					continue;
				}
				QuestNeed need = questNeeds.computeIfAbsent(quest, k -> new QuestNeed());
				if (need.driver == null)
				{
					need.driver = who;
				}
			}
		}
		if (otherRequirements != null)
		{
			for (String text : otherRequirements)
			{
				if (text != null && !text.isEmpty())
				{
					other.add(text);
				}
			}
		}
	}

	/** Every skill that is asked for (met or not), sorted by skill name. */
	public List<SkillGap> skillGaps(Levels levels)
	{
		List<SkillGap> out = new ArrayList<>();
		for (Map.Entry<String, SkillNeed> e : skillNeeds.entrySet())
		{
			SkillNeed need = e.getValue();
			int current = levels == null ? -1 : levels.level(e.getKey());
			out.add(new SkillGap(e.getKey(), need.level, current, need.boostable, need.driver, need.others));
		}
		return out;
	}

	/** Only the quests you have not yet done enough of (unknown states are included). */
	public List<QuestGap> questGaps(Quests quests)
	{
		List<QuestGap> out = new ArrayList<>();
		for (Map.Entry<String, QuestNeed> e : questNeeds.entrySet())
		{
			QuestProgress state = quests == null ? QuestProgress.UNKNOWN : quests.state(e.getKey());
			if (state == null)
			{
				state = QuestProgress.UNKNOWN;
			}
			QuestNeed need = e.getValue();
			boolean unmet;
			if (need.mustFinish)
			{
				unmet = state != QuestProgress.FINISHED;
			}
			else
			{
				unmet = state == QuestProgress.NOT_STARTED || state == QuestProgress.UNKNOWN;
			}
			if (unmet)
			{
				out.add(new QuestGap(e.getKey(), state, need.mustFinish, need.driver));
			}
		}
		return out;
	}

	/** Items, combat level and other requirements the plugin can't check. */
	public Set<String> otherRequirements()
	{
		return other;
	}

	public boolean isEmpty()
	{
		return skillNeeds.isEmpty() && questNeeds.isEmpty() && other.isEmpty();
	}
}
