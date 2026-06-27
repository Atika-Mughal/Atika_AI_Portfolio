// ==================== UserManagement.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class UserManagement extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField usernameField, passwordField, fullNameField, emailField;
    private JComboBox<String> userTypeCombo;

    public UserManagement() {
        ThemeUtil.applyGlobalTheme();

        setTitle("User Management");
        setSize(1040, 660);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("User Management",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ThemeUtil.SURFACE);
        formPanel.setBorder(ThemeUtil.sectionBorder("User Details"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        usernameField  = new JTextField();
        passwordField  = new JTextField();
        fullNameField  = new JTextField();
        emailField     = new JTextField();
        userTypeCombo  = new JComboBox<>(new String[]{"Admin", "Driver", "Passenger"});
        for (JComponent c : new JComponent[]{usernameField, passwordField, fullNameField, emailField, userTypeCombo}) {
            ThemeUtil.styleField(c);
        }

        addField(formPanel, g, 0, 0, "Username",  usernameField);
        addField(formPanel, g, 1, 0, "Password",  passwordField);
        addField(formPanel, g, 2, 0, "User Type", userTypeCombo);
        addField(formPanel, g, 0, 1, "Full Name", fullNameField);
        addField(formPanel, g, 1, 1, "Email",     emailField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton addBtn    = new JButton("Add User");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton clearBtn  = new JButton("Clear");

        ThemeUtil.styleButton(addBtn,    ThemeUtil.SUCCESS);
        ThemeUtil.styleButton(updateBtn, ThemeUtil.WARNING);
        ThemeUtil.styleButton(deleteBtn, ThemeUtil.DANGER);
        ThemeUtil.styleGhostButton(clearBtn);

        addBtn.addActionListener(e -> addUser());
        updateBtn.addActionListener(e -> updateUser());
        deleteBtn.addActionListener(e -> deleteUser());
        clearBtn.addActionListener(e -> clearFields());

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        String[] columns = {"ID", "Username", "Password", "User Type", "Full Name", "Email"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        ThemeUtil.styleTable(table);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { fillFieldsFromTable(); }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(ThemeUtil.BORDER, 1, true));
        sp.getViewport().setBackground(ThemeUtil.SURFACE);

        content.add(top, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        loadUsers();
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

    private void loadUsers() {
        tableModel.setRowCount(0);
        String query = "SELECT id, username, password, user_type, full_name, email FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("user_type"),
                        rs.getString("full_name"),
                        rs.getString("email")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void addUser() {
        if (usernameField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required!");
            return;
        }

        String query = "INSERT INTO users (username, password, user_type, full_name, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, usernameField.getText().trim());
            pst.setString(2, passwordField.getText().trim());
            pst.setString(3, (String) userTypeCombo.getSelectedItem());
            pst.setString(4, fullNameField.getText().trim());
            pst.setString(5, emailField.getText().trim());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User added successfully!");
            loadUsers();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a user to update."); return; }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String query = "UPDATE users SET username=?, password=?, user_type=?, full_name=?, email=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, usernameField.getText().trim());
            pst.setString(2, passwordField.getText().trim());
            pst.setString(3, (String) userTypeCombo.getSelectedItem());
            pst.setString(4, fullNameField.getText().trim());
            pst.setString(5, emailField.getText().trim());
            pst.setInt(6, id);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User updated successfully!");
            loadUsers();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a user to delete."); return; }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String query = "DELETE FROM users WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully!");
            loadUsers();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        fullNameField.setText("");
        emailField.setText("");
        userTypeCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private void fillFieldsFromTable() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;

        usernameField.setText(tableModel.getValueAt(selectedRow, 1).toString());
        passwordField.setText(tableModel.getValueAt(selectedRow, 2).toString());
        userTypeCombo.setSelectedItem(tableModel.getValueAt(selectedRow, 3).toString());
        fullNameField.setText(tableModel.getValueAt(selectedRow, 4).toString());
        emailField.setText(tableModel.getValueAt(selectedRow, 5).toString());
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new UserManagement().setVisible(true));
    }
}
