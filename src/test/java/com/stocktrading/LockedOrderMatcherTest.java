package com.stocktrading;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OrderMatcher class.
 */
public class LockedOrderMatcherTest {
    
    @Test
    public void testExactMatch() throws InterruptedException {
        LockedOrderBook orderBook = new LockedOrderBook();
        LockedOrderMatcher matcher = new LockedOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
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
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testPartialMatch() throws InterruptedException {
        LockedOrderBook orderBook = new LockedOrderBook();
        LockedOrderMatcher matcher = new LockedOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
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
        Order remainingBuyOrder = orderBook.getBuyOrders("AAPL").poll();
        assertEquals(5, remainingBuyOrder.getQuantity());
        
        // Check that a trade was created
        assertEquals(1, matcher.getTrades().size());
        Trade trade = matcher.getTrades().poll();
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(150.0, trade.getPrice());
        assertEquals(10, trade.getQuantity());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    public void testMultipleMatches() throws InterruptedException {
        LockedOrderBook orderBook = new LockedOrderBook();
        LockedOrderMatcher matcher = new LockedOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
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
        LockedOrderBook orderBook = new LockedOrderBook();
        LockedOrderMatcher matcher = new LockedOrderMatcher(orderBook, Arrays.asList("AAPL"));
        
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
        Order remainingBuyOrder = orderBook.getBuyOrders("AAPL").poll();
        assertEquals(150.0, remainingBuyOrder.getPrice());
        assertEquals(5, remainingBuyOrder.getQuantity());
        
        // Stop the matcher
        matcher.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
} 