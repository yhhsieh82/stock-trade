package com.stocktrading;

import org.junit.jupiter.api.Test;
import java.util.concurrent.PriorityBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OrderBook class.
 */
public class OrderBookTest {
    
    @Test
    public void testAddBuyOrder() {
        OrderBook orderBook = new OrderBook();
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        orderBook.addOrder(order);
        
        assertTrue(orderBook.hasBuyOrders("AAPL"));
        assertFalse(orderBook.hasSellOrders("AAPL"));
        
        PriorityBlockingQueue<Order> buyOrders = orderBook.getBuyOrders("AAPL");
        assertEquals(1, buyOrders.size());
        assertEquals(order, buyOrders.peek());
    }
    
    @Test
    public void testAddSellOrder() {
        OrderBook orderBook = new OrderBook();
        Order order = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        
        orderBook.addOrder(order);
        
        assertTrue(orderBook.hasSellOrders("AAPL"));
        assertFalse(orderBook.hasBuyOrders("AAPL"));
        
        PriorityBlockingQueue<Order> sellOrders = orderBook.getSellOrders("AAPL");
        assertEquals(1, sellOrders.size());
        assertEquals(order, sellOrders.peek());
    }
    
    @Test
    public void testBuyOrderPriority() {
        OrderBook orderBook = new OrderBook();
        
        // Higher price should have higher priority
        Order order1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order order2 = new Order("AAPL", Order.Type.BUY, 155.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        PriorityBlockingQueue<Order> buyOrders = orderBook.getBuyOrders("AAPL");
        assertEquals(order2, buyOrders.poll()); // Higher price first
        assertEquals(order1, buyOrders.poll()); // Lower price second
    }
    
    @Test
    public void testSellOrderPriority() {
        OrderBook orderBook = new OrderBook();
        
        // Lower price should have higher priority
        Order order1 = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        Order order2 = new Order("AAPL", Order.Type.SELL, 145.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        PriorityBlockingQueue<Order> sellOrders = orderBook.getSellOrders("AAPL");
        assertEquals(order2, sellOrders.poll()); // Lower price first
        assertEquals(order1, sellOrders.poll()); // Higher price second
    }
    
    @Test
    public void testTimePriority() throws InterruptedException {
        OrderBook orderBook = new OrderBook();
        
        // Same price, earlier timestamp should have higher priority
        Order order1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Thread.sleep(10); // Ensure different timestamps
        Order order2 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        PriorityBlockingQueue<Order> buyOrders = orderBook.getBuyOrders("AAPL");
        assertEquals(order1, buyOrders.poll()); // Earlier timestamp first
        assertEquals(order2, buyOrders.poll()); // Later timestamp second
    }
} 