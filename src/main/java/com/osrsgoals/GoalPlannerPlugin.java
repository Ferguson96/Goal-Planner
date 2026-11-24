package com.osrsgoals;

import com.google.gson.Gson;
import com.osrsgoals.core.DiaryTier;
import com.osrsgoals.core.Goal;
import com.osrsgoals.core.Names;
import com.osrsgoals.core.QuestProgress;
import com.osrsgoals.core.XpTable;
import com.osrsgoals.data.BossRepository;
import com.osrsgoals.data.DiaryRepository;
import com.osrsgoals.data.GoalEvaluator;
import com.osrsgoals.data.GoalStore;
import com.osrsgoals.data.MethodRepository;
import com.osrsgoals.ui.GoalPlannerPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Goal Planner",
	description = "Set skill, diary, boss and drop goals; see time and cost from OSRS Wiki rates; spin a wheel for what to do next",
	tags = {"goals", "skilling", "calculator", "planner", "diary", "boss", "drops"}
)
public class GoalPlannerPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "goalplanner";
	/** "Your Zulrah kill count is: 12." and similar. */
	private static final Pattern KILL_COUNT = Pattern.compile("^Your (.+?) (?:kill|chest|completion) count is: ([\\d,]+)");
	/** "Your completed Chambers of Xeric count is: 3." */
	private static final Pattern COMPLETED_COUNT = Pattern.compile("^Your completed (.+?) count is: ([\\d,]+)");
	private static final int QUEST_REFRESH_TICKS = 100;
	/** "Tier complete" varbits per diary, in Easy, Medium, Hard, Elite order. */
	private static final Map<String, int[]> DIARY_TIER_VARBITS = new HashMap<>();

	static
	{
		DIARY_TIER_VARBITS.put("ardougne", new int[]{VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
			VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("desert", new int[]{VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
			VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("falador", new int[]{VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
			VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("fremennik", new int[]{VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
			VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("kandarin", new int[]{VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
			VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("karamja", new int[]{VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
			VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("kourend_kebos", new int[]{VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
			VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("lumbridge_draynor", new int[]{VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
			VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("morytania", new int[]{VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
			VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("varrock", new int[]{VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
			VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("western_provinces", new int[]{VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
			VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE});
		DIARY_TIER_VARBITS.put("wilderness", new int[]{VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
			VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE});
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	private final MethodRepository methods = new MethodRepository();
	private final DiaryRepository diaries = new DiaryRepository();
	private final BossRepository bosses = new BossRepository();
	private GoalStore store;
	private GoalEvaluator evaluator;

	/** XP per skill, -1 until seen. Written on the client thread, read by the UI. */
	private final AtomicIntegerArray xpCache = new AtomicIntegerArray(Skill.values().length);
	private final Map<String, QuestProgress> questCache = new ConcurrentHashMap<>();
	/** "diaryId:TIER" for every tier the game reports as complete. */
	private final Map<String, Boolean> diaryTierCache = new ConcurrentHashMap<>();
	private final Map<String, Integer> priceCache = new ConcurrentHashMap<>();
	private volatile Map<String, Integer> inventoryItems = Collections.emptyMap();
	private volatile Map<String, Integer> equipmentItems = Collections.emptyMap();
	private volatile Map<String, Integer> bankItems = Collections.emptyMap();
	private volatile boolean bankSeen;
	private int ticks;

	private GoalPlannerPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		for (int i = 0; i < xpCache.length(); i++)
		{
			xpCache.set(i, -1);
		}
		methods.load(gson);
		diaries.load(gson);
		bosses.load(gson);

		store = new GoalStore(new GoalStore.Backend()
		{
			@Override
			public String get(String key)
			{
				return configManager.getConfiguration(CONFIG_GROUP, key);
			}

			@Override
			public void set(String key, String value)
			{
				configManager.setConfiguration(CONFIG_GROUP, key, value);
			}
		}, gson);
		evaluator = new GoalEvaluator(methods, diaries, bosses, store, new GoalEvaluator.State()
		{
			@Override
			public int xp(String skill)
			{
				return GoalPlannerPlugin.this.xp(skill);
			}

			@Override
			public QuestProgress quest(String quest)
			{
				return GoalPlannerPlugin.this.quest(quest);
			}

			@Override
			public int killCount(String bossName)
			{
				return GoalPlannerPlugin.this.killCount(bossName);
			}

			@Override
			public boolean diaryTierComplete(String diaryId, DiaryTier tier)
			{
				return GoalPlannerPlugin.this.diaryTierComplete(diaryId, tier);
			}
		});

		panel = new GoalPlannerPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Goal Planner")
			.icon(buildIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		// If the plugin is switched on while already logged in, read everything right away.
		refreshNow();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	// ---- game events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshAll();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		xpCache.set(event.getSkill().ordinal(), event.getXp());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbit = event.getVarbitId();
		for (int[] ids : DIARY_TIER_VARBITS.values())
		{
			for (int id : ids)
			{
				if (id == varbit)
				{
					refreshDiaries();
					return;
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (++ticks % QUEST_REFRESH_TICKS == 0)
		{
			refreshQuests();
			refreshDiaries();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer container = event.getItemContainer();
		if (container == client.getItemContainer(InventoryID.INVENTORY))
		{
			inventoryItems = snapshot(container);
		}
		else if (container == client.getItemContainer(InventoryID.EQUIPMENT))
		{
			equipmentItems = snapshot(container);
		}
		else if (container == client.getItemContainer(InventoryID.BANK))
		{
			bankItems = snapshot(container);
			bankSeen = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE || store == null)
		{
			return;
		}
		String message = net.runelite.client.util.Text.removeTags(event.getMessage());
		Matcher matcher = KILL_COUNT.matcher(message);
		if (!matcher.find())
		{
			matcher = COMPLETED_COUNT.matcher(message);
			if (!matcher.find())
			{
				return;
			}
		}
		try
		{
			store.setKillCount(Names.bossKey(matcher.group(1)), Integer.parseInt(matcher.group(2).replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			log.debug("Could not read kill count from: {}", message);
		}
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.goalsChanged();
			}
		});
	}

	// ---- reading the game (client thread) ----

	/** Reads XP, quests and items on the client thread, then redraws the panel. Safe to call from anywhere. */
	public void refreshNow()
	{
		clientThread.invokeLater(this::refreshAll);
	}

	private void refreshAll()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				xpCache.set(skill.ordinal(), client.getSkillExperience(skill));
			}
		}
		refreshQuests();
		refreshDiaries();
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null)
		{
			inventoryItems = snapshot(inventory);
		}
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null)
		{
			equipmentItems = snapshot(equipment);
		}
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.refresh();
			}
		});
	}

	/** Reads which diary tiers the game reports as complete. Client thread only. */
	private void refreshDiaries()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		DiaryTier[] tiers = DiaryTier.values();
		for (Map.Entry<String, int[]> entry : DIARY_TIER_VARBITS.entrySet())
		{
			for (int i = 0; i < tiers.length; i++)
			{
				boolean complete = client.getVarbitValue(entry.getValue()[i]) >= 1;
				diaryTierCache.put(entry.getKey() + ":" + tiers[i].name(), complete);
			}
		}
	}

	/** True if the game reports this diary tier as finished (false until logged in). */
	public boolean diaryTierComplete(String diaryId, DiaryTier tier)
	{
		return Boolean.TRUE.equals(diaryTierCache.get(diaryId + ":" + tier.name()));
	}

	private void refreshQuests()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		for (Quest quest : Quest.values())
		{
			try
			{
				QuestState state = quest.getState(client);
				QuestProgress progress;
				switch (state)
				{
					case FINISHED:
						progress = QuestProgress.FINISHED;
						break;
					case IN_PROGRESS:
						progress = QuestProgress.IN_PROGRESS;
						break;
					default:
						progress = QuestProgress.NOT_STARTED;
						break;
				}
				questCache.put(Names.key(quest.getName()), progress);
			}
			catch (RuntimeException e)
			{
				log.debug("Could not read quest {}", quest, e);
			}
		}
	}

	private Map<String, Integer> snapshot(ItemContainer container)
	{
		Map<String, Integer> counts = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(item.getId()).getName();
			if (name == null || name.equals("null"))
			{
				continue;
			}
			counts.merge(Names.item(name), item.getQuantity(), Integer::sum);
		}
		return counts;
	}

	// ---- what the panel asks for (safe to call from the UI thread) ----

	public MethodRepository methods()
	{
		return methods;
	}

	public DiaryRepository diaries()
	{
		return diaries;
	}

	public BossRepository bosses()
	{
		return bosses;
	}

	public GoalStore store()
	{
		return store;
	}

	public GoalEvaluator evaluator()
	{
		return evaluator;
	}

	/** Total XP for a skill (RuneLite skill name such as "FISHING"), or -1 if unknown. */
	public int xp(String skillName)
	{
		try
		{
			return xpCache.get(Skill.valueOf(skillName).ordinal());
		}
		catch (IllegalArgumentException e)
		{
			return -1;
		}
	}

	/** Level for a skill, or -1 if unknown. */
	public int level(String skillName)
	{
		int xp = xp(skillName);
		return xp < 0 ? -1 : XpTable.levelForXp(xp);
	}

	public QuestProgress quest(String wikiName)
	{
		QuestProgress progress = questCache.get(Names.key(wikiName));
		return progress == null ? QuestProgress.UNKNOWN : progress;
	}

	/** Kill count seen in chat, or -1. */
	public int killCount(String bossName)
	{
		return store == null ? -1 : store.killCount(Names.bossKey(bossName));
	}

	/** How many of an item you have in inventory, equipment and (once opened) bank. */
	public int owned(String itemName)
	{
		String key = Names.item(itemName);
		return inventoryItems.getOrDefault(key, 0) + equipmentItems.getOrDefault(key, 0)
			+ bankItems.getOrDefault(key, 0);
	}

	public boolean bankSeen()
	{
		return bankSeen;
	}

	/** Grand Exchange price for an item by exact name, or -1 if unknown. */
	public int price(String itemName)
	{
		return priceCache.computeIfAbsent(Names.item(itemName), key ->
		{
			for (var result : itemManager.search(itemName))
			{
				if (result.getName().equalsIgnoreCase(itemName) && result.getPrice() > 0)
				{
					// RuneLite reports prices as a long; cap it so it fits in an int.
					return (int) Math.min(Integer.MAX_VALUE, result.getPrice());
				}
			}
			return -1;
		});
	}

	public void addGoal(Goal goal)
	{
		store.addGoal(goal);
		goalsChanged();
	}

	public void goalsChanged()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.goalsChanged();
			}
		});
	}

	/** A simple placeholder icon so we don't need an image file yet. */
	private static BufferedImage buildIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0xE0B040));
		g.fillRoundRect(1, 1, 14, 14, 4, 4);
		g.setColor(Color.BLACK);
		g.drawString("G", 4, 13);
		g.dispose();
		return image;
	}
}
