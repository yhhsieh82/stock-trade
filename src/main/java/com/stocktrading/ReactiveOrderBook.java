package com.stocktrading;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Collections;

/**
 * Reactive implementation of the order book that manages buy and sell orders.
 */
public class ReactiveOrderBook {
    // Maps from symbol to buy orders for that symbol (sorted by price descending)
    private final Map<String, ConcurrentSkipListMap<Double, Queue<Order>>> buyOrdersBySymbol;
    
    // Maps from symbol to sell orders for that symbol (sorted by price ascending)
    private final Map<String, ConcurrentSkipListMap<Double, Queue<Order>>> sellOrdersBySymbol;
    
    /**
     * Creates a new reactive order book.
     */
    public ReactiveOrderBook() {
        this.buyOrdersBySymbol = new ConcurrentHashMap<>();
        this.sellOrdersBySymbol = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds an order to the order book.
     *
     * @param order The order to add
     */
    public void addOrder(Order order) {
        String symbol = order.getSymbol();
        
        if (order.getType() == Order.Type.BUY) {
            // Get or create the buy orders map for this symbol
            ConcurrentSkipListMap<Double, Queue<Order>> buyOrders = 
                buyOrdersBySymbol.computeIfAbsent(symbol, k -> 
                    new ConcurrentSkipListMap<>(Collections.reverseOrder()));
            
            // Add the order to the appropriate price level
            buyOrders.computeIfAbsent(order.getPrice(), price -> 
                new ConcurrentLinkedQueue<>()).add(order);
        } else {
            // Get or create the sell orders map for this symbol
            ConcurrentSkipListMap<Double, Queue<Order>> sellOrders = 
                sellOrdersBySymbol.computeIfAbsent(symbol, k -> 
                    new ConcurrentSkipListMap<>());
            
            // Add the order to the appropriate price level
            sellOrders.computeIfAbsent(order.getPrice(), price -> 
                new ConcurrentLinkedQueue<>()).add(order);
        }
    }
    
    /**
     * Gets the best bid (highest buy price) for a symbol.
     *
     * @param symbol The stock symbol
     * @return The best bid price, or null if no buy orders exist
     */
    public Double getBestBid(String symbol) {
        ConcurrentSkipListMap<Double, Queue<Order>> buyOrders = buyOrdersBySymbol.get(symbol);
        return buyOrders != null && !buyOrders.isEmpty() ? buyOrders.firstKey() : null;
    }
    
    /**
     * Gets the best ask (lowest sell price) for a symbol.
     *
     * @param symbol The stock symbol
     * @return The best ask price, or null if no sell orders exist
     */
    public Double getBestAsk(String symbol) {
        ConcurrentSkipListMap<Double, Queue<Order>> sellOrders = sellOrdersBySymbol.get(symbol);
        return sellOrders != null && !sellOrders.isEmpty() ? sellOrders.firstKey() : null;
    }
    
    /**
     * Gets the buy orders for a symbol at a specific price.
     *
     * @param symbol The stock symbol
     * @param price The price level
     * @return The queue of buy orders at that price
     */
    public Queue<Order> getBuyOrders(String symbol, Double price) {
        ConcurrentSkipListMap<Double, Queue<Order>> buyOrders = buyOrdersBySymbol.get(symbol);
        return buyOrders != null ? buyOrders.get(price) : null;
    }
    
    /**
     * Gets the sell orders for a symbol at a specific price.
     *
     * @param symbol The stock symbol
     * @param price The price level
     * @return The queue of sell orders at that price
     */
    public Queue<Order> getSellOrders(String symbol, Double price) {
        ConcurrentSkipListMap<Double, Queue<Order>> sellOrders = sellOrdersBySymbol.get(symbol);
        return sellOrders != null ? sellOrders.get(price) : null;
    }
    
    /**
     * Removes an empty price level from the buy orders.
     *
     * @param symbol The stock symbol
     * @param price The price level to remove
     */
    public void removeBuyPriceLevel(String symbol, Double price) {
        ConcurrentSkipListMap<Double, Queue<Order>> buyOrders = buyOrdersBySymbol.get(symbol);
        if (buyOrders != null) {
            buyOrders.remove(price);
        }
    }
    
    /**
     * Removes an empty price level from the sell orders.
     *
     * @param symbol The stock symbol
     * @param price The price level to remove
     */
    public void removeSellPriceLevel(String symbol, Double price) {
        ConcurrentSkipListMap<Double, Queue<Order>> sellOrders = sellOrdersBySymbol.get(symbol);
        if (sellOrders != null) {
            sellOrders.remove(price);
        }
    }
    
    /**
     * Checks if there are any buy orders for a symbol.
     *
     * @param symbol The stock symbol
     * @return True if there are buy orders, false otherwise
     */
    public boolean hasBuyOrders(String symbol) {
        ConcurrentSkipListMap<Double, Queue<Order>> buyOrders = buyOrdersBySymbol.get(symbol);
        return buyOrders != null && !buyOrders.isEmpty();
    }
    
    /**
     * Checks if there are any sell orders for a symbol.
     *
     * @param symbol The stock symbol
     * @return True if there are sell orders, false otherwise
     */
    public boolean hasSellOrders(String symbol) {
        ConcurrentSkipListMap<Double, Queue<Order>> sellOrders = sellOrdersBySymbol.get(symbol);
        return sellOrders != null && !sellOrders.isEmpty();
    }
} 