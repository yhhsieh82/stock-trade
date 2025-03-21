package com.stocktrading;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LockFreeOrderMatcher class.
 */
public class LockFreeOrderMatcherTest {
    
    @Test
    public void testExactMatch() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
        // Start the matcher in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(matcher);
        
        // Add a buy and sell order with the same quantity
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);
        
        // Wait for the matcher to process the orders
        Thread.sleep(100);
        
        // Check that both orders were matched
        assertFalse(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        // Check that a trade was created
        assertEquals(1, matcher.getTrades().size());
        Trade trade = matcher.getTrades().poll();
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(150.0, trade.getPrice());
        assertEquals(10, trade.getQuantity());
        assertEquals(buyOrder.getId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getId(), trade.getSellOrderId());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testPartialMatch() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
        // Start the matcher in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(matcher);
        
        // Add a buy order with more quantity than the sell order
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 150.0, 15);
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);
        
        // Wait for the matcher to process the orders
        Thread.sleep(100);
        
        // Check that the buy order was partially matched
        assertTrue(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        // Check the remaining buy order
        assertEquals(1, orderBook.getBuyOrdersMap("AAPL").get(150.0).size());
        Order remainingBuyOrder = orderBook.getBuyOrdersMap("AAPL").get(150.0).peek();
        assertEquals(5, remainingBuyOrder.getQuantity());
        assertEquals(buyOrder.getId(), remainingBuyOrder.getId());
        
        // Check that a trade was created
        assertEquals(1, matcher.getTrades().size());
        Trade trade = matcher.getTrades().poll();
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(150.0, trade.getPrice());
        assertEquals(10, trade.getQuantity());
        assertEquals(buyOrder.getId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getId(), trade.getSellOrderId());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testMultipleMatches() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
        // Start the matcher in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(matcher);
        
        // Add multiple buy and sell orders
        Order buyOrder1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order buyOrder2 = new Order("AAPL", Order.Type.BUY, 151.0, 5);
        Order sellOrder1 = new Order("AAPL", Order.Type.SELL, 149.0, 10);
        Order sellOrder2 = new Order("AAPL", Order.Type.SELL, 148.0, 5);
        
        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(sellOrder2);
        
        // Wait for the matcher to process the orders
        Thread.sleep(500);
        
        // Check that all orders were matched
        assertFalse(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        // Check that trades were created
        assertEquals(2, matcher.getTrades().size());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testPricePriority() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
        // Start the matcher in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(matcher);
        
        // Add buy orders with different prices
        Order buyOrder1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order buyOrder2 = new Order("AAPL", Order.Type.BUY, 155.0, 10);
        
        // Add a sell order that matches both buy orders
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 149.0, 15);
        
        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(sellOrder);
        
        // Wait for the matcher to process the orders
        Thread.sleep(100);
        
        // Check that the higher priced buy order was matched first
        assertTrue(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        // Check the remaining buy order (should be the lower priced one)
        assertEquals(1, orderBook.getBuyOrdersMap("AAPL").size());
        assertTrue(orderBook.getBuyOrdersMap("AAPL").containsKey(150.0));
        Order remainingBuyOrder = orderBook.getBuyOrdersMap("AAPL").get(150.0).peek();
        assertEquals(5, remainingBuyOrder.getQuantity());
        assertEquals(buyOrder1.getId(), remainingBuyOrder.getId());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testConcurrentMatchingAndOrdering() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
        // Start the matcher in a separate thread
        ExecutorService matcherExecutor = Executors.newSingleThreadExecutor();
        matcherExecutor.submit(matcher);
        
        // Create threads to add orders
        int numThreads = 5;
        int ordersPerThread = 20;
        ExecutorService orderExecutor = Executors.newFixedThreadPool(numThreads);
        
        // Add buy and sell orders concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            orderExecutor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        // Alternate between buy and sell orders
                        Order.Type type = (j % 2 == 0) ? Order.Type.BUY : Order.Type.SELL;
                        
                        // Use prices that will match (buys >= sells)
                        double price;
                        if (type == Order.Type.BUY) {
                            price = 100.0 + (threadId * 0.1) + (j * 0.01);
                        } else {
                            price = 100.0 - (threadId * 0.1) - (j * 0.01);
                        }
                        
                        Order order = new Order("AAPL", type, price, 1);
                        orderBook.addOrder(order);
                        
                        // Small delay to allow matching to occur
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Wait for order threads to complete
        orderExecutor.shutdown();
        orderExecutor.awaitTermination(10, TimeUnit.SECONDS);
        
        // Wait a bit more for matcher to process any remaining orders
        Thread.sleep(500);
        
        // Check that most orders were matched
        int totalOrders = numThreads * ordersPerThread;
        int expectedMatches = totalOrders / 2; // Half buy, half sell
        
        // Allow for some flexibility in the number of matches
        int actualMatches = matcher.getTrades().size();
        assertTrue(actualMatches >= expectedMatches * 0.8, 
                "Expected at least 80% of possible matches, but got " + actualMatches + " out of " + expectedMatches);
        
        // Stop the matcher
        matcher.stop();
        matcherExecutor.shutdown();
        matcherExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }
} 