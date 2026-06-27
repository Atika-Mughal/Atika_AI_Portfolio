// ==================== RouteManagement.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class RouteManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtRouteName, txtStartPoint, txtEndPoint, txtDistance, txtTime;
    private JComboBox<String> cmbStatus;

    public RouteManagement() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Route Management");
        setSize(960, 660);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Route Management",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ThemeUtil.SURFACE);
        formPanel.setBorder(ThemeUtil.sectionBorder("Route Details"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        txtRouteName  = new JTextField();
        txtStartPoint = new JTextField();
        txtEndPoint   = new JTextField();
        txtDistance   = new JTextField();
        txtTime       = new JTextField();
        cmbStatus     = new JComboBox<>(new String[]{"Active", "Closed", "Under Maintenance"});
        for (JComponent c : new JComponent[]{txtRouteName, txtStartPoint, txtEndPoint, txtDistance, txtTime, cmbStatus}) {
            ThemeUtil.styleField(c);
        }

        addField(formPanel, g, 0, 0, "Route Name", txtRouteName);
        addField(formPanel, g, 1, 0, "Status",     cmbStatus);
        addField(formPanel, g, 0, 1, "Start Point", txtStartPoint);
        addField(formPanel, g, 1, 1, "End Point",   txtEndPoint);
        addField(formPanel, g, 0, 2, "Distance (km)", txtDistance);
        addField(formPanel, g, 1, 2, "Estimated Time (min)", txtTime);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton btnAdd    = new JButton("Add Route");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear  = new JButton("Clear");

        ThemeUtil.styleButton(btnAdd,    ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(btnUpdate, ThemeUtil.WARNING);
        ThemeUtil.styleButton(btnDelete, ThemeUtil.DANGER);
        ThemeUtil.styleGhostButton(btnClear);

        btnAdd.addActionListener(e -> addRoute());
        btnUpdate.addActionListener(e -> updateRoute());
        btnDelete.addActionListener(e -> deleteRoute());
        btnClear.addActionListener(e -> clearFields());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        String[] columns = {"ID", "Route Name", "Start", "End", "Distance", "Time", "Status"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        ThemeUtil.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) loadSelectedRow();
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(ThemeUtil.BORDER, 1, true));
        sp.getViewport().setBackground(ThemeUtil.SURFACE);

        content.add(top, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        loadData();
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

    private void loadData() {
        model.setRowCount(0);
        String query = "SELECT * FROM routes";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("route_name"),
                    rs.getString("start_point"),
                    rs.getString("end_point"),
                    rs.getDouble("distance_km"),
                    rs.getInt("estimated_time"),
                    rs.getString("status")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        txtRouteName.setText(model.getValueAt(row, 1).toString());
        txtStartPoint.setText(model.getValueAt(row, 2).toString());
        txtEndPoint.setText(model.getValueAt(row, 3).toString());
        txtDistance.setText(model.getValueAt(row, 4).toString());
        txtTime.setText(model.getValueAt(row, 5).toString());
        cmbStatus.setSelectedItem(model.getValueAt(row, 6).toString());
    }

    private void addRoute() {
        String query = "INSERT INTO routes (route_name, start_point, end_point, distance_km, estimated_time, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, txtRouteName.getText());
            pst.setString(2, txtStartPoint.getText());
            pst.setString(3, txtEndPoint.getText());
            pst.setDouble(4, Double.parseDouble(txtDistance.getText()));
            pst.setInt(5, Integer.parseInt(txtTime.getText()));
            pst.setString(6, cmbStatus.getSelectedItem().toString());
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Route added successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateRoute() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a route!"); return; }

        int id = (int) model.getValueAt(row, 0);
        String query = "UPDATE routes SET route_name=?, start_point=?, end_point=?, distance_km=?, estimated_time=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, txtRouteName.getText());
            pst.setString(2, txtStartPoint.getText());
            pst.setString(3, txtEndPoint.getText());
            pst.setDouble(4, Double.parseDouble(txtDistance.getText()));
            pst.setInt(5, Integer.parseInt(txtTime.getText()));
            pst.setString(6, cmbStatus.getSelectedItem().toString());
            pst.setInt(7, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Route updated successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteRoute() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a route!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this route?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) model.getValueAt(row, 0);
            String query = "DELETE FROM routes WHERE id=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, id);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Route deleted!");
                loadData();
                clearFields();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        txtRouteName.setText("");
        txtStartPoint.setText("");
        txtEndPoint.setText("");
        txtDistance.setText("");
        txtTime.setText("");
        cmbStatus.setSelectedIndex(0);
        table.clearSelection();
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new RouteManagement().setVisible(true));
    }
}
