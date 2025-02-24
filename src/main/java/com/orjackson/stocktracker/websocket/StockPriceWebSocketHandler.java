package com.orjackson.stocktracker.websocket;

import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class StockPriceWebSocketHandler extends TextWebSocketHandler {
    private static final String API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");
    private static final String API_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=" + API_KEY;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("New WebSocket connection established");
        try {
            // Fetch default stock price (Apple - AAPL) on connection
            String stockSymbol = "AAPL";
            String stockPrice = getStockPrice(stockSymbol);
            session.sendMessage(new TextMessage("AAPL Price: $" + stockPrice));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Received message: " + message.getPayload());
        String stockSymbol = message.getPayload().toUpperCase(); // 🔹 Get the requested stock symbol from client
        String stockPrice = getStockPrice(stockSymbol);
        session.sendMessage(new TextMessage(stockSymbol + " Price: $" + stockPrice));
    }

    private String getStockPrice(String symbol) {
        System.out.println("Getting stock price for: " + symbol);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(API_URL, symbol);
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);
            JSONObject stockData = json.getJSONObject("Global Quote");

            return stockData.getString("05. price");  // 🔹 Extract stock price
        } catch (Exception e) {
            return "Error fetching stock data!";
        }
    }
}
