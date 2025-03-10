package com.stocktrading;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Order class.
 */
public class OrderTest {
    
    @Test
    public void testOrderCreation() {
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        assertEquals("AAPL", order.getSymbol());
        assertEquals(Order.Type.BUY, order.getType());
        assertEquals(150.0, order.getPrice());
        assertEquals(10, order.getQuantity());
        assertTrue(order.getTimestamp() > 0);
    }
    
    @Test
    public void testReduceQuantity() {
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        order.reduceQuantity(5);
        assertEquals(5, order.getQuantity());
        
        order.reduceQuantity(5);
        assertEquals(0, order.getQuantity());
    }
    
    @Test
    public void testReduceQuantityInvalid() {
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        
        assertThrows(IllegalArgumentException.class, () -> {
            order.reduceQuantity(11);
        });
    }
} 