package com.stocktrading;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ReactiveTradingEngineTest {

    @Test
    public void testOrderSubmissionAndMatching() {
        // Create a reactive trading engine with test symbols
        List<String> symbols = Arrays.asList("AAPL");
        ReactiveTradingEngine engine = new ReactiveTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Create test orders
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 149.0, 10);
        
        // Get the trade stream for verification
        Flux<Trade> tradeStream = engine.getTradeStream();
        
        // Use StepVerifier to test the reactive streams
        StepVerifier.create(tradeStream.take(1))
            .then(() -> {
                // Submit orders after subscribing to the trade stream
                engine.submitOrder(buyOrder);
                engine.submitOrder(sellOrder);
            })
            .assertNext(trade -> {
                assertEquals("AAPL", trade.getSymbol());
                assertEquals(149.0, trade.getPrice());
                assertEquals(10, trade.getQuantity());
                assertEquals(buyOrder.getId(), trade.getBuyOrderId());
                assertEquals(sellOrder.getId(), trade.getSellOrderId());
            })
            .verifyComplete();
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testPartialOrderMatching() {
        // Create a reactive trading engine
        List<String> symbols = Arrays.asList("AAPL");
        ReactiveTradingEngine engine = new ReactiveTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Create test orders with different quantities
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 150.0, 20);
        Order sellOrder = new Order("AAPL", Order.Type.SELL, 149.0, 10);
        
        // Get the trade stream
        Flux<Trade> tradeStream = engine.getTradeStream();
        
        // Verify partial matching
        StepVerifier.create(tradeStream.take(1))
            .then(() -> {
                engine.submitOrder(buyOrder);
                engine.submitOrder(sellOrder);
            })
            .assertNext(trade -> {
                assertEquals(10, trade.getQuantity()); // Only 10 shares matched
                assertEquals(buyOrder.getId(), trade.getBuyOrderId());
                assertEquals(sellOrder.getId(), trade.getSellOrderId());
            })
            .verifyComplete();
        
        // Verify remaining quantity in buy order
        assertEquals(10, buyOrder.getQuantity());
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testMultipleOrderMatching() {
        // Create a reactive trading engine
        List<String> symbols = Arrays.asList("AAPL");
        ReactiveTradingEngine engine = new ReactiveTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Create multiple test orders
        Order buyOrder1 = new Order("AAPL", Order.Type.BUY, 150.0, 10);
        Order buyOrder2 = new Order("AAPL", Order.Type.BUY, 151.0, 5);
        Order sellOrder1 = new Order("AAPL", Order.Type.SELL, 149.0, 8);
        Order sellOrder2 = new Order("AAPL", Order.Type.SELL, 148.0, 7);
        
        // Get the trade stream and count trades
        AtomicInteger tradeCount = new AtomicInteger(0);
        engine.getTradeStream()
            .take(Duration.ofSeconds(1))
            .subscribe(trade -> tradeCount.incrementAndGet());
        
        // Submit orders
        engine.submitOrder(buyOrder1);
        engine.submitOrder(buyOrder2);
        engine.submitOrder(sellOrder1);
        engine.submitOrder(sellOrder2);
        
        // Wait a bit for processing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify that the correct number of trades were executed
        assertEquals(3, tradeCount.get());
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testBackpressureHandling() {
        // Create a reactive trading engine
        List<String> symbols = Arrays.asList("AAPL");
        ReactiveTradingEngine engine = new ReactiveTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Create a slow consumer with controlled request rate
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        
        // Get the order stream
        Flux<Order> orderStream = engine.getOrderStream();
        
        // Submit many orders rapidly
        int orderCount = 1000;
        
        // Create a step verifier with limited request rate
        StepVerifier.withVirtualTime(() -> orderStream
                .take(orderCount)
                .subscribeOn(scheduler), 
                () -> scheduler, 
                Long.MAX_VALUE)
            .then(() -> {
                // Submit orders rapidly
                for (int i = 0; i < orderCount; i++) {
                    Order order = new Order(
                        "AAPL", 
                        i % 2 == 0 ? Order.Type.BUY : Order.Type.SELL,
                        100.0 + (i % 10),
                        1
                    );
                    engine.submitOrder(order);
                }
            })
            .thenRequest(10) // Request only 10 items initially
            .expectNextCount(10)
            .thenAwait(Duration.ofMillis(50))
            .thenRequest(100) // Request 100 more
            .expectNextCount(100)
            .thenAwait(Duration.ofMillis(50))
            .thenRequest(orderCount) // Request all remaining
            .expectNextCount(orderCount - 110)
            .verifyComplete();
        
        // Stop the engine
        engine.stop();
    }
    
    @Test
    public void testConcurrentOrderProcessing() {
        // Create a reactive trading engine with multiple symbols
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        ReactiveTradingEngine engine = new ReactiveTradingEngine(symbols);
        
        // Start the engine
        engine.start();
        
        // Submit orders for different symbols concurrently
        int ordersPerSymbol = 100;
        AtomicInteger tradeCount = new AtomicInteger(0);
        
        // Subscribe to trades
        engine.getTradeStream()
            .subscribe(trade -> tradeCount.incrementAndGet());
        
        // Submit orders for each symbol
        for (String symbol : symbols) {
            for (int i = 0; i < ordersPerSymbol; i++) {
                Order buyOrder = new Order(
                    symbol, 
                    Order.Type.BUY,
                    100.0 + (i * 0.01),
                    1
                );
                Order sellOrder = new Order(
                    symbol, 
                    Order.Type.SELL,
                    99.0 + (i * 0.01),
                    1
                );
                
                engine.submitOrder(buyOrder);
                engine.submitOrder(sellOrder);
            }
        }
        
        // Wait for processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify that all orders were matched
        assertEquals(symbols.size() * ordersPerSymbol, tradeCount.get());
        
        // Stop the engine
        engine.stop();
    }
} 