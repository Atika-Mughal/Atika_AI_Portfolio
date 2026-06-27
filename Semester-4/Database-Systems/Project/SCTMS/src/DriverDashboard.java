// ==================== DriverDashboard.java ====================
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class DriverDashboard extends JFrame {

    private static final Color ACCENT      = new Color(34, 167, 94);
    private static final Color ACCENT_DARK = new Color(20, 120, 70);

    public DriverDashboard(String username) {
        ThemeUtil.applyGlobalTheme();

        setTitle("Driver Dashboard");
        setSize(900, 680);
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
        user.setForeground(new Color(235, 245, 235));
        right.add(user);

        JButton logout = new JButton("Logout");
        ThemeUtil.styleButton(logout, new Color(255, 255, 255, 38), Color.WHITE);
        logout.setBorder(new CompoundBorder(
                new LineBorder(new Color(255, 255, 255, 120), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        right.add(logout);

        return ThemeUtil.gradientHeader("Driver Dashboard", ACCENT, ACCENT_DARK, right);
    }

    private JPanel buildMenu() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ThemeUtil.BG);
        wrap.setBorder(new EmptyBorder(28, 32, 32, 32));

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);

        grid.add(card("\uD83D\uDDFA", "My Routes",       "View your routes",       "routes"));
        grid.add(card("\uD83D\uDEA6", "Traffic Updates", "Real-time traffic info", "traffic"));
        grid.add(card("\uD83D\uDCCD", "Navigation",      "Get directions",         "navigation"));
        grid.add(card("\u26A0",       "Report Issue",    "Report traffic issues",  "report"));

        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel card(String icon, String title, String desc, String action) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(ThemeUtil.SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(ThemeUtil.BORDER, 1, true),
                new EmptyBorder(28, 22, 28, 22)));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        p.add(iconLbl, BorderLayout.NORTH);

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font(ThemeUtil.FAMILY, Font.BOLD, 18));
        titleLbl.setForeground(ThemeUtil.TEXT);
        p.add(titleLbl, BorderLayout.CENTER);

        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER);
        descLbl.setFont(ThemeUtil.BODY);
        descLbl.setForeground(ThemeUtil.TEXT_MUTED);
        p.add(descLbl, BorderLayout.SOUTH);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openModule(action); }
            @Override public void mouseEntered(MouseEvent e) {
                p.setBorder(new CompoundBorder(
                        new LineBorder(ACCENT, 1, true),
                        new EmptyBorder(28, 22, 28, 22)));
                p.setBackground(new Color(244, 252, 247));
            }
            @Override public void mouseExited(MouseEvent e) {
                p.setBorder(new CompoundBorder(
                        new LineBorder(ThemeUtil.BORDER, 1, true),
                        new EmptyBorder(28, 22, 28, 22)));
                p.setBackground(ThemeUtil.SURFACE);
            }
        });
        return p;
    }

    private void openModule(String action) {
        switch (action) {
            case "routes":     new RouteManagement().setVisible(true); break;
            case "traffic":    new TrafficLightManagement().setVisible(true); break;
            case "navigation": new LiveMap().setVisible(true); break;
            case "report":     JOptionPane.showMessageDialog(this, "Opening module coming soon..."); break;
        }
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new DriverDashboard("Ayesha").setVisible(true));
    }
}
