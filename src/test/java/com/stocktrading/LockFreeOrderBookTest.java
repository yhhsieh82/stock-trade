package com.stocktrading;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LockFreeOrderBook class.
 */
public class LockFreeOrderBookTest {
    
    @Test
    public void testAddBuyOrder() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        orderBook.addOrder(order);
        
        assertTrue(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap("AAPL");
        assertEquals(1, buyOrders.size());
        assertTrue(buyOrders.containsKey(150.0));
        assertEquals(1, buyOrders.get(150.0).size());
        assertEquals(order.getId(), buyOrders.get(150.0).peek().getId());
    }
    
    @Test
    public void testAddSellOrder() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        Order order = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        
        orderBook.addOrder(order);
        
        assertFalse(orderBook.hasBuyOrders("AAPL"));
        assertTrue(orderBook.hasSellOrders("AAPL"));
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = orderBook.getSellOrdersMap("AAPL");
        assertEquals(1, sellOrders.size());
        assertTrue(sellOrders.containsKey(150.0));
        assertEquals(1, sellOrders.get(150.0).size());
        assertEquals(order.getId(), sellOrders.get(150.0).peek().getId());
    }
    
    @Test
    public void testMultipleSymbols() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        
        Order aaplBuy = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order msftBuy = new Order("MSFT", Order.Type.BUY, 250.0, 5);
        
        orderBook.addOrder(aaplBuy);
        orderBook.addOrder(msftBuy);
        
        assertTrue(orderBook.hasBuyOrders("AAPL"));
        assertTrue(orderBook.hasBuyOrders("MSFT"));
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> aaplBuyOrders = orderBook.getBuyOrdersMap("AAPL");
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> msftBuyOrders = orderBook.getBuyOrdersMap("MSFT");
        
        assertEquals(aaplBuy.getId(), aaplBuyOrders.get(150.0).peek().getId());
        assertEquals(msftBuy.getId(), msftBuyOrders.get(250.0).peek().getId());
    }
    
    @Test
    public void testBuyOrderPriority() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        
        // Higher price should have higher priority in the map (reverse order)
        Order order1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order order2 = new Order("AAPL", Order.Type.BUY, 155.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap("AAPL");
        assertEquals(2, buyOrders.size());
        
        // First entry should be the highest price (155.0)
        Map.Entry<Double, ConcurrentLinkedQueue<Order>> firstEntry = buyOrders.firstEntry();
        assertEquals(155.0, firstEntry.getKey());
        assertEquals(order2.getId(), firstEntry.getValue().peek().getId());
    }
    
    @Test
    public void testSellOrderPriority() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        
        // Lower price should have higher priority in the map (natural order)
        Order order1 = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        Order order2 = new Order("AAPL", Order.Type.SELL, 145.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = orderBook.getSellOrdersMap("AAPL");
        assertEquals(2, sellOrders.size());
        
        // First entry should be the lowest price (145.0)
        Map.Entry<Double, ConcurrentLinkedQueue<Order>> firstEntry = sellOrders.firstEntry();
        assertEquals(145.0, firstEntry.getKey());
        assertEquals(order2.getId(), firstEntry.getValue().peek().getId());
    }
    
    @Test
    public void testTimePriority() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        
        // Same price, earlier timestamp should have higher priority in the queue
        Order order1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Thread.sleep(10); // Ensure different timestamps
        Order order2 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap("AAPL");
        ConcurrentLinkedQueue<Order> ordersAtPrice = buyOrders.get(150.0);
        
        assertEquals(2, ordersAtPrice.size());
        assertEquals(order1.getId(), ordersAtPrice.poll().getId()); // Earlier timestamp first
        assertEquals(order2.getId(), ordersAtPrice.poll().getId()); // Later timestamp second
    }
    
    @Test
    public void testEmptyOrderBook() {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        
        assertFalse(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        assertNull(orderBook.getBuyOrdersMap("AAPL"));
        assertNull(orderBook.getSellOrdersMap("AAPL"));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        LockFreeOrderBook orderBook = new LockFreeOrderBook();
        int numThreads = 10;
        int ordersPerThread = 100;
        
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        Order.Type type = (j % 2 == 0) ? Order.Type.BUY : Order.Type.SELL;
                        double price = 100.0 + (threadId * 0.1) + (j * 0.01);
                        Order order = new Order("AAPL", type, price, 1);
                        orderBook.addOrder(order);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // Count the total number of orders
        int totalBuyOrders = 0;
        int totalSellOrders = 0;
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap("AAPL");
        if (buyOrders != null) {
            for (ConcurrentLinkedQueue<Order> queue : buyOrders.values()) {
                totalBuyOrders += queue.size();
            }
        }
        
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = orderBook.getSellOrdersMap("AAPL");
        if (sellOrders != null) {
            for (ConcurrentLinkedQueue<Order> queue : sellOrders.values()) {
                totalSellOrders += queue.size();
            }
        }
        
        // Verify that all orders were added
        assertEquals(numThreads * ordersPerThread / 2, totalBuyOrders);
        assertEquals(numThreads * ordersPerThread / 2, totalSellOrders);
    }
} 