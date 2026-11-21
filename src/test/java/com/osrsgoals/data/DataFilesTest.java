package com.osrsgoals.data;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.osrsgoals.core.Boss;
import com.osrsgoals.core.BossDrop;
import com.osrsgoals.core.Diary;
import com.osrsgoals.core.DiaryTask;
import com.osrsgoals.core.DiaryTier;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.Playstyle;
import com.osrsgoals.core.RateBreakpoint;
import com.osrsgoals.core.SortMode;
import com.osrsgoals.core.TrainingMethod;
import com.osrsgoals.core.XpTable;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assume;
import org.junit.Test;

/** Checks that the bundled wiki data loads and is consistent (the Python validators check the details). */
public class DataFilesTest
{
	@Test
	public void trainingMethodsLoadAndWork()
	{
		MethodRepository repo = new MethodRepository();
		repo.load(new Gson());
		assertFalse("no skills loaded", repo.skills().isEmpty());
		assertTrue(repo.skills().contains("FISHING"));

		Set<String> ids = new HashSet<>();
		for (String skill : repo.skills())
		{
			for (TrainingMethod m : repo.methodsFor(skill))
			{
				assertTrue("duplicate id " + m.id, ids.add(m.id));
				assertEquals(skill, m.skill);
				int previous = 0;
				for (RateBreakpoint p : m.rates)
				{
					assertTrue(m.id + " levels must ascend", p.level > previous);
					previous = p.level;
				}
				// Every method can be evaluated at any level without throwing.
				GoalCalculator.evaluate(m, XpTable.xpForLevel(Math.max(1, m.minLevel)), 99, Playstyle.BEST);
			}
			GoalCalculator.rank(repo.methodsFor(skill), 0, 99, Playstyle.BEST, SortMode.FASTEST);
		}
	}

	@Test
	public void fishingHasRealNumbers()
	{
		MethodRepository repo = new MethodRepository();
		repo.load(new Gson());
		int usable = 0;
		for (TrainingMethod m : repo.methodsFor("FISHING"))
		{
			if (GoalCalculator.evaluate(m, XpTable.xpForLevel(78), 82, Playstyle.BEST).hoursNeeded != null)
			{
				usable++;
			}
		}
		assertTrue("expected several Fishing methods with rates for 78 to 82, found " + usable, usable >= 3);
	}

	@Test
	public void diariesLoadWithUniqueTasks()
	{
		DiaryRepository repo = new DiaryRepository();
		repo.load(new Gson());
		Set<String> ids = new HashSet<>();
		for (Diary diary : repo.all())
		{
			assertTrue(Arrays.asList(DiaryRepository.IDS).contains(diary.id));
			for (DiaryTier tier : DiaryTier.values())
			{
				for (DiaryTask task : diary.tasks(tier))
				{
					assertTrue("duplicate task id " + task.id, ids.add(task.id));
					assertNotNull(task.description);
				}
			}
		}
	}

	@Test
	public void bossIndexCoversEveryFile()
	{
		File dir = new File("src/main/resources/bosses");
		Assume.assumeTrue(dir.isDirectory());
		Set<String> indexed = new HashSet<>(JsonResources.lines("/bosses/_index.txt"));
		String[] files = dir.list((d, name) -> name.endsWith(".json"));
		for (String file : files)
		{
			String id = file.substring(0, file.length() - 5);
			assertTrue(id + " is missing from bosses/_index.txt (run tools/build_index.py)", indexed.contains(id));
		}
		assertEquals(files.length, indexed.size());
	}

	@Test
	public void bossesLoadWithConsistentDrops()
	{
		BossRepository repo = new BossRepository();
		repo.load(new Gson());
		for (Boss boss : repo.all())
		{
			assertNotNull(boss.name);
			for (BossDrop drop : boss.drops)
			{
				String[] parts = drop.rarity.split("/");
				assertEquals(boss.id + " " + drop.item, Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]),
					drop.chance, 1e-9);
			}
		}
	}
}
