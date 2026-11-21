package com.osrsgoals.data;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads the data files bundled inside the plugin. */
final class JsonResources
{
	private static final Logger log = LoggerFactory.getLogger(JsonResources.class);

	private JsonResources()
	{
	}

	/** Parses a bundled JSON file; null if it is missing or broken (the problem is logged). */
	static <T> T read(Gson gson, String path, Class<T> type)
	{
		try (InputStream in = JsonResources.class.getResourceAsStream(path))
		{
			if (in == null)
			{
				log.debug("No bundled file {}", path);
				return null;
			}
			return gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not read {}", path, e);
			return null;
		}
	}

	/** Non-empty lines of a bundled text file, ignoring lines starting with '#'. */
	static List<String> lines(String path)
	{
		List<String> lines = new ArrayList<>();
		try (InputStream in = JsonResources.class.getResourceAsStream(path))
		{
			if (in == null)
			{
				return lines;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#"))
				{
					lines.add(line);
				}
			}
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", path, e);
		}
		return lines;
	}
}
