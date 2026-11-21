package com.osrsgoals.core;

import java.util.Locale;

/** Name matching helpers: wiki names, RuneLite names and chat messages differ in spacing and punctuation. */
public final class Names
{
	private Names()
	{
	}

	/** Lower case with everything except letters and digits removed ("Dragon Slayer I" matches "dragon slayer i"). */
	public static String key(String name)
	{
		if (name == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (char c : name.toLowerCase(Locale.ROOT).toCharArray())
		{
			if (Character.isLetterOrDigit(c))
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/** Like key, but without a leading "the" ("The Gauntlet" matches "Gauntlet"). */
	public static String bossKey(String name)
	{
		String key = key(name);
		return key.startsWith("the") && key.length() > 6 ? key.substring(3) : key;
	}

	/** Item names are compared case-insensitively. */
	public static String item(String name)
	{
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}
}
