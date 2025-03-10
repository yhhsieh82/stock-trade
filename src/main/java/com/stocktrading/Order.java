package com.stocktrading;

/**
 * Represents a market order in the trading system.
 */
public class Order {
    public enum Type { BUY, SELL }
    
    private final String symbol;
    private final Type type;
    private final double price;
    private int quantity;
    private final long timestamp;
    
    /**
     * Creates a new order.
     * 
     * @param symbol The stock symbol
     * @param type The order type (BUY or SELL)
     * @param price The price per unit
     * @param quantity The quantity of units
     */
    public Order(String symbol, Type type, double price, int quantity) {
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getSymbol() { return symbol; }
    public Type getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
    
    /**
     * Reduces the quantity of this order by the specified amount.
     * 
     * @param amount The amount to reduce by
     */
    public void reduceQuantity(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("Cannot reduce by more than the current quantity");
        }
        quantity -= amount;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %d@%.2f (time: %d)", 
                type, symbol, quantity, price, timestamp);
    }
} 