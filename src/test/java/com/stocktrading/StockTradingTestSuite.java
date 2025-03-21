package com.stocktrading;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite that runs all stock trading tests.
 * 
 * Run this class to execute all tests in a single go.
 */
@Suite
@SelectClasses({
    OrderTest.class,
    LockedOrderBookTest.class,
    LockedOrderMatcherTest.class,
	LockFreeOrderBookTest.class,
	LockFreeOrderMatcherTest.class,
    TradingEngineTest.class,
    LockedTradingEngineRaceConditionTest.class,
	LockedOrderMatcherRaceConditionTest.class
})
public class StockTradingTestSuite {
    // This class serves as a test suite container
    // No implementation needed
} 