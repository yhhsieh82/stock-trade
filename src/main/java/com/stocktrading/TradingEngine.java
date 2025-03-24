package com.stocktrading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main trading engine that coordinates the order book and matcher.
 */
public class TradingEngine {
    private final OrderBook orderBook;
    private final OrderMatcher orderMatcher;
    private final ExecutorService executor;

    /**
     * Creates a new trading engine.
     */
    public TradingEngine(OrderBook orderBook, OrderMatcher orderMatcher) {
        this.orderBook = orderBook;
        this.orderMatcher = orderMatcher;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Starts the trading engine.
     */
    public void start() {
        executor.submit(orderMatcher);
        System.out.println("Trading engine started");
    }
    
    /**
     * Stops the trading engine.
     */
    public void stop() {
        orderMatcher.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Trading engine stopped");
    }
    
    /**
     * Submits an order to the trading engine.
     * 
     * @param order The order to submit
     */
    public void submitOrder(Order order) {
        orderBook.addOrder(order);
        System.out.println("Order submitted: " + order);
    }
    
    /**
     * Gets the order book.
     * 
     * @return The order book
     */
    public OrderBook getOrderBook() {
        return orderBook;
    }
    
    /**
     * Gets the order matcher.
     * 
     * @return The order matcher
     */
    public OrderMatcher getOrderMatcher() {
        return orderMatcher;
    }
} 