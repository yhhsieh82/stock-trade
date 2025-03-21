package com.stocktrading;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Matches buy and sell orders based on price-time priority.
 * Supports partial matching of orders.
 */
public class LockedOrderMatcher implements OrderMatcher, Runnable {
    private final LockedOrderBook orderBook;
    private final BlockingQueue<Trade> trades;
    private final List<String> symbols;
    private volatile boolean running = true;
    
    /**
     * Creates a new order matcher.
     * 
     * @param orderBook The order book to match orders from
     * @param symbols The list of stock symbols to match orders for
     */
    public LockedOrderMatcher(LockedOrderBook orderBook, List<String> symbols) {
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
    public void matchOrders(String symbol) {
        // Continue matching as long as there are both buy and sell orders
        while (orderBook.hasBuyOrders(symbol) && orderBook.hasSellOrders(symbol)) {
            synchronized (orderBook.getBuyOrders(symbol)) {
                synchronized (orderBook.getSellOrders(symbol)) {
                    // Get the highest priority buy and sell orders
                    Order buyOrder = orderBook.getBuyOrders(symbol).peek();
                    Order sellOrder = orderBook.getSellOrders(symbol).peek();
                    
                    if (buyOrder == null || sellOrder == null) {
                        break;
                    }
                    
                    // Check if the orders can be matched (buy price >= sell price)
                    if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                        // Remove the orders from the queues - must be the same as what we peeked
                        // due to synchronization
                        Order pollBuyOrder = orderBook.getBuyOrders(symbol).poll();
                        Order pollSellOrder = orderBook.getSellOrders(symbol).poll();

                        assert Objects.equals(pollBuyOrder, buyOrder) && Objects.equals(pollSellOrder, sellOrder) :
                            "Race condition: peek/poll mismatch detected";

                        // Determine the matched quantity and price
                        int matchedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
                        double tradePrice = sellOrder.getPrice(); // Use the sell price for the trade
                        
                        // Create a trade
                        Trade trade = new Trade(symbol, tradePrice, matchedQuantity, buyOrder.getId(), sellOrder.getId());
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
    }
} 