# OceanView Resort — Reservation System SPEC

> **Stack:** Spring Boot 3.4.1 · PostgreSQL · React (Vite) · JWT · Lombok · JPA
> **Assessment:** CIS6003 Advanced Programming — 100% WRIT1
> **Grading target:** 70–100 (Excellent) across all four tasks

---

## Critical Analysis of the Assessment

### What the brief actually demands (vs. what beginners miss)

| Requirement              | Surface reading      | What earns Excellent (70+)                                                                                            |
| ------------------------ | -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Task B — distributed app | "just a web app"     | True REST API + separate React SPA, CORS-enabled, stateless JWT auth, public home page + protected booking zone       |
| Task B — design patterns | "mention patterns"   | Patterns visible **in code**: Repository, Service Layer, Strategy (pricing), Builder (DTOs), Singleton (Spring beans) |
| Task B — database        | "store things in DB" | Normalised schema, **proper FK relationships** between User ↔ Room ↔ Reservation, `@PrePersist` hooks                 |
| Task C — TDD             | "write some tests"   | Tests written **before** implementation, JUnit 5 + Mockito, test automation proof via screenshots                     |
| Task D — GitHub          | "upload code"        | Multiple commits per day, **branch strategy**, CI/CD via GitHub Actions, link in report                               |

### Design pattern justification (required by marking criteria)

- **Repository Pattern** — Spring Data JPA `JpaRepository`; decouples data access from business logic
- **Service Layer Pattern** — `@Service` classes mediate between controllers and repositories; single responsibility
- **Strategy Pattern** — `RoomPricingStrategy` interface with concrete implementations per `RoomType`; pricing rules swappable without touching reservation logic
- **Builder Pattern** — Lombok `@Builder` on DTOs; readable object construction, eliminates telescoping constructors
- **Singleton Pattern** — Spring `@Bean` components (JWT util, Security config) managed as singletons by the IoC container
- **DTO Pattern** — Request/Response DTOs prevent entity leakage and allow independent API evolution

---

## 1. System Architecture (3-Tier)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Tier 1 — Presentation (React + Vite, port 5173)                    │
│  PUBLIC:    HomePage · RoomsPage · RoomDetailPage · Login · Register │
│  PROTECTED: MyReservationsPage · NewReservationPage · BillPage      │
│  ADMIN:     AdminDashboard · ManageRooms · ManageReservations        │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTP/REST (JSON) — JWT Bearer token
┌────────────────────────▼────────────────────────────────────────────┐
│  Tier 2 — Business Logic (Spring Boot, port 8080)                   │
│  Controllers → Services → Repositories                              │
│  JWT Security Filter · Role-based Access Control                    │
└────────────────────────┬────────────────────────────────────────────┘
                         │ JPA / Hibernate
┌────────────────────────▼────────────────────────────────────────────┐
│  Tier 3 — Data (PostgreSQL, port 5432)                              │
│  Tables: users · user_roles · rooms · reservations                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Domain Model

### Entity Relationship

```
User (1) ──────────────────────── (N) Reservation
                                          │
Room (1) ──────────────────────── (N) Reservation
  │
RoomType (enum)
```

### Entity: `User`

```java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;                          // BCrypt hashed

    private String fullName;
    private String email;
    private String phoneNumber;

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();
}
```

### Entity: `Room`

```java
@Entity @Table(name = "rooms")
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomNumber;                        // e.g. "101", "SUITE-3"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;                        // SINGLE/DOUBLE/SUITE/DELUXE/PENTHOUSE

    private String description;
    private String imageUrl;                          // path or URL to room photo
    private Integer floor;
    private Integer maxOccupancy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    private Boolean available = true;                 // false when under maintenance

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();
}
```

### Enum: `RoomType`

