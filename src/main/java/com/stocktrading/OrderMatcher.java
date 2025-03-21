package com.stocktrading;

import java.util.Queue;

public interface OrderMatcher extends Runnable {
	void matchOrders(String symbol);
	void stop();
	Queue<Trade> getTrades();
}