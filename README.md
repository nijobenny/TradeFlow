# TradeFlow ⚡📦

**B2B wholesale ordering, inventory and invoicing — a full-stack Kotlin project.**
A **Ktor + PostgreSQL** REST & WebSocket backend and a **Compose Multiplatform** app (Android & iOS), sharing one API contract and one set of business rules.

![TradeFlow screens](screenshots/banner.jpg)

The demo company, *Harbour & Co. Wholesale* in Rotterdam, supplies cafés, hotels and restaurants across the Netherlands, Belgium and Germany: 36 products, 16 business clients, 5 staff with different roles, and around 75 days of orders, invoices and payments.

---

## Screenshots

### Getting started

| Splash | Sign in & demo roles | Server check · PostgreSQL |
|:-:|:-:|:-:|
| <img src="screenshots/start_01_splash.jpg" width="170"/> | <img src="screenshots/start_02_login.jpg" width="170"/> | <img src="screenshots/start_03_server.jpg" width="170"/> |

### Overview dashboard

| Revenue & KPIs | Revenue chart & pipeline | Best sellers & top clients | Live activity |
|:-:|:-:|:-:|:-:|
| <img src="screenshots/overview_01_kpis.jpg" width="170"/> | <img src="screenshots/overview_02_chart.jpg" width="170"/> | <img src="screenshots/overview_03_rankings.jpg" width="170"/> | <img src="screenshots/overview_04_activity.jpg" width="170"/> |

### Orders

| Order list | Order detail & stepper | Live status banner | Order history |
|:-:|:-:|:-:|:-:|
| <img src="screenshots/orders_01_list.jpg" width="170"/> | <img src="screenshots/orders_02_detail.jpg" width="170"/> | <img src="screenshots/orders_03_live.jpg" width="170"/> | <img src="screenshots/orders_04_history.jpg" width="170"/> |

### New order flow

| 1 · Choose a client | 2 · Add products | 3 · Review & place | Order placed |
|:-:|:-:|:-:|:-:|
| <img src="screenshots/neworder_01_client.jpg" width="170"/> | <img src="screenshots/neworder_02_products.jpg" width="170"/> | <img src="screenshots/neworder_03_review.jpg" width="170"/> | <img src="screenshots/neworder_04_placed.jpg" width="170"/> |

### Inventory

| Inventory | Product & stock | New product |
|:-:|:-:|:-:|
| <img src="screenshots/inventory_01_grid.jpg" width="170"/> | <img src="screenshots/inventory_02_product.jpg" width="170"/> | <img src="screenshots/inventory_03_form.jpg" width="170"/> |

### Clients

| Clients | Client detail | New client |
|:-:|:-:|:-:|
| <img src="screenshots/clients_01_list.jpg" width="170"/> | <img src="screenshots/clients_02_detail.jpg" width="170"/> | <img src="screenshots/clients_03_form.jpg" width="170"/> |

### Invoices

| Invoices | Invoice detail |
|:-:|:-:|
| <img src="screenshots/finance_01_invoices.jpg" width="170"/> | <img src="screenshots/finance_02_invoice.jpg" width="170"/> |

### Team, activity & roles

| More | Team leaderboard | Activity log | Role permissions |
|:-:|:-:|:-:|:-:|
| <img src="screenshots/more_01_menu.jpg" width="170"/> | <img src="screenshots/more_02_team.jpg" width="170"/> | <img src="screenshots/more_03_activity.jpg" width="170"/> | <img src="screenshots/more_04_roles.jpg" width="170"/> |

## Highlights

