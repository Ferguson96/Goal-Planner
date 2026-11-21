package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import com.osrsgoals.core.GapSummary;
import com.osrsgoals.core.QuestProgress;
import com.osrsgoals.core.Text;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;

/** Draws a GapSummary: skill levels (met or missing), quests, and everything else the wiki asks for. */
final class GapView
{
	interface PlanHandler
	{
		void plan(String skill, int level);
	}

	private GapView()
	{
	}

	/** Adds the rows to target; returns how many skill levels are still missing. */
	static int render(JPanel target, GapSummary summary, GoalPlannerPlugin plugin, PlanHandler onPlan)
	{
		int missing = 0;
		for (GapSummary.SkillGap gap : summary.skillGaps(plugin::level))
		{
			String name = Text.pretty(gap.skill);
			String need = gap.required + (gap.boostable ? " (boostable)" : "");
			String text;
			Color color;
			if (!gap.known())
			{
				text = name + " " + need;
				color = Ui.MUTED;
			}
			else if (gap.met())
			{
				text = "✔ " + name + " " + gap.current + "/" + gap.required;
				color = Ui.GOOD;
			}
			else
			{
				missing++;
				text = "✘ " + name + " " + gap.current + " → " + need;
				color = Ui.BAD;
			}
			String by = Ui.neededBy(gap.driver, gap.alsoNeededBy);
			if (!by.isEmpty())
			{
				text += "<br><small>" + by + "</small>";
			}

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			row.add(Ui.label(text, color, 130), BorderLayout.CENTER);
			if (onPlan != null && gap.known() && !gap.met())
			{
				row.add(Ui.button("Plan", () -> onPlan.plan(gap.skill, gap.required)), BorderLayout.EAST);
			}
			target.add(row);
		}

		for (GapSummary.QuestGap quest : summary.questGaps(plugin::quest))
		{
			String state;
			if (quest.state == QuestProgress.UNKNOWN)
			{
				state = "status unknown";
			}
			else
			{
				state = quest.state == QuestProgress.IN_PROGRESS ? "in progress" : "not started";
			}
			String text = "✘ " + Ui.esc(quest.name) + (quest.mustFinish ? "" : " (start only)")
				+ " – " + state;
			String by = Ui.neededBy(quest.driver, 0);
			if (!by.isEmpty())
			{
				text += "<br><small>" + by + "</small>";
			}
			target.add(Ui.label(text, quest.state == QuestProgress.UNKNOWN ? Ui.MUTED : Ui.BAD));
		}

		Set<String> other = summary.otherRequirements();
		if (!other.isEmpty())
		{
			List<String> escaped = new ArrayList<>();
			for (String item : other)
			{
				escaped.add(Ui.esc(item));
			}
			target.add(Ui.label("<small>Also needed: " + String.join("; ", escaped) + "</small>", Ui.MUTED));
		}
		return missing;
	}
}
