package com.osrsgoals;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Run this class (or `./gradlew run`) to start RuneLite in developer mode with the plugin loaded. */
public class GoalPlannerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GoalPlannerPlugin.class);
		RuneLite.main(args);
	}
}
