import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * LiveMap — interactive map with:
 *   • City filter  → loads stops from MySQL for that city only
 *   • Route panel  → shows available routes through those stops
 *   • Navigation   → OSRM driving route between two stops
 *   • Map panel    → styled status panel in-window; full Leaflet/OSM map
 *                    opens in the system browser (no JavaFX required)
 *
 * Map:    OpenStreetMap / Leaflet   (no API key)
 * Routes: OSRM public service       (no API key)
 * Deps:   Standard JDK only — no JavaFX, no extra jars
 */
public class LiveMap extends JFrame {

    // ── palette ────────────────────────────────────────────────────────────
    private static final Color C_PRIMARY   = new Color(72,  133, 237);
    private static final Color C_GRAD_END  = new Color(30,   87, 153);
    private static final Color C_BG        = new Color(245, 247, 250);
    private static final Color C_CARD      = Color.WHITE;
    private static final Color C_GREEN     = new Color(46,  213, 115);
    private static final Color C_ORANGE    = new Color(255, 159,  64);
    private static final Color C_TEXT_HINT = new Color(130, 130, 130);
    private static final Color C_BORDER    = new Color(220, 220, 225);
    private static final Color C_MAP_BG    = new Color(228, 236, 248);

    // ── state ──────────────────────────────────────────────────────────────
    /** stop_name → [lat, lng] */
    private final Map<String, double[]>     allStops   = new LinkedHashMap<>();
    /** city → list of stop names in that city */
    private final Map<String, List<String>> cityStops  = new LinkedHashMap<>();
    /** city → list of route descriptions touching that city */
    private final Map<String, List<String>> cityRoutes = new LinkedHashMap<>();

    // ── widgets kept for cross-method access ──────────────────────────────
    private JComboBox<String>        cbCity;
    private JList<String>            lstRoutes;
    private DefaultListModel<String> routeModel = new DefaultListModel<>();
    private JComboBox<String>        cbFrom, cbTo;
    private JLabel                   lblStopCount;

    // ── map-panel status labels (updated when a map action fires) ─────────
    private JLabel lblMapTitle;
    private JLabel lblMapStatus;
    private JLabel lblMapHint;

    // ══════════════════════════════════════════════════════════════════════
    public LiveMap() {
        setTitle("Live City Map & Navigation");
        setSize(1280, 760);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(C_BG);

        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildControlPanel(), buildMapPlaceholder());
        split.setDividerLocation(430);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setResizeWeight(0.0);
        add(split, BorderLayout.CENTER);

