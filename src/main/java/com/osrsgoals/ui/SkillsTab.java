package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.MethodResult;
import com.osrsgoals.core.Planner;
import com.osrsgoals.core.Playstyle;
import com.osrsgoals.core.SortMode;
import com.osrsgoals.core.Text;
import com.osrsgoals.core.TrainingMethod;
import com.osrsgoals.core.XpTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Pick a skill and a target level; see the fastest path and every method with time, XP/hr and GP. */
class SkillsTab extends JPanel implements Refreshable
{
	private final GoalPlannerPlugin plugin;
	private final JComboBox<String> skillBox = new JComboBox<>();
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(82, 2, 99, 1));
	private final JComboBox<Playstyle> styleBox = new JComboBox<>(Playstyle.values());
	private final JComboBox<SortMode> sortBox = new JComboBox<>(SortMode.values());
	private final JPanel results = Ui.column();
	private boolean calculated;

	SkillsTab(GoalPlannerPlugin plugin)
	{
		super(new BorderLayout(0, 8));
		this.plugin = plugin;
		setOpaque(false);

		for (String skill : plugin.methods().skills())
		{
			skillBox.addItem(skill);
		}
		skillBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				return super.getListCellRendererComponent(list, value == null ? "" : Text.pretty(value.toString()),
					index, isSelected, cellHasFocus);
			}
		});

		JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
		form.setOpaque(false);
		form.add(new JLabel("Skill"));
		form.add(skillBox);
		form.add(new JLabel("Target level"));
		form.add(targetSpinner);
		form.add(new JLabel("Playstyle"));
		form.add(styleBox);
		form.add(new JLabel("Sort by"));
		form.add(sortBox);

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(form, BorderLayout.NORTH);
		top.add(Ui.button("Calculate", this::recalculate), BorderLayout.SOUTH);

		add(top, BorderLayout.NORTH);
		add(results, BorderLayout.CENTER);
		results.add(Ui.label("Pick a skill and a target level, then press Calculate.", Ui.MUTED));
	}

	/** Called from the Diaries and Bosses views: jump to a skill and target and calculate. */
	void plan(String skill, int level)
	{
		boolean known = false;
		for (int i = 0; i < skillBox.getItemCount(); i++)
		{
			if (skillBox.getItemAt(i).equals(skill))
			{
				known = true;
			}
		}
		if (!known)
		{
			skillBox.addItem(skill);
		}
		skillBox.setSelectedItem(skill);
		targetSpinner.setValue(Math.max(2, Math.min(99, level)));
		recalculate();
	}

	@Override
	public void refresh()
	{
		if (calculated)
		{
			recalculate();
		}
	}

	private void recalculate()
	{
		calculated = true;
		results.removeAll();

		String skill = (String) skillBox.getSelectedItem();
		if (skill == null)
		{
			message("No skill data is bundled yet.", Ui.MUTED);
			return;
		}
		int xp = plugin.xp(skill);
		if (xp < 0)
		{
			message("Log in to the game so the plugin can read your " + Text.pretty(skill) + " XP.", Ui.WARN);
			return;
		}

		int target = (Integer) targetSpinner.getValue();
		int level = XpTable.levelForXp(xp);
		long needed = GoalCalculator.xpNeeded(xp, target);
		results.add(Ui.label("<b>" + Text.pretty(skill) + " " + level + " → " + target + "</b><br>"
			+ Text.number(needed) + " XP to go", Color.WHITE));
		if (needed == 0)
		{
			results.add(Ui.label("You already have this level.", Ui.MUTED));
			Ui.refresh(results);
			return;
		}
		results.add(Ui.button("Add as a goal", () -> plugin.addGoal(Goal.skill(skill, target))));

		List<TrainingMethod> methods = plugin.methods().methodsFor(skill);
		if (methods.isEmpty())
		{
			results.add(Ui.label("The wiki lists no per-hour rates for this skill yet.", Ui.WARN));
			Ui.refresh(results);
			return;
		}

		Playstyle style = (Playstyle) styleBox.getSelectedItem();
		SortMode sort = (SortMode) sortBox.getSelectedItem();
		results.add(pathCard(Planner.fastestPath(methods, xp, target, style)));
		for (MethodResult result : GoalCalculator.rank(methods, xp, target, style, sort))
		{
			results.add(methodCard(result));
		}
		Ui.refresh(results);
	}

	private JPanel pathCard(Planner.PathResult path)
	{
		StringBuilder html = new StringBuilder("<b>Fastest overall path</b>");
		if (path.segments.isEmpty())
		{
			html.append("<br>No method with a wiki rate for your level.");
		}
		for (Planner.Segment segment : path.segments)
		{
			html.append("<br>").append(segment.fromLevel).append(" → ").append(segment.toLevel).append(": ")
				.append(Ui.esc(segment.method.name)).append(" (").append(GoalCalculator.formatHours(segment.hours))
				.append(")");
		}
		if (path.complete)
		{
			html.append("<br><b>Total: ").append(GoalCalculator.formatHours(path.hours)).append("</b>");
			html.append("<br>GP overall: ").append(Ui.signed(path.gpTotal));
		}
		else if (!path.segments.isEmpty())
		{
			html.append("<br>No wiki rate from level ").append(path.blockedAtLevel).append(" onward.");
		}
		return Ui.card(Ui.label(html.toString(), Ui.GOOD));
	}

	private JPanel methodCard(MethodResult r)
	{
		StringBuilder html = new StringBuilder("<b>").append(Ui.esc(r.method.name)).append("</b>");
		if (!r.eligible)
		{
			html.append("<br>").append(Ui.esc(r.blockedReason));
		}
		else
		{
			html.append("<br>XP/hr (average): ")
				.append(r.xpPerHour == null ? "no wiki rate" : Text.number(Math.round(r.xpPerHour)));
			html.append("<br>Time: ").append(r.hoursNeeded == null ? "unknown" : GoalCalculator.formatHours(r.hoursNeeded));
			html.append("<br>GP/hr: ").append(Ui.signed(r.netGpPerHour));
			html.append("<br>GP total: ").append(Ui.signed(r.netGpTotal));
		}
		JLabel main = Ui.label(html.toString(), r.eligible ? Color.WHITE : Ui.MUTED);

		StringBuilder extra = new StringBuilder();
		if (r.method.requirements != null && !r.method.requirements.isEmpty())
		{
			extra.append("Needs: ");
			for (int i = 0; i < r.method.requirements.size(); i++)
			{
				extra.append(i > 0 ? "; " : "").append(Ui.esc(r.method.requirements.get(i)));
			}
		}
		if (r.method.notes != null && !r.method.notes.isEmpty())
		{
			extra.append(extra.length() > 0 ? "<br>" : "").append(Ui.esc(r.method.notes));
		}
		if (r.method.retrieved != null)
		{
			extra.append(extra.length() > 0 ? "<br>" : "").append("OSRS Wiki rates, read ")
				.append(Ui.esc(r.method.retrieved));
		}
		return extra.length() == 0 ? Ui.card(main) : Ui.card(main, Ui.label("<small>" + extra + "</small>", Ui.MUTED));
	}

	private void message(String text, Color color)
	{
		results.add(Ui.label(text, color));
		Ui.refresh(results);
	}
}
