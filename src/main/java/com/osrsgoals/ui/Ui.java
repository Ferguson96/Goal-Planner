package com.osrsgoals.ui;

import com.osrsgoals.core.Text;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/** Small helpers so every view looks the same. */
final class Ui
{
	static final Color GOOD = new Color(0x6FCF6F);
	static final Color BAD = new Color(0xE57373);
	static final Color WARN = new Color(0xE0B040);
	static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private Ui()
	{
	}

	/** A vertical stack where every row gets the full width and its own height. */
	static JPanel column()
	{
		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
		panel.setOpaque(false);
		return panel;
	}

	static String html(String inner, int width)
	{
		return "<html><body style='width:" + width + "px'>" + inner + "</body></html>";
	}

	static JLabel label(String inner, Color color)
	{
		return label(inner, color, 185);
	}

	static JLabel label(String inner, Color color, int width)
	{
		JLabel label = new JLabel(html(inner, width));
		label.setForeground(color);
		return label;
	}

	static JPanel card(JComponent... parts)
	{
		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		for (JComponent part : parts)
		{
			card.add(part);
		}
		return card;
	}

	static JButton button(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.addActionListener(e -> action.run());
		return button;
	}

	static String esc(String text)
	{
		return Text.escapeHtml(text);
	}

	/** "needed for: task +2 more" */
	static String neededBy(String driver, int alsoNeededBy)
	{
		if (driver == null || driver.isEmpty())
		{
			return "";
		}
		return "needed for: " + esc(driver) + (alsoNeededBy > 0 ? " +" + alsoNeededBy + " more" : "");
	}

	/** "+1,234", "-1,234" or "n/a". */
	static String signed(Double gp)
	{
		if (gp == null)
		{
			return "n/a";
		}
		long rounded = Math.round(gp);
		return (rounded >= 0 ? "+" : "-") + Text.number(Math.abs(rounded));
	}

	static void refresh(JComponent component)
	{
		component.revalidate();
		component.repaint();
	}
}
