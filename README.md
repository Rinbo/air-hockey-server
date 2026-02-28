# 🏒 Air Hockey Server

**High-performance, real-time game server for multiplayer air hockey.**

A server-authoritative game engine built with **Spring Boot 4** and **Java 25**
that handles physics simulation, collision detection, player matchmaking, and
state synchronization at 50 Hz over a custom binary WebSocket protocol. Supports
both **two-player online** and **single-player vs AI** modes. Designed for
commercial deployment with low-latency gameplay as the primary objective.

> **Frontend repository:**
> [borjessons-air-hockey](https://github.com/Rinbo/borjessons-air-hockey)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Game Engine](#game-engine)
- [Networking & Protocol](#networking--protocol)
- [Deployment](#deployment)
- [Testing](#testing)
- [License](#license)

---

## Overview

The Air Hockey Server is the authoritative backend for the Börjessons Air Hockey
platform. It owns the game simulation — all physics, scoring, and collision
logic runs server-side to prevent cheating and ensure consistency across
clients. The server broadcasts the computed board state to both players at 50
frames per second using a compact binary protocol.

The application is structured around three communication channels:

1. **REST API** — game creation, player registration, game listing
2. **STOMP over WebSocket** — lobby management, chat, game state machine
   transitions
3. **Raw Binary WebSocket** — high-frequency board-state synchronization (handle
   positions, puck trajectory, timer)

---

## Features

| Category                         | Details                                                                                                   |
| -------------------------------- | --------------------------------------------------------------------------------------------------------- |
| **Server-Authoritative Physics** | All puck movement, friction, wall bounces, and handle collisions computed server-side at 50 FPS           |
| **Binary WebSocket Protocol**    | Custom `Float64`-based binary wire format (40 bytes per broadcast) for minimal overhead                   |
| **Collision Detection**          | Circle-circle (puck ↔ handle) and circle-wall collision with ricochet physics and vector-based reflection |
| **Goal Detection**               | Dynamic goal-zone collision with score tracking and automatic puck reset                                  |
| **Single-Player AI**             | Server-side AI opponent with puck-tracking, defensive positioning, and lerp-smoothed movement             |
| **Game Rooms**                   | Create, join, and manage game rooms with player readiness checks                                          |
| **Lobby Chat**                   | STOMP-based real-time chat within game rooms                                                              |
| **State Machine**                | Full game lifecycle: `LOBBY → GAME_RUNNING → SCORE_SCREEN`, with disconnect handling                      |
| **Concurrency**                  | Each game instance runs on a Java virtual thread with precise nanosecond-level frame timing               |
| **Session Management**           | Automatic cleanup of stale games and disconnected users via background workers                            |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Air Hockey Server                           │
│                                                                     │
│  ┌──────────────┐  ┌───────────────────┐  ┌──────────────────────┐  │
│  │   REST API   │  │  STOMP Broker      │  │  Binary WS Handler  │  │
│  │              │  │                   │  │                      │  │
│  │ GET /games   │  │ /topic/game/*/    │  │ /ws/game/{id}/{p}   │  │
│  │ POST /games  │  │   chat            │  │                      │  │
│  │ GET /users   │  │   players         │  │ 40-byte broadcast    │  │
│  │              │  │   game-state      │  │ 16-byte input        │  │
│  └──────┬───────┘  └────────┬──────────┘  └──────────┬───────────┘  │
│         │                   │                        │              │
│         └───────────────────┼────────────────────────┘              │
│                             │                                       │
│                    ┌────────▼────────┐                               │
│                    │   Game Service  │                               │
│                    │                 │                               │
│                    │  GameStore ←──→ GameEngine                      │
│                    │                    │                            │
│                    │              ┌─────▼──────┐                    │
│                    │              │ Game Loop   │ (Virtual Thread)   │
│                    │              │ @ 50 FPS    │                    │
│                    │              │             │                    │
│                    │              │ BoardState  │                    │
│                    │              │  ├─ Puck    │                    │
│                    │              │  ├─ Handle1 │                    │
│                    │              │  └─ Handle2 │                    │
│                    │              └─────────────┘                    │
│                    └────────────────┘                                │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Background Workers                                          │   │
│  │  · PingWorker — heartbeat monitoring                         │   │
│  │  · RepositoryCleaner — stale game/session cleanup            │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer                | Technology                                                       |
| -------------------- | ---------------------------------------------------------------- |
| **Language**         | Java 25                                                          |
| **Framework**        | Spring Boot 4.0.3                                                |
| **Web**              | Spring Web (REST controllers)                                    |
| **WebSocket**        | Spring WebSocket (STOMP + raw binary handler)                    |
| **Serialization**    | Protocol Buffers 4.34 (available), custom binary for board state |
| **Concurrency**      | Java Virtual Threads (`Thread.ofVirtual()`)                      |
| **Build**            | Maven with Spring Boot Maven Plugin                              |
| **Testing**          | JUnit 5, Spring Boot Test, JavaFX (visual debugging)             |
| **Containerization** | Docker (IBM Semeru JRE)                                          |
| **Hosting**          | Fly.io (Stockholm `arn` region)                                  |

---

## Getting Started

### Prerequisites

- **Java** ≥ 25 (with preview features)
- **Maven** ≥ 3.9

### Build

```bash
./mvnw clean package
```

### Run Locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The server starts on `http://localhost:8443`.

### Run with Docker

```bash
./mvnw clean package -DskipTests
docker build -t air-hockey-server .
docker run -p 8443:8443 air-hockey-server
```

---

## Project Structure

```
src/main/java/nu/borjessons/airhockeyserver/
├── AirHockeyServerApplication.java     # Spring Boot entry point
├── config/                             # Configuration
│   ├── AppConfig.java                  #   Application beans
│   ├── GameWebSocketConfig.java        #   Binary WebSocket endpoint registration
│   ├── WebConfig.java                  #   CORS configuration
│   └── WebSocketConfig.java            #   STOMP broker configuration
├── controller/                         # REST & WebSocket controllers
│   ├── GameController.java             #   Game lifecycle endpoints (STOMP)
│   ├── UserController.java             #   User management (REST)
│   └── security/
│       └── GameValidator.java          #   Request validation
├── event/
│   └── WebsocketEventListener.java     #   Connection/disconnection events
├── game/                               # Core game engine
│   ├── AiPlayer.java                   #   Server-side AI opponent logic
│   ├── BoardState.java                 #   Mutable game state record
│   ├── BroadcastState.java             #   Serializable output state
│   ├── GameEngine.java                 #   Engine lifecycle management
│   ├── GameRunnable.java               #   Main game loop (physics + broadcast)
│   ├── objects/
│   │   ├── Circle.java                 #   Base class for circular game objects
│   │   ├── Handle.java                 #   Player handle (paddle)
│   │   └── Puck.java                   #   Puck with speed, friction, collision
│   └── properties/
│       ├── Collision.java              #   Collision type enum
│       ├── GameConstants.java          #   Physics constants & initial state
│       ├── Position.java               #   2D position (normalized 0–1)
│       ├── Radius.java                 #   Elliptical radius
│       ├── Speed.java                  #   Velocity vector
│       └── Vector.java                 #   Geometric vector for reflection
├── model/                              # Domain model
│   ├── Agency.java                     #   Player role (PLAYER_1 / PLAYER_2)
│   ├── AuthRecord.java                 #   Authentication data
│   ├── Game.java                       #   Game room model
│   ├── GameId.java                     #   Type-safe game identifier
│   ├── GameState.java                  #   State machine enum
│   ├── Notification.java              #   System notification
│   ├── Player.java                     #   Player model
│   ├── UserMessage.java               #   Chat message
│   └── Username.java                  #   Type-safe username
├── repository/                         # In-memory data stores
│   ├── GameStore.java                  #   Per-game state store
│   ├── GameStoreConnector.java         #   Game store ↔ engine bridge
│   └── UserStore.java                 #   Connected user registry
├── service/                            # Business logic
│   ├── api/
│   │   ├── CountdownService.java       #   Countdown abstraction
│   │   └── GameService.java            #   Game service interface
│   ├── CountdownServiceImpl.java       #   Pre-game countdown
│   └── GameServiceImpl.java            #   Game lifecycle orchestration
├── utils/                              # Utilities
│   ├── AppUtils.java                  #   General helpers
│   ├── HeaderUtils.java               #   HTTP header parsing
│   └── TopicUtils.java               #   STOMP topic builders
├── websocket/
│   └── GameWebSocketHandler.java       #   Binary WebSocket handler
└── worker/                             # Background tasks
    ├── PingWorker.java                 #   Client heartbeat checker
    └── RepositoryCleaner.java          #   Stale resource cleanup
```

---

## Game Engine

### Physics Model

The game world uses a **normalized coordinate system** where positions are
expressed as values between `0.0` and `1.0`, independent of screen resolution.
The board has a fixed aspect ratio of **0.625** (width:height).

Key physics parameters:

| Constant                | Value  | Description                       |
| ----------------------- | ------ | --------------------------------- |
| `FRAME_RATE`            | 50 FPS | Server tick rate                  |
| `BOARD_ASPECT_RATIO`    | 0.625  | Width-to-height ratio             |
| `HANDLE_RADIUS`         | 0.09   | Player paddle radius (normalized) |
| `PUCK_RADIUS`           | 0.06   | Puck radius (normalized)          |
| `FRICTION_MODIFIER`     | 20,000 | Friction decay constant           |
| `MAX_SPEED_CONSTITUENT` | ~0.042 | Speed cap per axis                |
| `GAME_DURATION`         | 120 s  | Match length                      |

### Collision Detection

Each tick, the engine runs collision checks in priority order:

1. **Puck ↔ Handle** — circle-circle distance check. On collision, the puck
   speed is set based on the handle's delta position, and the puck is offset to
   prevent overlap.
2. **Puck ↔ Wall** — boundary checks with speed negation for left/right walls.
3. **Puck ↔ Goal** — top/bottom wall hits check for goal-zone entry. A goal
   triggers score increment, puck reset, and a 1-second freeze.
4. **Stale Puck Recovery** — if the puck has zero velocity and is pressed
   against a wall, a small recovery impulse is applied.

### Game Loop

The game loop runs on a **Java virtual thread** with nanosecond-precision timing
to prevent frame drift:

```
while (!interrupted && remainingSeconds > 0) {
    frameStart = nanoTime()

    if (aiMode) aiPlayer.tick()  // compute AI handle position
    puck.onTick()                // apply velocity + friction
    detectCollision()            // walls, handles, goals
    broadcast(state)             // send to both players via binary WS

    sleep(FRAME_DURATION - elapsed)
}
```

---

## Networking & Protocol

### Binary WebSocket — Board State (50 Hz)

The high-frequency game state channel uses raw binary WebSocket to eliminate
JSON/STOMP overhead:

**Server → Client (40 bytes):**

| Offset | Type      | Field             |
| ------ | --------- | ----------------- |
| 0      | `Float64` | Opponent handle X |
| 8      | `Float64` | Opponent handle Y |
| 16     | `Float64` | Puck X            |
| 24     | `Float64` | Puck Y            |
| 32     | `Float64` | Remaining seconds |

**Client → Server (16 bytes):**

| Offset | Type      | Field    |
| ------ | --------- | -------- |
| 0      | `Float64` | Handle X |
| 8      | `Float64` | Handle Y |

All values use **little-endian** byte order.

### STOMP over WebSocket — Game Events

Used for lower-frequency operations:

- **Game creation/joining** via `/app/game/{id}/connect`
- **Add AI opponent** via `/app/game/{id}/add-ai`
- **Chat** via `/app/game/{id}/chat`
- **Ready toggle** via `/app/game/{id}/toggle-ready`
- **Player disconnection** via `/app/game/{id}/disconnect`

### REST API

| Method | Endpoint | Description                |
| ------ | -------- | -------------------------- |
| `GET`  | `/games` | List all active game rooms |
| `POST` | `/games` | Create a new game room     |
| `GET`  | `/users` | List online users          |

---

## Deployment

The server is containerized with Docker and deployed to **Fly.io** in the
Stockholm (`arn`) region for low-latency access within Scandinavia.

### Fly.io Configuration

- **Internal port:** 8080
- **Concurrency:** 20 soft / 25 hard limit
- **Health checks:** TCP every 10s with 2s timeout
- **TLS termination:** handled by Fly.io edge

### Deploy

```bash
./mvnw clean package -DskipTests
fly deploy
```

---

## Testing

The project includes unit and integration tests covering the core game logic:

```bash
./mvnw test
```

### Test Coverage

| Module            | Tests                                               |
| ----------------- | --------------------------------------------------- |
| `AiPlayer`        | Boundary constraints, puck tracking, lerp smoothing |
| `GameRunnable`    | Game loop lifecycle, tick simulation                |
| `Puck`            | Speed, friction, collision math                     |
| `Vector`          | Geometric operations                                |
| `Player`          | Model validation                                    |
| `Username`        | Input sanitization                                  |
| `GameStore`       | State management                                    |
| `GameService`     | Service orchestration                               |
| `UserController`  | REST endpoint integration                           |
| `Canvas` (visual) | JavaFX-based visual physics debugging               |

---

## License

Copyright © 2023–2026 Börjessons. All rights reserved.
