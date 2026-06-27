// ==================== LoginFrame.java ====================
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> userTypeCombo;
    private JButton loginButton;

    public LoginFrame() {
        ThemeUtil.applyGlobalTheme();

        setTitle("Smart City Transport Management - Login");
        setSize(460, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(ThemeUtil.BG);
        setLayout(new GridBagLayout());

        // Card container
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeUtil.SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(ThemeUtil.BORDER, 1, true),
                new EmptyBorder(32, 36, 32, 36)));
        card.setPreferredSize(new Dimension(380, 540));

        // Logo
        try {
            ImageIcon logo = new ImageIcon(getClass().getResource("/icons/img.png"));
            Image scaled = logo.getImage().getScaledInstance(72, 72, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(logoLabel);
            card.add(Box.createVerticalStrut(14));
        } catch (Exception ignored) {}

        JLabel title = new JLabel("Smart City Transport");
        title.setFont(new Font(ThemeUtil.FAMILY, Font.BOLD, 22));
        title.setForeground(ThemeUtil.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);

        JLabel subtitle = new JLabel("Sign in to continue");
        subtitle.setFont(ThemeUtil.BODY);
        subtitle.setForeground(ThemeUtil.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 24, 0));
        card.add(subtitle);

        card.add(buildField("Login as", userTypeCombo = new JComboBox<>(new String[]{"Admin", "Driver", "Passenger"})));
        card.add(Box.createVerticalStrut(14));
        card.add(buildField("Username", usernameField = new JTextField()));
        card.add(Box.createVerticalStrut(14));
        card.add(buildField("Password", passwordField = new JPasswordField()));
        card.add(Box.createVerticalStrut(22));

        loginButton = new JButton("Sign in");
        ThemeUtil.styleButton(loginButton, ThemeUtil.PRIMARY);
        loginButton.setFont(new Font(ThemeUtil.FAMILY, Font.BOLD, 15));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.addActionListener(e -> handleLogin());
        card.add(loginButton);

        add(card);
    }

    private JPanel buildField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(ThemeUtil.BODY_B);
        l.setForeground(ThemeUtil.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(l);

        ThemeUtil.styleField(field);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(field);
        return p;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String userType = (String) userTypeCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (DatabaseConnection.validateLogin(username, password, userType)) {
            JOptionPane.showMessageDialog(this, "Login Successful!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();

            switch (userType) {
                case "Admin":
                    new AdminDashboard(username).setVisible(true);
                    break;
                case "Driver":
                    new DriverDashboard(username).setVisible(true);
                    break;
                case "Passenger":
                    new PassengerDashboard(username).setVisible(true);
                    break;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials!",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        ThemeUtil.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
