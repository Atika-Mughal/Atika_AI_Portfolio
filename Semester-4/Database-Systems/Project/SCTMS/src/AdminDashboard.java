// ==================== AdminDashboard.java ====================
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class AdminDashboard extends JFrame {

    public AdminDashboard(String username) {
        ThemeUtil.applyGlobalTheme();

        setTitle("Admin Dashboard");
        setSize(960, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(buildHeader(username), BorderLayout.NORTH);
        add(buildMenu(), BorderLayout.CENTER);
    }

    private JPanel buildHeader(String username) {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JLabel user = new JLabel("Welcome, " + username);
        user.setFont(ThemeUtil.BODY);
        user.setForeground(new Color(235, 240, 255));
        right.add(user);

        JButton logout = new JButton("Logout");
        ThemeUtil.styleButton(logout, new Color(255, 255, 255, 38), Color.WHITE);
        logout.setBorder(new CompoundBorder(
                new LineBorder(new Color(255, 255, 255, 120), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        right.add(logout);

        return ThemeUtil.gradientHeader("Admin Dashboard", ThemeUtil.PRIMARY, ThemeUtil.PRIMARY_DARK, right);
    }

    private JPanel buildMenu() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ThemeUtil.BG);
        wrap.setBorder(new EmptyBorder(28, 32, 32, 32));

        JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
        grid.setOpaque(false);

        grid.add(card("\uD83D\uDE97", "Vehicle Management", "Manage all vehicles", "vehicles"));
        grid.add(card("\uD83D\uDDFA", "Route Management",   "Manage traffic routes", "routes"));
        grid.add(card("\uD83D\uDEA6", "Traffic Control",    "Control traffic lights", "traffic"));
        grid.add(card("\uD83D\uDC65", "User Management",    "Manage system users", "users"));
        grid.add(card("\uD83D\uDCCA", "View Reports",       "View all data", "reports"));
        grid.add(card("\u2699",       "Settings",           "System settings", "settings"));
        grid.add(card("\uD83C\uDF0D", "Live Map",           "Online navigation", "livemap"));

        wrap.add(grid, BorderLayout.NORTH);
        return wrap;
    }

    private JPanel card(String icon, String title, String desc, String action) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(ThemeUtil.SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(ThemeUtil.BORDER, 1, true),
                new EmptyBorder(22, 18, 22, 18)));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setPreferredSize(new Dimension(0, 170));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        p.add(iconLbl, BorderLayout.NORTH);

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font(ThemeUtil.FAMILY, Font.BOLD, 15));
        titleLbl.setForeground(ThemeUtil.TEXT);
        p.add(titleLbl, BorderLayout.CENTER);

        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER);
        descLbl.setFont(ThemeUtil.SMALL);
        descLbl.setForeground(ThemeUtil.TEXT_MUTED);
        p.add(descLbl, BorderLayout.SOUTH);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openModule(action); }
            @Override public void mouseEntered(MouseEvent e) {
                p.setBorder(new CompoundBorder(
                        new LineBorder(ThemeUtil.PRIMARY, 1, true),
                        new EmptyBorder(22, 18, 22, 18)));
                p.setBackground(new Color(248, 250, 255));
            }
            @Override public void mouseExited(MouseEvent e) {
                p.setBorder(new CompoundBorder(
                        new LineBorder(ThemeUtil.BORDER, 1, true),
                        new EmptyBorder(22, 18, 22, 18)));
                p.setBackground(ThemeUtil.SURFACE);
            }
        });
        return p;
    }

    private void openModule(String action) {
        switch (action) {
            case "vehicles": new VehicleManagement().setVisible(true); break;
            case "routes":   new RouteManagement().setVisible(true);   break;
            case "traffic":  new TrafficLightManagement().setVisible(true); break;
            case "users":    new UserManagement().setVisible(true);    break;
            case "reports":  new DataViewer().setVisible(true);        break;
            case "settings": JOptionPane.showMessageDialog(this, "Settings module coming soon!"); break;
            case "livemap":  new LiveMap().setVisible(true);           break;
        }
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new AdminDashboard("Ayesha").setVisible(true));
    }
}
