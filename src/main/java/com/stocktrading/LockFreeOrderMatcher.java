package com.stocktrading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

public class LockFreeOrderMatcher implements OrderMatcher, Runnable {
	private final LockFreeOrderBook orderBook;
	private final List<String> symbols;
	private final BlockingQueue<Trade> trades;
	private volatile boolean running = true;

	/**
	 * Creates a new order matcher.
	 *
	 * @param orderBook The order book to match orders from
	 * @param symbols The list of stock symbols to match orders for
	 */
	public LockFreeOrderMatcher(LockFreeOrderBook orderBook, List<String> symbols) {
		this.orderBook = orderBook;
		this.trades = new LinkedBlockingQueue<>();
		this.symbols = new ArrayList<>(symbols);
	}

	/**
	 * Stops the order matcher.
	 */
	public void stop() {
		running = false;
	}

	/**
	 * Gets the trades that have been executed.
	 *
	 * @return The queue of trades
	 */
	public BlockingQueue<Trade> getTrades() {
		return trades;
	}

	@Override
	public void run() {
		try {
			while (running) {
				// Process each symbol
				for (String symbol : symbols) {
					matchOrders(symbol);
				}

				// Sleep a bit to avoid consuming too much CPU
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void matchOrders(String symbol) {
		while (orderBook.hasBuyOrders(symbol) && orderBook.hasSellOrders(symbol)) {
			ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap(symbol);
			ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = orderBook.getSellOrdersMap(symbol);

			// Get best bid (highest buy price) and best ask (lowest sell price)
			Map.Entry<Double, ConcurrentLinkedQueue<Order>> bestBidEntry = buyOrders.firstEntry();
			Map.Entry<Double, ConcurrentLinkedQueue<Order>> bestAskEntry = sellOrders.firstEntry();

			if (bestBidEntry == null || bestAskEntry == null) {
				break;
			}

			double bestBid = bestBidEntry.getKey();
			double bestAsk = bestAskEntry.getKey();

			// If best bid is greater than or equal to best ask, we have a match
			if (bestBid >= bestAsk) {
				ConcurrentLinkedQueue<Order> buyQueue = bestBidEntry.getValue();
				ConcurrentLinkedQueue<Order> sellQueue = bestAskEntry.getValue();

				Order buyOrder = buyQueue.poll(); // Get the earliest buy order
				if (buyOrder == null) {
					// Remove empty queue
					buyOrders.remove(bestBid);
					continue;
				}

				Order sellOrder = sellQueue.poll(); // Get the earliest sell order
				if (sellOrder == null) {
					// Remove empty queue
					sellOrders.remove(bestAsk);
					// Put the buy order back
					buyQueue.add(buyOrder);
					continue;
				}

				int matchedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

				// Update quantities atomically
				boolean buySuccess = buyOrder.reduceQuantity(matchedQuantity);
				boolean sellSuccess = sellOrder.reduceQuantity(matchedQuantity);

				if (buySuccess && sellSuccess) {
					recordTrade(buyOrder, sellOrder, matchedQuantity);
				}

				// If orders have remaining quantity, add them back
				if (buyOrder.getQuantity() > 0) {
					buyQueue.add(buyOrder);
				}

				if (sellOrder.getQuantity() > 0) {
					sellQueue.add(sellOrder);
				}

				// Remove price levels if orders at that price are fully matched
				if (buyQueue.isEmpty()) {
					buyOrders.remove(bestBid);
				}
				if (sellQueue.isEmpty()) {
					sellOrders.remove(bestAsk);
				}
			} else {
				// No more matching orders
				break;
			}
		}
	}

	private void recordTrade(Order buyOrder, Order sellOrder, int quantity) {
		Trade trade = new Trade(buyOrder.getSymbol(), sellOrder.getPrice(), quantity,
			buyOrder.getId(), sellOrder.getId());
		trades.add(trade);
	}
}