package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import com.osrsgoals.core.Diary;
import com.osrsgoals.core.DiaryTask;
import com.osrsgoals.core.DiaryTier;
import com.osrsgoals.core.GapSummary;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.SkillRequirement;
import com.osrsgoals.core.Text;
import com.osrsgoals.data.GoalStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * An achievement diary and tier: which tasks are done, what levels and quests the rest need,
 * and how long the missing levels should take with the fastest wiki methods.
 */
class DiariesTab extends JPanel implements Refreshable
{
	private final GoalPlannerPlugin plugin;
	private final BiConsumer<String, Integer> openPlan;
	private final JComboBox<Diary> diaryBox = new JComboBox<>();
	private final JComboBox<DiaryTier> tierBox = new JComboBox<>(DiaryTier.values());
	private final JPanel body = Ui.column();

	DiariesTab(GoalPlannerPlugin plugin, BiConsumer<String, Integer> openPlan)
	{
		super(new BorderLayout(0, 8));
		this.plugin = plugin;
		this.openPlan = openPlan;
		setOpaque(false);

		for (Diary diary : plugin.diaries().all())
		{
			diaryBox.addItem(diary);
		}
		diaryBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				String name = value instanceof Diary ? ((Diary) value).name : "";
				return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
			}
		});
		tierBox.setSelectedItem(DiaryTier.ELITE);

		JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
		form.setOpaque(false);
		form.add(new JLabel("Diary"));
		form.add(diaryBox);
		form.add(new JLabel("Tier"));
		form.add(tierBox);

		diaryBox.addActionListener(e -> refresh());
		tierBox.addActionListener(e -> refresh());

		add(form, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		refresh();
	}

	@Override
	public void refresh()
	{
		body.removeAll();
		Diary diary = (Diary) diaryBox.getSelectedItem();
		DiaryTier tier = (DiaryTier) tierBox.getSelectedItem();
		if (diary == null || tier == null)
		{
			body.add(Ui.label("No diary data is bundled yet.", Ui.MUTED));
			Ui.refresh(body);
			return;
		}
		List<DiaryTask> tasks = diary.tasks(tier);
		if (tasks.isEmpty())
		{
			body.add(Ui.label("The wiki lists no tasks for this tier.", Ui.MUTED));
			Ui.refresh(body);
			return;
		}

		GoalStore store = plugin.store();
		List<DiaryTask> todo = new ArrayList<>();
		for (DiaryTask task : tasks)
		{
			if (!store.isTaskDone(task.id))
			{
				todo.add(task);
			}
		}
		int done = tasks.size() - todo.size();
		body.add(Ui.label("<b>" + Ui.esc(diary.name) + " – " + tier + "</b><br>" + done + "/" + tasks.size()
			+ " tasks done", todo.isEmpty() ? Ui.GOOD : Color.WHITE));
		body.add(Ui.button("Add as a goal", () -> plugin.addGoal(Goal.diary(diary.id, diary.name, tier))));

		body.add(Ui.label("<small>Tick tasks as you finish them. The game does not expose each task, so this is manual.</small>",
			Ui.MUTED));
		for (DiaryTask task : tasks)
		{
			JCheckBox box = new JCheckBox(Ui.html(Ui.esc(task.description), 150));
			box.setOpaque(false);
			box.setSelected(store.isTaskDone(task.id));
			box.addActionListener(e ->
			{
				store.setTaskDone(task.id, box.isSelected());
				plugin.goalsChanged();
				SwingUtilities.invokeLater(this::refresh);
			});
			String needs = requirementText(task);
			body.add(needs.isEmpty() ? Ui.card(box) : Ui.card(box, Ui.label("<small>" + needs + "</small>", Ui.MUTED)));
		}

		if (todo.isEmpty())
		{
			Ui.refresh(body);
			return;
		}

		GapSummary summary = new GapSummary();
		for (DiaryTask task : todo)
		{
			summary.add(shorten(task.description), task.skills, task.quests, task.questsStarted, task.other);
		}
		body.add(Ui.label("<b>What the " + todo.size() + " remaining tasks need</b>", Color.WHITE));
		GapView.render(body, summary, plugin, openPlan::accept);

		double hours = 0;
		int unknown = 0;
		for (GapSummary.SkillGap gap : summary.skillGaps(plugin::level))
		{
			if (!gap.known() || gap.met())
			{
				continue;
			}
			Double h = plugin.evaluator().bestHoursFor(gap.skill, gap.required);
			if (h == null)
			{
				unknown++;
			}
			else
			{
				hours += h;
			}
		}
		String estimate = "<b>Training still needed: " + GoalCalculator.formatHours(hours) + "</b><br>"
			+ "<small>with the fastest wiki methods; boosts not counted"
			+ (unknown > 0 ? "; " + unknown + " skill(s) have no wiki rate" : "") + "</small>";
		body.add(Ui.label(estimate, Ui.WARN));
		Ui.refresh(body);
	}

	private static String requirementText(DiaryTask task)
	{
		List<String> parts = new ArrayList<>();
		if (task.skills != null)
		{
			for (SkillRequirement skill : task.skills)
			{
				parts.add(Text.pretty(skill.skill) + " " + skill.level + (skill.boostable ? " (boostable)" : ""));
			}
		}
		if (task.quests != null)
		{
			for (String quest : task.quests)
			{
				parts.add("quest: " + Ui.esc(quest));
			}
		}
		if (task.questsStarted != null)
		{
			for (String quest : task.questsStarted)
			{
				parts.add("start quest: " + Ui.esc(quest));
			}
		}
		if (task.other != null)
		{
			for (String other : task.other)
			{
				parts.add(Ui.esc(other));
			}
		}
		return String.join("; ", parts);
	}

	private static String shorten(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.length() <= 60 ? text : text.substring(0, 57) + "...";
	}
}
