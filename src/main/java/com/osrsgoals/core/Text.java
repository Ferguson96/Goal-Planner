package com.osrsgoals.core;

import java.util.Locale;

/** Small formatting helpers shared by the core logic and the UI. */
public final class Text
{
	private Text()
	{
	}

	/** "RUNECRAFT" becomes "Runecraft". */
	public static String pretty(String enumName)
	{
		if (enumName == null || enumName.isEmpty())
		{
			return "";
		}
		return enumName.substring(0, 1).toUpperCase(Locale.ROOT) + enumName.substring(1).toLowerCase(Locale.ROOT);
	}

	/** 1234567 becomes "1,234,567". */
	public static String number(long value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	/** 12.5 -> "12.5", 10.0 -> "10", 1234.5 -> "1,234.5". */
	public static String decimal(double value)
	{
		if (value == Math.rint(value))
		{
			return number((long) value);
		}
		return String.format(Locale.US, "%,.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	public static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
