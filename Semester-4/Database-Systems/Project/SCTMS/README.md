# Smart City Transport Management System (Java + MySQL + Live Map)

Improved version of your 4th-semester project. Adds:

- ✅ **Login → Admin / Driver / Passenger** dashboards (kept your original theme — blue/green/orange).
- ✅ **MySQL database** stored on **your own system** (full schema in `database/schema.sql`).
- ✅ **Live online Map + Navigation** module (`LiveMap.java`) — uses OpenStreetMap & OSRM,
  **no API key required**. Opens in your default browser, shows real bus stops loaded
  from your MySQL database, and computes a real driving route between two stops.
- ✅ Polished GUI: gradient headers, hover effects on cards, hand cursors, modern fonts.
- ✅ Original modules preserved: Vehicles, Routes, Traffic Lights, Users, Reports,
  Bus Schedule, Nearby Stops.

---

## 1. Setup MySQL (one-time)

1. Install **MySQL 8** + **MySQL Workbench**.
2. In Workbench, open `database/schema.sql` and click **Execute (⚡)**.
   This creates database `smart_city_transport`, all tables, and demo data.
3. Open `src/DatabaseConnection.java` and update if your MySQL password differs:
   ```java
   private static final String USER     = "root";
   private static final String PASSWORD = "pakistan"; // <-- change to yours
   ```

**Demo logins** (created by the seed data):

| Role      | Username   | Password   |
|-----------|------------|------------|
| Admin     | admin      | admin      |
| Driver    | driver     | driver     |
| Passenger | passenger  | passenger  |

---

## 2. Run the project

### Option A — NetBeans (recommended, your original setup)

1. Open the project folder in NetBeans (`File → Open Project`).
2. The `dist/lib/mysql-connector-j-9.5.0.jar` is already bundled.
3. Right-click project → **Clean and Build**, then **Run** (`F6`).

### Option B — From the command line

```bash
# compile
javac -d build/classes -cp dist/lib/mysql-connector-j-9.5.0.jar src/*.java

# run
java -cp "build/classes:dist/lib/mysql-connector-j-9.5.0.jar" LoginFrame
# (Windows: use ; instead of : in the classpath)
java -cp "build/classes;dist/lib/mysql-connector-j-9.5.0.jar" LoginFrame
```

---

## 3. Live Map & Navigation

- Click **Live Map** (Admin / Passenger) or **Navigation** (Driver).
- Click **Open Live Map** → an interactive OpenStreetMap opens in your browser
  with all stops from MySQL plotted as markers.
- To get directions, type two stop names (e.g. `Saddar` → `Clifton`) and click
  **Navigate**. A red route line is drawn with distance & ETA.

No Google Maps API key needed — it uses free public services
(OpenStreetMap tiles + OSRM routing).

---

## 4. Project structure

```
Smart_City_Transport_Management_System/
├── database/
│   └── schema.sql              ← run this in MySQL Workbench first
├── dist/
│   └── lib/mysql-connector-j-9.5.0.jar
├── src/
│   ├── LoginFrame.java
│   ├── DatabaseConnection.java
│   ├── AdminDashboard.java
│   ├── DriverDashboard.java
│   ├── PassengerDashboard.java
│   ├── LiveMap.java            ← NEW: online map + routing
│   ├── VehicleManagement.java
│   ├── RouteManagement.java
│   ├── TrafficLightManagement.java
│   ├── UserManagement.java
│   ├── BusSchedule.java
│   ├── NearbyStops.java
│   ├── DataViewer.java
│   └── icons/
└── README.md
```

---

## 5. Troubleshooting

- **"Communications link failure"** → MySQL service isn't running. Start it from
  Services (Windows) or `sudo service mysql start` (Linux).
- **"Unknown database 'smart_city_transport'"** → run `database/schema.sql`.
- **"Access denied for user 'root'"** → update password in `DatabaseConnection.java`.
- **Live Map page is blank** → you need an internet connection (it loads
  OpenStreetMap tiles online).
