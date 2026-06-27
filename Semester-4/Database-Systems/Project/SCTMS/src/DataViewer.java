// ==================== DataViewer.java ====================
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DataViewer extends JFrame {
    private JComboBox<String> tableSelector;
    private JTable table;
    private DefaultTableModel tableModel;

    public DataViewer() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Data Viewer");
        setSize(1040, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(ThemeUtil.gradientHeader("Data Viewer",
                ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, null), BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        toolbar.setBackground(ThemeUtil.SURFACE);
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(ThemeUtil.BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel label = new JLabel("Select Table");
        label.setFont(ThemeUtil.BODY_B);

        tableSelector = new JComboBox<>();
        ThemeUtil.styleField(tableSelector);
        tableSelector.setPreferredSize(new Dimension(220, 36));

        JButton loadBtn = new JButton("Load Data");
        ThemeUtil.styleButton(loadBtn, ThemeUtil.PRIMARY);
        loadBtn.addActionListener(e -> loadTableData());

        toolbar.add(label);
        toolbar.add(tableSelector);
        toolbar.add(loadBtn);

        // Table
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        ThemeUtil.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(ThemeUtil.BORDER, 1, true));
        sp.getViewport().setBackground(ThemeUtil.SURFACE);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        loadTableNames();
    }

    private void loadTableNames() {
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {

            tableSelector.removeAllItems();
            while (rs.next()) {
                tableSelector.addItem(rs.getString("TABLE_NAME"));
            }

            if (tableSelector.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this, "No tables found in database!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading table names: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTableData() {
        String selectedTable = (String) tableSelector.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Please select a table first!",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + selectedTable)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);

            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnName(i));
            }

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) row.add(rs.getObject(i));
                tableModel.addRow(row);
            }

            ThemeUtil.styleTable(table); // re-apply renderer for new columns

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new DataViewer().setVisible(true));
    }
}
