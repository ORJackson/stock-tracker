# 📈 Stock Tracker

A **real-time stock price tracker** built with **Spring Boot** and **WebSockets**, fetching stock data using the **Alpha Vantage API**.

## 🚀 Features
✅ Real-time stock price updates  
✅ WebSocket-based live streaming  
✅ REST API for manual stock lookups  
✅ Supports multiple stock symbols

## 🔧 Tech Stack
- **Backend:** Spring Boot, WebSockets, Alpha Vantage API
- **Build Tool:** Gradle
- **Java Version:** 21

## 📡 WebSocket Endpoint
- **URL:** `ws://localhost:8080/stocks`
- **How to Use:**
    1. Connect using a WebSocket client ([WebSocket Tester](https://www.piesocket.com/websocket-tester)).
    2. Send a stock symbol (e.g., `AAPL`, `TSLA`).
    3. Receive real-time stock price updates.

## 🔥 Running the Project
### **1️⃣ Clone the Repository**
```sh
git clone https://github.com/orjackson/stock-tracker.git
cd stock-tracker
