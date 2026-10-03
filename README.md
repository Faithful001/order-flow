# OrderFlow

OrderFlow is a backend order management and matching system built with Spring Boot. It simulates the core mechanics of a financial exchange: instruments are registered, orders are submitted, and a price-time priority matching engine pairs buyers with sellers in real time. When a trade occurs or a book changes, connected clients receive an instant update over WebSocket.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Core Concepts](#core-concepts)
  - [Instruments](#instruments)
  - [Order Book](#order-book)
  - [Order Matching](#order-matching)
  - [Instrument Engine](#instrument-engine)
  - [Async Order Pipeline](#async-order-pipeline)
  - [Real-Time Book Updates](#real-time-book-updates)
- [API Reference](#api-reference)
  - [Instruments API](#instruments-api)
  - [Orders API](#orders-api)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [Testing](#testing)

---

## Overview

At its heart, OrderFlow maintains one **order book per tradeable instrument**. Each book is a pair of sorted price levels: a **bid side** (buyers, sorted highest price first) and an **ask side** (sellers, sorted lowest price first). When a new order arrives, the engine tries to find a crossing price on the opposite side and fills as much of the order as possible before resting any unfilled remainder back in the book.

The system is designed with concurrency in mind. Every instrument gets its own dedicated single-thread executor, which means book mutations for different instruments never block each other, and a single instrument's book is never touched by more than one thread at a time.

Order submission is decoupled from matching via RabbitMQ. An HTTP client that posts an order gets a `202 Accepted` response immediately, with the actual matching happening asynchronously on the consumer side. This makes the submission endpoint fast and non-blocking even under load.

---

## Architecture

```
HTTP Client
    |
    v
OrderController  ---- submit ---->  OrderEventPublisher --> RabbitMQ (order.queue)
    |                                                              |
    |                                                     OrderCapturedListener
    |                                                              |
    |                                                        OrderService.process()
    |                                                              |
    v                                                    InstrumentEngine (per-instrument)
OrderController  ---- cancel/book --------------------------->  OrderBook
                                                                   |
                                                          BookUpdatePublisher
                                                                   |
                                                    WebSocket /topic/book/{instrument}
```

The flow for order submission is:

1. A `POST /api/v1/orders` request arrives.
2. `OrderService.submit()` validates that the instrument exists and is actively trading.
3. A UUID is assigned to the order and an `OrderCapturedEvent` is published to RabbitMQ.
4. The HTTP response (`202 Accepted`) is returned immediately with the generated order ID.
5. `OrderCapturedListener` picks up the event from `order.queue`.
6. `OrderService.process()` constructs an `Order` object and submits it to the correct `InstrumentEngine`.
7. The `InstrumentEngine` runs the matching logic inside its dedicated thread via `CompletableFuture`.
8. After matching, the updated book snapshot is broadcast over WebSocket.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1.0 |
| Language | Java 17 |
| Persistence | Spring Data JPA + PostgreSQL |
| Messaging | Spring AMQP + RabbitMQ |
| Real-time Push | Spring WebSocket (STOMP over SockJS) |
| Validation | Jakarta Validation (`@NotBlank`, `@Positive`, etc.) |
| Mapping | MapStruct 1.6.3 |
| Boilerplate Reduction | Lombok |
| Build Tool | Maven (with Maven Wrapper) |
| Containerisation | Docker Compose |
| Testing | JUnit 5, Mockito |

---

## Project Structure

```
src/main/java/com/king/orderflow/
├── OrderFlowApplication.java          # Spring Boot entry point
│
├── domain/
│   ├── instrument/                    # Instrument management domain
│   │   ├── Instrument.java            # JPA entity
│   │   ├── InstrumentController.java  # REST controller
│   │   ├── InstrumentEngine.java      # Per-instrument matching engine
│   │   ├── InstrumentMapper.java      # MapStruct DTO mapper
│   │   ├── InstrumentRepository.java  # Spring Data JPA repository
│   │   ├── InstrumentService.java     # Application service
│   │   ├── dto/
│   │   │   ├── CreateInstrumentRequest.java
│   │   │   └── InstrumentResponse.java
│   │   └── enums/
│   │       └── InstrumentStatus.java  # TRADING | HALTED | PAUSED
│   │
│   └── order/                         # Order matching domain
│       ├── Order.java                 # In-memory order model
│       ├── OrderBook.java             # Core matching logic
│       ├── OrderController.java       # REST controller
│       ├── OrderService.java          # Orchestrates engines and messaging
│       ├── dto/
│       │   ├── BookSnapshot.java      # Serialisable book state
│       │   ├── PriceLevel.java        # Price + aggregated quantity
│       │   ├── SubmitOrderRequest.java
│       │   └── Trade.java             # Executed trade record
│       ├── enums/
│       │   ├── OrderSide.java         # BUY | SELL
│       │   ├── OrderStatus.java       # OPEN | PARTIALLY_FILLED | FILLED | CANCELLED
│       │   └── OrderType.java         # LIMIT | MARKET
│       └── message/
│           └── OrderCapturedEvent.java # RabbitMQ message payload
│
├── infrastructure/
│   ├── rabbitmq/
│   │   ├── OrderCapturedListener.java # Consumes from order.queue
│   │   ├── OrderEventPublisher.java   # Publishes to order.exchange
│   │   └── RabbitMQConfig.java        # Exchange, queue, DLQ, binding
│   └── websocket/
│       ├── BookUpdatePublisher.java   # Pushes snapshots over STOMP
│       └── WebSocketConfig.java       # STOMP endpoint and broker config
│
└── shared/
    ├── exception/
    │   ├── DomainException.java              # Base unchecked exception
    │   ├── InstrumentAlreadyExistsException.java
    │   ├── InstrumentNotTradingException.java
    │   ├── InvalidOrderException.java
    │   ├── UnknownInstrumentException.java
    │   └── handler/
    │       └── GlobalExceptionHandler.java   # @RestControllerAdvice
    └── response/
        └── Response.java                     # Generic API envelope
```

---

## Core Concepts

### Instruments

An **instrument** represents a tradeable pair, for example `BTC-USD` or `ETH-GBP`. Each instrument is a JPA entity persisted in PostgreSQL and carries the following metadata:

| Field | Description |
|---|---|
| `symbol` | Unique ticker string (e.g. `BTC-USD`) |
| `baseCurrency` | The asset being bought or sold |
| `quoteCurrency` | The currency used to price it |
| `tickSize` | Minimum price increment |
| `minQuantity` | Minimum order size |
| `status` | Current trading state (`TRADING`, `HALTED`, or `PAUSED`) |

Only instruments with status `TRADING` will accept new orders. Attempting to submit an order against a halted or paused instrument results in an `InstrumentNotTradingException`.

---

### Order Book

`OrderBook` is the central data structure and it lives purely in memory. It holds two `TreeMap<BigDecimal, Deque<Order>>` collections:

- **`bids`** sorted in reverse order (highest bid first)
- **`asks`** sorted in natural order (lowest ask first)

This arrangement means `bids.firstKey()` always returns the best bid and `asks.firstKey()` always returns the best ask, both in O(log n) time.

A secondary `HashMap<UUID, Order> restingById` acts as a fast lookup table for cancellation, so removing an order does not require scanning every price level.

---

### Order Matching

When `OrderBook.submit(order)` is called, three things happen in sequence:

**1. Validation**

The book performs strict upfront validation before touching any internal state:
- The order must have a non-null ID, side, type, and instrument.
- The instrument on the order must match the book's own instrument.
- Remaining quantity must be positive.
- `LIMIT` orders must have a positive price; `MARKET` orders must not have one.
- The order ID must not already be resting in the book.

**2. Matching**

The engine looks at the opposite side of the book (a buy order looks at asks, a sell order looks at bids) and walks through price levels from best to worst, consuming resting orders queue by queue. Each consumed match generates a `Trade` record containing the IDs of both sides, the execution price, and the filled quantity.

An order "crosses" a price level if:
- It is a `MARKET` order (always crosses), or
- It is a `LIMIT` buy whose price is greater than or equal to the ask level's price, or
- It is a `LIMIT` sell whose price is less than or equal to the bid level's price.

The matching loop continues until either the incoming order is fully filled or there are no more crossing price levels.

**3. Settlement**

After matching:
- If the incoming order has no remaining quantity, it is marked `FILLED`.
- If it has remaining quantity and is a `LIMIT` order, it is rested in the book (marked `OPEN` if it never matched at all, or `PARTIALLY_FILLED` if it did) and added to `restingById`.
- If it has remaining quantity and is a `MARKET` order, the unfilled remainder is discarded and the order is marked `CANCELLED`.

---

### Instrument Engine

`InstrumentEngine` wraps an `OrderBook` and a dedicated `ExecutorService` with a single backing thread named `engine-{instrument}`. All mutations (submit and cancel) are dispatched to this thread via `CompletableFuture.supplyAsync(...)`, ensuring the book is never accessed concurrently.

After every successful mutation, the engine calls `takeSnapshot()` to capture the current bid and ask levels as an immutable `BookSnapshot`. This snapshot is stored as a `volatile` field so it can be safely read from any thread without locking.

`OrderService` keeps a `ConcurrentHashMap<String, InstrumentEngine>` of live engines, creating one lazily on first use with `computeIfAbsent`. When the Spring application context shuts down, `@PreDestroy` shuts each engine's executor down gracefully.

---

### Async Order Pipeline

Order submission and order matching are intentionally separated:

- **Submit path (synchronous):** The HTTP request is validated quickly, an order ID is generated, and an `OrderCapturedEvent` is dropped into RabbitMQ. The caller gets a `202 Accepted` with the order ID.
- **Process path (asynchronous):** `OrderCapturedListener` receives the message and calls `OrderService.process()`, which builds the full `Order` object and routes it to the right `InstrumentEngine`.

If `OrderService.process()` throws a `DomainException` (for example, because of an invalid order), the listener catches it and re-throws it as `AmqpRejectAndDontRequeueException`. This tells RabbitMQ not to redeliver the message and instead route it to the dead-letter queue (`order.queue.dlq`), preventing an infinite retry loop for fundamentally invalid messages.

---

### Real-Time Book Updates

After every successful submit or cancel, the current `BookSnapshot` is pushed over WebSocket using Spring's `SimpMessagingTemplate`. The destination topic follows the pattern:

```
/topic/book/{instrument}
```

Clients connect using SockJS at `ws://localhost:9000/api/v1/ws` and subscribe to the topic for the instruments they care about. The WebSocket endpoint accepts connections from any origin.

A `BookSnapshot` contains:
- `instrument` (string)
- `bids` (list of `PriceLevel` objects, each holding a price and the total quantity resting at that level)
- `asks` (same structure)

---

## API Reference

All endpoints are served under the context path `/api/v1`. The server runs on port `9000` by default.

Every REST response (except the book snapshot and cancel endpoints) is wrapped in a standard envelope:

```json
{
  "success": true,
  "message": "Request successful",
  "data": { ... }
}
```

---

### Instruments API

#### List all instruments

```
GET /api/v1/instruments
```

Returns all registered instruments.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "All instruments",
  "data": [
    {
      "symbol": "BTC-USD",
      "baseCurrency": "BTC",
      "quoteCurrency": "USD",
      "tickSize": 0.01,
      "minQuantity": 0.001,
      "status": "TRADING"
    }
  ]
}
```

---

#### Get instrument by symbol

```
GET /api/v1/instruments/{symbol}
```

Returns a single instrument by its ticker symbol.

| Parameter | Type | Description |
|---|---|---|
| `symbol` | path | The instrument ticker, e.g. `BTC-USD` |

**Response `200 OK`:** Returns the instrument object.

**Response `400 Bad Request`:** Returned if the symbol does not exist.

---

#### Create an instrument

```
POST /api/v1/instruments
Content-Type: application/json
```

Registers a new tradeable instrument. The status defaults to `TRADING` automatically.

**Request body:**
```json
{
  "symbol": "ETH-USD",
  "baseCurrency": "ETH",
  "quoteCurrency": "USD",
  "tickSize": 0.01,
  "minQuantity": 0.01
}
```

| Field | Required | Constraints |
|---|---|---|
| `symbol` | Yes | Non-blank, must be unique |
| `baseCurrency` | Yes | Non-blank |
| `quoteCurrency` | Yes | Non-blank |
| `tickSize` | Yes | Positive decimal |
| `minQuantity` | Yes | Positive decimal |

**Response `201 Created`:** Returns the created instrument.

**Response `400 Bad Request`:** Returned if the symbol already exists or validation fails.

---

### Orders API

#### Submit an order

```
POST /api/v1/orders
Content-Type: application/json
```

Queues an order for matching. The response is returned immediately; matching happens asynchronously.

**Request body:**
```json
{
  "instrument": "BTC-USD",
  "side": "BUY",
  "type": "LIMIT",
  "price": 65000.00,
  "quantity": 0.5
}
```

| Field | Required | Constraints |
|---|---|---|
| `instrument` | Yes | Non-blank, must exist and be `TRADING` |
| `side` | Yes | `BUY` or `SELL` |
| `type` | Yes | `LIMIT` or `MARKET` |
| `price` | Conditional | Required for `LIMIT`, must be absent for `MARKET` |
| `quantity` | Yes | Positive decimal |

**Response `202 Accepted`:**
```json
{
  "success": true,
  "message": "Order queued",
  "data": {
    "orderId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Response `400 Bad Request`:** Returned if the instrument does not exist or is not trading.

---

#### Cancel an order

```
DELETE /api/v1/orders/{orderId}?instrument={symbol}
```

Attempts to cancel a resting order. The cancellation runs on the engine's thread and the result is awaited before responding.

| Parameter | Type | Description |
|---|---|---|
| `orderId` | path | UUID of the order to cancel |
| `instrument` | query | The instrument the order was placed on |

**Response `204 No Content`:** The order was found and cancelled.

**Response `404 Not Found`:** The order was not found in the book (it may have already been filled).

---

#### Get the order book snapshot

```
GET /api/v1/orders/{instrument}/book
```

Returns the current in-memory state of the order book for the given instrument.

| Parameter | Type | Description |
|---|---|---|
| `instrument` | path | The instrument symbol |

**Response `200 OK`:**
```json
{
  "instrument": "BTC-USD",
  "bids": [
    { "price": 65000.00, "quantity": 1.5 },
    { "price": 64990.00, "quantity": 0.8 }
  ],
  "asks": [
    { "price": 65010.00, "quantity": 2.0 },
    { "price": 65020.00, "quantity": 0.5 }
  ]
}
```

Bids are sorted from highest to lowest. Asks are sorted from lowest to highest. Quantities shown are the total volume resting at each price level, not individual order sizes.

---

## Data Models

### Order lifecycle

An order can move through the following states:

```
OPEN --> PARTIALLY_FILLED --> FILLED
  |                    |
  +-----> CANCELLED <--+
```

- **`OPEN`:** The order is resting in the book with no fills yet.
- **`PARTIALLY_FILLED`:** Some of the order's quantity has been matched; the remainder is still resting.
- **`FILLED`:** The entire quantity has been matched; the order is removed from the book.
- **`CANCELLED`:** The order was either explicitly cancelled by the client, or it was a market order whose quantity could not be fully consumed.

### Instrument lifecycle

| Status | Meaning |
|---|---|
| `TRADING` | The instrument is active and accepts new orders. |
| `HALTED` | Trading has been suspended; orders are rejected. |
| `PAUSED` | Similar to halted; the instrument is temporarily unavailable for trading. |

---

## Error Handling

All domain errors extend a common base `DomainException` (itself an unchecked `RuntimeException`). The `GlobalExceptionHandler` (`@RestControllerAdvice`) maps each exception type to the appropriate HTTP status:

| Exception | HTTP Status |
|---|---|
| `UnknownInstrumentException` | `400 Bad Request` |
| `InstrumentAlreadyExistsException` | `400 Bad Request` |
| `InstrumentNotTradingException` | `400 Bad Request` |
| `InvalidOrderException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `ResponseStatusException` | Mirrors the status embedded in the exception |

All error responses follow the same envelope structure with `"success": false`.

---

## Configuration

Configuration is split between `application.properties` and a `.env` file that supplies the secret values.

**`application.properties` (key settings):**

| Property | Default / Value |
|---|---|
| `server.port` | `9000` |
| `server.servlet.context-path` | `/api/v1` |
| `spring.datasource.url` | Reads `${DB_URL}` from environment |
| `spring.datasource.hikari.maximum-pool-size` | `10` |
| `spring.rabbitmq.host` | `localhost` |
| `spring.rabbitmq.port` | Reads `${RABBITMQ_PORT}` from environment |
| `spring.jpa.hibernate.ddl-auto` | `update` |

**`.env` (example values):**

```env
DB_URL=jdbc:postgresql://localhost:5441/orderflow_db
DB_USER=orderflow
DB_PASSWORD=orderflow321
DB_NAME=orderflow_db

RABBITMQ_USER=orderflow
RABBITMQ_PASSWORD=orderflow321
RABBITMQ_PORT=5674
RABBITMQ_UI_PORT=15672
```

> Do not commit `.env` to source control. Add it to `.gitignore` if it is not already there.

**RabbitMQ topology:**

| Resource | Name |
|---|---|
| Exchange | `order.exchange` (direct) |
| Queue | `order.queue` (durable) |
| Routing key | `order.captured` |
| Dead-letter queue | `order.queue.dlq` |

---

## Running Locally

### Prerequisites

- Java 17+
- Maven (or use the included `mvnw` wrapper)
- Docker and Docker Compose

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** on port `5441` (mapped from container port `5432`)
- **RabbitMQ** on port `5674` (AMQP) and `15672` (management UI)

### 2. Set environment variables

Either export the variables from `.env` into your shell, or use a tool like `dotenv`. On most Unix-like shells:

```bash
export $(cat .env | xargs)
```

On Windows (PowerShell):
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
    }
}
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/order-flow-0.0.1-SNAPSHOT.jar
```

The application will be available at `http://localhost:9000/api/v1`.

The RabbitMQ management console is at `http://localhost:15672` (login with the credentials in `.env`).

---

## Testing

Run the full test suite with:

```bash
./mvnw test
```

### Test coverage

**`OrderBookTest`** is the most detailed test class and covers the matching engine directly without any Spring context or mocks. It tests:

- Full fill against a single resting order at the same price level.
- Full fill that sweeps across multiple orders within the same price level (time priority observed).
- Partial fill where the resting order's remainder correctly stays in the book.
- No-cross scenario where an incoming order rests entirely without matching.
- Multi-level sweep where a single incoming order walks across three separate price levels.

**`OrderServiceTest`** sets up the service layer with Mockito mocks and is structured with JUnit 5 `@Nested` test groups. The test class scaffolding is in place and ready to be expanded.
