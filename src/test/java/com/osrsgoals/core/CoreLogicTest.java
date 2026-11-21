package com.osrsgoals.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class CoreLogicTest
{
	// ---- helpers ----

	private static RateBreakpoint rate(int level, Double afk, Double active, Double gp)
	{
		RateBreakpoint r = new RateBreakpoint();
		r.level = level;
		r.afk = afk;
		r.active = active;
		r.gpPerHour = gp;
		return r;
	}

	/** A method whose rate does not change with level. */
	private static TrainingMethod flat(String id, int minLevel, Double afk, Double active, Double gp)
	{
		TrainingMethod m = new TrainingMethod();
		m.id = id;
		m.skill = "FISHING";
		m.name = id;
		m.minLevel = minLevel;
		m.rates.add(rate(minLevel, afk, active, gp));
		m.rates.add(rate(99, afk, active, gp));
		return m;
	}

	private static SkillRequirement req(String skill, int level, boolean boostable)
	{
		SkillRequirement r = new SkillRequirement();
		r.skill = skill;
		r.level = level;
		r.boostable = boostable;
		return r;
	}

	// ---- XP table ----

	@Test
	public void xpTableMatchesKnownValues()
	{
		assertEquals(83, XpTable.xpForLevel(2));
		assertEquals(1_986_068, XpTable.xpForLevel(80));
		assertEquals(2_673_114, XpTable.xpForLevel(83));
		assertEquals(5_346_332, XpTable.xpForLevel(90));
		assertEquals(6_517_253, XpTable.xpForLevel(92));
		assertEquals(13_034_431, XpTable.xpForLevel(99));
		assertEquals(2, XpTable.levelForXp(83));
		assertEquals(1, XpTable.levelForXp(82));
		assertEquals(99, XpTable.levelForXp(200_000_000));
	}

	// ---- calculator ----

	@Test
	public void computesTimeAndGpFromRates()
	{
		int xp = XpTable.xpForLevel(78);
		long needed = GoalCalculator.xpNeeded(xp, 82);
		TrainingMethod m = flat("a", 76, 50_000.0, 100_000.0, 800_000.0);

		MethodResult active = GoalCalculator.evaluate(m, xp, 82, Playstyle.ACTIVE);
		assertTrue(active.eligible);
		assertEquals(needed / 100_000.0, active.hoursNeeded, 1e-9);
		assertEquals(100_000.0, active.xpPerHour, 1e-6);
		assertEquals(800_000.0 * active.hoursNeeded, active.netGpTotal, 1e-3);

		assertEquals(needed / 50_000.0, GoalCalculator.evaluate(m, xp, 82, Playstyle.AFK).hoursNeeded, 1e-9);
		assertEquals(needed / 100_000.0, GoalCalculator.evaluate(m, xp, 82, Playstyle.BEST).hoursNeeded, 1e-9);
	}

	@Test
	public void ratesAreInterpolatedBetweenListedLevels()
	{
		TrainingMethod m = new TrainingMethod();
		m.rates.add(rate(70, null, 40_000.0, null));
		m.rates.add(rate(80, null, 50_000.0, null));
		assertEquals(45_000.0, GoalCalculator.rateAt(m.rates, 75, GoalCalculator.xpField(Playstyle.ACTIVE)), 1e-9);
		// No extrapolation outside the listed levels.
		assertEquals(40_000.0, GoalCalculator.rateAt(m.rates, 60, GoalCalculator.xpField(Playstyle.ACTIVE)), 1e-9);
		assertEquals(50_000.0, GoalCalculator.rateAt(m.rates, 99, GoalCalculator.xpField(Playstyle.ACTIVE)), 1e-9);
	}

	@Test
	public void blockedAndUnknownMethods()
	{
		TrainingMethod m = flat("a", 76, 50_000.0, 100_000.0, null);
		MethodResult blocked = GoalCalculator.evaluate(m, XpTable.xpForLevel(70), 82, Playstyle.ACTIVE);
		assertFalse(blocked.eligible);
		assertEquals("Needs level 76", blocked.blockedReason);

		TrainingMethod noAfk = flat("b", 1, null, 100_000.0, null);
		assertNull(GoalCalculator.evaluate(noAfk, XpTable.xpForLevel(78), 82, Playstyle.AFK).hoursNeeded);
		assertNull(GoalCalculator.evaluate(m, XpTable.xpForLevel(78), 82, Playstyle.ACTIVE).netGpTotal);
	}

	@Test
	public void rankPutsFastestFirstThenUnknownThenBlocked()
	{
		TrainingMethod fast = flat("fast", 1, null, 100_000.0, null);
		TrainingMethod slow = flat("slow", 1, null, 50_000.0, null);
		TrainingMethod unknown = flat("unknown", 1, null, null, null);
		TrainingMethod blocked = flat("blocked", 90, null, 200_000.0, null);
		List<MethodResult> ranked = GoalCalculator.rank(Arrays.asList(blocked, unknown, slow, fast),
			XpTable.xpForLevel(78), 82, Playstyle.ACTIVE, SortMode.FASTEST);
		assertEquals("fast", ranked.get(0).method.id);
		assertEquals("slow", ranked.get(1).method.id);
		assertEquals("unknown", ranked.get(2).method.id);
		assertEquals("blocked", ranked.get(3).method.id);
	}

	@Test
	public void formatsHours()
	{
		assertEquals("12h 30m", GoalCalculator.formatHours(12.5));
	}

	// ---- planner ----

	@Test
	public void fastestPathSwitchesMethodWhenABetterOneUnlocks()
	{
		TrainingMethod a = flat("a", 1, null, 50_000.0, null);
		TrainingMethod b = flat("b", 80, null, 100_000.0, null);
		int xp = XpTable.xpForLevel(78);
		Planner.PathResult path = Planner.fastestPath(Arrays.asList(a, b), xp, 82, Playstyle.ACTIVE);
		assertTrue(path.complete);
		assertEquals(2, path.segments.size());
		assertEquals("a", path.segments.get(0).method.id);
		assertEquals(78, path.segments.get(0).fromLevel);
		assertEquals(80, path.segments.get(0).toLevel);
		assertEquals("b", path.segments.get(1).method.id);
		assertEquals(80, path.segments.get(1).fromLevel);
		assertEquals(82, path.segments.get(1).toLevel);
		double expected = (XpTable.xpForLevel(80) - xp) / 50_000.0
			+ (XpTable.xpForLevel(82) - XpTable.xpForLevel(80)) / 100_000.0;
		assertEquals(expected, path.hours, 1e-9);
	}

	@Test
	public void fastestPathReportsWhereRatesRunOut()
	{
		TrainingMethod a = flat("a", 1, null, 50_000.0, null);
		a.rates.clear();
		Planner.PathResult path = Planner.fastestPath(Collections.singletonList(a), XpTable.xpForLevel(78), 82,
			Playstyle.ACTIVE);
		assertFalse(path.complete);
		assertEquals(78, path.blockedAtLevel);
		assertTrue(path.segments.isEmpty());
	}

	// ---- drops ----

	@Test
	public void dropMath()
	{
		assertEquals(512.0, DropMath.expectedKills(1.0 / 512), 1e-9);
		long median = DropMath.killsForConfidence(1.0 / 512, 0.5);
		assertTrue("median kills was " + median, median >= 354 && median <= 356);
		assertEquals(0.5, DropMath.probabilityWithin(1.0 / 512, median), 0.01);
		assertEquals(0.0, DropMath.probabilityWithin(0.01, 0), 1e-12);
		assertNull(DropMath.hoursFor(100, null));
		assertEquals(2.0, DropMath.hoursFor(100, 50.0), 1e-9);
	}

	// ---- requirement gaps ----

	@Test
	public void gapSummaryKeepsHighestLevelAndDriver()
	{
		GapSummary summary = new GapSummary();
		summary.add("Task A", Arrays.asList(req("FISHING", 82, true)), Collections.<String>emptyList(),
			Collections.<String>emptyList(), Collections.<String>emptyList());
		summary.add("Task B", Arrays.asList(req("FISHING", 78, false), req("RUNECRAFT", 74, false)),
			Arrays.asList("Some Quest"), Collections.<String>emptyList(), Arrays.asList("Rune pouch"));
		summary.add("Task C", Arrays.asList(req("FISHING", 82, false)), Collections.<String>emptyList(),
			Collections.<String>emptyList(), Collections.<String>emptyList());

		List<GapSummary.SkillGap> gaps = summary.skillGaps(skill -> skill.equals("FISHING") ? 78 : 80);
		assertEquals(2, gaps.size());
		GapSummary.SkillGap fishing = gaps.get(0);
		assertEquals("FISHING", fishing.skill);
		assertEquals(82, fishing.required);
		assertEquals("Task A", fishing.driver);
		assertFalse("one of the 82 requirements is not boostable", fishing.boostable);
		assertFalse(fishing.met());
		assertTrue(gaps.get(1).met());

		List<GapSummary.QuestGap> quests = summary.questGaps(q -> QuestProgress.NOT_STARTED);
		assertEquals(1, quests.size());
		assertTrue(summary.questGaps(q -> QuestProgress.FINISHED).isEmpty());
		assertTrue(summary.otherRequirements().contains("Rune pouch"));
	}

	@Test
	public void unknownLevelsAreNotMet()
	{
		GapSummary summary = new GapSummary();
		summary.add("Task", Arrays.asList(req("FISHING", 82, false)), Collections.<String>emptyList(),
			Collections.<String>emptyList(), Collections.<String>emptyList());
		GapSummary.SkillGap gap = summary.skillGaps(skill -> -1).get(0);
		assertFalse(gap.known());
		assertFalse(gap.met());
	}

	// ---- gear checklist ----

	@Test
	public void setupCheckerCountsOwnedItemsAndMissingCost()
	{
		BossSetup setup = new BossSetup();
		setup.name = "Mid";
		Map<String, List<String>> equipment = new LinkedHashMap<>();
		equipment.put("HEAD", Arrays.asList("Helm A", "Helm B"));
		equipment.put("WEAPON", Arrays.asList("Staff C"));
		setup.equipment = equipment;
		SetupItem potion = new SetupItem();
		potion.item = "Prayer potion(4)";
		potion.quantity = 4;
		setup.inventory = new ArrayList<>(Collections.singletonList(potion));

		Map<String, Integer> have = new LinkedHashMap<>();
		have.put("helm b", 1);
		have.put("prayer potion(4)", 2);
		Map<String, Integer> prices = new LinkedHashMap<>();
		prices.put("staff c", 1000);
		prices.put("prayer potion(4)", 10);

		SetupChecker.Report report = SetupChecker.check(setup,
			name -> have.getOrDefault(name.toLowerCase(), 0),
			name -> prices.getOrDefault(name.toLowerCase(), -1));

		assertTrue(report.equipment.get(0).satisfied());
		assertEquals("Helm B", report.equipment.get(0).ownedItem);
		assertFalse(report.equipment.get(1).satisfied());
		assertEquals(Long.valueOf(1000), report.equipment.get(1).missingCost);
		assertFalse(report.inventory.get(0).satisfied());
		assertEquals(2, report.inventory.get(0).owned);
		assertEquals(1000 + 2 * 10, report.missingCost);
		assertEquals(2, report.missingRows);
		assertEquals(0, report.unpriced);
	}

	// ---- wheel ----

	private static Wheel.Entry entry(String label, int priority, boolean complete, boolean doable, Double hours)
	{
		Goal goal = new Goal();
		goal.type = GoalType.SKILL;
		goal.label = label;
		goal.priority = priority;
		return new Wheel.Entry(goal, new GoalStatus(complete, "", hours, doable, ""));
	}

	@Test
	public void wheelSkipsFinishedAndNotStartableGoals()
	{
		List<Wheel.Entry> entries = Arrays.asList(
			entry("done", 3, true, true, 0.0),
			entry("blocked", 3, false, false, 5.0),
			entry("open", 3, false, true, 5.0));
		for (int i = 0; i < 20; i++)
		{
			assertEquals("open", Wheel.pick(entries, WeightMode.EQUAL, true, new Random(i)).goal.label);
		}
		assertEquals(2, Wheel.candidates(entries, false).size());
		assertNull(Wheel.pick(Collections.singletonList(entry("done", 3, true, true, 0.0)), WeightMode.EQUAL, false,
			new Random(1)));
	}

	@Test
	public void wheelWeightsFollowPriorityAndTime()
	{
		Wheel.Entry high = entry("high", 5, false, true, 10.0);
		Wheel.Entry low = entry("low", 1, false, true, 1.0);
		assertEquals(5.0, Wheel.weight(high, WeightMode.PRIORITY), 1e-9);
		assertEquals(1.0, Wheel.weight(low, WeightMode.PRIORITY), 1e-9);
		assertTrue(Wheel.weight(low, WeightMode.QUICKEST) > Wheel.weight(high, WeightMode.QUICKEST));
		assertEquals(0.1, Wheel.weight(entry("?", 3, false, true, null), WeightMode.QUICKEST), 1e-9);

		int highPicked = 0;
		Random random = new Random(42);
		List<Wheel.Entry> both = Arrays.asList(high, low);
		for (int i = 0; i < 1000; i++)
		{
			if (Wheel.pick(both, WeightMode.PRIORITY, false, random) == high)
			{
				highPicked++;
			}
		}
		assertTrue("high priority picked " + highPicked + "/1000", highPicked > 750 && highPicked < 920);
	}

	// ---- names ----

	@Test
	public void nameMatching()
	{
		assertEquals(Names.key("Dragon Slayer I"), Names.key("dragon-slayer i"));
		assertEquals(Names.bossKey("The Gauntlet"), Names.bossKey("Gauntlet"));
		assertEquals(Names.bossKey("Kree'arra"), Names.bossKey("kreearra"));
		assertNotNull(Names.item(" Abyssal whip "));
		assertEquals("abyssal whip", Names.item(" Abyssal whip "));
	}
}
