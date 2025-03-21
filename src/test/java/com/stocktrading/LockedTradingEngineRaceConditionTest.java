package com.stocktrading;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the concurrency aspects of the trading engine.
 */
public class LockedTradingEngineRaceConditionTest {
    
    @Test
    public void testConcurrentOrderSubmission() throws InterruptedException {
        // Create a trading engine
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        engine.start();
        
        // Number of threads and orders per thread
        int numThreads = 10;
        int ordersPerThread = 100;
        
        // Use a CountDownLatch to wait for all threads to finish
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Use an AtomicInteger to count the total number of orders submitted
        AtomicInteger totalOrders = new AtomicInteger(0);
        
        // Create a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Submit orders from multiple threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        // Alternate between buy and sell orders
                        Order.Type type = (j % 2 == 0) ? Order.Type.BUY : Order.Type.SELL;
                        
                        // Use different symbols
                        String symbol = symbols.get(j % symbols.size());
                        
                        // Create an order with a random price and quantity
                        double price = 100.0 + (threadId * 10) + (j % 10);
                        int quantity = 1 + (j % 10);
                        
                        Order order = new Order(symbol, type, price, quantity);
                        engine.submitOrder(order);
                        
                        // Increment the total orders counter
                        totalOrders.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to finish
        latch.await(10, TimeUnit.SECONDS);
        
        // Shutdown the executor
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        
        // Let the engine process all orders
        Thread.sleep(1000);
        
        // Stop the engine
        engine.stop();
        
        // Verify that all orders were submitted
        assertEquals(numThreads * ordersPerThread, totalOrders.get());
        
        // Verify that trades were executed
        assertTrue(engine.getOrderMatcher().getTrades().size() > 0);
    }
    
    @Test
    public void testOrderMatchingUnderLoad() throws InterruptedException {
        // Create a trading engine with a single symbol
        List<String> symbols = Arrays.asList("AAPL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        engine.start();
        
        // Submit a large number of matching orders
        int numOrders = 1000;
        
        // Submit buy orders
        for (int i = 0; i < numOrders; i++) {
            engine.submitOrder(new Order("AAPL", Order.Type.BUY, 150.0, 1));
        }
        
        // Submit sell orders
        for (int i = 0; i < numOrders; i++) {
            engine.submitOrder(new Order("AAPL", Order.Type.SELL, 150.0, 1));
        }
        
        // Let the engine process all orders
        Thread.sleep(2000);
        
        // Stop the engine
        engine.stop();
        
        // Verify that all orders were matched
        assertFalse(engine.getOrderBook().hasBuyOrders("AAPL"));
        assertFalse(engine.getOrderBook().hasSellOrders("AAPL"));
        
        // Verify that the correct number of trades were executed
        assertEquals(numOrders, engine.getOrderMatcher().getTrades().size());
    }


    @Test
    public void testOrderMatchingRaceCondition() throws InterruptedException {
        // Create a trading engine with a single symbol
        List<String> symbols = Arrays.asList("AAPL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        engine.start();

        // Submit a large number of matching orders
        int numOrders = 1000;

        // Submit buy orders
        for (int i = 0; i < numOrders; i++) {
            engine.submitOrder(new Order("AAPL", Order.Type.BUY, 10.0, 1));
        }

        // Submit sell orders
        for (int i = 0; i < numOrders; i++) {
            engine.submitOrder(new Order("AAPL", Order.Type.SELL, 9.0, 1));
        }

        // Let the engine process all orders
        Thread.sleep(2000);

        // Stop the engine
        engine.stop();

        // Verify that all orders were matched
        assertFalse(engine.getOrderBook().hasBuyOrders("AAPL"));
        assertFalse(engine.getOrderBook().hasSellOrders("AAPL"));

        // Verify that the correct number of trades were executed
        assertEquals(numOrders, engine.getOrderMatcher().getTrades().size());
    }
} 