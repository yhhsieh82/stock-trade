package com.stocktrading;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class to demonstrate the trading engine.
 */
public class Main {
    public static void main(String[] args) {
        // Define the stock symbols to trade
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        
        // Create and start the trading engine
        TradingEngine engine = TradingEngineFactory.createLockedTradingEngine(symbols);
        engine.start();
        
        // Create a thread pool for submitting orders
        ExecutorService orderExecutor = Executors.newFixedThreadPool(5);
        
        // Submit some buy orders
        orderExecutor.submit(() -> {
            engine.submitOrder(new Order("AAPL", Order.Type.BUY, 150.0, 10));
            engine.submitOrder(new Order("MSFT", Order.Type.BUY, 250.0, 5));
            engine.submitOrder(new Order("GOOGL", Order.Type.BUY, 2000.0, 2));
            engine.submitOrder(new Order("AAPL", Order.Type.BUY, 151.0, 15));
        });
        
        // Submit some sell orders
        orderExecutor.submit(() -> {
            engine.submitOrder(new Order("AAPL", Order.Type.SELL, 149.0, 5));
            engine.submitOrder(new Order("MSFT", Order.Type.SELL, 251.0, 10));
            engine.submitOrder(new Order("GOOGL", Order.Type.SELL, 1990.0, 3));
            engine.submitOrder(new Order("AAPL", Order.Type.SELL, 148.0, 8));
        });
        
        // Submit more orders with a delay
        orderExecutor.submit(() -> {
            try {
                Thread.sleep(500);
                engine.submitOrder(new Order("AAPL", Order.Type.BUY, 152.0, 20));
                engine.submitOrder(new Order("MSFT", Order.Type.SELL, 249.0, 7));
                engine.submitOrder(new Order("GOOGL", Order.Type.BUY, 2010.0, 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Shutdown the order executor
        orderExecutor.shutdown();
        try {
            if (!orderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                orderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            orderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Let the engine run for a while to process all orders
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the trading engine
        engine.stop();
    }
} 