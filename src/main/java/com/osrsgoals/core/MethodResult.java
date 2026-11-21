package com.osrsgoals.core;

/** The planner's answer for one method toward one goal. Null numbers mean "the wiki lists no rate". */
public class MethodResult
{
	public final TrainingMethod method;
	public final boolean eligible;
	/** Why the method can't be used yet (e.g. "Needs level 76"); null when eligible. */
	public final String blockedReason;
	public final long xpNeeded;
	/** Average XP/hr over the whole goal (rates change as you level). */
	public final Double xpPerHour;
	public final Double hoursNeeded;
	/** Average GP per hour over the goal (negative = costs money). Wiki snapshot prices. */
	public final Double netGpPerHour;
	/** Total GP gained (positive) or spent (negative) over the goal. */
	public final Double netGpTotal;

	public MethodResult(TrainingMethod method, boolean eligible, String blockedReason, long xpNeeded,
		Double xpPerHour, Double hoursNeeded, Double netGpPerHour, Double netGpTotal)
	{
		this.method = method;
		this.eligible = eligible;
		this.blockedReason = blockedReason;
		this.xpNeeded = xpNeeded;
		this.xpPerHour = xpPerHour;
		this.hoursNeeded = hoursNeeded;
		this.netGpPerHour = netGpPerHour;
		this.netGpTotal = netGpTotal;
	}
}
