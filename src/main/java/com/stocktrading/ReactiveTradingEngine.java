package com.stocktrading;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive implementation of the trading engine that uses reactive streams
 * for processing orders and generating trades.
 */
public class ReactiveTradingEngine {
    private final List<String> symbols;
    private final ReactiveOrderBook orderBook;
    private final ReactiveOrderMatcher orderMatcher;
    
    // Sinks for publishing orders and trades
    private final Sinks.Many<Order> orderSink;
    private final Sinks.Many<Trade> tradeSink;
    
    // Streams for subscribing to orders and trades
    private final Flux<Order> orderStream;
    private final Flux<Trade> tradeStream;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * Creates a new reactive trading engine.
     *
     * @param symbols The list of stock symbols to trade
     */
    public ReactiveTradingEngine(List<String> symbols) {
        this.symbols = symbols;
        this.orderBook = new ReactiveOrderBook();
        this.orderMatcher = new ReactiveOrderMatcher(orderBook);
        
        // Create multicast sinks with buffer overflow strategy
        this.orderSink = Sinks.many().multicast().onBackpressureBuffer();
        this.tradeSink = Sinks.many().multicast().onBackpressureBuffer();
        
        // Create streams from sinks
        this.orderStream = orderSink.asFlux().share();
        this.tradeStream = tradeSink.asFlux().share();
    }
    
    /**
     * Starts the trading engine.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Process orders and generate trades
            orderStream
                .doOnNext(orderBook::addOrder)
                .groupBy(Order::getSymbol)
                .flatMap(symbolGroup -> 
                    Flux.interval(java.time.Duration.ofMillis(10))
                        .onBackpressureDrop()
                        .flatMap(tick -> 
                            orderMatcher.matchOrders(symbolGroup.key())
                                .subscribeOn(Schedulers.parallel())
                        )
                )
                .subscribe(trade -> {
                    tradeSink.tryEmitNext(trade);
                });
                
            System.out.println("Reactive trading engine started");
        }
    }
    
    /**
     * Stops the trading engine.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            orderSink.tryEmitComplete();
            tradeSink.tryEmitComplete();
            System.out.println("Reactive trading engine stopped");
        }
    }
    
    /**
     * Submits an order to the trading engine.
     *
     * @param order The order to submit
     */
    public void submitOrder(Order order) {
        if (running.get()) {
            orderSink.tryEmitNext(order);
            System.out.println("Order submitted: " + order);
        }
    }
    
    /**
     * Gets the stream of orders.
     *
     * @return The order stream
     */
    public Flux<Order> getOrderStream() {
        return orderStream;
    }
    
    /**
     * Gets the stream of trades.
     *
     * @return The trade stream
     */
    public Flux<Trade> getTradeStream() {
        return tradeStream;
    }
    
    /**
     * Gets the order book.
     *
     * @return The order book
     */
    public ReactiveOrderBook getOrderBook() {
        return orderBook;
    }
} 