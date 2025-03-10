# Mock Stock Order Trading Engine

A simple Java-based mock stock order trading engine that demonstrates concurrent order processing and matching using Java's multithreading capabilities.

## Overview

This project implements a basic stock trading engine with the following features:

- Market order submission and matching
- Price-time priority for order matching
- Support for partial order matching
- Concurrent order processing using Java multithreading

## Components

- **Order**: Represents a buy or sell order with attributes like symbol, price, and quantity.
- **OrderBook**: Manages buy and sell orders using concurrent data structures.
- **OrderMatcher**: Matches orders based on price-time priority and supports partial matching.
- **Trade**: Represents a completed trade between a buy and sell order.
- **TradingEngine**: Coordinates the order book and matcher.

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven

### Building the Project

```bash
mvn clean install
```

### Running the Application

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.Main"
```

## Testing

The project includes comprehensive test cases to verify the functionality of the trading engine:

- **OrderTest**: Tests the Order class functionality.
- **OrderBookTest**: Tests the OrderBook class functionality.
- **OrderMatcherTest**: Tests the OrderMatcher class functionality.
- **TradingEngineTest**: Tests the TradingEngine class functionality.
- **ConcurrencyTest**: Tests the concurrency aspects of the trading engine.

Run the tests using:

```bash
mvn test
```

## Concurrency Features

The trading engine demonstrates several Java concurrency features:

- **Thread-safe Collections**: Uses `ConcurrentHashMap` and `PriorityBlockingQueue` for thread-safe operations.
- **ExecutorService**: Manages thread pools for order submission and matching.
- **Atomic Operations**: Uses atomic variables for thread-safe counters.
- **Volatile Variables**: Uses volatile variables for thread visibility.
- **Thread Coordination**: Uses CountDownLatch for coordinating multiple threads.

## Example Usage

```java
// Create a trading engine
List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
TradingEngine engine = new TradingEngine(symbols);
engine.start();

// Submit buy and sell orders
engine.submitOrder(new Order("AAPL", Order.Type.BUY, 150.0, 10));
engine.submitOrder(new Order("AAPL", Order.Type.SELL, 149.0, 5));

// Stop the engine when done
engine.stop();
```

## Tasks

- [x] 20250310 Fix race condition in OrderMatcher#matchOrder and OrderBook#addOrder
```java
race condition example:
step1. OrderMatcher#matchOrder peek a sellOrder at 10
step2. OrderBook#addOrder add a sellOrder at 8
step3. OrderMatcher#matchOrder poll the sellOrder at 8. causing the created trade sets the sell price at 10, but it is 8 that is acutally traded.
```