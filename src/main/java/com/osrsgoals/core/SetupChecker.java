package com.osrsgoals.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Compares a recommended setup against what you own and prices what is missing. */
public final class SetupChecker
{
	/** How many of an item you own (inventory, worn, bank). Names are matched case-insensitively. */
	public interface Owned
	{
		int count(String itemName);
	}

	/** Grand Exchange price by item name; negative if unknown. */
	public interface Prices
	{
		int price(String itemName);
	}

	public static final class Row
	{
		/** Slot name for equipment, "Inventory" for inventory items. */
		public final String label;
		/** The wiki's best choice first, then its alternatives. */
		public final List<String> options;
		public final int needed;
		public final int owned;
		/** The option you own, or null. */
		public final String ownedItem;
		/** GP to buy what is missing; 0 if nothing is missing; null if no price is known. */
		public final Long missingCost;

		Row(String label, List<String> options, int needed, int owned, String ownedItem, Long missingCost)
		{
			this.label = label;
			this.options = options;
			this.needed = needed;
			this.owned = owned;
			this.ownedItem = ownedItem;
			this.missingCost = missingCost;
		}

		public boolean satisfied()
		{
			return owned >= needed;
		}
	}

	public static final class Report
	{
		public final List<Row> equipment;
		public final List<Row> inventory;
		/** Sum of the known costs of everything missing. */
		public final long missingCost;
		/** Missing rows with no known price (so missingCost is a lower bound). */
		public final int unpriced;
		public final int missingRows;

		Report(List<Row> equipment, List<Row> inventory, long missingCost, int unpriced, int missingRows)
		{
			this.equipment = equipment;
			this.inventory = inventory;
			this.missingCost = missingCost;
			this.unpriced = unpriced;
			this.missingRows = missingRows;
		}
	}

	private SetupChecker()
	{
	}

	public static Report check(BossSetup setup, Owned owned, Prices prices)
	{
		List<Row> equipment = new ArrayList<>();
		List<Row> inventory = new ArrayList<>();

		if (setup.equipment != null)
		{
			for (Map.Entry<String, List<String>> e : setup.equipment.entrySet())
			{
				List<String> options = e.getValue() == null ? new ArrayList<String>() : e.getValue();
				if (options.isEmpty())
				{
					continue;
				}
				String ownedItem = null;
				for (String option : options)
				{
					if (owned.count(option) >= 1)
					{
						ownedItem = option;
						break;
					}
				}
				Long cost = 0L;
				if (ownedItem == null)
				{
					cost = cheapest(options, prices);
				}
				equipment.add(new Row(e.getKey(), options, 1, ownedItem == null ? 0 : 1, ownedItem, cost));
			}
		}

		if (setup.inventory != null)
		{
			for (SetupItem item : setup.inventory)
			{
				if (item == null || item.item == null)
				{
					continue;
				}
				int qty = Math.max(1, item.quantity);
				int have = owned.count(item.item);
				List<String> options = new ArrayList<>();
				options.add(item.item);
				Long cost = 0L;
				if (have < qty)
				{
					int price = prices.price(item.item);
					cost = price > 0 ? Long.valueOf((long) price * (qty - have)) : null;
				}
				inventory.add(new Row("Inventory", options, qty, have, have > 0 ? item.item : null, cost));
			}
		}

		long total = 0;
		int unpriced = 0;
		int missing = 0;
		List<Row> all = new ArrayList<>(equipment);
		all.addAll(inventory);
		for (Row row : all)
		{
			if (row.satisfied())
			{
				continue;
			}
			missing++;
			if (row.missingCost == null)
			{
				unpriced++;
			}
			else
			{
				total += row.missingCost;
			}
		}
		return new Report(equipment, inventory, total, unpriced, missing);
	}

	private static Long cheapest(List<String> options, Prices prices)
	{
		Long best = null;
		for (String option : options)
		{
			int price = prices.price(option);
			if (price > 0 && (best == null || price < best))
			{
				best = (long) price;
			}
		}
		return best;
	}
}
