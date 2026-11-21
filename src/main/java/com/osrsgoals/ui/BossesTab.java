package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import com.osrsgoals.core.Boss;
import com.osrsgoals.core.BossDrop;
import com.osrsgoals.core.BossSetup;
import com.osrsgoals.core.DropMath;
import com.osrsgoals.core.GapSummary;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.GoalCalculator;
import com.osrsgoals.core.SetupChecker;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Pick a boss: requirements, drop odds and time, kill goals, and the gear checklist with its on/off switch. */
class BossesTab extends JPanel implements Refreshable
{
	private static final String GEAR_FLAG = "showGear";

	private final GoalPlannerPlugin plugin;
	private final BiConsumer<String, Integer> openPlan;
	private final JComboBox<Boss> bossBox = new JComboBox<>();
	private final JPanel body = Ui.column();
	private int setupIndex;

	BossesTab(GoalPlannerPlugin plugin, BiConsumer<String, Integer> openPlan)
	{
		super(new BorderLayout(0, 8));
		this.plugin = plugin;
		this.openPlan = openPlan;
		setOpaque(false);

		for (Boss boss : plugin.bosses().all())
		{
			bossBox.addItem(boss);
		}
		bossBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean selected, boolean focus)
			{
				String text = value instanceof Boss ? ((Boss) value).name : "";
				return super.getListCellRendererComponent(list, text, index, selected, focus);
			}
		});

		JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
		form.setOpaque(false);
		form.add(new JLabel("Boss"));
		form.add(bossBox);

		add(form, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);

		bossBox.addActionListener(e ->
		{
			setupIndex = 0;
			refresh();
		});
		refresh();
	}

	@Override
	public void refresh()
	{
		body.removeAll();
		Boss boss = (Boss) bossBox.getSelectedItem();
		if (boss == null)
		{
			body.add(Ui.label("No boss data is bundled yet.", Ui.MUTED));
			Ui.refresh(body);
			return;
		}
		GoalStore store = plugin.store();

		body.add(Ui.label("<b>" + Ui.esc(boss.name) + "</b>"
			+ (boss.category == null ? "" : "<br>" + Text.pretty(boss.category.replace('_', ' '))), Color.WHITE));

		// Requirements
		GapSummary summary = new GapSummary();
		summary.add(boss.name, boss.requirements);
		body.add(Ui.label("<b>Requirements</b>", Color.WHITE));
		if (summary.isEmpty())
		{
			body.add(Ui.label("The wiki lists none.", Ui.MUTED));
		}
		else
		{
			GapView.render(body, summary, plugin, openPlan::accept);
		}

		// Kills per hour
		Double own = store.killsPerHour(boss.id);
		JSpinner kphSpinner = new JSpinner(new SpinnerNumberModel(own == null ? 0 : own.intValue(), 0, 5000, 1));
		kphSpinner.addChangeListener(e ->
		{
			int value = (Integer) kphSpinner.getValue();
			store.setKillsPerHour(boss.id, value <= 0 ? null : (double) value);
			plugin.goalsChanged();
			SwingUtilities.invokeLater(this::refresh);
		});
		JPanel kphRow = new JPanel(new BorderLayout(4, 0));
		kphRow.setOpaque(false);
		kphRow.add(Ui.label("Your kills/hr (0 = use wiki figure)", Color.WHITE, 120), BorderLayout.CENTER);
		kphRow.add(kphSpinner, BorderLayout.EAST);
		body.add(kphRow);
		if (boss.killsPerHour != null)
		{
			body.add(Ui.label("<small>Wiki figure: " + Text.number(Math.round(boss.killsPerHour)) + " kills/hr</small>",
				Ui.MUTED));
		}
		Double kph = plugin.evaluator().killsPerHour(boss);
		int kc = plugin.killCount(boss.name);
		body.add(Ui.label("Kill count: " + (kc < 0 ? "not seen yet (it updates when the game prints it)" : kc),
			Ui.MUTED));

		// Kill-count goal
		JSpinner goalSpinner = new JSpinner(new SpinnerNumberModel(Math.max(100, kc + 1), 1, 100000, 1));
		JPanel goalRow = new JPanel(new BorderLayout(4, 0));
		goalRow.setOpaque(false);
		goalRow.add(goalSpinner, BorderLayout.CENTER);
		goalRow.add(Ui.button("Add kill goal", () ->
			plugin.addGoal(Goal.bossKills(boss.id, boss.name, (Integer) goalSpinner.getValue()))), BorderLayout.EAST);
		body.add(goalRow);

		// Drops
		body.add(Ui.label("<b>Rare drops</b>", Color.WHITE));
		if (boss.drops.isEmpty())
		{
			body.add(Ui.label("The wiki lists none rarer than 1/50.", Ui.MUTED));
		}
		for (BossDrop drop : boss.drops)
		{
			double expected = DropMath.expectedKills(drop.chance);
			StringBuilder html = new StringBuilder("<b>").append(Ui.esc(drop.item)).append("</b> ")
				.append(Ui.esc(drop.rarity));
			html.append("<br>Average: ").append(Text.number(Math.round(expected))).append(" kills");
			html.append("<br>50% by ").append(Text.number(DropMath.killsForConfidence(drop.chance, 0.5)))
				.append(", 90% by ").append(Text.number(DropMath.killsForConfidence(drop.chance, 0.9)));
			Double hours = DropMath.hoursFor(expected, kph);
			if (hours != null)
			{
				html.append("<br>About ").append(GoalCalculator.formatHours(hours)).append(" on average");
			}
			if (drop.notes != null && !drop.notes.isEmpty())
			{
				html.append("<br><small>").append(Ui.esc(drop.notes)).append("</small>");
			}
			body.add(Ui.card(Ui.label(html.toString(), Color.WHITE),
				Ui.button("Add drop goal", () -> plugin.addGoal(Goal.drop(boss.id, boss.name, drop.item)))));
		}

		// Gear checklist
		JCheckBox gear = new JCheckBox("Show recommended gear and inventory");
		gear.setOpaque(false);
		gear.setSelected(store.flag(GEAR_FLAG, false));
		gear.addActionListener(e ->
		{
			store.setFlag(GEAR_FLAG, gear.isSelected());
			SwingUtilities.invokeLater(this::refresh);
		});
		body.add(gear);
		if (gear.isSelected())
		{
			renderSetup(boss);
		}
		Ui.refresh(body);
	}

	private void renderSetup(Boss boss)
	{
		if (boss.setups.isEmpty())
		{
			body.add(Ui.label("The wiki lists no recommended setup for this boss.", Ui.MUTED));
			return;
		}
		setupIndex = Math.max(0, Math.min(setupIndex, boss.setups.size() - 1));
		if (boss.setups.size() > 1)
		{
			JComboBox<String> setupBox = new JComboBox<>();
			for (BossSetup setup : boss.setups)
			{
				setupBox.addItem(setup.name);
			}
			setupBox.setSelectedIndex(setupIndex);
			setupBox.addActionListener(e ->
			{
				setupIndex = setupBox.getSelectedIndex();
				SwingUtilities.invokeLater(this::refresh);
			});
			body.add(setupBox);
		}
		BossSetup setup = boss.setups.get(setupIndex);
		SetupChecker.Report report = SetupChecker.check(setup, plugin::owned, plugin::price);

		body.add(Ui.label("<b>Equipment</b>", Color.WHITE));
		for (SetupChecker.Row row : report.equipment)
		{
			body.add(rowLabel(row, false));
		}
		body.add(Ui.label("<b>Inventory</b>", Color.WHITE));
		for (SetupChecker.Row row : report.inventory)
		{
			body.add(rowLabel(row, true));
		}

		String total;
		if (report.missingRows == 0)
		{
			total = "You have everything in this setup.";
		}
		else
		{
			total = report.missingRows + " item" + (report.missingRows == 1 ? "" : "s") + " missing, about "
				+ Text.number(report.missingCost) + " gp"
				+ (report.unpriced > 0 ? " (" + report.unpriced + " without a Grand Exchange price)" : "");
		}
		body.add(Ui.label("<b>" + total + "</b>", report.missingRows == 0 ? Ui.GOOD : Ui.WARN));
		if (!plugin.bankSeen())
		{
			body.add(Ui.label("<small>Open your bank once so the plugin can count what is in it.</small>", Ui.MUTED));
		}
		body.add(Ui.label("<small>Source: " + Ui.esc(setup.source) + "</small>", Ui.MUTED));
		if (setup.notes != null && !setup.notes.isEmpty())
		{
			body.add(Ui.label("<small>" + Ui.esc(setup.notes) + "</small>", Ui.MUTED));
		}
	}

	private JLabel rowLabel(SetupChecker.Row row, boolean inventory)
	{
		List<String> options = new ArrayList<>();
		for (String option : row.options)
		{
			options.add(Ui.esc(option));
		}
		String title = inventory ? options.get(0) + " x" + row.needed
			: Ui.esc(row.label) + ": " + String.join(" / ", options);
		if (row.satisfied())
		{
			String shown = inventory ? title + " (have " + row.owned + ")"
				: Ui.esc(row.label) + ": " + Ui.esc(row.ownedItem);
			return Ui.label("✔ " + shown, Ui.GOOD);
		}
		String cost = row.missingCost == null ? " – no price" : " – about " + Text.number(row.missingCost) + " gp";
		String have = inventory ? " (have " + row.owned + ")" : "";
		return Ui.label("✘ " + title + have + cost, Ui.BAD);
	}
}
