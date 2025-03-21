package com.stocktrading;

import java.util.concurrent.*;
import java.util.*;

public class LockFreeOrderBook implements OrderBook {
    // Map from symbol to buy orders for that symbol
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>>> buyOrdersBySymbol;
    
    // Map from symbol to sell orders for that symbol
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>>> sellOrdersBySymbol;

    public LockFreeOrderBook() {
        buyOrdersBySymbol = new ConcurrentHashMap<>();
        sellOrdersBySymbol = new ConcurrentHashMap<>();
    }

    @Override
    public boolean hasBuyOrders(String symbol) {
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> map = buyOrdersBySymbol.get(symbol);
        return map != null && !map.isEmpty();
    }

    @Override
    public boolean hasSellOrders(String symbol) {
        ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> map = sellOrdersBySymbol.get(symbol);
        return map != null && !map.isEmpty();
    }

    @Override
    public void addOrder(Order order) {
        String symbol = order.getSymbol();
        
        if (order.getType() == Order.Type.BUY) {
            // Get or create the buy orders map for this symbol
            ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> buyOrders = 
                buyOrdersBySymbol.computeIfAbsent(symbol, k -> 
                    new ConcurrentSkipListMap<>(Collections.reverseOrder()));
            
            // Add the order to the appropriate price level
            buyOrders.computeIfAbsent(order.getPrice(), price -> 
                new ConcurrentLinkedQueue<>()).add(order);
        } else {
            // Get or create the sell orders map for this symbol
            ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> sellOrders = 
                sellOrdersBySymbol.computeIfAbsent(symbol, k -> 
                    new ConcurrentSkipListMap<>());
            
            // Add the order to the appropriate price level
            sellOrders.computeIfAbsent(order.getPrice(), price -> 
                new ConcurrentLinkedQueue<>()).add(order);
        }
    }

    public ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> getBuyOrdersMap(String symbol) {
        return buyOrdersBySymbol.get(symbol);
    }

    public ConcurrentSkipListMap<Double, ConcurrentLinkedQueue<Order>> getSellOrdersMap(String symbol) {
        return sellOrdersBySymbol.get(symbol);
    }
}
