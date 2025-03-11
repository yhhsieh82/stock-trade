package com.stocktrading.benchmark;

import com.stocktrading.Order;
import com.stocktrading.OrderBook;
import com.stocktrading.OrderMatcher;
import com.stocktrading.Trade;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for comparing different OrderBook implementations.
 * 
 * Run with: mvn clean install && java -jar target/benchmarks.jar
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(1)
@Threads(1)
public class OrderBookBenchmark {

    @Param({"1", "2", "4", "8", "16", "32"})
    private int numThreads;

    @Param({"100", "1000", "10000"})
    private int numOrders;

    @Param({"AAPL"})
    private String symbol;

    private OrderBook synchronizedOrderBook;
    // Future: private LockFreeOrderBook lockFreeOrderBook;

    private List<Order> buyOrders;
    private List<Order> sellOrders;
    private Random random;

    @Setup
    public void setup() {
        synchronizedOrderBook = new OrderBook();
        // Future: lockFreeOrderBook = new LockFreeOrderBook();

        random = new Random(42); // Fixed seed for reproducibility
        buyOrders = generateOrders(numOrders, Order.Type.BUY);
        sellOrders = generateOrders(numOrders, Order.Type.SELL);
    }

    private List<Order> generateOrders(int count, Order.Type type) {
        List<Order> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double price;
            if (type == Order.Type.BUY) {
                // Buy prices between 90 and 100
                price = 90 + random.nextDouble() * 10;
            } else {
                // Sell prices between 90 and 100
                price = 90 + random.nextDouble() * 10;
            }
            int quantity = 1 + random.nextInt(100);
            orders.add(new Order(symbol, type, price, quantity));
        }
        return orders;
    }

    @Benchmark
    public void synchronizedOrderBookAddOnly(Blackhole blackhole) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Divide orders among threads
        int ordersPerThread = numOrders / numThreads;

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < ordersPerThread; i++) {
                        int buyIndex = threadId * ordersPerThread + i;
                        int sellIndex = threadId * ordersPerThread + i;
                        
                        if (buyIndex < buyOrders.size()) {
                            synchronizedOrderBook.addOrder(buyOrders.get(buyIndex));
                        }
                        
                        if (sellIndex < sellOrders.size()) {
                            synchronizedOrderBook.addOrder(sellOrders.get(sellIndex));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        
        // Return something to prevent dead code elimination
        blackhole.consume(synchronizedOrderBook);
    }

    @Benchmark
    public void synchronizedOrderBookMatchingWorkload(Blackhole blackhole) throws InterruptedException {
        OrderBook orderBook = new OrderBook();
        OrderMatcher matcher = new OrderMatcher(orderBook, Collections.singletonList(symbol));
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Half the threads add orders, half match
        int ordersPerThread = numOrders / (numThreads / 2);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // This thread adds orders
                        for (int i = 0; i < ordersPerThread; i++) {
                            int index = (threadId / 2) * ordersPerThread + i;
                            if (index < buyOrders.size()) {
                                orderBook.addOrder(buyOrders.get(index));
                            }
                            if (index < sellOrders.size()) {
                                orderBook.addOrder(sellOrders.get(index));
                            }
                            
                            // Small delay to allow matching to occur
                            if (i % 10 == 0) {
                                Thread.yield();
                            }
                        }
                    } else {
                        // This thread matches orders
                        for (int i = 0; i < ordersPerThread * 2; i++) {
                            matcher.matchOrders(symbol);
                            
                            // Small delay to allow orders to be added
                            if (i % 10 == 0) {
                                Thread.yield();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        
        BlockingQueue<Trade> trades = matcher.getTrades();
        blackhole.consume(trades.size());
    }

    @Benchmark
    public void synchronizedOrderBookHighContention(Blackhole blackhole) throws InterruptedException {
        OrderBook orderBook = new OrderBook();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);

        // All threads operate on the same symbol to maximize contention
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    // Each thread alternates between adding buy and sell orders
                    for (int i = 0; i < numOrders / numThreads; i++) {
                        Order buyOrder = new Order(symbol, Order.Type.BUY, 
                                90 + random.nextDouble() * 10, 1 + random.nextInt(10));
                        orderBook.addOrder(buyOrder);
                        
                        Order sellOrder = new Order(symbol, Order.Type.SELL, 
                                90 + random.nextDouble() * 10, 1 + random.nextInt(10));
                        orderBook.addOrder(sellOrder);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads simultaneously
        completionLatch.await();
        executor.shutdown();
        
        blackhole.consume(orderBook);
    }

    // Future benchmark methods for lock-free implementation
    /*
    @Benchmark
    public void lockFreeOrderBookAddOnly(Blackhole blackhole) throws InterruptedException {
        // Similar to synchronizedOrderBookAddOnly but using lockFreeOrderBook
    }
    
    @Benchmark
    public void lockFreeOrderBookMatchingWorkload(Blackhole blackhole) throws InterruptedException {
        // Similar to synchronizedOrderBookMatchingWorkload but using lockFreeOrderBook
    }
    
    @Benchmark
    public void lockFreeOrderBookHighContention(Blackhole blackhole) throws InterruptedException {
        // Similar to synchronizedOrderBookHighContention but using lockFreeOrderBook
    }
    */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
} 