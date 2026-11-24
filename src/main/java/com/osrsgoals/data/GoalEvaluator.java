package com.osrsgoals.data;

import com.osrsgoals.core.Boss;
import com.osrsgoals.core.BossDrop;
import com.osrsgoals.core.Diary;
import com.osrsgoals.core.DiaryTask;
import com.osrsgoals.core.DiaryTier;
import com.osrsgoals.core.DropMath;
import com.osrsgoals.core.GapSummary;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.GoalStatus;
import com.osrsgoals.core.Planner;
import com.osrsgoals.core.Playstyle;
import com.osrsgoals.core.QuestProgress;
import com.osrsgoals.core.RequirementSet;
import com.osrsgoals.core.Text;
import com.osrsgoals.core.XpTable;
import java.util.ArrayList;
import java.util.List;

/** Works out where each goal stands, from the player's state and the bundled wiki data. */
public class GoalEvaluator
{
	/** What the plugin knows about the player right now. */
	public interface State
	{
		/** Total XP in the skill (RuneLite skill name), or -1 if not known (not logged in). */
		int xp(String skill);

		QuestProgress quest(String quest);

		/** Kill count for a boss by display name, or -1 if none seen yet. */
		int killCount(String bossName);

		/** True if the game reports the whole diary tier as finished. */
		default boolean diaryTierComplete(String diaryId, DiaryTier tier)
		{
			return false;
		}
	}

	private final MethodRepository methods;
	private final DiaryRepository diaries;
	private final BossRepository bosses;
	private final GoalStore store;
	private final State state;

	public GoalEvaluator(MethodRepository methods, DiaryRepository diaries, BossRepository bosses, GoalStore store,
		State state)
	{
		this.methods = methods;
		this.diaries = diaries;
		this.bosses = bosses;
		this.store = store;
		this.state = state;
	}

	/** Hours to reach the level with the fastest wiki methods; 0 if already there; null if unknown. */
	public Double bestHoursFor(String skill, int targetLevel)
	{
		int xp = state.xp(skill);
		if (xp < 0)
		{
			return null;
		}
		if (XpTable.levelForXp(xp) >= targetLevel)
		{
			return 0.0;
		}
		Planner.PathResult path = Planner.fastestPath(methods.methodsFor(skill), xp, targetLevel, Playstyle.BEST);
		return path.complete ? path.hours : null;
	}

	/** The player's own kills/hr if they entered one, else the wiki's figure, else null. */
	public Double killsPerHour(Boss boss)
	{
		Double own = store.killsPerHour(boss.id);
		if (own != null && own > 0)
		{
			return own;
		}
		return boss.killsPerHour != null && boss.killsPerHour > 0 ? boss.killsPerHour : null;
	}

	public GoalStatus evaluate(Goal goal)
	{
		if (goal.type == null)
		{
			return new GoalStatus(false, "", null, true, "");
		}
		switch (goal.type)
		{
			case SKILL:
				return skill(goal);
			case DIARY:
				return diary(goal);
			case BOSS_KILLS:
				return bossKills(goal);
			case DROP:
				return drop(goal);
			default:
				return new GoalStatus(false, "", null, true, "");
		}
	}

	private GoalStatus skill(Goal goal)
	{
		String name = Text.pretty(goal.skill);
		int xp = state.xp(goal.skill);
		if (xp < 0)
		{
			return new GoalStatus(false, name + " to " + goal.targetLevel, null, true,
				"Log in so the plugin can read your " + name + " level.");
		}
		int level = XpTable.levelForXp(xp);
		String progress = name + " " + level + "/" + goal.targetLevel;
		if (level >= goal.targetLevel)
		{
			return new GoalStatus(true, progress, 0.0, true, "Done.");
		}
		Planner.PathResult path = Planner.fastestPath(methods.methodsFor(goal.skill), xp, goal.targetLevel,
			Playstyle.BEST);
		if (path.segments.isEmpty())
		{
			return new GoalStatus(false, progress, null, true,
				"No wiki rate for a method you can use yet; choose a method yourself.");
		}
		Planner.Segment first = path.segments.get(0);
		if (!path.complete)
		{
			return new GoalStatus(false, progress, null, true,
				"Train " + first.method.name + " from level " + first.fromLevel + "; no wiki rate past level "
					+ path.blockedAtLevel + ".");
		}
		return new GoalStatus(false, progress, path.hours, true,
			"Train " + first.method.name + " from level " + first.fromLevel + " (about "
				+ GoalCalculator.formatHours(path.hours) + " in total).");
	}

