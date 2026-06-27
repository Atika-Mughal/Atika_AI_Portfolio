// ==================== TrafficLightManagement.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class TrafficLightManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtLocation;
    private JComboBox<String> cmbStatus;

    public TrafficLightManagement() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Traffic Light Management");
        setSize(900, 620);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Traffic Light Management",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ThemeUtil.SURFACE);
        formPanel.setBorder(ThemeUtil.sectionBorder("Traffic Light Details"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        txtLocation = new JTextField();
        cmbStatus   = new JComboBox<>(new String[]{"Working", "Faulty", "Under Maintenance"});
        ThemeUtil.styleField(txtLocation);
        ThemeUtil.styleField(cmbStatus);

        addField(formPanel, g, 0, 0, "Location", txtLocation);
        addField(formPanel, g, 1, 0, "Status",   cmbStatus);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton btnAdd    = new JButton("Add Traffic Light");
        JButton btnUpdate = new JButton("Update Status");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear  = new JButton("Clear");

        ThemeUtil.styleButton(btnAdd,    ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(btnUpdate, ThemeUtil.WARNING);
        ThemeUtil.styleButton(btnDelete, ThemeUtil.DANGER);
        ThemeUtil.styleGhostButton(btnClear);

        btnAdd.addActionListener(e -> addTrafficLight());
        btnUpdate.addActionListener(e -> updateTrafficLight());
        btnDelete.addActionListener(e -> deleteTrafficLight());
        btnClear.addActionListener(e -> clearFields());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        String[] columns = {"ID", "Location", "Status", "Last Maintenance"};
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
        String query = "SELECT * FROM traffic_lights";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("location"),
                    rs.getString("status"),
                    rs.getDate("last_maintenance")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        txtLocation.setText(model.getValueAt(row, 1).toString());
        cmbStatus.setSelectedItem(model.getValueAt(row, 2).toString());
    }

    private void addTrafficLight() {
        String query = "INSERT INTO traffic_lights (location, status, last_maintenance) VALUES (?, ?, CURDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, txtLocation.getText());
            pst.setString(2, cmbStatus.getSelectedItem().toString());
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Traffic light added successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateTrafficLight() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a traffic light!"); return; }

        int id = (int) model.getValueAt(row, 0);
        String query = "UPDATE traffic_lights SET location=?, status=?, last_maintenance=CURDATE() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, txtLocation.getText());
            pst.setString(2, cmbStatus.getSelectedItem().toString());
            pst.setInt(3, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Traffic light updated successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteTrafficLight() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a traffic light!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this traffic light?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) model.getValueAt(row, 0);
            String query = "DELETE FROM traffic_lights WHERE id=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, id);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Traffic light deleted!");
                loadData();
                clearFields();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        txtLocation.setText("");
        cmbStatus.setSelectedIndex(0);
        table.clearSelection();
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new TrafficLightManagement().setVisible(true));
    }
}
