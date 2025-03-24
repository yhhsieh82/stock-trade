package com.stocktrading;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Reactive implementation of the order matcher that matches buy and sell orders.
 */
public class ReactiveOrderMatcher {
    private final ReactiveOrderBook orderBook;
    
    /**
     * Creates a new reactive order matcher.
     *
     * @param orderBook The order book to match orders from
     */
    public ReactiveOrderMatcher(ReactiveOrderBook orderBook) {
        this.orderBook = orderBook;
    }
    
    /**
     * Matches orders for a specific symbol.
     *
     * @param symbol The stock symbol
     * @return A flux of trades that were executed
     */
    public Flux<Trade> matchOrders(String symbol) {
        return Mono.fromCallable(() -> {
            List<Trade> trades = new ArrayList<>();
            
            // Continue matching as long as there are matching orders
            while (true) {
                // Get the best bid and ask prices
                Double bestBid = orderBook.getBestBid(symbol);
                Double bestAsk = orderBook.getBestAsk(symbol);
                
                // If either is null or they don't match, we're done
                if (bestBid == null || bestAsk == null || bestBid < bestAsk) {
                    break;
                }
                
                // Get the order queues at the best prices
                Queue<Order> buyOrders = orderBook.getBuyOrders(symbol, bestBid);
                Queue<Order> sellOrders = orderBook.getSellOrders(symbol, bestAsk);
                
                // Get the earliest orders
                Order buyOrder = buyOrders.poll();
                if (buyOrder == null) {
                    // Remove empty price level
                    orderBook.removeBuyPriceLevel(symbol, bestBid);
                    continue;
                }
                
                Order sellOrder = sellOrders.poll();
                if (sellOrder == null) {
                    // Remove empty price level
                    orderBook.removeSellPriceLevel(symbol, bestAsk);
                    // Put the buy order back
                    buyOrders.add(buyOrder);
                    continue;
                }
                
                // Match the orders
                int matchedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
                
                // Update quantities
                boolean buySuccess = buyOrder.reduceQuantity(matchedQuantity);
                boolean sellSuccess = sellOrder.reduceQuantity(matchedQuantity);
                
                if (buySuccess && sellSuccess) {
                    // Create a trade
                    Trade trade = new Trade(
                        symbol,
                        sellOrder.getPrice(),
                        matchedQuantity,
                        buyOrder.getId(),
                        sellOrder.getId()
                    );
                    trades.add(trade);
                }
                
                // Put back orders with remaining quantity
                if (buyOrder.getQuantity() > 0) {
                    buyOrders.add(buyOrder);
                }
                
                if (sellOrder.getQuantity() > 0) {
                    sellOrders.add(sellOrder);
                }
                
                // Remove empty price levels
                if (buyOrders.isEmpty()) {
                    orderBook.removeBuyPriceLevel(symbol, bestBid);
                }
                
                if (sellOrders.isEmpty()) {
                    orderBook.removeSellPriceLevel(symbol, bestAsk);
                }
            }
            
            return trades;
        })
        .flatMapMany(Flux::fromIterable)
        .onErrorResume(e -> {
            System.err.println("Error matching orders for " + symbol + ": " + e.getMessage());
            return Flux.empty();
        });
    }
} 