package com.osrsgoals.core;

/** A quest's state as far as the planner cares. UNKNOWN = not logged in, or the name isn't a known quest. */
public enum QuestProgress
{
	NOT_STARTED,
	IN_PROGRESS,
	FINISHED,
	UNKNOWN
}
