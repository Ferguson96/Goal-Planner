package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.GoalStatus;
import com.osrsgoals.core.GoalType;
import com.osrsgoals.core.WeightMode;
import com.osrsgoals.core.Wheel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Your saved goals with their progress, and the wheel that picks what to work on next. */
class GoalsTab extends JPanel implements Refreshable
{
	private final GoalPlannerPlugin plugin;
	private final Random random = new Random();
	private final JComboBox<WeightMode> modeBox = new JComboBox<>(WeightMode.values());
	private final JCheckBox doableBox = new JCheckBox("Only goals I can start now");
	private final JPanel wheelResult = Ui.column();
	private final JPanel list = Ui.column();
	private Timer spinning;

	GoalsTab(GoalPlannerPlugin plugin)
	{
		super(new BorderLayout(0, 8));
		this.plugin = plugin;
		setOpaque(false);

		doableBox.setOpaque(false);
		doableBox.setSelected(true);

		JPanel wheel = Ui.column();
		wheel.add(Ui.label("<b>What should I work on next?</b>", Color.WHITE));
		JPanel options = new JPanel(new GridLayout(0, 1, 0, 4));
		options.setOpaque(false);
		options.add(modeBox);
		options.add(doableBox);
		wheel.add(options);
		wheel.add(Ui.button("Spin the wheel", this::spin));
		wheel.add(wheelResult);

		add(wheel, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
		refresh();
	}

	@Override
	public void refresh()
	{
		list.removeAll();
		list.add(Ui.label("<b>Your goals</b>", Color.WHITE));
		List<Goal> goals = plugin.store().goals();
		if (goals.isEmpty())
		{
			list.add(Ui.label("No goals yet. Use \"Add as a goal\" in the Skills, Diaries and Bosses views.",
				Ui.MUTED));
		}
		for (Goal goal : goals)
		{
			list.add(goalCard(goal));
		}
		Ui.refresh(list);
	}

	private JPanel goalCard(Goal goal)
	{
		GoalStatus status = plugin.evaluator().evaluate(goal);
		Color color = status.complete ? Ui.GOOD : Color.WHITE;
		StringBuilder html = new StringBuilder("<b>").append(Ui.esc(goal.label)).append("</b>");
		html.append("<br>").append(Ui.esc(status.progress));
		if (!status.complete)
		{
			if (status.hoursRemaining != null)
			{
				html.append("<br>About ").append(GoalCalculator.formatHours(status.hoursRemaining)).append(" left");
			}
			if (!status.doableNow)
			{
				html.append("<br>Requirements not met yet");
			}
			if (status.nextStep != null && !status.nextStep.isEmpty())
			{
				html.append("<br><small>").append(Ui.esc(status.nextStep)).append("</small>");
			}
		}

		JSpinner priority = new JSpinner(new SpinnerNumberModel(Math.max(1, Math.min(5, goal.priority)), 1, 5, 1));
		priority.addChangeListener(e ->
		{
			goal.priority = (Integer) priority.getValue();
			plugin.store().updateGoal(goal);
		});
		JPanel priorityRow = new JPanel(new BorderLayout(4, 0));
		priorityRow.setOpaque(false);
		priorityRow.add(Ui.label("Priority (5 = high)", Ui.MUTED, 110), BorderLayout.CENTER);
		priorityRow.add(priority, BorderLayout.EAST);

		JPanel buttons = new JPanel(new GridLayout(1, 0, 4, 0));
		buttons.setOpaque(false);
		if (goal.type == GoalType.DROP)
		{
			buttons.add(Ui.button(goal.done ? "Undo" : "Got it", () ->
			{
				goal.done = !goal.done;
				plugin.store().updateGoal(goal);
				refresh();
			}));
		}
		buttons.add(Ui.button("Remove", () ->
		{
			plugin.store().removeGoal(goal.id);
			refresh();
		}));

		return Ui.card(Ui.label(html.toString(), color), priorityRow, buttons);
	}

	private void spin()
	{
		if (spinning != null && spinning.isRunning())
		{
			spinning.stop();
		}
		List<Wheel.Entry> entries = new ArrayList<>();
		for (Goal goal : plugin.store().goals())
		{
			entries.add(new Wheel.Entry(goal, plugin.evaluator().evaluate(goal)));
		}
		WeightMode mode = (WeightMode) modeBox.getSelectedItem();
		boolean onlyDoable = doableBox.isSelected();
		Wheel.Entry winner = Wheel.pick(entries, mode, onlyDoable, random);
		wheelResult.removeAll();
		if (winner == null)
		{
			wheelResult.add(Ui.label(entries.isEmpty() ? "Add some goals first."
				: "Nothing to pick: every goal is finished" + (onlyDoable ? " or not startable yet." : "."), Ui.WARN));
			Ui.refresh(wheelResult);
			return;
		}

		List<Wheel.Entry> pool = Wheel.candidates(entries, onlyDoable);
		JLabel rolling = Ui.label("", Ui.WARN);
		wheelResult.add(rolling);
		Ui.refresh(wheelResult);

		final int frames = 18;
		final int[] frame = {0};
		spinning = new Timer(60, null);
		spinning.addActionListener(e ->
		{
			frame[0]++;
			if (frame[0] >= frames)
			{
				spinning.stop();
				showWinner(winner);
				return;
			}
			Wheel.Entry shown = pool.get(random.nextInt(pool.size()));
			rolling.setText("<html><body style='width:185px'><b>" + Ui.esc(shown.goal.label) + "</b></body></html>");
			spinning.setDelay(60 + frame[0] * 12);
		});
		spinning.start();
	}

	private void showWinner(Wheel.Entry winner)
	{
		wheelResult.removeAll();
		StringBuilder html = new StringBuilder("<b>Next up: ").append(Ui.esc(winner.goal.label)).append("</b>");
		html.append("<br>").append(Ui.esc(winner.status.progress));
		if (winner.status.nextStep != null && !winner.status.nextStep.isEmpty())
		{
			html.append("<br>").append(Ui.esc(winner.status.nextStep));
		}
		wheelResult.add(Ui.card(Ui.label(html.toString(), Ui.GOOD)));
		Ui.refresh(wheelResult);
		SwingUtilities.invokeLater(() -> Ui.refresh(this));
	}
}
