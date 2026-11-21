package com.osrsgoals.data;

import com.google.gson.Gson;
import com.osrsgoals.core.TrainingMethod;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads training methods from the JSON files bundled in the plugin (resources/methods/&lt;skill&gt;.json).
 * Drop a new skill file in that folder and it is picked up automatically; missing files are skipped.
 */
@Slf4j
public class MethodRepository
{
	/** Every skill in the game; a file named after the skill (lower case) is loaded if it exists. */
	private static final String[] SKILLS = {
		"attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking",
		"woodcutting", "fletching", "fishing", "firemaking", "crafting", "smithing", "mining",
		"herblore", "agility", "thieving", "slayer", "farming", "runecraft", "hunter",
		"construction", "sailing"
	};

	private final Map<String, List<TrainingMethod>> bySkill = new TreeMap<>();

	/** Loads every bundled skill file, using RuneLite's shared Gson. */
	public void load(Gson gson)
	{
		for (String skill : SKILLS)
		{
			String path = "/methods/" + skill + ".json";
			try (InputStream in = MethodRepository.class.getResourceAsStream(path))
			{
				if (in == null)
				{
					log.debug("No methods file for {}", skill);
					continue;
				}
				TrainingMethod[] methods = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
					TrainingMethod[].class);
				for (TrainingMethod method : methods)
				{
					bySkill.computeIfAbsent(method.skill, k -> new ArrayList<>()).add(method);
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Could not load {}", path, e);
			}
		}
	}

	/** Skill names (matching RuneLite's Skill enum, e.g. "FISHING") that have methods loaded. */
	public List<String> skills()
	{
		return new ArrayList<>(bySkill.keySet());
	}

	public List<TrainingMethod> methodsFor(String skill)
	{
		return bySkill.getOrDefault(skill, Collections.emptyList());
	}
}
