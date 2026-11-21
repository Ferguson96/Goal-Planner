package com.osrsgoals.ui;

import com.osrsgoals.GoalPlannerPlugin;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

/** The sidebar panel: a view switcher on top and one of four views below. */
public class GoalPlannerPanel extends PluginPanel
{
	private static final String[] VIEWS = {"Skills", "Diaries", "Bosses", "Goals and wheel"};

	private final JComboBox<String> viewBox = new JComboBox<>(VIEWS);
	private final JPanel content = new JPanel(new BorderLayout());
	private final SkillsTab skillsTab;
	private final DiariesTab diariesTab;
	private final BossesTab bossesTab;
	private final GoalsTab goalsTab;
	private Refreshable current;

	public GoalPlannerPanel(GoalPlannerPlugin plugin)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		skillsTab = new SkillsTab(plugin);
		diariesTab = new DiariesTab(plugin, this::openSkillPlan);
		bossesTab = new BossesTab(plugin, this::openSkillPlan);
		goalsTab = new GoalsTab(plugin);

		JPanel top = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		top.setOpaque(false);
		top.add(viewBox);
		top.add(Ui.button("Refresh from game", plugin::refreshNow));

		content.setOpaque(false);
		add(top, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		viewBox.addActionListener(e -> showSelected());
		showSelected();
	}

	private void showSelected()
	{
		Component tab;
		switch (viewBox.getSelectedIndex())
		{
			case 1:
				tab = diariesTab;
				break;
			case 2:
				tab = bossesTab;
				break;
			case 3:
				tab = goalsTab;
				break;
			default:
				tab = skillsTab;
				break;
		}
		content.removeAll();
		content.add(tab, BorderLayout.NORTH);
		current = (Refreshable) tab;
		current.refresh();
		Ui.refresh(content);
	}

	/** Redraws the visible view from the latest game state. */
	public void refresh()
	{
		if (current != null)
		{
			current.refresh();
		}
	}

	/** Called when a goal is added, removed or changed somewhere. */
	public void goalsChanged()
	{
		goalsTab.refresh();
	}

	/** Used by the diary and boss views: open the Skills view with a plan for this level. */
	void openSkillPlan(String skill, Integer level)
	{
		viewBox.setSelectedIndex(0);
		skillsTab.plan(skill, level);
	}
}