```java
public enum RoomType {
    SINGLE,       // $80/night  — 1 bed, 1 guest
    DOUBLE,       // $120/night — 1 double bed, 2 guests
    SUITE,        // $200/night — living area + bedroom, 2 guests
    DELUXE,       // $300/night — ocean view, king bed, 3 guests
    PENTHOUSE     // $500/night — full floor, private terrace, 4 guests
}
```

### Enum: `Role`

```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
```

### Entity: `Reservation`

```java
@Entity @Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID reservationNumber;

    // ── Relationships ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // ── Guest Details (snapshot at booking time) ──────────────────
    private String guestName;
    private String guestAddress;
    private String contactNumber;

    // ── Booking Details ───────────────────────────────────────────
    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;                    // computed on save

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;

    private LocalDateTime createdAt;

    // ── Lifecycle ─────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (reservationNumber == null) reservationNumber = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, CHECKED_OUT
    }
}
```

---

## 3. Database Schema

### `users`

| Column       | Type         | Constraints       |
| ------------ | ------------ | ----------------- |
| id           | BIGSERIAL    | PK                |
| username     | VARCHAR(50)  | UNIQUE NOT NULL   |
| password     | VARCHAR(255) | NOT NULL (BCrypt) |
| full_name    | VARCHAR(100) |                   |
| email        | VARCHAR(100) | UNIQUE            |
| phone_number | VARCHAR(20)  |                   |

### `user_roles`

| Column  | Type        | Constraints            |
| ------- | ----------- | ---------------------- |
| user_id | BIGINT      | FK → users.id          |
| roles   | VARCHAR(20) | ROLE_USER / ROLE_ADMIN |

### `rooms`

| Column          | Type          | Constraints                                     |
| --------------- | ------------- | ----------------------------------------------- |
| id              | BIGSERIAL     | PK                                              |
| room_number     | VARCHAR(20)   | UNIQUE NOT NULL                                 |
| room_type       | VARCHAR(20)   | NOT NULL — SINGLE/DOUBLE/SUITE/DELUXE/PENTHOUSE |
| description     | TEXT          |                                                 |
| image_url       | VARCHAR(500)  |                                                 |
| floor           | INTEGER       |                                                 |
| max_occupancy   | INTEGER       |                                                 |
| price_per_night | NUMERIC(10,2) | NOT NULL                                        |
| available       | BOOLEAN       | DEFAULT true                                    |

### `reservations`

| Column             | Type          | Constraints                                   |
| ------------------ | ------------- | --------------------------------------------- |
| id                 | BIGSERIAL     | PK                                            |
| reservation_number | UUID          | UNIQUE NOT NULL                               |
| user_id            | BIGINT        | FK → users.id NOT NULL                        |
| room_id            | BIGINT        | FK → rooms.id NOT NULL                        |
| guest_name         | VARCHAR(100)  | NOT NULL                                      |
| guest_address      | TEXT          | NOT NULL                                      |
| contact_number     | VARCHAR(20)   | NOT NULL                                      |
| check_in_date      | DATE          | NOT NULL                                      |
| check_out_date     | DATE          | NOT NULL                                      |
| total_cost         | NUMERIC(10,2) | NOT NULL                                      |
| status             | VARCHAR(20)   | PENDING / CONFIRMED / CANCELLED / CHECKED_OUT |
| created_at         | TIMESTAMP     | auto-set on insert                            |

---

## 4. Room Pricing (Strategy Pattern)

| Room Type | Default Nightly Rate (USD) | Max Occupancy |
| --------- | -------------------------- | ------------- |
| SINGLE    | 80.00                      | 1             |
| DOUBLE    | 120.00                     | 2             |
| SUITE     | 200.00                     | 2             |
| DELUXE    | 300.00                     | 3             |
| PENTHOUSE | 500.00                     | 4             |

> `pricePerNight` is stored on the `Room` entity (can differ per room). The Strategy Pattern drives the **default** rate used when seeding rooms.

`totalCost = room.pricePerNight × numberOfNights`
`numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate)`

