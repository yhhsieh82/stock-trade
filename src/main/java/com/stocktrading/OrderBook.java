package com.stocktrading;

public interface OrderBook {
	void addOrder(Order order);

	/**
	 * Checks if there are any buy orders for a specific symbol.
	 *
	 * @param symbol The stock symbol
	 * @return True if there are buy orders, false otherwise
	 */
	boolean hasBuyOrders(String symbol);

	/**
	 * Checks if there are any sell orders for a specific symbol.
	 *
	 * @param symbol The stock symbol
	 * @return True if there are sell orders, false otherwise
	 */
	boolean hasSellOrders(String symbol);
}
