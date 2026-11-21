package com.osrsgoals.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.osrsgoals.core.DiaryTier;
import com.osrsgoals.core.Goal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GoalStoreTest
{
	private static GoalStore newStore(Map<String, String> backing)
	{
		return new GoalStore(new GoalStore.Backend()
		{
			@Override
			public String get(String key)
			{
				return backing.get(key);
			}

			@Override
			public void set(String key, String value)
			{
				backing.put(key, value);
			}
		});
	}

	@Test
	public void goalsAreSavedAndDeduplicated()
	{
		Map<String, String> backing = new HashMap<>();
		GoalStore store = newStore(backing);
		assertTrue(store.goals().isEmpty());

		assertTrue(store.addGoal(Goal.skill("FISHING", 82)));
		assertFalse("the same goal twice", store.addGoal(Goal.skill("FISHING", 82)));
		assertTrue(store.addGoal(Goal.diary("fremennik", "Fremennik Diary", DiaryTier.ELITE)));
		assertTrue(store.addGoal(Goal.drop("zulrah", "Zulrah", "Tanzanite fang")));

		// A new store over the same storage sees the saved goals (as after restarting the client).
		List<Goal> goals = newStore(backing).goals();
		assertEquals(3, goals.size());
		assertNotNull(goals.get(0).id);

		Goal first = goals.get(0);
		first.priority = 5;
		store.updateGoal(first);
		assertEquals(5, newStore(backing).goals().get(0).priority);

		store.removeGoal(first.id);
		assertEquals(2, store.goals().size());
	}

	@Test
	public void diaryTasksKillsPerHourAndFlags()
	{
		GoalStore store = newStore(new HashMap<>());
		assertFalse(store.isTaskDone("fremennik_elite_1"));
		store.setTaskDone("fremennik_elite_1", true);
		assertTrue(store.isTaskDone("fremennik_elite_1"));
		store.setTaskDone("fremennik_elite_1", false);
		assertFalse(store.isTaskDone("fremennik_elite_1"));

		assertNull(store.killsPerHour("zulrah"));
		store.setKillsPerHour("zulrah", 25.0);
		assertEquals(25.0, store.killsPerHour("zulrah"), 1e-9);
		store.setKillsPerHour("zulrah", null);
		assertNull(store.killsPerHour("zulrah"));

		assertEquals(-1, store.killCount("zulrah"));
		store.setKillCount("zulrah", 42);
		assertEquals(42, store.killCount("zulrah"));

		assertFalse(store.flag("showGear", false));
		store.setFlag("showGear", true);
		assertTrue(store.flag("showGear", false));
	}

	@Test
	public void brokenSavedDataDoesNotCrash()
	{
		Map<String, String> backing = new HashMap<>();
		backing.put("goals", "this is not json");
		backing.put("doneTasks", "{\"oops\":1}");
		GoalStore store = newStore(backing);
		assertTrue(store.goals().isEmpty());
		assertTrue(store.doneTasks().isEmpty());
	}
}