The `RoomPricingStrategy` interface is implemented by each room type class, selected via a `PricingStrategyFactory`.

---

## 5. Backend — Full File List to Implement

### `model/`

- [x] `User.java` — add `fullName`, `email`, `phoneNumber`, `@OneToMany reservations`
- [x] `Role.java` — `enum { ROLE_USER, ROLE_ADMIN }`
- [x] `Reservation.java` — add `@ManyToOne user`, `@ManyToOne room`, `ReservationStatus` enum, remove standalone `roomType` field
- [ ] `Room.java` — new entity (see domain model above)

### `repository/`

- [ ] `UserRepository.java` — `findByUsername`, `existsByUsername`, `existsByEmail`
- [ ] `RoomRepository.java` — `findByAvailableTrue()`, `findByRoomType(RoomType)`, `existsByRoomNumber(String)`
- [ ] `ReservationRepository.java` — `findByReservationNumber(UUID)`, `findByUserId(Long)`, `findByRoomId(Long)`, `existsConflict(roomId, checkIn, checkOut)` (@Query)

### `dto/`

- [ ] `AuthRequest.java` — `{ username, password }`
- [ ] `RegisterRequest.java` — `{ username, password, fullName, email, phoneNumber }`
- [ ] `AuthResponse.java` — `{ token, username, roles, fullName }`
- [ ] `RoomRequest.java` — `{ roomNumber, roomType, description, imageUrl, floor, maxOccupancy, pricePerNight }`
- [ ] `RoomResponse.java` — all Room fields + `available`
- [ ] `ReservationRequest.java` — `{ roomId, guestName, guestAddress, contactNumber, checkInDate, checkOutDate }`
- [ ] `ReservationResponse.java` — all fields + `reservationNumber` + `totalCost` + `status` + nested `RoomResponse`
- [ ] `BillResponse.java` — `{ reservationNumber, guestName, roomNumber, roomType, checkInDate, checkOutDate, nights, pricePerNight, totalCost, status }`

### `service/`

- [ ] `UserDetailsServiceImpl.java` — implements `UserDetailsService`
- [ ] `AuthService.java` — register, login
- [ ] `RoomService.java` — addRoom, updateRoom, deleteRoom, getAllRooms, getRoomById, getAvailableRooms, searchAvailableRooms(dates, roomType)
- [ ] `ReservationService.java` — createReservation (validates no date conflict), findAllByUser, findById, cancel, updateStatus, generateBill
- [ ] `BillService.java` — builds `BillResponse` from a `Reservation`

### `service/pricing/` (Strategy Pattern)

- [ ] `RoomPricingStrategy.java` — interface: `BigDecimal getDefaultNightlyRate()`
- [ ] `SingleRoomPricing.java`, `DoubleRoomPricing.java`, `SuiteRoomPricing.java`, `DeluxeRoomPricing.java`, `PenthouseRoomPricing.java`
- [ ] `PricingStrategyFactory.java` — `getStrategy(RoomType)` → correct implementation

### `security/`

- [ ] `JwtUtil.java` — generate, validate, extract claims
- [ ] `JwtAuthFilter.java` — `OncePerRequestFilter`
- [ ] `SecurityConfig.java` — public: `/api/auth/**`, `GET /api/rooms/**`; protected: everything else

### `config/`

- [ ] `CorsConfig.java` — allow `http://localhost:5173`
- [ ] `AppConfig.java` — `BCryptPasswordEncoder` bean

### `controller/`

- [ ] `AuthController.java` — register, login
- [ ] `RoomController.java` — public GET + admin POST/PUT/DELETE
- [ ] `ReservationController.java` — user own reservations + admin full access
- [ ] `DashboardController.java` — admin stats

### `exeption/`

- [ ] `ResourceNotFoundException.java` — `RuntimeException` → 404
- [ ] `RoomNotAvailableException.java` — 409 Conflict
- [ ] `GlobalExceptionHandler.java` — `@RestControllerAdvice`

