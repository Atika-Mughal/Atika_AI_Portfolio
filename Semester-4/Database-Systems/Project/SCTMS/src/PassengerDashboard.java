// ==================== PassengerDashboard.java ====================
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class PassengerDashboard extends JFrame {

    private static final Color ACCENT      = new Color(245, 140, 50);
    private static final Color ACCENT_DARK = new Color(210, 95, 20);

    private final String username;

    public PassengerDashboard(String username) {
        this.username = username;
        ThemeUtil.applyGlobalTheme();

        setTitle("Passenger Dashboard");
        setSize(960, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeUtil.BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMenu(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JLabel user = new JLabel("Welcome, " + username);
        user.setFont(ThemeUtil.BODY);
        user.setForeground(new Color(255, 245, 235));
        right.add(user);

        JButton logout = new JButton("Logout");
        ThemeUtil.styleButton(logout, new Color(255, 255, 255, 38), Color.WHITE);
        logout.setBorder(new CompoundBorder(
                new LineBorder(new Color(255, 255, 255, 120), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        right.add(logout);

        return ThemeUtil.gradientHeader("Passenger Dashboard", ACCENT, ACCENT_DARK, right);
    }

    private JPanel buildMenu() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ThemeUtil.BG);
        wrap.setBorder(new EmptyBorder(28, 32, 32, 32));

        JPanel grid = new JPanel(new GridLayout(2, 3, 18, 18));
        grid.setOpaque(false);

        grid.add(card("\uD83D\uDE8C", "Bus Schedule",  "View bus timings"));
        grid.add(card("\uD83D\uDCCD", "Nearby Stops",  "Find bus stops"));
        grid.add(card("\uD83C\uDFAB", "Book Ticket",   "Book your ride"));
        grid.add(card("\uD83D\uDDFA", "Plan Trip",     "Plan your journey"));
        grid.add(card("\uD83C\uDF0D", "Live Map",      "Online map & navigation"));
        grid.add(card("\uD83D\uDEA6", "Traffic",       "Live traffic lights"));

        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    private void openModule(String moduleName) {
        switch (moduleName) {
            case "Bus Schedule":
                JOptionPane.showMessageDialog(this, "Opening Bus Schedule module...");
                new BusSchedule().setVisible(true);
                break;
            case "Nearby Stops":
                JOptionPane.showMessageDialog(this, "Opening Nearby Stops module...");
                new NearbyStops().setVisible(true);
                break;
            case "Book Ticket":
                JOptionPane.showMessageDialog(this, "Opening Ticket Booking module...");
                break;
            case "Plan Trip":
            case "Live Map":
                new LiveMap().setVisible(true);
                break;
            case "Traffic":
                new TrafficLightManagement().setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Module not available.");
        }
    }

    private JPanel card(String icon, String title, String desc) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(ThemeUtil.SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(ThemeUtil.BORDER, 1, true),
                new EmptyBorder(22, 18, 22, 18)));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setPreferredSize(new Dimension(0, 170));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        p.add(iconLbl, BorderLayout.NORTH);

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font(ThemeUtil.FAMILY, Font.BOLD, 16));
        titleLbl.setForeground(ThemeUtil.TEXT);
        p.add(titleLbl, BorderLayout.CENTER);

        JLabel descLbl = new JLabel(desc, SwingConstants.CENTER);
        descLbl.setFont(ThemeUtil.SMALL);
        descLbl.setForeground(ThemeUtil.TEXT_MUTED);
        p.add(descLbl, BorderLayout.SOUTH);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openModule(title); }
            @Override public void mouseEntered(MouseEvent e) {
                p.setBorder(new CompoundBorder(
                        new LineBorder(ACCENT, 1, true),
                        new EmptyBorder(22, 18, 22, 18)));
                p.setBackground(new Color(255, 250, 244));
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

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new PassengerDashboard("Ayesha").setVisible(true));
    }
}
