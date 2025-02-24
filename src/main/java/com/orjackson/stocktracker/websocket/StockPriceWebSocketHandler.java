package com.orjackson.stocktracker.websocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class StockPriceWebSocketHandler extends TextWebSocketHandler {
    private static final String API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");
    private static final String API_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=" + API_KEY;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("New WebSocket connection established: " + session.getId());
        try {
            // Fetch default stock price (Apple - AAPL) on connection
            String stockSymbol = "AAPL";
            String stockPrice = getStockPrice(stockSymbol);
            session.sendMessage(new TextMessage("AAPL Price: $" + stockPrice));
        } catch (Exception e) {
            System.err.println("Error sending initial stock price: " + e.getMessage());
            try {
                session.sendMessage(new TextMessage("Error: Unable to fetch stock data."));
            } catch (IOException ioException) {
                System.err.println("Failed to send error message: " + ioException.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("WebSocket closed: " + session.getId() + " Reason: " + status);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String stockSymbol = message.getPayload().trim().toUpperCase();
        System.out.println("Received message: " + stockSymbol);

        // Validate stock symbol (basic check)
        if (!validateStockSymbol(session, stockSymbol)) return;

        try {
            String stockPrice = getStockPrice(stockSymbol);
            session.sendMessage(new TextMessage(stockSymbol + " Price: $" + stockPrice));
        } catch (Exception e) {
            System.err.println("Error fetching stock price: " + e.getMessage());
            try {
                session.sendMessage(new TextMessage("Error: Unable to fetch stock data for " + stockSymbol));
            } catch (IOException ioException) {
                System.err.println("Failed to send error message: " + ioException.getMessage());
            }
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

    private boolean validateStockSymbol(WebSocketSession session, String stockSymbol) {
        if (stockSymbol.isEmpty() || stockSymbol.length() > 5) {
            try {
                session.sendMessage(new TextMessage("Error: Invalid stock symbol."));
            } catch (IOException e) {
                System.err.println("Failed to send invalid stock symbol error: " + e.getMessage());
            }
            return false;
        }
        return true;
    }

    private String getStockPrice(String symbol) {
        System.out.println("Fetching stock price for: " + symbol);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(API_URL, symbol);
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);
            JSONObject stockData = json.getJSONObject("Global Quote");

            return stockData.getString("05. price");
        } catch (JSONException e) {
            System.err.println("JSON parsing error: " + e.getMessage());
            return "Error: Invalid response from API.";
        } catch (Exception e) {
            System.err.println("API request failed: " + e.getMessage());
            return "Error: API request failed.";
        }
    }
}

