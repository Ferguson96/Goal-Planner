package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;

/** One boss, loaded from src/main/resources/bosses/&lt;id&gt;.json. See docs/boss-schema.md. */
public class Boss
{
	public String id;
	public String name;
	public String category;
	public String source;
	public String retrieved;
	public RequirementSet requirements = new RequirementSet();
	/** Kills per hour, only if a wiki page states one. */
	public Double killsPerHour;
	public String killsPerHourSource;
	public List<BossDrop> drops = new ArrayList<>();
	public List<BossSetup> setups = new ArrayList<>();
	public String notes;
}