        loadDataFromDB();
        refreshCityCombo();
    }

    // ── header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, C_PRIMARY, getWidth(), getHeight(), C_GRAD_END));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        h.setLayout(new BorderLayout());
        h.setPreferredSize(new Dimension(1280, 72));
        h.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("🗺  Live Map & Navigation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        h.add(title, BorderLayout.WEST);

        lblStopCount = new JLabel("Loading…");
        lblStopCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStopCount.setForeground(new Color(210, 230, 255));
        h.add(lblStopCount, BorderLayout.EAST);
        return h;
    }

    // ── left control panel ─────────────────────────────────────────────────
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 8));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill    = GridBagConstraints.BOTH;
        gc.insets  = new Insets(5, 5, 5, 5);
        gc.weightx = 1;

        gc.gridy = 0; gc.weighty = 0;
        panel.add(buildCityCard(), gc);

        gc.gridy = 1; gc.weighty = 1;
        panel.add(buildRouteCard(), gc);

        gc.gridy = 2; gc.weighty = 0;
        panel.add(buildNavCard(), gc);

        return panel;
    }

    // ── card: city selector ────────────────────────────────────────────────
    private JPanel buildCityCard() {
        JPanel card = card("🏙  Select City");

        JLabel lbl = new JLabel("City:");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        cbCity = new JComboBox<>();
        cbCity.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbCity.addItem("All Cities");
        cbCity.addActionListener(e -> onCityChanged());

        JButton btnOpenAll = btn("🌍  Open Map for Selected City", C_PRIMARY);
        btnOpenAll.addActionListener(e -> openMap(getSelectedCity(), null, null));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(lbl, BorderLayout.WEST);
        row.add(cbCity, BorderLayout.CENTER);

        card.add(row);
        card.add(Box.createVerticalStrut(10));
        card.add(btnOpenAll);
        return card;
    }

    // ── card: available routes ─────────────────────────────────────────────
    private JPanel buildRouteCard() {
        JPanel card = card("🚌  Available Routes in Selected City");

        lstRoutes = new JList<>(routeModel);
        lstRoutes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lstRoutes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstRoutes.setFixedCellHeight(28);
        lstRoutes.setCellRenderer(new RouteCellRenderer());
        lstRoutes.setBackground(C_BG);

        JScrollPane scroll = new JScrollPane(lstRoutes);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JButton btnRouteMap = btn("📍  Show Route on Map", C_ORANGE);
        btnRouteMap.addActionListener(e -> {
            String sel = lstRoutes.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Please select a route first.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String routeId = sel.split("\\|")[0].trim();
            openMapForRoute(getSelectedCity(), routeId);
        });

        card.add(scroll);
        card.add(Box.createVerticalStrut(8));
        card.add(btnRouteMap);
        return card;
    }

    // ── card: navigate ─────────────────────────────────────────────────────
    private JPanel buildNavCard() {
        JPanel card = card("🧭  Navigate Between Stops");

        JLabel lblFrom = lbl("From stop:");
        cbFrom = new JComboBox<>();
        cbFrom.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbFrom.setAlignmentX(LEFT_ALIGNMENT);
        cbFrom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblTo = lbl("To stop:");
        cbTo = new JComboBox<>();
        cbTo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbTo.setAlignmentX(LEFT_ALIGNMENT);
        cbTo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JButton btnNav = btn("🧭  Get Driving Route", C_GREEN);
        btnNav.addActionListener(e -> {
            String from = (String) cbFrom.getSelectedItem();
            String to   = (String) cbTo.getSelectedItem();
            if (from == null || to == null || from.equals(to)) {
                JOptionPane.showMessageDialog(this,
                    "Please select two different stops.", "Invalid selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            openMap(getSelectedCity(), from, to);
        });

        JLabel hint = new JLabel("<html><i>Stops update when you change the city filter.</i></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(C_TEXT_HINT);

        card.add(lblFrom);
        card.add(Box.createVerticalStrut(3));
        card.add(cbFrom);
        card.add(Box.createVerticalStrut(10));
        card.add(lblTo);
        card.add(Box.createVerticalStrut(3));
        card.add(cbTo);
        card.add(Box.createVerticalStrut(14));
        card.add(btnNav);
        card.add(Box.createVerticalStrut(10));
        card.add(hint);
        return card;
    }

    // ── right: styled map status placeholder (no JavaFX needed) ───────────
    private JPanel buildMapPlaceholder() {
        // Outer panel with a subtle grid pattern painted like a map background
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // grid lines
                g2.setColor(new Color(200, 215, 235));
                for (int x = 0; x < getWidth();  x += 40) g2.drawLine(x, 0, x, getHeight());
                for (int y = 0; y < getHeight(); y += 40) g2.drawLine(0, y, getWidth(), y);
                // soft vignette
                GradientPaint vg = new GradientPaint(
                    getWidth() / 2f, getHeight() / 2f, new Color(228, 236, 248, 0),
                    0, 0, new Color(200, 218, 240, 100));
                g2.setPaint(vg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        outer.setBackground(C_MAP_BG);
        outer.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, C_BORDER));

        // Centre card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            new EmptyBorder(28, 36, 28, 36)));
        card.setOpaque(true);
        card.setMaximumSize(new Dimension(420, 320));

        JLabel icon = new JLabel("🗺", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        lblMapTitle = new JLabel("Map opens in your browser", SwingConstants.CENTER);
        lblMapTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMapTitle.setForeground(C_PRIMARY);
        lblMapTitle.setAlignmentX(CENTER_ALIGNMENT);

        lblMapStatus = new JLabel(
            "<html><center>Use the controls on the left<br>to view stops, routes, or navigate.</center></html>",
            SwingConstants.CENTER);
        lblMapStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMapStatus.setForeground(new Color(80, 80, 100));
        lblMapStatus.setAlignmentX(CENTER_ALIGNMENT);

        lblMapHint = new JLabel(" ", SwingConstants.CENTER);
        lblMapHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblMapHint.setForeground(C_TEXT_HINT);
        lblMapHint.setAlignmentX(CENTER_ALIGNMENT);

        // Legend strip
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(CENTER_ALIGNMENT);
        legend.add(legendDot(C_PRIMARY,                  "Bus Stop"));
        legend.add(legendDot(C_GREEN,                    "From"));
        legend.add(legendDot(new Color(231, 76, 60),     "To / Route"));
        legend.add(legendDot(C_ORANGE,                   "Route Line"));

        card.add(icon);
        card.add(Box.createVerticalStrut(14));
        card.add(lblMapTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblMapStatus);
        card.add(Box.createVerticalStrut(16));
        card.add(legend);
        card.add(Box.createVerticalStrut(14));
        card.add(lblMapHint);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.add(card);

        outer.add(centre, BorderLayout.CENTER);
        return outer;
    }

    /** Small coloured dot + label used in the legend. */
    private JPanel legendDot(Color c, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(c);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel txt = new JLabel(text);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txt.setForeground(new Color(80, 80, 100));
        p.add(dot);
        p.add(txt);
        return p;
    }

    // ── update the in-window status card ───────────────────────────────────
    private void setMapStatus(String title, String status, String hint) {
        SwingUtilities.invokeLater(() -> {
            if (lblMapTitle  != null) lblMapTitle.setText(title);
            if (lblMapStatus != null) lblMapStatus.setText(
                "<html><center>" + status + "</center></html>");
            if (lblMapHint   != null) lblMapHint.setText(hint);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Data loading
    // ══════════════════════════════════════════════════════════════════════

    private void loadDataFromDB() {
        try (Connection c = DatabaseConnection.getConnection()) {

            // 1. stops
            String stopSql =
                "SELECT stop_name, city, latitude, longitude " +
                "FROM stops WHERE status = 'Active' ORDER BY city, stop_name";
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(stopSql)) {
                while (r.next()) {
                    String name = r.getString("stop_name");
                    String city = r.getString("city");
                    double lat  = r.getDouble("latitude");
                    double lng  = r.getDouble("longitude");
                    allStops.put(name, new double[]{lat, lng});
                    cityStops.computeIfAbsent(city, k -> new ArrayList<>()).add(name);
                }
            }

            // 2. routes per city
            String routeSql =
                "SELECT DISTINCT s.city, " +
                "       r.id, r.route_name, r.start_point, r.end_point, " +
                "       r.distance_km, r.estimated_time, r.status " +
                "FROM stops s " +
                "JOIN route_stops rs ON rs.stop_id = s.id " +
                "JOIN routes r      ON r.id = rs.route_id " +
                "WHERE s.status = 'Active' " +
                "ORDER BY s.city, r.route_name";
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(routeSql)) {
                while (r.next()) {
                    String city  = r.getString("city");
                    String entry = String.format("%d | %s  [%s → %s]  %.1f km · ~%d min  (%s)",
                        r.getInt("id"),
                        r.getString("route_name"),
                        r.getString("start_point"),
                        r.getString("end_point"),
                        r.getDouble("distance_km"),
                        r.getInt("estimated_time"),
                        r.getString("status"));
                    cityRoutes.computeIfAbsent(city, k -> new ArrayList<>()).add(entry);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Could not load data from MySQL.\n" +
                    "Ensure the DB is running and sctms_stops.sql was executed.\n\n" + ex.getMessage(),
                    "Database error", JOptionPane.WARNING_MESSAGE));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI updates
    // ══════════════════════════════════════════════════════════════════════

    private void refreshCityCombo() {
        cbCity.removeAllItems();
        cbCity.addItem("All Cities");
        for (String city : cityStops.keySet()) cbCity.addItem(city);
        lblStopCount.setText(allStops.size() + " stops · " + cityStops.size() + " cities loaded");
        onCityChanged();
    }

    private void onCityChanged() {
        String city = getSelectedCity();

        routeModel.clear();
        if (city == null) {
            Set<String> seen = new LinkedHashSet<>();
            for (List<String> routes : cityRoutes.values()) seen.addAll(routes);
            seen.forEach(routeModel::addElement);
        } else {
            List<String> routes = cityRoutes.getOrDefault(city, Collections.emptyList());
            if (routes.isEmpty())
                routeModel.addElement("— No routes found for " + city + " —");
            else
                routes.forEach(routeModel::addElement);
        }

        List<String> stops = city == null
            ? new ArrayList<>(allStops.keySet())
            : cityStops.getOrDefault(city, Collections.emptyList());

        cbFrom.removeAllItems();
        cbTo.removeAllItems();
        for (String s : stops) { cbFrom.addItem(s); cbTo.addItem(s); }
        if (stops.size() > 1) cbTo.setSelectedIndex(1);
    }

    /** Returns null when "All Cities" is selected. */
    private String getSelectedCity() {
        String sel = (String) cbCity.getSelectedItem();
        return "All Cities".equals(sel) ? null : sel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Map generation  (HTML → temp file → system browser)
    // ══════════════════════════════════════════════════════════════════════

    /** Open map for a city, optionally with a driving route overlay. */
    private void openMap(String city, String fromStop, String toStop) {
        List<String> stopNames = city == null
            ? new ArrayList<>(allStops.keySet())
            : cityStops.getOrDefault(city, Collections.emptyList());

        if (stopNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No stops found for this city.",
                "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double[] from = fromStop != null ? allStops.get(fromStop) : null;
        double[] to   = toStop   != null ? allStops.get(toStop)   : null;

        double avgLat = stopNames.stream()
            .mapToDouble(n -> allStops.getOrDefault(n, new double[]{0,0})[0]).average().orElse(30.0);
        double avgLng = stopNames.stream()
            .mapToDouble(n -> allStops.getOrDefault(n, new double[]{0,0})[1]).average().orElse(70.0);

        StringBuilder markers = new StringBuilder();
        for (String name : stopNames) {
            double[] coord = allStops.get(name);
            if (coord == null) continue;
            markers.append(String.format(
                "L.marker([%.7f,%.7f],{icon:busIcon}).addTo(map)" +
                ".bindPopup('<b>%s</b><br><small>%s</small>');\n",
                coord[0], coord[1],
                name.replace("'", "\\'"),
                city != null ? city : "Pakistan"));
        }

        if (from != null)
            markers.append(String.format(
                "L.marker([%.7f,%.7f],{icon:fromIcon}).addTo(map)" +
                ".bindPopup('<b>FROM:</b> %s').openPopup();\n",
                from[0], from[1], fromStop.replace("'", "\\'")));
        if (to != null)
            markers.append(String.format(
                "L.marker([%.7f,%.7f],{icon:toIcon}).addTo(map)" +
                ".bindPopup('<b>TO:</b> %s');\n",
                to[0], to[1], toStop.replace("'", "\\'")));

        String routeJs = "";
        if (from != null && to != null) {
            routeJs = String.format(
                "fetch('https://router.project-osrm.org/route/v1/driving/%.7f,%.7f;%.7f,%.7f?overview=full&geometries=geojson')" +
                ".then(r=>r.json()).then(d=>{" +
                "  if(!d.routes||!d.routes.length){alert('No route found');return;}" +
                "  var pts=d.routes[0].geometry.coordinates.map(c=>[c[1],c[0]]);" +
                "  L.polyline(pts,{color:'#e74c3c',weight:6,opacity:0.9}).addTo(map);" +
                "  map.fitBounds(pts);" +
                "  var km=(d.routes[0].distance/1000).toFixed(1);" +
                "  var min=Math.round(d.routes[0].duration/60);" +
                "  document.getElementById('infobar').innerHTML=" +
                "    '<b>🚗 Route:</b> %s &nbsp;→&nbsp; %s &nbsp;|&nbsp; '+km+' km · ~'+min+' min';" +
                "}).catch(e=>alert('Routing error: '+e));",
                from[1], from[0], to[1], to[0],
                fromStop, toStop);
        }

        String titleStr = city != null ? city : "All Pakistan";
        String infoStr  = fromStop != null
            ? "Computing driving route…"
            : "Showing " + stopNames.size() + " stops in " + titleStr + ". Click a marker for details.";

        writeAndOpen(buildBaseHtml(titleStr, infoStr, avgLat, avgLng, 12,
                                   markers.toString(), routeJs));

        if (fromStop != null && toStop != null) {
            setMapStatus("Driving Route",
                fromStop + " → " + toStop,
                "Leaflet map opened in browser · red line = OSRM route");
        } else {
            setMapStatus("City: " + titleStr,
                stopNames.size() + " bus stops shown",
                "Leaflet map opened in browser");
        }
    }

    /** Open map highlighting all stops belonging to a specific route. */
    private void openMapForRoute(String city, String routeId) {
        List<double[]> pts    = new ArrayList<>();
        List<String>   pNames = new ArrayList<>();
        String         rName  = "";

        try (Connection c = DatabaseConnection.getConnection()) {
            String sql =
                "SELECT s.stop_name, s.latitude, s.longitude, r.route_name " +
                "FROM route_stops rs " +
                "JOIN stops  s ON s.id = rs.stop_id " +
                "JOIN routes r ON r.id = rs.route_id " +
                "WHERE rs.route_id = ? AND s.status = 'Active' " +
                "ORDER BY rs.stop_order";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(routeId));
                ResultSet r = ps.executeQuery();
                while (r.next()) {
                    pNames.add(r.getString("stop_name"));
                    pts.add(new double[]{ r.getDouble("latitude"), r.getDouble("longitude") });
                    rName = r.getString("route_name");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Failed to load route stops:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No stops are linked to this route in route_stops table yet.",
                "No data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double avgLat = pts.stream().mapToDouble(p -> p[0]).average().orElse(30.0);
        double avgLng = pts.stream().mapToDouble(p -> p[1]).average().orElse(70.0);

        StringBuilder markers = new StringBuilder();
        StringBuilder latLngs = new StringBuilder("[");
        for (int i = 0; i < pts.size(); i++) {
            double[] p = pts.get(i);
            String   n = pNames.get(i).replace("'", "\\'");
            String   seq = (i == 0 ? "🟢 Start" : i == pts.size()-1 ? "🔴 End" : "⚪ Stop " + (i+1));
            markers.append(String.format(
                "L.marker([%.7f,%.7f]).addTo(map)" +
                ".bindPopup('%s<br><small>%s</small>');\n",
                p[0], p[1], seq + ": " + n, rName));
            latLngs.append(String.format("[%.7f,%.7f]", p[0], p[1]));
            if (i < pts.size()-1) latLngs.append(",");
        }
        latLngs.append("]");

        String polylineJs =
            "var pts=" + latLngs + ";" +
            "if(pts.length>1){" +
            "  L.polyline(pts,{color:'#e67e22',weight:5,dashArray:'8 4',opacity:0.9}).addTo(map);" +
            "  map.fitBounds(pts,{padding:[30,30]});" +
            "}";

        String html =
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<title>Route: " + rName + "</title>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<style>body{margin:0;font-family:'Segoe UI',Arial}" +
            "#map{height:calc(100vh - 52px)}" +
            "header{background:linear-gradient(135deg,#ff9940,#e67e22);color:#fff;" +
            "  padding:8px 18px;font-size:15px;font-weight:600}" +
            "#infobar{background:#fff;padding:7px 18px;border-bottom:1px solid #ddd;font-size:13px}" +
            ".legend{position:absolute;bottom:30px;left:12px;background:rgba(255,255,255,.92);" +
            "  border-radius:8px;padding:10px 14px;z-index:999;font-size:12px;line-height:1.8}" +
            ".dot{display:inline-block;width:12px;height:12px;border-radius:50%;margin-right:6px}" +
            "</style></head><body>" +
            "<header>🚌 Route: " + rName + "</header>" +
            "<div id='infobar'>Showing " + pts.size() + " stops · Click markers for details.</div>" +
            "<div id='map'></div>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<script>" +
            "var map=L.map('map').setView([" + avgLat + "," + avgLng + "],13);" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'© OpenStreetMap',maxZoom:19}).addTo(map);" +
            markers.toString() + polylineJs +
            "var legend=L.control({position:'bottomleft'});" +
            "legend.onAdd=function(){var d=L.DomUtil.create('div','legend');" +
            "d.innerHTML='<b>Legend</b><br>" +
            "<span class=dot style=background:#27ae60></span>Start<br>" +
            "<span class=dot style=background:#e74c3c></span>End<br>" +
            "<span class=dot style=background:#888></span>Stop<br>" +
            "<span style=color:#e67e22;font-weight:700>──</span> Route line';" +
            "return d;};legend.addTo(map);" +
            "</script></body></html>";

        writeAndOpen(html);
        setMapStatus("Route: " + rName,
            pts.size() + " stops · orange dashed line",
            "Leaflet map opened in browser");
    }

    // ── shared HTML builder ────────────────────────────────────────────────
    private String buildBaseHtml(String titleStr, String infoStr,
            double centerLat, double centerLng, int zoom,
            String markersJs, String routeJs) {

        return
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<title>SCTMS Live Map – " + titleStr + "</title>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<style>" +
            "  *{box-sizing:border-box} body{margin:0;font-family:'Segoe UI',Arial}" +
            "  #map{height:calc(100vh - 52px)}" +
            "  header{display:flex;align-items:center;gap:12px;" +
            "    background:linear-gradient(135deg,#4885ed,#1e5799);" +
            "    color:#fff;padding:8px 18px;font-size:15px;font-weight:600}" +
            "  #infobar{background:#fff;padding:7px 18px;border-bottom:1px solid #ddd;" +
            "    font-size:13px;color:#222;min-height:30px}" +
            "  .legend{position:absolute;bottom:30px;left:12px;background:rgba(255,255,255,.92);" +
            "    border-radius:8px;padding:10px 14px;z-index:999;font-size:12px;line-height:1.8}" +
            "  .dot{display:inline-block;width:12px;height:12px;border-radius:50%;margin-right:6px}" +
            "</style></head><body>" +
            "<header>🚍 SCTMS Live Map — " + titleStr + "</header>" +
            "<div id='infobar'>" + infoStr + "</div>" +
            "<div id='map'></div>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<script>" +
            "var map=L.map('map').setView([" + centerLat + "," + centerLng + "]," + zoom + ");" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'© <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors',maxZoom:19}).addTo(map);" +
            "var busIcon=L.divIcon({className:'',html:" +
            "'<div style=\"background:#4885ed;color:#fff;border-radius:50%;width:26px;height:26px;" +
            "display:flex;align-items:center;justify-content:center;font-size:13px;" +
            "border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4);\">🚌</div>'," +
            "iconSize:[26,26],iconAnchor:[13,13]});" +
            "var fromIcon=L.divIcon({className:'',html:" +
            "'<div style=\"background:#27ae60;color:#fff;border-radius:50%;width:30px;height:30px;" +
            "display:flex;align-items:center;justify-content:center;font-size:15px;" +
            "border:2px solid #fff;box-shadow:0 1px 6px rgba(0,0,0,.5);\">🟢</div>'," +
            "iconSize:[30,30],iconAnchor:[15,15]});" +
            "var toIcon=L.divIcon({className:'',html:" +
            "'<div style=\"background:#e74c3c;color:#fff;border-radius:50%;width:30px;height:30px;" +
            "display:flex;align-items:center;justify-content:center;font-size:15px;" +
            "border:2px solid #fff;box-shadow:0 1px 6px rgba(0,0,0,.5);\">🔴</div>'," +
            "iconSize:[30,30],iconAnchor:[15,15]});" +
            markersJs + routeJs +
            "var legend=L.control({position:'bottomleft'});" +
            "legend.onAdd=function(){var d=L.DomUtil.create('div','legend');" +
            "d.innerHTML='<b>Legend</b><br>" +
            "<span class=dot style=background:#4885ed></span>Bus Stop<br>" +
            "<span class=dot style=background:#27ae60></span>From<br>" +
            "<span class=dot style=background:#e74c3c></span>To / Route';" +
            "return d;};legend.addTo(map);" +
            "</script></body></html>";
    }

    private void writeAndOpen(String html) {
        try {
            Path tmp = Files.createTempFile("sctms_map_", ".html");
            Files.write(tmp, html.getBytes("UTF-8"));
            if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tmp.toUri());
            } else {
                JOptionPane.showMessageDialog(this,
                    "Open this file in your browser:\n" + tmp.toAbsolutePath());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Failed to open map: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private JPanel card(String heading) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(14, 16, 14, 16)));
        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 14));
        h.setForeground(C_PRIMARY);
        h.setAlignmentX(LEFT_ALIGNMENT);
        p.add(h);
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return b;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // Custom renderer: colour-code route status
    private static class RouteCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int idx, boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, value, idx, sel, focus);
            String v = value == null ? "" : value.toString();
            if (!sel) {
                if      (v.contains("(Active)"))            setForeground(new Color(30, 130, 70));
                else if (v.contains("(Under Maintenance)")) setForeground(new Color(180, 100, 0));
                else if (v.contains("(Closed)"))            setForeground(new Color(180, 40, 40));
                else                                        setForeground(Color.GRAY);
            }
            setBorder(new EmptyBorder(2, 8, 2, 8));
            return this;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LiveMap().setVisible(true);
        });
    }
}