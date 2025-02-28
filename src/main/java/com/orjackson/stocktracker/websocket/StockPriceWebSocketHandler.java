package com.orjackson.stocktracker.websocket;

import com.orjackson.stocktracker.model.CachedStock;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StockPriceWebSocketHandler extends TextWebSocketHandler {
    private static final String API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");

    private static final String API_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=" + API_KEY;

    // 10 minute cache duration, this is a hobby project and the Alpha Vantage api has a 25 call per day rate limit!
    private static final long CACHE_DURATION_SECONDS = 600;
    private static final ConcurrentHashMap<String, CachedStock> cache = new ConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("New WebSocket connection established: " + session.getId());
        // run cache cleanup
        cleanUpExpiredCache();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("WebSocket closed: " + session.getId() + " Reason: " + status);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String stockSymbol = message.getPayload().trim().toUpperCase();
        System.out.println("Received message: " + stockSymbol);

        if (!validateStockSymbol(stockSymbol)) {
            sendMessage(session, "Error: Invalid stock symbol.");
            return;
        }

        try {
            // Check cache first
            if (cache.containsKey(stockSymbol)) {
                CachedStock cachedStock = cache.get(stockSymbol);
                if (Instant.now().isBefore(cachedStock.createdAt().plusSeconds(CACHE_DURATION_SECONDS))) {
                    System.out.println("Returning cached data for: " + stockSymbol);
                    sendMessage(session, stockSymbol + " Price: $" + cachedStock.price().toPlainString());
                    return;
                }
            }

            // Fetch new stock price
            String stockPriceResponse = getStockPriceResponse(stockSymbol);

            // Handle errors (return response directly if it contains an error)
            if (stockPriceResponse.startsWith("Error")) {
                sendMessage(session, stockPriceResponse);
                return;
            }

            // Convert valid price to BigDecimal and cache it
            BigDecimal price = new BigDecimal(stockPriceResponse);
            cache.put(stockSymbol, new CachedStock(price, Instant.now()));
            limitCacheSize();

            sendMessage(session, stockSymbol + " Price: $" + price.toPlainString());
        } catch (Exception e) {
            System.err.println("Error fetching stock price: " + e.getMessage());
            sendMessage(session, "Error: Unable to fetch stock data for " + stockSymbol);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("WebSocket transport error for session " + session.getId() + ": " + exception.getMessage());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            System.err.println("Failed to close session after transport error: " + e.getMessage());
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    private boolean validateStockSymbol(String symbol) {
        return symbol.matches("^[A-Z]{1,5}$"); // Basic validation (1-5 uppercase letters)
    }

    private String getStockPriceResponse(String symbol) {
        System.out.println("Fetching stock price for: " + symbol);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(API_URL, symbol);
            String response = restTemplate.getForObject(url, String.class);

            // Log full API response
            System.out.println("Raw API Response for " + symbol + ": " + response);

            JSONObject json = new JSONObject(response);

            // Handle rate limit message
            if (json.has("Information")) {
                System.err.println("Rate limit exceeded. Full response: " + response);
                return "Error: Free API limit reached. Try again tomorrow.";
            }

            // Handle missing "Global Quote"
            if (!json.has("Global Quote")) {
                System.err.println("Error: 'Global Quote' key is missing! Full response: " + response);
                return "Error: No stock data available.";
            }

            JSONObject stockData = json.getJSONObject("Global Quote");

            // Handle missing "05. price"
            if (!stockData.has("05. price")) {
                System.err.println("Error: '05. price' key is missing! Full response: " + response);
                return "Error: No price data available.";
            }

            return stockData.getString("05. price");

        } catch (Exception e) {
            System.err.println("Exception while fetching stock data: " + e.getMessage());
            return "Error: API request failed.";
        }
    }

    private void limitCacheSize(){
        if (cache.size() > 50){
            String oldestKey = cache.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().createdAt()))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (oldestKey != null){
                cache.remove(oldestKey);
            }
        }
    }

    private void cleanUpExpiredCache() {
        System.out.println("Running cache cleanup...");
        cache.entrySet().removeIf(entry ->
                Instant.now().isAfter(entry.getValue().createdAt().plusSeconds(CACHE_DURATION_SECONDS))
        );
    }
}

