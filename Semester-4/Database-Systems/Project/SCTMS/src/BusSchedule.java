// ==================== BusSchedule.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class BusSchedule extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;

    private JComboBox<String> routeCombo;
    private JTextField busNumberField, departureField, arrivalField, fareField, seatsField;
    private JLabel statusLabel;

    public BusSchedule() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Bus Schedule Management");
        setSize(1040, 660);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Bus Schedule",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ThemeUtil.SURFACE);
        formPanel.setBorder(ThemeUtil.sectionBorder("Add / Edit Schedule"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        routeCombo      = new JComboBox<>();
        busNumberField  = new JTextField("BUS-101");
        departureField  = new JTextField("08:00");
        arrivalField    = new JTextField("09:00");
        fareField       = new JTextField("5.00");
        seatsField      = new JTextField("50");
        for (JComponent c : new JComponent[]{routeCombo, busNumberField, departureField, arrivalField, fareField, seatsField}) {
            ThemeUtil.styleField(c);
        }
        loadRoutes();

        addField(formPanel, g, 0, 0, "Route",             routeCombo);
        addField(formPanel, g, 1, 0, "Bus Number",        busNumberField);
        addField(formPanel, g, 2, 0, "Departure (HH:MM)", departureField);
        addField(formPanel, g, 0, 1, "Arrival (HH:MM)",   arrivalField);
        addField(formPanel, g, 1, 1, "Fare ($)",          fareField);
        addField(formPanel, g, 2, 1, "Available Seats",   seatsField);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton addBtn     = new JButton("Add Schedule");
        JButton updateBtn  = new JButton("Update");
        JButton deleteBtn  = new JButton("Delete");
        JButton clearBtn   = new JButton("Clear");
        JButton refreshBtn = new JButton("Refresh");

        ThemeUtil.styleButton(addBtn,     ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(updateBtn,  ThemeUtil.WARNING);
        ThemeUtil.styleButton(deleteBtn,  ThemeUtil.DANGER);
        ThemeUtil.styleGhostButton(clearBtn);
        ThemeUtil.styleButton(refreshBtn, ThemeUtil.PRIMARY);

        addBtn.addActionListener(e -> addSchedule());
        updateBtn.addActionListener(e -> updateSchedule());
        deleteBtn.addActionListener(e -> deleteSchedule());
        clearBtn.addActionListener(e -> clearFields());
        refreshBtn.addActionListener(e -> loadSchedules());

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(refreshBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        // Table
        String[] columns = {"ID", "Route ID", "Route Name", "Bus Number", "Departure", "Arrival", "Fare", "Seats"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        ThemeUtil.styleTable(table);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { fillFormFromTable(); }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(ThemeUtil.BORDER, 1, true));
        sp.getViewport().setBackground(ThemeUtil.SURFACE);

        statusLabel = new JLabel("Ready. Click \"Refresh\" to load schedules.");
        statusLabel.setFont(ThemeUtil.SMALL);
        statusLabel.setForeground(ThemeUtil.TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 4, 0, 0));

        content.add(top, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        loadSchedules();
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

    private void loadRoutes() {
        routeCombo.removeAllItems();
        String query = "SELECT id, route_name FROM routes ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                routeCombo.addItem(rs.getInt("id") + " - " + rs.getString("route_name"));
            }
            if (routeCombo.getItemCount() == 0) {
                routeCombo.addItem("1 - Route A");
                routeCombo.addItem("2 - Route B");
                routeCombo.addItem("3 - Route C");
            }
        } catch (Exception e) {
            System.out.println("Error loading routes: " + e.getMessage());
            routeCombo.addItem("1 - Route A");
            routeCombo.addItem("2 - Route B");
            routeCombo.addItem("3 - Route C");
        }
    }

    private void loadSchedules() {
        tableModel.setRowCount(0);
        String query = "SELECT bs.id, bs.route_id, r.route_name, bs.bus_number, " +
                      "bs.departure_time, bs.arrival_time, bs.fare, bs.available_seats " +
                      "FROM bus_schedule bs " +
                      "LEFT JOIN routes r ON bs.route_id = r.id " +
                      "ORDER BY bs.departure_time";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("route_id"),
                    rs.getString("route_name"),
                    rs.getString("bus_number"),
                    rs.getString("departure_time").substring(0, 5),
                    rs.getString("arrival_time").substring(0, 5),
                    rs.getDouble("fare"),
                    rs.getInt("available_seats")
                });
                count++;
            }

            ThemeUtil.styleTable(table);
            if (statusLabel != null) statusLabel.setText(count + " schedules loaded.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading schedules:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int getSelectedRouteId() {
        String selected = (String) routeCombo.getSelectedItem();
        if (selected != null && selected.contains("-")) {
            return Integer.parseInt(selected.split("-")[0].trim());
        }
        return 1;
    }

    private void addSchedule() {
        if (busNumberField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bus number is required!");
            return;
        }

        String query = "INSERT INTO bus_schedule (route_id, bus_number, departure_time, arrival_time, fare, available_seats) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setInt(1, getSelectedRouteId());
            pst.setString(2, busNumberField.getText().trim());
            pst.setString(3, departureField.getText().trim() + ":00");
            pst.setString(4, arrivalField.getText().trim() + ":00");
            pst.setDouble(5, Double.parseDouble(fareField.getText().trim()));
            pst.setInt(6, Integer.parseInt(seatsField.getText().trim()));

            int result = pst.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Schedule added successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadSchedules();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "SQL Error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for fare and seats!",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateSchedule() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a schedule to update!"); return; }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String query = "UPDATE bus_schedule SET route_id=?, bus_number=?, departure_time=?, " +
                      "arrival_time=?, fare=?, available_seats=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setInt(1, getSelectedRouteId());
            pst.setString(2, busNumberField.getText().trim());
            pst.setString(3, departureField.getText().trim() + ":00");
            pst.setString(4, arrivalField.getText().trim() + ":00");
            pst.setDouble(5, Double.parseDouble(fareField.getText().trim()));
            pst.setInt(6, Integer.parseInt(seatsField.getText().trim()));
            pst.setInt(7, id);

            int result = pst.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Schedule updated successfully!");
                clearFields();
                loadSchedules();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSchedule() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a schedule to delete!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this schedule?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String query = "DELETE FROM bus_schedule WHERE id=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, id);
                int result = pst.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Schedule deleted successfully!");
                    clearFields();
                    loadSchedules();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void fillFormFromTable() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int routeId = (int) tableModel.getValueAt(selectedRow, 1);
            for (int i = 0; i < routeCombo.getItemCount(); i++) {
                if (routeCombo.getItemAt(i).startsWith(routeId + " ")) {
                    routeCombo.setSelectedIndex(i);
                    break;
                }
            }
            busNumberField.setText(tableModel.getValueAt(selectedRow, 3).toString());
            departureField.setText(tableModel.getValueAt(selectedRow, 4).toString());
            arrivalField.setText(tableModel.getValueAt(selectedRow, 5).toString());
            fareField.setText(tableModel.getValueAt(selectedRow, 6).toString());
            seatsField.setText(tableModel.getValueAt(selectedRow, 7).toString());
        }
    }

    private void clearFields() {
        if (routeCombo.getItemCount() > 0) routeCombo.setSelectedIndex(0);
        busNumberField.setText("BUS-101");
        departureField.setText("08:00");
        arrivalField.setText("09:00");
        fareField.setText("5.00");
        seatsField.setText("50");
        table.clearSelection();
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new BusSchedule().setVisible(true));
    }
}
