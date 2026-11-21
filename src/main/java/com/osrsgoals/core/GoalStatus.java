package com.osrsgoals.core;

/** Where a goal stands right now. */
public final class GoalStatus
{
	public final boolean complete;
	/** Short progress text, e.g. "Fishing 78/82" or "4/6 tasks done". */
	public final String progress;
	/** Estimated hours left with the fastest wiki methods; null if unknown. */
	public final Double hoursRemaining;
	/** False if a requirement you can check (level, quest) is not met yet. */
	public final boolean doableNow;
	/** What to do next, in a sentence. */
	public final String nextStep;

	public GoalStatus(boolean complete, String progress, Double hoursRemaining, boolean doableNow, String nextStep)
	{
		this.complete = complete;
		this.progress = progress;
		this.hoursRemaining = hoursRemaining;
		this.doableNow = doableNow;
		this.nextStep = nextStep;
	}
}
