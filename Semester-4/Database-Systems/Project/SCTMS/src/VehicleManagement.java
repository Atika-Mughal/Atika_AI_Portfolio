// ==================== VehicleManagement.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class VehicleManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtVehicleNumber, txtVehicleType, txtOwnerName;
    private JComboBox<String> cmbStatus;

    public VehicleManagement() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Vehicle Management");
        setSize(960, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Vehicle Management",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ThemeUtil.SURFACE);
        formPanel.setBorder(ThemeUtil.sectionBorder("Vehicle Details"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        txtVehicleNumber = new JTextField();
        txtVehicleType   = new JTextField();
        txtOwnerName     = new JTextField();
        cmbStatus        = new JComboBox<>(new String[]{"Active", "Inactive"});
        ThemeUtil.styleField(txtVehicleNumber);
        ThemeUtil.styleField(txtVehicleType);
        ThemeUtil.styleField(txtOwnerName);
        ThemeUtil.styleField(cmbStatus);

        addField(formPanel, g, 0, 0, "Vehicle Number", txtVehicleNumber);
        addField(formPanel, g, 1, 0, "Vehicle Type",   txtVehicleType);
        addField(formPanel, g, 0, 1, "Owner Name",     txtOwnerName);
        addField(formPanel, g, 1, 1, "Status",         cmbStatus);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton btnAdd    = new JButton("Add Vehicle");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear  = new JButton("Clear");

        ThemeUtil.styleButton(btnAdd,    ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(btnUpdate, ThemeUtil.WARNING);
        ThemeUtil.styleButton(btnDelete, ThemeUtil.DANGER);
        ThemeUtil.styleGhostButton(btnClear);

        btnAdd.addActionListener(e -> addVehicle());
        btnUpdate.addActionListener(e -> updateVehicle());
        btnDelete.addActionListener(e -> deleteVehicle());
        btnClear.addActionListener(e -> clearFields());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        // Table
        String[] columns = {"ID", "Vehicle Number", "Type", "Owner", "Status"};
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
        String query = "SELECT * FROM vehicles";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("vehicle_number"),
                    rs.getString("vehicle_type"),
                    rs.getString("owner_name"),
                    rs.getString("status")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        txtVehicleNumber.setText(model.getValueAt(row, 1).toString());
        txtVehicleType.setText(model.getValueAt(row, 2).toString());
        txtOwnerName.setText(model.getValueAt(row, 3).toString());
        cmbStatus.setSelectedItem(model.getValueAt(row, 4).toString());
    }

    private void addVehicle() {
        String query = "INSERT INTO vehicles (vehicle_number, vehicle_type, owner_name, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, txtVehicleNumber.getText());
            pst.setString(2, txtVehicleType.getText());
            pst.setString(3, txtOwnerName.getText());
            pst.setString(4, cmbStatus.getSelectedItem().toString());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Vehicle added successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateVehicle() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a vehicle!"); return; }

        int id = (int) model.getValueAt(row, 0);
        String query = "UPDATE vehicles SET vehicle_number=?, vehicle_type=?, owner_name=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, txtVehicleNumber.getText());
            pst.setString(2, txtVehicleType.getText());
            pst.setString(3, txtOwnerName.getText());
            pst.setString(4, cmbStatus.getSelectedItem().toString());
            pst.setInt(5, id);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Vehicle updated successfully!");
            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteVehicle() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a vehicle!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this vehicle?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) model.getValueAt(row, 0);
            String query = "DELETE FROM vehicles WHERE id=?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, id);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Vehicle deleted!");
                loadData();
                clearFields();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        txtVehicleNumber.setText("");
        txtVehicleType.setText("");
        txtOwnerName.setText("");
        cmbStatus.setSelectedIndex(0);
        table.clearSelection();
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new VehicleManagement().setVisible(true));
    }
}
