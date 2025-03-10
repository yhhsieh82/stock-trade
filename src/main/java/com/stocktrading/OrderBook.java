package com.stocktrading;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Map;

/**
 * Manages the order book for different stock symbols.
 * Uses concurrent data structures to handle multiple threads safely.
 */
public class OrderBook {
    // Maps stock symbols to their respective buy and sell order queues
    private final Map<String, PriorityBlockingQueue<Order>> buyOrders;
    private final Map<String, PriorityBlockingQueue<Order>> sellOrders;
    
    /**
     * Creates a new order book.
     */
    public OrderBook() {
        // Using ConcurrentHashMap for thread safety
        buyOrders = new ConcurrentHashMap<>();
        sellOrders = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds an order to the appropriate queue based on its type.
     * 
     * @param order The order to add
     */
    public void addOrder(Order order) {
        String symbol = order.getSymbol();
        
        if (order.getType() == Order.Type.BUY) {
            // For buy orders, we want higher prices to have higher priority
            buyOrders.computeIfAbsent(symbol, k -> new PriorityBlockingQueue<>(
                11, Comparator.<Order>comparingDouble(o -> -o.getPrice())
                    .thenComparingLong(Order::getTimestamp)));
            buyOrders.get(symbol).add(order);
        } else {
            // For sell orders, we want lower prices to have higher priority
            sellOrders.computeIfAbsent(symbol, k -> new PriorityBlockingQueue<>(
                11, Comparator.<Order>comparingDouble(Order::getPrice)
                    .thenComparingLong(Order::getTimestamp)));
            sellOrders.get(symbol).add(order);
        }
    }
    
    /**
     * Gets the buy orders for a specific symbol.
     * 
     * @param symbol The stock symbol
     * @return The queue of buy orders
     */
    public PriorityBlockingQueue<Order> getBuyOrders(String symbol) {
        return buyOrders.getOrDefault(symbol, new PriorityBlockingQueue<>());
    }
    
    /**
     * Gets the sell orders for a specific symbol.
     * 
     * @param symbol The stock symbol
     * @return The queue of sell orders
     */
    public PriorityBlockingQueue<Order> getSellOrders(String symbol) {
        return sellOrders.getOrDefault(symbol, new PriorityBlockingQueue<>());
    }
    
    /**
     * Checks if there are any buy orders for a specific symbol.
     * 
     * @param symbol The stock symbol
     * @return True if there are buy orders, false otherwise
     */
    public boolean hasBuyOrders(String symbol) {
        PriorityBlockingQueue<Order> queue = buyOrders.get(symbol);
        return queue != null && !queue.isEmpty();
    }
    
    /**
     * Checks if there are any sell orders for a specific symbol.
     * 
     * @param symbol The stock symbol
     * @return True if there are sell orders, false otherwise
     */
    public boolean hasSellOrders(String symbol) {
        PriorityBlockingQueue<Order> queue = sellOrders.get(symbol);
        return queue != null && !queue.isEmpty();
    }
} 