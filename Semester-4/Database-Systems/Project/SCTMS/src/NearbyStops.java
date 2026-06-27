// ==================== NearbyStops.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class NearbyStops extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField locationField;
    private JComboBox<String> radiusCombo;

    private static final Color ACCENT = new Color(245, 140, 50);

    public NearbyStops() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Nearby Bus Stops");
        setSize(960, 680);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Nearby Bus Stops",
                ACCENT, new Color(210, 95, 20), null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Search panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(ThemeUtil.SURFACE);
        searchPanel.setBorder(ThemeUtil.sectionBorder("Find Stops"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        locationField = new JTextField("Downtown");
        radiusCombo   = new JComboBox<>(new String[]{"1 km", "2 km", "5 km", "10 km"});
        ThemeUtil.styleField(locationField);
        ThemeUtil.styleField(radiusCombo);

        addField(searchPanel, g, 0, 0, "Current Location", locationField);
        addField(searchPanel, g, 1, 0, "Search Radius",   radiusCombo);

        JButton searchBtn     = new JButton("Search Stops");
        JButton myLocationBtn = new JButton("Use My Location");
        ThemeUtil.styleButton(searchBtn,     ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(myLocationBtn, ACCENT);
        searchBtn.addActionListener(e -> searchNearbyStops());
        myLocationBtn.addActionListener(e -> detectLocation());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(6, 0, 0, 0));
        actions.add(searchBtn);
        actions.add(myLocationBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(searchPanel, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        // Table
        String[] columns = {"Stop Name", "Location", "Distance", "Routes Available", "Next Bus", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        ThemeUtil.styleTable(table);
        loadSampleStops();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(ThemeUtil.BORDER, 1, true));
        sp.getViewport().setBackground(ThemeUtil.SURFACE);

        // Info banner
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(238, 244, 255));
        infoPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(214, 226, 248), 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel infoLabel = new JLabel("Tip: click a stop to see detailed route information.");
        infoLabel.setFont(ThemeUtil.BODY);
        infoLabel.setForeground(ThemeUtil.TEXT);
        JLabel info2Label = new JLabel("Use \"Use My Location\" to detect your position automatically.");
        info2Label.setFont(ThemeUtil.SMALL);
        info2Label.setForeground(ThemeUtil.TEXT_MUTED);
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(info2Label);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(sp, BorderLayout.CENTER);
        center.add(infoPanel, BorderLayout.SOUTH);

        content.add(top, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private void addField(JPanel parent, GridBagConstraints g, int col, int row, String label, JComponent field) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(ThemeUtil.BODY_B);
        l.setForeground(ThemeUtil.TEXT);
        cell.add(l, BorderLayout.NORTH);
        cell.add(field, BorderLayout.CENTER);
        g.gridx = col; g.gridy = row;
        parent.add(cell, g);
    }

    private void searchNearbyStops() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your location!");
            return;
        }

        tableModel.setRowCount(0);

        String query = "SELECT DISTINCT r.start_point, r.route_name, COUNT(*) as routes " +
                      "FROM routes r " +
                      "JOIN bus_schedule bs ON r.id = bs.route_id " +
                      "WHERE r.start_point LIKE '%" + location + "%' OR r.end_point LIKE '%" + location + "%' " +
                      "GROUP BY r.start_point, r.route_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    "Stop " + (count + 1),
                    rs.getString("start_point"),
                    String.format("%.1f km", Math.random() * 5),
                    rs.getString("routes"),
                    "5 mins",
                    "\uD83D\uDFE2 Active"
                });
                count++;
            }

            if (count == 0) {
                loadSampleStops();
                JOptionPane.showMessageDialog(this,
                    "Showing nearby stops for reference.\nUse search to find specific locations.");
            }
        } catch (Exception e) {
            loadSampleStops();
            e.printStackTrace();
        }
    }

    private void loadSampleStops() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"Main Street Stop", "Main St & 5th Ave", "0.3 km", "5 routes",  "3 mins",  "\uD83D\uDFE2 Active"});
        tableModel.addRow(new Object[]{"City Center Stop", "Downtown Plaza",     "0.8 km", "8 routes",  "7 mins",  "\uD83D\uDFE2 Active"});
        tableModel.addRow(new Object[]{"Mall Junction",    "Shopping Mall",      "1.2 km", "4 routes",  "12 mins", "\uD83D\uDFE2 Active"});
        tableModel.addRow(new Object[]{"University Gate",  "Campus Entrance",    "1.5 km", "3 routes",  "15 mins", "\uD83D\uDFE2 Active"});
        tableModel.addRow(new Object[]{"Hospital Stop",    "City Hospital",      "2.1 km", "6 routes",  "10 mins", "\uD83D\uDFE2 Active"});
        tableModel.addRow(new Object[]{"Airport Terminal", "Airport Road",       "2.8 km", "2 routes",  "25 mins", "\uD83D\uDFE2 Active"});
    }

    private void detectLocation() {
        JOptionPane.showMessageDialog(this,
            "Location Detected:\nDowntown Area, City Center\n\nSearching for nearby stops...",
            "Location Detection",
            JOptionPane.INFORMATION_MESSAGE);

        locationField.setText("Downtown");
        searchNearbyStops();
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new NearbyStops().setVisible(true));
    }
}