---

## 6. REST API Reference

### Auth (Public)

| Method | Endpoint             | Description        |
| ------ | -------------------- | ------------------ |
| POST   | `/api/auth/register` | Register new user  |
| POST   | `/api/auth/login`    | Login, returns JWT |

### Rooms

| Method | Endpoint                       | Auth  | Description                        |
| ------ | ------------------------------ | ----- | ---------------------------------- |
| GET    | `/api/rooms`                   | None  | Get all rooms (public — home page) |
| GET    | `/api/rooms/{id}`              | None  | Get single room detail (public)    |
| GET    | `/api/rooms/available`         | None  | Get all currently available rooms  |
| GET    | `/api/rooms/search`            | None  | Search by dates + optional type    |
| POST   | `/api/rooms`                   | ADMIN | Add new room                       |
| PUT    | `/api/rooms/{id}`              | ADMIN | Update room details                |
| DELETE | `/api/rooms/{id}`              | ADMIN | Delete room                        |
| PATCH  | `/api/rooms/{id}/availability` | ADMIN | Toggle room availability           |

### Reservations

| Method | Endpoint                          | Auth       | Description                           |
| ------ | --------------------------------- | ---------- | ------------------------------------- |
| POST   | `/api/reservations`               | USER/ADMIN | Create reservation (conflict check)   |
| GET    | `/api/reservations/my`            | USER/ADMIN | Get current user's reservations       |
| GET    | `/api/reservations/{id}`          | USER/ADMIN | Get reservation by ID                 |
| GET    | `/api/reservations/number/{uuid}` | USER/ADMIN | Get by reservation number             |
| GET    | `/api/reservations`               | ADMIN      | Get all reservations                  |
| PUT    | `/api/reservations/{id}`          | USER/ADMIN | Update reservation                    |
| PATCH  | `/api/reservations/{id}/cancel`   | USER/ADMIN | Cancel reservation                    |
| PATCH  | `/api/reservations/{id}/status`   | ADMIN      | Update status (CONFIRMED/CHECKED_OUT) |
| DELETE | `/api/reservations/{id}`          | ADMIN      | Delete reservation                    |
| GET    | `/api/reservations/{id}/bill`     | USER/ADMIN | Generate bill                         |

### Dashboard (Admin)

| Method | Endpoint                 | Auth  | Description                               |
| ------ | ------------------------ | ----- | ----------------------------------------- |
| GET    | `/api/dashboard/stats`   | ADMIN | totalReservations, revenue, occupancyRate |
| GET    | `/api/dashboard/revenue` | ADMIN | Revenue breakdown by room type            |

---

## 7. Frontend — Full File Structure

### User Journey

```
[Home Page] ──→ browse rooms with photos & prices
      ↓ click "Book Now" on a room
[Login / Register] ──→ JWT stored in localStorage
      ↓ authenticated
[New Reservation Page] ──→ room pre-selected, pick dates, fill guest details
[My Reservations Page] ──→ list of own bookings
[Reservation Detail Page] ──→ view details + print bill
```

### File Structure

