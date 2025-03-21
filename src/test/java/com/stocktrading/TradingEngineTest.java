package com.stocktrading;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TradingEngine class.
 */
public class TradingEngineTest {
    
    @Test
    public void testStartAndStop() throws InterruptedException {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");

        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Wait a bit
        Thread.sleep(100);
        
        // Stop the engine
        engine.stop();
        
        // The test passes if no exceptions are thrown
    }
    
    @Test
    public void testSubmitOrder() {
        List<String> symbols = Arrays.asList("AAPL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Submit an order
        Order order = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        engine.submitOrder(order);
        
        // Verify that the order was added to the order book
        assertTrue(engine.getOrderBook().hasBuyOrders("AAPL"));
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testOrderMatching() throws InterruptedException {
        List<String> symbols = Arrays.asList("AAPL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Submit matching orders
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        
        engine.submitOrder(buyOrder);
        engine.submitOrder(sellOrder);
        
        // Wait for the orders to be matched
        Thread.sleep(100);
        
        // Verify that the orders were matched
        assertFalse(engine.getOrderBook().hasBuyOrders("AAPL"));
        assertFalse(engine.getOrderBook().hasSellOrders("AAPL"));
        
        // Verify that a trade was created
        assertEquals(1, engine.getOrderMatcher().getTrades().size());
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testMultipleSymbols() throws InterruptedException {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Submit orders for different symbols
        Order aaplBuy = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order aaplSell = new Order("AAPL", Order.Type.SELL, 150.0, 10);
        Order msftBuy = new Order("MSFT", Order.Type.BUY, 250.0, 5);
        Order msftSell = new Order("MSFT", Order.Type.SELL, 250.0, 5);
        Order googlBuy = new Order("GOOGL", Order.Type.BUY, 2000.0, 2);
        Order googlSell = new Order("GOOGL", Order.Type.SELL, 2000.0, 2);
        
        engine.submitOrder(aaplBuy);
        engine.submitOrder(aaplSell);
        engine.submitOrder(msftBuy);
        engine.submitOrder(msftSell);
        engine.submitOrder(googlBuy);
        engine.submitOrder(googlSell);
        
        // Wait for the orders to be matched
        Thread.sleep(100);
        
        // Verify that all orders were matched
        assertFalse(engine.getOrderBook().hasBuyOrders("AAPL"));
        assertFalse(engine.getOrderBook().hasSellOrders("AAPL"));
        assertFalse(engine.getOrderBook().hasBuyOrders("MSFT"));
        assertFalse(engine.getOrderBook().hasSellOrders("MSFT"));
        assertFalse(engine.getOrderBook().hasBuyOrders("GOOGL"));
        assertFalse(engine.getOrderBook().hasSellOrders("GOOGL"));
        
        // Verify that trades were created
        assertEquals(3, engine.getOrderMatcher().getTrades().size());
        
        // Stop the engine
        engine.stop();
    }
} 