package com.stocktrading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Matches buy and sell orders based on price-time priority.
 * Supports partial matching of orders.
 */
public class OrderMatcher implements Runnable {
    private final OrderBook orderBook;
    private final BlockingQueue<Trade> trades;
    private final List<String> symbols;
    private volatile boolean running = true;
    
    /**
     * Creates a new order matcher.
     * 
     * @param orderBook The order book to match orders from
     * @param symbols The list of stock symbols to match orders for
     */
    public OrderMatcher(OrderBook orderBook, List<String> symbols) {
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
    
    /**
     * Matches orders for a specific symbol.
     * 
     * @param symbol The stock symbol
     */
    void matchOrders(String symbol) {
        // Continue matching as long as there are both buy and sell orders
        while (orderBook.hasBuyOrders(symbol) && orderBook.hasSellOrders(symbol)) {
            // Get the highest priority buy and sell orders
            Order buyOrder = orderBook.getBuyOrders(symbol).peek();
            Order sellOrder = orderBook.getSellOrders(symbol).peek();
            
            if (buyOrder == null || sellOrder == null) {
                break;
            }
            
            // Check if the orders can be matched (buy price >= sell price)
            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                // Remove the orders from the queues
                Order pollBuyOrder = orderBook.getBuyOrders(symbol).poll();
                Order pollSellOrder = orderBook.getSellOrders(symbol).poll();

                // TODO test race condition
                if (!pollBuyOrder.equals(buyOrder) || !pollSellOrder.equals(sellOrder)) {
                    throw new IllegalArgumentException("poll not equals to peek");
                }

                // Determine the matched quantity and price
                int matchedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
                double tradePrice = sellOrder.getPrice(); // Use the sell price for the trade

                // Create a trade
                Trade trade = new Trade(symbol, tradePrice, matchedQuantity);
                trades.add(trade);

                System.out.println(trade);

                // Handle partial matches
                if (matchedQuantity < buyOrder.getQuantity()) {
                    // Create a new buy order with the remaining quantity
                    buyOrder.reduceQuantity(matchedQuantity);
                    orderBook.addOrder(buyOrder);
                }

                if (matchedQuantity < sellOrder.getQuantity()) {
                    // Create a new sell order with the remaining quantity
                    sellOrder.reduceQuantity(matchedQuantity);
                    orderBook.addOrder(sellOrder);
                }
            } else {
                // Orders cannot be matched, so stop trying
                break;
            }
        }
    }
} 