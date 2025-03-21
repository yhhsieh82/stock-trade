package com.stocktrading;

import java.util.List;

public class TradingEngineFactory {
	public static TradingEngine createLockFreeTradingEngine(List<String> symbols) {
		LockFreeOrderBook orderBook = new LockFreeOrderBook();
		LockFreeOrderMatcher matcher = new LockFreeOrderMatcher(orderBook, symbols);
		return new TradingEngine(orderBook, matcher);
	}

	public static TradingEngine createLockedTradingEngine(List<String> symbols) {
		LockedOrderBook orderBook = new LockedOrderBook();
		OrderMatcher matcher = new LockedOrderMatcher(orderBook, symbols);
		return new TradingEngine(orderBook, matcher);
	}
}
