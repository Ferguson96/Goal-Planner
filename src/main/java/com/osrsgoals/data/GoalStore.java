package com.osrsgoals.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osrsgoals.core.Goal;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the player saves: goals, ticked-off diary tasks, their own kills per hour, kill counts and
 * a few switches. Stored as JSON strings through a Backend (RuneLite's config in the plugin, a map in tests).
 */
public class GoalStore
{
	public interface Backend
	{
		String get(String key);

		void set(String key, String value);
	}

	private static final Type GOAL_LIST = new TypeToken<List<Goal>>()
	{
	}.getType();
	private static final Type STRING_LIST = new TypeToken<List<String>>()
	{
	}.getType();
	private static final Type DOUBLE_MAP = new TypeToken<Map<String, Double>>()
	{
	}.getType();
	private static final Type INT_MAP = new TypeToken<Map<String, Integer>>()
	{
	}.getType();

	private static final String GOALS = "goals";
	private static final String DONE_TASKS = "doneTasks";
	private static final String KILLS_PER_HOUR = "killsPerHour";
	private static final String KILL_COUNTS = "killCounts";
	private static final String FLAG_PREFIX = "flag.";

	private final Backend backend;
	private final Gson gson;

	public GoalStore(Backend backend, Gson gson)
	{
		this.backend = backend;
		this.gson = gson;
	}

	private <T> T read(String key, Type type, T fallback)
	{
		try
		{
			String json = backend.get(key);
			if (json == null || json.isEmpty())
			{
				return fallback;
			}
			T value = gson.fromJson(json, type);
			return value == null ? fallback : value;
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
	}

	private void write(String key, Object value)
	{
		backend.set(key, gson.toJson(value));
	}

	// ---- goals ----

	public List<Goal> goals()
	{
		List<Goal> goals = this.<List<Goal>>read(GOALS, GOAL_LIST, new ArrayList<Goal>());
		List<Goal> clean = new ArrayList<>();
		for (Goal goal : goals)
		{
			if (goal != null && goal.type != null)
			{
				clean.add(goal);
			}
		}
		return clean;
	}

	public void saveGoals(List<Goal> goals)
	{
		write(GOALS, goals);
	}

	/** Adds the goal unless the same one is already saved. Returns true if it was added. */
	public boolean addGoal(Goal goal)
	{
		List<Goal> goals = goals();
		for (Goal existing : goals)
		{
			if (existing.key().equals(goal.key()))
			{
				return false;
			}
		}
		if (goal.id == null)
		{
			goal.id = UUID.randomUUID().toString();
		}
		goals.add(goal);
		saveGoals(goals);
		return true;
	}

	public void removeGoal(String id)
	{
		List<Goal> goals = goals();
		goals.removeIf(g -> id != null && id.equals(g.id));
		saveGoals(goals);
	}

	/** Saves changes to a goal (priority, done) found by its id. */
	public void updateGoal(Goal goal)
	{
		List<Goal> goals = goals();
		for (int i = 0; i < goals.size(); i++)
		{
			if (goals.get(i).id != null && goals.get(i).id.equals(goal.id))
			{
				goals.set(i, goal);
			}
		}
		saveGoals(goals);
	}

	// ---- diary tasks ----

	public Set<String> doneTasks()
	{
		return new LinkedHashSet<>(this.<List<String>>read(DONE_TASKS, STRING_LIST, new ArrayList<String>()));
	}

	public boolean isTaskDone(String taskId)
	{
		return doneTasks().contains(taskId);
	}

	public void setTaskDone(String taskId, boolean done)
	{
		Set<String> tasks = doneTasks();
		if (done)
		{
			tasks.add(taskId);
		}
		else
		{
			tasks.remove(taskId);
		}
		write(DONE_TASKS, new ArrayList<>(tasks));
	}

	// ---- kills per hour (the player's own figure) ----

	public Double killsPerHour(String bossId)
	{
		return this.<Map<String, Double>>read(KILLS_PER_HOUR, DOUBLE_MAP, new HashMap<String, Double>()).get(bossId);
	}

	/** A null or non-positive value clears the player's figure. */
	public void setKillsPerHour(String bossId, Double value)
	{
		Map<String, Double> map = this.<Map<String, Double>>read(KILLS_PER_HOUR, DOUBLE_MAP, new HashMap<String, Double>());
		if (value == null || value <= 0)
		{
			map.remove(bossId);
		}
		else
		{
			map.put(bossId, value);
		}
		write(KILLS_PER_HOUR, map);
	}

	// ---- kill counts seen in chat ----

	/** -1 if no kill count has been seen for this key. */
	public int killCount(String key)
	{
		Integer value = this.<Map<String, Integer>>read(KILL_COUNTS, INT_MAP, new HashMap<String, Integer>()).get(key);
		return value == null ? -1 : value;
	}

	public void setKillCount(String key, int kills)
	{
		Map<String, Integer> map = this.<Map<String, Integer>>read(KILL_COUNTS, INT_MAP, new HashMap<String, Integer>());
		map.put(key, kills);
		write(KILL_COUNTS, map);
	}

	// ---- switches ----

	public boolean flag(String name, boolean defaultValue)
	{
		String value = backend.get(FLAG_PREFIX + name);
		return value == null || value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
	}

	public void setFlag(String name, boolean value)
	{
		backend.set(FLAG_PREFIX + name, Boolean.toString(value));
	}
}
