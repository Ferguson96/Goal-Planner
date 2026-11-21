package com.osrsgoals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MaxLevelTest
{
	private static TrainingMethod method(String id, int min, Integer max, double rate)
	{
		TrainingMethod m = new TrainingMethod();
		m.id = id;
		m.skill = "SMITHING";
		m.name = id;
		m.minLevel = min;
		m.maxLevel = max;
		RateBreakpoint p = new RateBreakpoint();
		p.level = min;
		p.active = rate;
		m.rates = new ArrayList<>();
		m.rates.add(p);
		return m;
	}

	@Test
	public void methodWithMaxLevelDoesNotCoverHigherLevels()
	{
		TrainingMethod ladder = method("ladder", 1, 40, 100000);
		// 1 to 40 is fully covered.
		assertTrue(GoalCalculator.evaluate(ladder, 0, 40, Playstyle.BEST).hoursNeeded != null);
		// 1 to 60 is not: no honest time can be given.
		assertNull(GoalCalculator.evaluate(ladder, 0, 60, Playstyle.BEST).hoursNeeded);
		// Already past it: not eligible.
		assertFalse(GoalCalculator.evaluate(ladder, XpTable.xpForLevel(45), 60, Playstyle.BEST).eligible);
	}

	@Test
	public void plannerSwitchesMethodAtTheLimit()
	{
		TrainingMethod ladder = method("ladder", 1, 40, 200000);
		TrainingMethod slow = method("slow", 1, null, 50000);
		List<TrainingMethod> methods = Arrays.asList(ladder, slow);
		Planner.PathResult path = Planner.fastestPath(methods, 0, 60, Playstyle.BEST);
		assertTrue(path.complete);
		assertEquals(2, path.segments.size());
		assertEquals("ladder", path.segments.get(0).method.id);
		assertEquals(40, path.segments.get(0).toLevel);
		assertEquals("slow", path.segments.get(1).method.id);
	}
}