```
Frontend/src/
├── pages/
│   ├── public/
│   │   ├── HomePage.jsx              ← hero banner, featured rooms, testimonials
│   │   ├── RoomsPage.jsx             ← all rooms grid with images + filter by type
│   │   └── RoomDetailPage.jsx        ← gallery, description, amenities, "Book Now"
│   ├── auth/
│   │   ├── LoginPage.jsx
│   │   └── RegisterPage.jsx
│   ├── user/
│   │   ├── NewReservationPage.jsx    ← pre-filled with selected room
│   │   ├── MyReservationsPage.jsx    ← cards/table of user's bookings
│   │   └── ReservationDetailPage.jsx ← detail + bill print
│   └── admin/
│       ├── AdminDashboardPage.jsx    ← stats cards + charts
│       ├── ManageRoomsPage.jsx       ← add/edit/delete rooms
│       └── ManageReservationsPage.jsx← all reservations, update status
├── components/
│   ├── layout/
│   │   ├── Navbar.jsx                ← public links + user menu when logged in
│   │   └── Footer.jsx
│   ├── home/
│   │   ├── HeroBanner.jsx            ← full-width beach image + CTAs
│   │   ├── RoomCard.jsx              ← image, type, price, "Book Now"
│   │   └── Testimonials.jsx
│   ├── rooms/
│   │   ├── RoomGrid.jsx
│   │   ├── RoomFilter.jsx            ← type chips + date range picker
│   │   └── RoomImageGallery.jsx
│   ├── reservation/
│   │   ├── ReservationForm.jsx       ← dates + guest details + live cost calc
│   │   ├── ReservationCard.jsx       ← summary card in My Reservations
│   │   └── BillModal.jsx             ← printable bill
│   ├── admin/
│   │   ├── RoomFormModal.jsx
│   │   ├── StatsCard.jsx
│   │   └── ReservationsTable.jsx
│   └── common/
│       ├── ProtectedRoute.jsx
│       ├── AdminRoute.jsx
│       └── LoadingSpinner.jsx
├── services/
│   ├── api.js                        ← Axios instance + JWT interceptor
│   ├── authService.js
│   ├── roomService.js
│   └── reservationService.js
├── context/
│   └── AuthContext.jsx               ← user state, login/logout helpers
├── App.jsx                           ← React Router v6 routes
└── main.jsx
```

### Routes

| Path                         | Component              | Access    |
| ---------------------------- | ---------------------- | --------- |
| `/`                          | HomePage               | Public    |
| `/rooms`                     | RoomsPage              | Public    |
| `/rooms/:id`                 | RoomDetailPage         | Public    |
| `/login`                     | LoginPage              | Public    |
| `/register`                  | RegisterPage           | Public    |
| `/reservations/new`          | NewReservationPage     | Protected |
| `/reservations/new?roomId=X` | NewReservationPage     | Protected |
| `/my-reservations`           | MyReservationsPage     | Protected |
| `/my-reservations/:id`       | ReservationDetailPage  | Protected |
| `/admin`                     | AdminDashboardPage     | Admin     |
| `/admin/rooms`               | ManageRoomsPage        | Admin     |
| `/admin/reservations`        | ManageReservationsPage | Admin     |

### Key UI Sections

#### `HomePage.jsx`

- **HeroBanner** — full-screen beach/resort image, headline "Escape to OceanView", CTAs: "Explore Rooms" + "Book Now"
- **Featured Rooms** — horizontal scroll of `RoomCard` components fetched from `GET /api/rooms`
- **Why Choose Us** — icon blocks (beachfront, 24h service, free WiFi, spa)
- **Testimonials** — guest quotes

#### `RoomsPage.jsx`

- Filter bar: room type chips + date range picker
- Calls `GET /api/rooms/search?checkIn=&checkOut=&type=` when dates selected
- Grid of `RoomCard` components with image, name, price/night, occupancy, "Book Now"

#### `RoomDetailPage.jsx`

- Large image gallery, room description, amenities list, price
- Date picker → "Check Availability" → "Book Now" → `/reservations/new?roomId=X&checkIn=Y&checkOut=Z`

#### `NewReservationPage.jsx`

- If `roomId` query param present → pre-selects room and shows room summary card
- Fields: guestName, guestAddress, contactNumber, checkInDate, checkOutDate
- Live total cost shown as user picks dates
- Submit → success screen with reservation number

### API Service (`api.js`)

```js
const api = axios.create({ baseURL: "http://localhost:8080/api" });
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
export default api;
```

### Form Validation (client-side)

- Guest name: required, min 2 chars
- Contact number: required, matches phone regex
- Check-in: today or future
- Check-out: strictly after check-in
- Room: required selection

