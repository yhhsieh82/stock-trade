package com.stocktrading.benchmark;

import com.stocktrading.Order;
import com.stocktrading.LockedOrderBook;
import com.stocktrading.LockedOrderMatcher;
import com.stocktrading.Trade;
import com.stocktrading.LockFreeOrderBook;
import com.stocktrading.LockFreeOrderMatcher;

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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private LockedOrderBook lockedOrderBook;
	private LockFreeOrderBook lockFreeOrderBook;

	private List<Order> buyOrders;
	private List<Order> sellOrders;
	private Random random;

	@Setup
	public void setup() {
		lockedOrderBook = new LockedOrderBook();
		lockFreeOrderBook = new LockFreeOrderBook();

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
	public void lockedOrderBookAddOnly(Blackhole blackhole) throws InterruptedException {
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
							lockedOrderBook.addOrder(buyOrders.get(buyIndex));
						}

						if (sellIndex < sellOrders.size()) {
							lockedOrderBook.addOrder(sellOrders.get(sellIndex));
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
		blackhole.consume(lockedOrderBook);
	}

	@Benchmark
	public void lockedOrderBookMatchingWorkload(Blackhole blackhole) throws InterruptedException {
		LockedOrderBook orderBook = new LockedOrderBook();
		LockedOrderMatcher matcher = new LockedOrderMatcher(orderBook, Collections.singletonList(symbol));

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
	public void lockedOrderBookHighContention(Blackhole blackhole) throws InterruptedException {
		LockedOrderBook orderBook = new LockedOrderBook();

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

	@Benchmark
	public void lockFreeOrderBookMatchingWorkload(Blackhole blackhole) throws InterruptedException {
		LockFreeOrderBook orderBook = new LockFreeOrderBook();
		LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, Collections.singletonList(symbol));

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

		// Consume the number of trades to prevent dead code elimination
		blackhole.consume(matcher.getTrades().size());
	}

	@Benchmark
	public void lockFreeOrderBookHighContention(Blackhole blackhole) throws InterruptedException {
		LockFreeOrderBook orderBook = new LockFreeOrderBook();

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

		// Start all threads simultaneously
		startLatch.countDown();
		completionLatch.await();
		executor.shutdown();

		// Count the total number of orders to prevent dead code elimination
		int totalBuyOrders = 0;
		int totalSellOrders = 0;
		
		ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = orderBook.getBuyOrdersMap(symbol);
		if (buyOrders != null) {
			for (ConcurrentLinkedQueue<Order> queue : buyOrders.values()) {
				totalBuyOrders += queue.size();
			}
		}
		
		ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = orderBook.getSellOrdersMap(symbol);
		if (sellOrders != null) {
			for (ConcurrentLinkedQueue<Order> queue : sellOrders.values()) {
				totalSellOrders += queue.size();
			}
		}
		
		blackhole.consume(totalBuyOrders);
		blackhole.consume(totalSellOrders);
	}

	@Benchmark
	public void compareOrderBookImplementations(Blackhole blackhole) throws InterruptedException {
		// Create both order book implementations
		LockedOrderBook lockedOrderBook = new LockedOrderBook();
		LockFreeOrderBook lockFreeOrderBook = new LockFreeOrderBook();
		
		// Create matchers
		LockedOrderMatcher lockedMatcher = new LockedOrderMatcher(lockedOrderBook, Collections.singletonList(symbol));
		LockFreeOrderMatcher lockFreeMatcher = new LockFreeOrderMatcher(lockFreeOrderBook, Collections.singletonList(symbol));
		
		// Start matchers in separate threads
		ExecutorService matcherExecutor = Executors.newFixedThreadPool(2);
		matcherExecutor.submit(lockedMatcher);
		matcherExecutor.submit(lockFreeMatcher);
		
		// Create threads for adding orders
		ExecutorService orderExecutor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch latch = new CountDownLatch(numThreads);
		
		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			orderExecutor.submit(() -> {
				try {
					for (int i = 0; i < numOrders / numThreads; i++) {
						// Create identical orders for both implementations
						Order.Type type = (i % 2 == 0) ? Order.Type.BUY : Order.Type.SELL;
						double price = 100.0 + (threadId * 0.1) + (i * 0.01);
						
						Order lockedOrder = new Order(symbol, type, price, 1);
						Order lockFreeOrder = new Order(symbol, type, price, 1);
						
						// Add to both order books
						lockedOrderBook.addOrder(lockedOrder);
						lockFreeOrderBook.addOrder(lockFreeOrder);
						
						// Small delay to allow matching to occur
						if (i % 10 == 0) {
							Thread.yield();
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
		orderExecutor.shutdown();
		
		// Wait a bit for matchers to process remaining orders
		Thread.sleep(100);
		
		// Stop matchers
		lockedMatcher.stop();
		lockFreeMatcher.stop();
		matcherExecutor.shutdown();
		matcherExecutor.awaitTermination(1, TimeUnit.SECONDS);
		
		// Consume results to prevent dead code elimination
		blackhole.consume(lockedMatcher.getTrades().size());
		blackhole.consume(lockFreeMatcher.getTrades().size());
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(OrderBookBenchmark.class.getSimpleName())
			.build();
		new Runner(opt).run();
	}
}