package com.stocktrading.benchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to analyze and visualize JMH benchmark results.
 * Run this after saving benchmark output to a file.
 *
 * Usage: java BenchmarkResultsAnalyzer benchmark-results.txt
 */
public class BenchmarkResultsAnalyzer {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Please provide the path to the benchmark results file");
			return;
		}
		
		String filePath = args[0];
		try {
			List<BenchmarkResult> results = parseResultsFile(filePath);
			
			if (results.isEmpty()) {
				System.out.println("No valid benchmark results found in the file.");
				System.out.println("The file may contain errors or have an unexpected format.");
				return;
			}
			
			System.out.println("=== BENCHMARK SUMMARY ===");
			printSummary(results);
			
			System.out.println("\n=== SCALING ANALYSIS ===");
			analyzeScaling(results);
			
		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
		}
	}

	private static List<BenchmarkResult> parseResultsFile(String filePath) throws IOException {
		List<BenchmarkResult> results = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			boolean resultsSection = false;
			
			while ((line = reader.readLine()) != null) {
				// Skip until we find the results table
				if (line.contains("Benchmark") && line.contains("Mode") && line.contains("Cnt")) {
					resultsSection = true;
					continue;
				}
				
				if (resultsSection && !line.trim().isEmpty() && !line.startsWith("#")) {
					try {
						BenchmarkResult result = parseBenchmarkLine(line);
						if (result != null) {
							results.add(result);
						}
					} catch (Exception e) {
						// Skip malformed lines
						System.err.println("Warning: Could not parse line: " + line);
						e.printStackTrace();
					}
				}
			}
		}
		
		return results;
	}

	private static BenchmarkResult parseBenchmarkLine(String line) {
		// Example line:
		// OrderBookBenchmark.lockFreeOrderBookMatchingWorkload          100             2      AAPL  thrpt    5  4989.641 ± 2604.760  ops/s
		
		String[] parts = line.trim().split("\\s+");
		
		// Extract the benchmark name
		String fullName = parts[0];
		String[] nameParts = fullName.split("\\.");
		String benchmarkName = nameParts[nameParts.length - 1];
		
		// Extract parameters - they are in fixed positions in the JMH output
		int numOrders = Integer.parseInt(parts[1]);
		int numThreads = Integer.parseInt(parts[2]);
		String symbol = parts[3];
		
		// Find the score and error
		// The format is typically: score ± error
		double score = 0;
		double error = 0;
		
		for (int i = 4; i < parts.length - 1; i++) {
			if (parts[i].equals("±")) {
				score = Double.parseDouble(parts[i-1]);
				error = Double.parseDouble(parts[i+1]);
				break;
			}
		}
		
		return new BenchmarkResult(benchmarkName, numOrders, numThreads, symbol, score, error);
	}

	private static void printSummary(List<BenchmarkResult> results) {
		// Group by benchmark name
		Map<String, List<BenchmarkResult>> byBenchmark = new HashMap<>();
		
		for (BenchmarkResult result : results) {
			byBenchmark.computeIfAbsent(result.benchmarkName, k -> new ArrayList<>()).add(result);
		}
		
		for (Map.Entry<String, List<BenchmarkResult>> entry : byBenchmark.entrySet()) {
			System.out.println("\nBenchmark: " + entry.getKey());
			System.out.println("Orders\tThreads\tThroughput (ops/s)");
			
			for (BenchmarkResult result : entry.getValue()) {
				System.out.printf("%d\t%d\t%.2f ± %.2f%n", 
						result.numOrders, result.numThreads, result.score, result.error);
			}
		}
	}

	private static void analyzeScaling(List<BenchmarkResult> results) {
		// Group by benchmark name and numOrders
		Map<String, Map<Integer, List<BenchmarkResult>>> grouped = new HashMap<>();
		
		for (BenchmarkResult result : results) {
			grouped.computeIfAbsent(result.benchmarkName, k -> new HashMap<>())
				   .computeIfAbsent(result.numOrders, k -> new ArrayList<>())
				   .add(result);
		}
		
		for (Map.Entry<String, Map<Integer, List<BenchmarkResult>>> benchmarkEntry : grouped.entrySet()) {
			String benchmarkName = benchmarkEntry.getKey();
			System.out.println("\nScaling analysis for: " + benchmarkName);
			
			for (Map.Entry<Integer, List<BenchmarkResult>> ordersEntry : benchmarkEntry.getValue().entrySet()) {
				int numOrders = ordersEntry.getKey();
				List<BenchmarkResult> resultsByThread = ordersEntry.getValue();
				
				// Sort by number of threads
				resultsByThread.sort((a, b) -> Integer.compare(a.numThreads, b.numThreads));
				
				if (resultsByThread.size() > 1) {
					System.out.println("\nOrders: " + numOrders);
					System.out.println("Threads\tThroughput\tScaling Factor");
					
					BenchmarkResult baseline = resultsByThread.get(0);
					System.out.printf("%d\t%.2f\t1.00 (baseline)%n", 
							baseline.numThreads, baseline.score);
					
					for (int i = 1; i < resultsByThread.size(); i++) {
						BenchmarkResult current = resultsByThread.get(i);
						double scalingFactor = current.score / baseline.score;
						System.out.printf("%d\t%.2f\t%.2f%n", 
								current.numThreads, current.score, scalingFactor);
					}
				}
			}
		}
	}

	static class BenchmarkResult {
		final String benchmarkName;
		final int numOrders;
		final int numThreads;
		final String symbol;
		final double score;
		final double error;
		
		BenchmarkResult(String benchmarkName, int numOrders, int numThreads, String symbol, 
						double score, double error) {
			this.benchmarkName = benchmarkName;
			this.numOrders = numOrders;
			this.numThreads = numThreads;
			this.symbol = symbol;
			this.score = score;
			this.error = error;
		}
	}
}