---

## 8. Security Implementation

```
POST /api/auth/login  →  AuthService.login()
                      →  AuthenticationManager.authenticate()
                      →  UserDetailsServiceImpl.loadUserByUsername()
                      →  JwtUtil.generateToken()
                      ←  { token, username, roles, fullName }

Public (no JWT): GET /api/rooms/**
Protected (JWT): POST /api/reservations, GET /api/reservations/my
Admin only:      POST /api/rooms, GET /api/reservations, GET /api/dashboard/**
```

**Role-based access:**

- `ROLE_USER` — browse rooms (public), create + view + cancel own reservations
- `ROLE_ADMIN` — full CRUD on rooms and all reservations, dashboard stats

---

## 9. Validation Rules (server-side)

### `ReservationRequest.java`

```java
@NotNull(message = "Room ID is required")
private Long roomId;

@NotBlank(message = "Guest name is required")
@Size(min = 2, max = 100)
private String guestName;

@NotBlank(message = "Address is required")
private String guestAddress;

@NotBlank
@Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Invalid contact number")
private String contactNumber;

@NotNull @FutureOrPresent(message = "Check-in must be today or later")
private LocalDate checkInDate;

@NotNull @Future(message = "Check-out must be a future date")
private LocalDate checkOutDate;
```

**Service-level validations:**

- `checkOutDate` must be strictly after `checkInDate`
- Room must exist and `available = true`
- Room must have no overlapping `PENDING`/`CONFIRMED` reservation for requested dates

```java
// Conflict check — in ReservationRepository
@Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.room.id = :roomId " +
       "AND r.status IN ('PENDING','CONFIRMED') " +
       "AND r.checkInDate < :checkOut AND r.checkOutDate > :checkIn")
boolean existsConflict(Long roomId, LocalDate checkIn, LocalDate checkOut);
```

---

## 10. Testing Plan (Task C — TDD Approach)

### Test classes (write BEFORE implementing)

#### `PricingStrategyTest.java`

- `singleRoom_3nights_shouldReturn240()`
- `suite_5nights_shouldReturn1000()`
- `penthouse_1night_shouldReturn500()`

#### `RoomServiceTest.java`

- `addRoom_shouldSaveWithCorrectPrice()`
- `searchAvailableRooms_noConflict_shouldReturnRoom()`
- `searchAvailableRooms_conflict_shouldExcludeRoom()`

#### `ReservationServiceTest.java`

- `createReservation_shouldComputeTotalCost()`
- `createReservation_shouldGenerateUUID()`
- `createReservation_roomNotAvailable_shouldThrow()`
- `createReservation_dateConflict_shouldThrow()`
- `cancelReservation_shouldSetStatusCancelled()`

#### `JwtUtilTest.java`

- `generateToken_shouldBeValid()`
- `extractUsername_shouldMatchInput()`
- `expiredToken_shouldReturnFalse()`

#### `AuthControllerIntegrationTest.java`

- `login_validCredentials_shouldReturn200WithToken()`
- `login_invalidPassword_shouldReturn401()`
- `register_duplicateUsername_shouldReturn409()`

#### `ReservationControllerIntegrationTest.java`

- `createReservation_unauthenticated_shouldReturn403()`
- `createReservation_authenticated_shouldReturn201()`
- `getMyReservations_shouldReturnOnlyOwnReservations()`

#### `RoomControllerIntegrationTest.java`

- `getAllRooms_shouldReturn200WithoutAuth()`
- `addRoom_asAdmin_shouldReturn201()`
- `addRoom_asUser_shouldReturn403()`

### Tools

- JUnit 5 (via `spring-boot-starter-test`)
- Mockito — mock repositories in service unit tests
- `@SpringBootTest` + `MockMvc` — integration tests
- `@DataJpaTest` — repository slice tests

---

## 11. GitHub Workflow (Task D)

