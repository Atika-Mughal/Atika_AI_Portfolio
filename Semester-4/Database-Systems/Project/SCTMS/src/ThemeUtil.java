import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Central design system for SCTMS.
 *
 *  Palette is intentionally restrained: one primary blue plus accent colors
 *  for success / warning / danger.  Components share consistent radii,
 *  spacing, typography and hover behaviour so screens feel like one app.
 */
public class ThemeUtil {

    // ── Palette ────────────────────────────────────────────────────────────
    public static final Color PRIMARY        = new Color(59, 130, 246);   // indigo-blue
    public static final Color PRIMARY_DARK   = new Color(37, 99, 200);
    public static final Color SUCCESS        = new Color(34, 167, 94);
    public static final Color WARNING        = new Color(234, 159, 26);
    public static final Color DANGER         = new Color(220, 70, 70);
    public static final Color NEUTRAL        = new Color(110, 119, 129);

    public static final Color BG             = new Color(245, 247, 250);
    public static final Color SURFACE        = Color.WHITE;
    public static final Color SURFACE_ALT    = new Color(249, 250, 252);
    public static final Color BORDER         = new Color(225, 229, 235);
    public static final Color BORDER_STRONG  = new Color(205, 211, 220);

    public static final Color TEXT           = new Color(28, 33, 41);
    public static final Color TEXT_MUTED     = new Color(110, 119, 129);

    // ── Typography ────────────────────────────────────────────────────────
    public static final String FAMILY = "Segoe UI";
    public static final Font H1     = new Font(FAMILY, Font.BOLD, 24);
    public static final Font H2     = new Font(FAMILY, Font.BOLD, 18);
    public static final Font H3     = new Font(FAMILY, Font.BOLD, 15);
    public static final Font BODY   = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font BODY_B = new Font(FAMILY, Font.BOLD, 13);
    public static final Font SMALL  = new Font(FAMILY, Font.PLAIN, 12);

    // ── Global L&F tweaks ─────────────────────────────────────────────────
    public static void applyGlobalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BODY_B);
        UIManager.put("Label.font", BODY);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("ComboBox.font", BODY);
        UIManager.put("Table.font", BODY);
        UIManager.put("TableHeader.font", BODY_B);
        UIManager.put("Button.font", BODY_B);
    }

    // ── Buttons ───────────────────────────────────────────────────────────
    public static void styleButton(JButton btn, Color color) {
        styleButton(btn, color, Color.WHITE);
    }

    public static void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(BODY_B);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));

        final Color base   = bg;
        final Color hover  = mix(bg, Color.BLACK, 0.08f);
        final Color press  = mix(bg, Color.BLACK, 0.15f);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(base);  }
            @Override public void mousePressed(MouseEvent e) { btn.setBackground(press); }
            @Override public void mouseReleased(MouseEvent e){ btn.setBackground(hover); }
        });
    }

    /** Subtle outlined button (used for Clear / secondary actions). */
    public static void styleGhostButton(JButton btn) {
        btn.setBackground(SURFACE);
        btn.setForeground(TEXT);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setFont(BODY_B);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(BORDER_STRONG, 1, true),
                new EmptyBorder(7, 17, 7, 17)));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(SURFACE_ALT); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(SURFACE);     }
        });
    }

    // ── Inputs ────────────────────────────────────────────────────────────
    public static void styleField(JComponent field) {
        field.setFont(BODY);
        field.setBackground(SURFACE);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)));
    }

    // ── Tables ────────────────────────────────────────────────────────────
    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(231, 240, 255));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(SURFACE_ALT);
        header.setForeground(TEXT);
        header.setFont(BODY_B);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_STRONG));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));

        // Zebra striping + cell padding
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                    c.setForeground(TEXT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    // ── Containers ────────────────────────────────────────────────────────
    /** White card with subtle border and rounded look. */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)));
        return p;
    }

    /** A titled section that looks lighter than the default Swing TitledBorder. */
    public static Border sectionBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                new LineBorder(BORDER, 1, true),
                "  " + title + "  ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font(FAMILY, Font.BOLD, 13), TEXT);
        return new CompoundBorder(tb, new EmptyBorder(12, 14, 14, 14));
    }

    /** Header bar with horizontal gradient + title and optional right-side panel. */
    public static JPanel gradientHeader(String title, Color a, Color b, Component east) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, a, getWidth(), getHeight(), b));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(10, 78));
        header.setBorder(new EmptyBorder(14, 26, 14, 26));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(FAMILY, Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        if (east != null) header.add(east, BorderLayout.EAST);
        return header;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static Color mix(Color a, Color b, float t) {
        float it = 1f - t;
        return new Color(
                Math.round(a.getRed()   * it + b.getRed()   * t),
                Math.round(a.getGreen() * it + b.getGreen() * t),
                Math.round(a.getBlue()  * it + b.getBlue()  * t));
    }
}
