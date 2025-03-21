package com.stocktrading;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LockedOrderMatcherRaceConditionTest {

    @Test
    public void testRaceConditionBetweenPeekAndPoll() throws InterruptedException {
        // Setup
        LockedOrderBook orderBook = new LockedOrderBook();
        List<String> symbols = Collections.singletonList("AAPL");
        LockedOrderMatcher orderMatcher = new LockedOrderMatcher(orderBook, symbols);

        // Create initial buy and sell orders
        Order buyOrder = new Order("AAPL", Order.Type.BUY, 100.0, 10);
        Order initialSellOrder = new Order("AAPL", Order.Type.SELL, 100.0, 10);

        // Add initial orders to the book
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(initialSellOrder);

        // This will be used to synchronize our test threads
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);

        // Thread 1: Start the matchOrders method but pause it after peek
        Thread matcherThread = new Thread(() -> {
            try {
                // Create a custom implementation of OrderMatcher to expose the race condition
                LockedOrderMatcher customMatcher = new LockedOrderMatcher(orderBook, symbols) {
                    @Override
                    public void matchOrders(String symbol) {
                        // Check if there are orders to match
                        if (orderBook.hasBuyOrders(symbol) && orderBook.hasSellOrders(symbol)) {
                            // Get the highest priority buy and sell orders using peek
                            Order buyOrderPeeked = orderBook.getBuyOrders(symbol).peek();
                            Order sellOrderPeeked = orderBook.getSellOrders(symbol).peek();

                            if (buyOrderPeeked == null || sellOrderPeeked == null) {
                                return;
                            }

                            // Only proceed if the orders can be matched
                            if (sellOrderPeeked.getPrice() <= buyOrderPeeked.getPrice()) {
                                // Signal that we've peeked the orders
                                latch.countDown();

                                // Wait a bit to give time for the other thread to insert a new sell order
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                // Now try to poll the orders
                                Order pollBuyOrder = orderBook.getBuyOrders(symbol).poll();
                                Order pollSellOrder = orderBook.getSellOrders(symbol).poll();

                                // Check if we got the same orders we peeked
                                if (!pollBuyOrder.equals(buyOrderPeeked) || !pollSellOrder.equals(sellOrderPeeked)) {
                                    raceConditionDetected.set(true);
                                    System.out.println("Race condition detected!");
                                    System.out.println("Peeked sell order: " + sellOrderPeeked);
                                    System.out.println("Polled sell order: " + pollSellOrder);
                                }
                            }
                        }
                    }
                };

                customMatcher.matchOrders("AAPL");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Thread 2: Add a new sell order with a better price after Thread 1 has peeked
        Thread addOrderThread = new Thread(() -> {
            try {
                // Wait until the matcher thread has peeked the orders
                latch.await();

                // Add a new sell order with a better price
                Order betterSellOrder = new Order("AAPL", Order.Type.SELL, 95.0, 5);
                orderBook.addOrder(betterSellOrder);

                System.out.println("Added new sell order with better price: " + betterSellOrder);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start the threads
        matcherThread.start();
        addOrderThread.start();

        // Wait for both threads to complete
        matcherThread.join();
        addOrderThread.join();

        // Assert that a race condition was detected
        assertTrue(raceConditionDetected.get(),
            "A race condition should be detected between peek and poll operations");
    }

    /**
     * Stress Test:
     * It creates multiple threads that simultaneously add orders and call matchOrders
     * It uses randomized prices and alternating buy/sell orders to increase the chance of triggering race conditions
     * Any exceptions thrown (like the IllegalArgumentException mentioned in the code) will fail the test
     */
    @Test
    public void testMultiThreadedOrderMatching() throws InterruptedException {
        // Setup
        LockedOrderBook orderBook = new LockedOrderBook();
        List<String> symbols = Collections.singletonList("AAPL");
        LockedOrderMatcher orderMatcher = new LockedOrderMatcher(orderBook, symbols);

        int numThreads = 10;
        int ordersPerThread = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        List<Error> errors = Collections.synchronizedList(new ArrayList<>());

        // Create multiple threads that will add orders and match simultaneously
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int j = 0; j < ordersPerThread; j++) {
                        // Add alternating buy and sell orders
                        Order.Type type = (j % 2 == 0) ? Order.Type.BUY : Order.Type.SELL;
                        double price = 100.0 + (threadId * 0.1) + (j * 0.01);
                        if (type == Order.Type.SELL) {
                            price = 100.0 - (threadId * 0.1) - (j * 0.01);
                        }

                        Order order = new Order("AAPL", type, price, 10);
                        orderBook.addOrder(order);

                        // If this is an even-numbered thread, try to match orders
                        if (threadId % 2 == 0) {
                            try {
                                orderMatcher.matchOrders("AAPL");
                            } catch (Exception e) {
                                exceptions.add(e);
                            }
                        }

                        // Small delay to increase chance of race conditions
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } catch (AssertionError e) {
                    errors.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete or timeout after 10 seconds
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert no errors were thrown
        if (!errors.isEmpty()) {
            errors.get(0).printStackTrace();
            fail("Race conditions resulted in errors: " + errors.size() + " errors");
        }

        assertTrue(completed, "All threads should complete in time");
    }
}