### Branch Strategy

```
main          ← production-ready code
develop       ← integration branch
feature/*     ← e.g. feature/room-entity, feature/jwt-auth
```

### Commit cadence (daily)

```
Day 1:  init project, User/Role/Room/Reservation models
Day 2:  repositories (UserRepo, RoomRepo, ReservationRepo)
Day 3:  JWT security (JwtUtil, JwtAuthFilter, SecurityConfig)
Day 4:  AuthService + AuthController (register + login)
Day 5:  RoomService + RoomController + RoomDTOs
Day 6:  ReservationService + ReservationController + pricing strategy
Day 7:  React — public pages (HomePage, RoomsPage, RoomDetailPage)
Day 8:  React — auth pages + ProtectedRoute
Day 9:  React — booking flow (NewReservationPage, MyReservationsPage, BillModal)
Day 10: React — Admin pages
Day 11: Tests (TDD — write first, implement to pass)
Day 12: Final cleanup, GitHub Actions CI yml
```

### GitHub Actions CI (`.github/workflows/ci.yml`)

```yaml
name: CI
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with: { java-version: "21", distribution: "temurin" }
      - run: cd Backend && ./mvnw test
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with: { node-version: "20" }
      - run: cd Frontend && npm install && npm run build
```

---

## 12. Implementation Order

```
Phase 1 — Backend Foundation
  1. Room.java entity
  2. Update Reservation.java (@ManyToOne room + user, ReservationStatus)
  3. Update User.java (fullName, email, phoneNumber, @OneToMany reservations)
  4. RoomRepository, UserRepository, ReservationRepository
  5. AppConfig (PasswordEncoder), CorsConfig
  6. JwtUtil → JwtAuthFilter → SecurityConfig
  7. UserDetailsServiceImpl
  8. AuthService + AuthController
  ✓ SMOKE TEST: POST /api/auth/login returns JWT

Phase 2 — Room API
  1. PricingStrategyFactory (Strategy Pattern)
  2. RoomService + RoomController + RoomDTOs
  ✓ SMOKE TEST: GET /api/rooms returns list

Phase 3 — Reservation API
  1. BillService
  2. ReservationService (with conflict-check query)
  3. ReservationController + ReservationDTOs
  4. GlobalExceptionHandler
  ✓ SMOKE TEST: full booking flow via Postman

Phase 4 — Frontend Public Pages
  1. api.js (Axios + interceptor) + AuthContext
  2. Navbar + Footer
  3. HomePage (HeroBanner + RoomCard grid)
  4. RoomsPage (filter + search by dates)
  5. RoomDetailPage (gallery + Book Now)
  ✓ TEST: browse rooms without login works

Phase 5 — Frontend Auth + Booking
  1. LoginPage + RegisterPage
  2. ProtectedRoute + AdminRoute
  3. NewReservationPage (pre-filled from query params, live cost calc)
  4. MyReservationsPage + ReservationCard
  5. ReservationDetailPage + BillModal
  ✓ TEST: full booking flow end-to-end

Phase 6 — Admin Pages
  1. AdminDashboardPage (stats cards)
  2. ManageRoomsPage (add/edit/toggle availability)
  3. ManageReservationsPage (all bookings + status updates)

Phase 7 — Tests & CI
  1. Write all test classes (TDD — write first, implement to pass)
  2. GitHub Actions yml
  3. Screenshot passing tests for report
```

---

## 13. `application.properties` (complete)

```properties
spring.application.name=reservation-system

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ocean_view_db
spring.datasource.username=postgres
spring.datasource.password=123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=OceanViewSuperSecretKey2026_Min32Chars!!
jwt.expiration=86400000

# Actuator
management.endpoints.web.exposure.include=health,info

# File upload (room images)
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

---

## 14. Frontend Dependencies

```bash
cd Frontend
npm install axios react-router-dom react-datepicker
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```
