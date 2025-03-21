package com.stocktrading;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public interface LockFreeOrderBookAccess extends OrderBook {
	/**
	 * Gets the map of buy orders for a specific symbol.
	 *
	 * @param symbol The stock symbol
	 * @return Map of price to orders at that price
	 */
	ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> getBuyOrdersMap(String symbol);

	/**
	 * Gets the map of sell orders for a specific symbol.
	 *
	 * @param symbol The stock symbol
	 * @return Map of price to orders at that price
	 */
	ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> getSellOrdersMap(String symbol);
}