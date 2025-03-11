package com.stocktrading.benchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to analyze and visualize JMH benchmark results.
 * Run this after saving benchmark output to a file.
 * 
 * Usage: java BenchmarkResultsAnalyzer benchmark-results.txt
 */
public class BenchmarkResultsAnalyzer {

    static class BenchmarkResult {
        String benchmark;
        int threads;
        int orders;
        double throughput;
        double error;
        
        @Override
        public String toString() {
            return String.format("%s (threads=%d, orders=%d): %.2f ±%.2f ops/s", 
                    benchmark, threads, orders, throughput, error);
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Please provide the path to the benchmark results file");
            return;
        }
        
        List<BenchmarkResult> results = parseResults(args[0]);
        generateReport(results);
    }
    
    private static List<BenchmarkResult> parseResults(String filePath) throws IOException {
        List<BenchmarkResult> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("OrderBookBenchmark\\.(\\w+):(\\w+)=(\\d+):(\\w+)=(\\d+)\\s+.*\\s+(\\d+\\.\\d+)\\s+±\\s+(\\d+\\.\\d+)\\s+ops/s");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    BenchmarkResult result = new BenchmarkResult();
                    result.benchmark = matcher.group(1);
                    
                    // Parse parameters (assuming numThreads and numOrders)
                    if ("numThreads".equals(matcher.group(2))) {
                        result.threads = Integer.parseInt(matcher.group(3));
                        result.orders = Integer.parseInt(matcher.group(5));
                    } else {
                        result.orders = Integer.parseInt(matcher.group(3));
                        result.threads = Integer.parseInt(matcher.group(5));
                    }
                    
                    result.throughput = Double.parseDouble(matcher.group(6));
                    result.error = Double.parseDouble(matcher.group(7));
                    
                    results.add(result);
                }
            }
        }
        
        return results;
    }
    
    private static void generateReport(List<BenchmarkResult> results) {
        // Group by benchmark type
        Map<String, List<BenchmarkResult>> byBenchmark = new HashMap<>();
        for (BenchmarkResult result : results) {
            byBenchmark.computeIfAbsent(result.benchmark, k -> new ArrayList<>()).add(result);
        }
        
        // Print summary by benchmark type
        System.out.println("=== BENCHMARK SUMMARY ===");
        for (Map.Entry<String, List<BenchmarkResult>> entry : byBenchmark.entrySet()) {
            System.out.println("\n== " + entry.getKey() + " ==");
            
            // Group by order count
            Map<Integer, List<BenchmarkResult>> byOrders = new HashMap<>();
            for (BenchmarkResult result : entry.getValue()) {
                byOrders.computeIfAbsent(result.orders, k -> new ArrayList<>()).add(result);
            }
            
            // Print throughput by thread count for each order count
            for (Map.Entry<Integer, List<BenchmarkResult>> orderEntry : byOrders.entrySet()) {
                System.out.println("\nOrders: " + orderEntry.getKey());
                System.out.println("Threads\tThroughput (ops/s)\tError");
                
                // Sort by thread count
                orderEntry.getValue().sort((a, b) -> Integer.compare(a.threads, b.threads));
                
                for (BenchmarkResult result : orderEntry.getValue()) {
                    System.out.printf("%d\t%.2f\t±%.2f\n", 
                            result.threads, result.throughput, result.error);
                }
            }
        }
        
        // Print scaling analysis
        System.out.println("\n=== SCALING ANALYSIS ===");
        for (Map.Entry<String, List<BenchmarkResult>> entry : byBenchmark.entrySet()) {
            System.out.println("\n== " + entry.getKey() + " ==");
            
            // Group by order count
            Map<Integer, List<BenchmarkResult>> byOrders = new HashMap<>();
            for (BenchmarkResult result : entry.getValue()) {
                byOrders.computeIfAbsent(result.orders, k -> new ArrayList<>()).add(result);
            }
            
            // Calculate scaling efficiency
            for (Map.Entry<Integer, List<BenchmarkResult>> orderEntry : byOrders.entrySet()) {
                System.out.println("\nOrders: " + orderEntry.getKey());
                System.out.println("Threads\tScaling Efficiency");
                
                // Sort by thread count
                List<BenchmarkResult> sortedResults = new ArrayList<>(orderEntry.getValue());
                sortedResults.sort((a, b) -> Integer.compare(a.threads, b.threads));
                
                if (!sortedResults.isEmpty()) {
                    double baselineThroughput = sortedResults.get(0).throughput;
                    int baselineThreads = sortedResults.get(0).threads;
                    
                    for (BenchmarkResult result : sortedResults) {
                        double idealThroughput = baselineThroughput * (result.threads / (double)baselineThreads);
                        double efficiency = (result.throughput / idealThroughput) * 100;
                        System.out.printf("%d\t%.2f%%\n", result.threads, efficiency);
                    }
                }
            }
        }
    }
} 