- **Every screen is backed by the Ktor API and PostgreSQL.** No mock data in the app.
- **One shared contract.** DTOs, enums, pricing rules, permissions and validation live in a Kotlin Multiplatform `core` module used by both the server (JVM) and the app (Android/iOS). The app's live price preview and the server's charge use the *same code*, so they always agree.
- **Real-time.** A WebSocket feed pushes every order, status change, stock movement and payment to all connected apps; dashboards refresh themselves and a banner pops up ("Arjun placed SO-10241 · €1,240").
- **Role-based access control.** Admin, Sales, Warehouse and Finance roles, enforced by the server (HTTP 403) and reflected in the UI.
- **Transactional business logic.** Placing an order locks product rows (`SELECT … FOR UPDATE`), checks stock and the client's credit limit, prices with tier and bulk discounts plus 21% VAT, reserves stock and writes an audit log, all in one transaction.
- **Zero-setup database.** The server starts a real embedded PostgreSQL on first run (data kept in `~/.tradeflow/pgdata`). Point it at any PostgreSQL with `DATABASE_URL` instead.

## App features

| Area | What you can do |
|---|---|
| 📊 **Overview** | Revenue this month with trend vs last month, KPI tiles, a 30-day revenue chart you can scrub with your finger, an order-pipeline donut, best sellers, top clients, reorder alerts and a live activity feed |
| 🧾 **Orders** | Search and filter by status (with counts), order detail with an animated fulfilment stepper, line items, VAT breakdown, invoice and full history; confirm, pack, ship, deliver or cancel (cancelling returns stock and voids the invoice) |
| 🛒 **New order** | 3-step flow: pick a client (credit available shown), add products with steppers (stock-aware), see live pricing with tier and bulk discounts, review and place, with a confetti celebration |
| 📦 **Inventory** | Product grid with stock bars and low-stock badges, stock value summary, product detail with margin, weekly sales chart and stock-movement history, adjust stock (in/out with reasons), add and edit products |
| 🤝 **Clients** | Tier badges (Bronze → Platinum), credit usage, lifetime spend, spend per month, invoices and orders; call/email, onboard and edit clients |
| 💶 **Invoices** | Outstanding / overdue / paid-this-month totals, status filters, invoice detail with payment progress and history, record full or partial payments (bank transfer, iDEAL, card, cash) |
| 🏆 **Team & activity** | Sales leaderboard, actions per person, full audit log |
| 🔍 **Search** | One search across products, clients, orders and invoices (server-side `ILIKE` queries) |
| 🎨 **Settings** | Light / Dark / System theme, 5 accent palettes with animated cross-fades, server status, sign out |

**Premium motion throughout:** animated splash, drifting aurora background, spring press effects, staggered entrances, shimmer skeletons, pull to refresh, animated counters and charts, a sliding tab indicator, toasts and live banners — all drawn in Compose, no image assets.

## Architecture

```
┌────────────────────── Compose Multiplatform app (shared/) ──────────────────────┐
│  Screens ─▶ Loader state holders ─▶ TradeFlowApi (Ktor client) ──── REST ────┐  │
│  SessionViewModel (Koin) ─▶ DataStore (token, server, theme)                 │  │
│                    ▲                                                          │  │
│                    └──────────── WebSocket live events ◀──────────────────┐  │  │
└────────────────────────────────────────────────────────────────────────────┼──┼──┘
                     core/ : DTOs · Pricing · Permissions · Validation        │  │
┌──────────────────────────── Ktor server (server/) ────────────────────────┼──┼──┐
│  /api/live (WebSockets) ◀── LiveBus ◀── routes ◀── JWT auth ◀─────────────┘  │  │
│  REST routes: auth · dashboard · products · customers · orders · invoices ◀──┘  │
│  Exposed DSL ─▶ HikariCP ─▶ PostgreSQL (embedded or DATABASE_URL)               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### API

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/health` | Status, version and PostgreSQL version (public) |
| POST | `/api/auth/login` | Returns a JWT, the user and company (public) |
| GET | `/api/me`, `/api/company` | Signed-in user, company settings |
| GET | `/api/dashboard` | All KPIs and charts in one call (SQL aggregates) |
| GET/POST/PUT | `/api/products`, `/api/products/{id}` | `?q=`, `?category=`, `?lowStock=true` |
| POST | `/api/products/{id}/stock` | Stock adjustment with reason |
| GET | `/api/products/categories` | Distinct categories |
| GET/POST/PUT | `/api/customers`, `/api/customers/{id}` | `?q=`, `?tier=` |
| GET/POST | `/api/orders`, `/api/orders/{id}` | `?status=`, `?q=`, `?customerId=`; counts per status |
| POST | `/api/orders/{id}/status` | Validated state machine + role check |
| GET | `/api/invoices`, `/api/invoices/{id}` | `?status=`; totals for outstanding/overdue |
| POST | `/api/invoices/{id}/payments` | Partial or full payments |
| GET | `/api/team`, `/api/activity`, `/api/search?q=` | Leaderboard, audit log, global search |
| WS | `/api/live?token=JWT` | Live business events |

