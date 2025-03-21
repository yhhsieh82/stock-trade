package com.stocktrading;

/**
 * Represents a completed trade between a buy and sell order.
 */
public class Trade {
    private final String symbol;
    private final double price;
    private final int quantity;
    private final long timestamp;
    private final long buyOrderId;
    private final long sellOrderId;
    
    /**
     * Creates a new trade.
     * 
     * @param symbol The stock symbol
     * @param price The price per unit
     * @param quantity The quantity of units
     * @param buyOrderId The ID of the buy order
     * @param sellOrderId The ID of the sell order
     */
    public Trade(String symbol, double price, int quantity, long buyOrderId, long sellOrderId) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
    }
    
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
    public long getBuyOrderId() { return buyOrderId; }
    public long getSellOrderId() { return sellOrderId; }
    
    @Override
    public String toString() {
        return String.format("TRADE: %s %d@%.2f (Buy Order: %d, Sell Order: %d, time: %d)", 
                symbol, quantity, price, buyOrderId, sellOrderId, timestamp);
    }
} 