	private GoalStatus diary(Goal goal)
	{
		Diary diary = diaries.get(goal.diaryId);
		if (diary == null)
		{
			return new GoalStatus(false, "Diary data missing", null, true, "");
		}
		DiaryTier tier;
		try
		{
			tier = DiaryTier.valueOf(goal.tier);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return new GoalStatus(false, "Unknown tier", null, true, "");
		}
		List<DiaryTask> tasks = diary.tasks(tier);
		List<DiaryTask> todo = new ArrayList<>();
		boolean tierDone = state.diaryTierComplete(diary.id, tier);
		for (DiaryTask task : tasks)
		{
			if (!tierDone && !store.isTaskDone(task.id))
			{
				todo.add(task);
			}
		}
		String progress = (tasks.size() - todo.size()) + "/" + tasks.size() + " tasks done";
		if (!tasks.isEmpty() && todo.isEmpty())
		{
			return new GoalStatus(true, progress, 0.0, true, "Done.");
		}

		GapSummary summary = new GapSummary();
		for (DiaryTask task : todo)
		{
			summary.add(shorten(task.description), task.skills, task.quests, task.questsStarted, task.other);
		}

		double hours = 0;
		boolean hoursKnown = true;
		GapSummary.SkillGap firstSkill = null;
		for (GapSummary.SkillGap gap : summary.skillGaps(levels()))
		{
			if (!gap.known() || gap.met())
			{
				continue;
			}
			if (firstSkill == null)
			{
				firstSkill = gap;
			}
			Double h = bestHoursFor(gap.skill, gap.required);
			if (h == null)
			{
				hoursKnown = false;
			}
			else
			{
				hours += h;
			}
		}
		GapSummary.QuestGap firstQuest = null;
		for (GapSummary.QuestGap gap : summary.questGaps(state::quest))
		{
			if (gap.state != QuestProgress.UNKNOWN)
			{
				firstQuest = gap;
				break;
			}
		}

		String next;
		if (firstSkill != null)
		{
			next = "Train " + Text.pretty(firstSkill.skill) + " to " + firstSkill.required + ".";
		}
		else if (firstQuest != null)
		{
			next = "Finish the quest " + firstQuest.name + ".";
		}
		else if (!todo.isEmpty())
		{
			next = "Do: " + shorten(todo.get(0).description);
		}
		else
		{
			next = "The wiki lists no tasks for this tier.";
		}
		return new GoalStatus(false, progress, hoursKnown ? hours : null, !blocked(summary), next);
	}

	private GoalStatus bossKills(Goal goal)
	{
		Boss boss = bosses.get(goal.bossId);
		if (boss == null)
		{
			return new GoalStatus(false, "Boss data missing", null, true, "");
		}
		int kc = state.killCount(boss.name);
		boolean complete = kc >= goal.targetKills;
		String progress = kc < 0 ? "Target " + goal.targetKills + " kills (count not seen yet)"
			: "KC " + kc + "/" + goal.targetKills;
		Double hours = DropMath.hoursFor(Math.max(0, goal.targetKills - Math.max(kc, 0)), killsPerHour(boss));
		boolean doable = !blocked(summary(boss.name, boss.requirements));
		if (complete)
		{
			return new GoalStatus(true, progress, 0.0, doable, "Done.");
		}
		String next = "Kill " + boss.name + (hours == null ? "." : " (about "
			+ GoalCalculator.formatHours(hours) + " left at your kills/hr).");
		return new GoalStatus(false, progress, hours, doable, next);
	}

	private GoalStatus drop(Goal goal)
	{
		Boss boss = bosses.get(goal.bossId);
		if (boss == null)
		{
			return new GoalStatus(false, "Boss data missing", null, true, "");
		}
		BossDrop found = null;
		for (BossDrop drop : boss.drops)
		{
			if (drop.item != null && drop.item.equalsIgnoreCase(goal.item))
			{
				found = drop;
			}
		}
		if (found == null)
		{
			return new GoalStatus(goal.done, "Drop data missing", null, true, "");
		}
		double expected = DropMath.expectedKills(found.chance);
		Double hours = DropMath.hoursFor(expected, killsPerHour(boss));
		String progress = found.rarity + ", about " + Text.number(Math.round(expected)) + " kills on average";
		boolean doable = !blocked(summary(boss.name, boss.requirements));
		if (goal.done)
		{
			return new GoalStatus(true, progress, 0.0, doable, "Obtained.");
		}
		String next = "Kill " + boss.name + (hours == null ? "." : " (about "
			+ GoalCalculator.formatHours(hours) + " on average).");
		return new GoalStatus(false, progress, hours, doable, next);
	}

	private GapSummary summary(String driver, RequirementSet requirements)
	{
		GapSummary summary = new GapSummary();
		summary.add(driver, requirements);
		return summary;
	}

	private GapSummary.Levels levels()
	{
		return skill ->
		{
			int xp = state.xp(skill);
			return xp < 0 ? -1 : XpTable.levelForXp(xp);
		};
	}

	/** True if a level or quest we can check is not met. Unknown things never block. */
	private boolean blocked(GapSummary summary)
	{
		for (GapSummary.SkillGap gap : summary.skillGaps(levels()))
		{
			if (gap.known() && !gap.met())
			{
				return true;
			}
		}
		for (GapSummary.QuestGap gap : summary.questGaps(state::quest))
		{
			if (gap.state != QuestProgress.UNKNOWN)
			{
				return true;
			}
		}
		return false;
	}

	private static String shorten(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.length() <= 70 ? text : text.substring(0, 67) + "...";
	}
}