Errors are always `{"error": "readable message"}` with the right HTTP status (400, 401, 403, 404, 409).

### Database

PostgreSQL schema (Exposed DSL): `users`, `products`, `stock_movements`, `customers`, `orders`, `order_lines`, `order_events`, `invoices`, `payments`, `activity_log`. Money is stored as integer cents and times as epoch milliseconds. Invoice status (unpaid, partial, paid, overdue, void) is derived from amounts and dates, so it can never go stale.

## Run it

**1. Start the server** (from the project folder):

```bash
./gradlew :server:run
```

The first start downloads dependencies, starts PostgreSQL and generates the demo data. It then prints the addresses to use:

```
  ⚡ TradeFlow API 1.0.0 is running
  Local:     http://localhost:8080
  Network:   http://192.168.1.20:8080   ← use this in the app on your phone (same Wi-Fi)
  Database:  PostgreSQL 14.x (embedded)
  Sign in:   maya@tradeflow.dev / demo1234
```

**2. Run the app.** Open the project in Android Studio, choose `androidApp` and your device, and press Run.

**3. Connect the phone to the server**, either way:
- **USB:** run `adb reverse tcp:8080 tcp:8080`, then use the default `http://127.0.0.1:8080`.
- **Wi-Fi:** on the sign-in screen open *Server*, enter the *Network* address and tap *Test connection*.

**Demo accounts** (password `demo1234`): `maya@` Admin · `arjun@` and `noah@` Sales · `lars@` Warehouse · `sofie@` Finance (all `@tradeflow.dev`). Try the same screen with different roles to see access control in action.

**Live demo traffic:** while an app is connected, the server places a realistic order or moves one along every couple of minutes so you can watch the dashboard update live. Turn it off with `DEMO_TRAFFIC=off`.

**Your own PostgreSQL** (optional): `docker compose up -d`, then

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/tradeflow DB_USER=tradeflow DB_PASSWORD=tradeflow ./gradlew :server:run
```

Tests: `./gradlew :core:jvmTest :server:test`.

## Tech stack

- **Backend:** Ktor 3 (Netty, Content Negotiation, Status Pages, Call Logging, CORS, JWT Authentication, WebSockets), Exposed, HikariCP, PostgreSQL, embedded PostgreSQL (zonky), BCrypt, Logback
- **App:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Ktor Client (OkHttp / Darwin) with WebSockets, Koin, DataStore, kotlinx.serialization, kotlinx-datetime
- **Shared:** a KMP `core` module (JVM + Android + iOS) with the API contract and business rules
- **Quality:** unit tests for pricing, order state machine, permissions, money formatting, invoice status and JWT; GitHub Actions CI builds the server and APK

## Project structure

```
core/      shared DTOs, enums, Pricing, Permissions, Validation (+ tests)
server/    Ktor API: routes/, db/ (tables, database setup, seeder), auth, live bus
shared/    Compose Multiplatform app: data/ (API client, session), ui/ (screens, components, theme)
androidApp/  Android entry point, adaptive icon, splash
iosApp/      iOS entry point
```

---

Built by **Nijo Benny** · Android & Kotlin Multiplatform developer
