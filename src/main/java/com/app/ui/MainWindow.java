package com.app.ui;

import com.app.config.ConfigManager;
import com.app.config.ThemeManager;
import com.app.i18n.LanguageManager;
import com.app.logging.AppLogger;
import com.app.model.Task;
import com.app.service.TaskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.BasicStroke;
import java.util.List;

public class MainWindow extends JFrame {

    // ── Singleton dependencies ──────────────────────────────────────────────
    private final AppLogger       logger  = AppLogger.getInstance();
    private final ConfigManager   config  = ConfigManager.getInstance();
    private final ThemeManager    theme   = ThemeManager.getInstance();
    private final LanguageManager lang    = LanguageManager.getInstance();
    private final TaskService     service = TaskService.getInstance();

    // ── Layout constants ────────────────────────────────────────────────────
    private static final int  PADDING_H    = 24;
    private static final int  PADDING_V    = 18;
    private static final int  RADIUS       = 10;
    private static final int  ROW_HEIGHT   = 48;
    private static final int  INPUT_HEIGHT = 40;
    private static final int  BTN_HEIGHT   = 34;
    private static final int  GAP_SM       = 6;
    private static final int  GAP_MD       = 10;
    private static final int  GAP_LG       = 16;
    private static final Font FONT_TOOLTIP = new Font("Segoe UI", Font.PLAIN, 12);

    // ── UI components ───────────────────────────────────────────────────────
    private JPanel      rootPanel;
    private JPanel      headerPanel;
    private JLabel      headerTitle;
    private JTextField  inputField;
    private FlatButton  btnAdd;
    private FlatButton  btnDelete;
    private FlatButton  btnClearCompleted;
    private JPanel      filterPanel;
    private FlatButton  btnFilterAll;
    private FlatButton  btnFilterPending;
    private FlatButton  btnFilterCompleted;
    private JPanel      taskListPanel;
    private JScrollPane scrollPane;
    private JLabel      lblTotal;
    private JLabel      lblPending;
    private JLabel      lblCompleted;
    private FlatCombo comboTheme;
    private FlatCombo comboLang;
    private JLabel      lblTheme;
    private JLabel      lblLang;

    private String activeFilter = "all";

    // ── Constructor ─────────────────────────────────────────────────────────
    public MainWindow() {
        configureTooltips();
        initFrame();
        buildUI();
        applyTheme();
        refreshTaskList();
        setVisible(true);
        logger.info("Aplicación iniciada. Versión: " + config.getVersion());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Frame & global setup
    // ══════════════════════════════════════════════════════════════════════

    private void configureTooltips() {
        UIManager.put("ToolTip.background", new Color(30, 30, 30));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font",       FONT_TOOLTIP);
        UIManager.put("ToolTip.border",
                BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(60, 60, 60), 1, true),
                        new EmptyBorder(5, 10, 5, 10)
                ));
    }

    private void initFrame() {
        setTitle(lang.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(0, 600));
        setPreferredSize(new Dimension(680, 700));
        setLocationRelativeTo(null);
        pack();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UI assembly
    // ══════════════════════════════════════════════════════════════════════

    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout(0, 0));
        setContentPane(rootPanel);
        rootPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        rootPanel.add(buildCenterPanel(), BorderLayout.CENTER);
        rootPanel.add(buildStatsPanel(),  BorderLayout.SOUTH);
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private JPanel buildHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(PADDING_V, PADDING_H, PADDING_V, PADDING_H));

        headerTitle = new JLabel(safeGet("app.header.title", "Mis Tareas"));
        headerPanel.add(headerTitle, BorderLayout.WEST);

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP_MD, 0));
        configPanel.setOpaque(false);

        lblTheme = new JLabel(safeGet("label.theme", "Tema"));
        comboTheme = buildStyledCombo(new String[]{
                safeGet("option.theme.light", "Claro"),
                safeGet("option.theme.dark",  "Oscuro")
        });
        comboTheme.setSelectedIndex("dark".equals(config.getTheme()) ? 1 : 0);
        comboTheme.setToolTipText(lang.get("tooltip.theme"));
        comboTheme.addActionListener(e -> onThemeChanged());

        lblLang = new JLabel(safeGet("label.language", "Idioma"));
        comboLang = buildStyledCombo(new String[]{
                safeGet("option.lang.es", "Español"),
                safeGet("option.lang.en", "English")
        });
        comboLang.setSelectedIndex("en".equals(config.getLanguage()) ? 1 : 0);
        comboLang.setToolTipText(lang.get("tooltip.language"));
        comboLang.addActionListener(e -> onLanguageChanged());

        configPanel.add(lblTheme);
        configPanel.add(comboTheme);
        configPanel.add(Box.createHorizontalStrut(GAP_LG));
        configPanel.add(lblLang);
        configPanel.add(comboLang);

        headerPanel.add(configPanel, BorderLayout.EAST);
        return headerPanel;
    }

    // ── Center ──────────────────────────────────────────────────────────────

    private JPanel buildCenterPanel() {
        JPanel full = new JPanel(new BorderLayout(0, GAP_LG));
        full.setOpaque(false);
        full.setBorder(new EmptyBorder(GAP_LG, PADDING_H, GAP_MD, PADDING_H));
        full.add(buildInputPanel(),  BorderLayout.NORTH);
        full.add(buildBelowInput(),  BorderLayout.CENTER);
        return full;
    }

    private JPanel buildBelowInput() {
        JPanel panel = new JPanel(new BorderLayout(0, GAP_LG));
        panel.setOpaque(false);
        panel.add(buildFilterPanel(), BorderLayout.NORTH);
        panel.add(buildTaskArea(),    BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(GAP_MD, 0));
        panel.setOpaque(false);

        inputField = new RoundTextField(safeGet("input.placeholder", "Nueva tarea…"), RADIUS);
        inputField.setPreferredSize(new Dimension(0, INPUT_HEIGHT));
        inputField.addActionListener(e -> onAddTask());

        btnAdd = createStyledButton(safeGet("btn.add", "Agregar"), "primary");
        btnAdd.setPreferredSize(new Dimension(100, INPUT_HEIGHT));
        btnAdd.setToolTipText(lang.get("tooltip.btn.add"));
        btnAdd.addActionListener(e -> onAddTask());
        btnAdd.setFont(theme.getFontNormal());

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(btnAdd,     BorderLayout.EAST);
        return panel;
    }

    /**
     * Two-row filter panel to avoid FlowLayout overflow.
     *
     *  Row 1 — filter toggles : [Todas] [Pendientes] [Completadas]
     *  Row 2 — action buttons : [Eliminar] [Limpiar completadas]
     *
     * Stacking them vertically guarantees every button is always visible
     * regardless of window width.
     */
    private JPanel buildFilterPanel() {
        filterPanel = new JPanel(new BorderLayout());
        filterPanel.setOpaque(false);

        // LEFT — filter toggles
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP_SM, 0));
        left.setOpaque(false);

        btnFilterAll       = createFilterButton(safeGet("filter.all",       "Todas"),       "all");
        btnFilterPending   = createFilterButton(safeGet("filter.pending",   "Pendientes"),  "pending");
        btnFilterCompleted = createFilterButton(safeGet("filter.completed", "Completadas"), "completed");

        left.add(btnFilterAll);
        left.add(btnFilterPending);
        left.add(btnFilterCompleted);

        // RIGHT — action buttons flush right, never overlap the filters
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP_SM, 0));
        right.setOpaque(false);

        btnDelete         = createStyledButton(safeGet("btn.delete", "Eliminar"), "danger");
        btnClearCompleted = createStyledButton(safeGet("btn.clear",   "Limpiar"), "success");
        btnDelete.setToolTipText(lang.get("tooltip.btn.delete"));
        btnClearCompleted.setToolTipText(lang.get("tooltip.btn.clear"));
        btnDelete.addActionListener(e -> onDeleteTask());
        btnClearCompleted.addActionListener(e -> onClearCompleted());

        right.add(btnDelete);
        right.add(btnClearCompleted);

        filterPanel.add(left,  BorderLayout.WEST);
        filterPanel.add(right, BorderLayout.EAST);
        return filterPanel;
    }

    private FlatButton createFilterButton(String text, String filter) {
        boolean active = activeFilter.equals(filter);
        Color bg   = active ? theme.getBackgroundButton()  : theme.getBackgroundPanel();
        Color fg   = active ? theme.getTextButton()        : theme.getTextPrimary();
        Font  font = active ? theme.getFontBold()          : theme.getFontNormal();
        FlatButton btn = new FlatButton(text, bg, fg, font, BTN_HEIGHT, RADIUS);
        btn.setOutlined(!active, theme.getBorder());
        btn.setToolTipText(switch (filter) {
            case "pending"   -> lang.get("tooltip.filter.pending");
            case "completed" -> lang.get("tooltip.filter.completed");
            default          -> lang.get("tooltip.filter.all");
        });
        btn.addActionListener(e -> {
            activeFilter = filter;
            logger.trace("Filtro: " + filter.toUpperCase());
            styleFilterFlatButton(btnFilterAll,       "all");
            styleFilterFlatButton(btnFilterPending,   "pending");
            styleFilterFlatButton(btnFilterCompleted, "completed");
            refreshTaskList();
        });
        return btn;
    }

    private JScrollPane buildTaskArea() {
        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setBorder(new EmptyBorder(GAP_SM, 0, GAP_SM, 0));

        scrollPane = new JScrollPane(taskListPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().putClientProperty("JScrollBar.showButtons", false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING_H, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(GAP_SM, PADDING_H, GAP_MD, PADDING_H));

        lblTotal     = new JLabel();
        lblPending   = new JLabel();
        lblCompleted = new JLabel();

        panel.add(lblTotal);
        panel.add(new JLabel("·"));
        panel.add(lblPending);
        panel.add(new JLabel("·"));
        panel.add(lblCompleted);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Event handlers
    // ══════════════════════════════════════════════════════════════════════

    private void onAddTask() {
        String title = inputField.getText().trim();
        if (title.isEmpty()) {
            showWarning(lang.get("msg.empty.task"));
            logger.warning("[USER_ACTION] Intento de agregar tarea vacía.");
            return;
        }
        try {
            service.addTask(title);
            inputField.setText("");
            animateAdd();
            refreshTaskList();
        } catch (IllegalArgumentException ex) {
            logger.logError("Error al agregar tarea", ex);
            showError(ex.getMessage());
        }
    }

    private void onDeleteTask() {
        TaskItemPanel selected = getSelectedTaskPanel();
        if (selected == null) {
            showWarning(lang.get("msg.no.selection"));
            logger.warning("[USER_ACTION] Intento de eliminar sin selección.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                lang.get("msg.confirm.delete"),
                lang.get("msg.confirm.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            service.deleteTask(selected.getTaskId());
            refreshTaskList();
        }
    }

    private void onClearCompleted() {
        int removed = service.clearCompleted();
        if (removed == 0) logger.info("[USER_ACTION] No había tareas completadas para limpiar.");
        refreshTaskList();
    }

    private void onThemeChanged() {
        String themeKey = comboTheme.getSelectedIndex() == 0 ? "light" : "dark";
        config.setTheme(themeKey);
        theme.reload();
        applyTheme();
        repaint();
    }

    private void onLanguageChanged() {
        String langKey = comboLang.getSelectedIndex() == 0 ? "es" : "en";
        config.setLanguage(langKey);
        lang.reload();
        refreshTexts();
        refreshTaskList();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  List refresh & empty state
    // ══════════════════════════════════════════════════════════════════════

    private void refreshTaskList() {
        long start = System.nanoTime();
        taskListPanel.removeAll();

        List<Task> tasks = switch (activeFilter) {
            case "pending"   -> service.getPendingTasks();
            case "completed" -> service.getCompletedTasks();
            default          -> service.getAllTasks();
        };

        if (tasks.isEmpty()) {
            taskListPanel.add(buildEmptyState());
        } else {
            for (Task task : tasks) {
                TaskItemPanel item = new TaskItemPanel(task);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT + 4));
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                taskListPanel.add(item);
                taskListPanel.add(Box.createVerticalStrut(GAP_SM));
            }
        }

        updateStats();
        applyThemeToTaskList();
        taskListPanel.revalidate();
        taskListPanel.repaint();

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > 50) logger.debug("refreshTaskList tomó " + elapsed + "ms (" + tasks.size() + " tareas)");
    }

    /**
     * Builds the "no tasks" placeholder.
     * Uses {@link #safeGet} so the string never renders as "[label.empty]"
     * even when the i18n bundle does not contain the key yet.
     */
    private JPanel buildEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setBorder(new EmptyBorder(48, 0, 0, 0));

        JLabel icon = new JLabel("✓");
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 32));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setForeground(theme.getTextSecondary());

        JLabel msg = new JLabel(safeGet("label.empty", "Sin tareas por aquí"));
        msg.setFont(theme.getFontSmall());
        msg.setForeground(theme.getTextSecondary());
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setBorder(new EmptyBorder(GAP_MD, 0, 0, 0));

        panel.add(icon);
        panel.add(msg);
        return panel;
    }

    private void updateStats() {
        lblTotal.setText(lang.get("label.tasks.total")     + " " + service.countTotal());
        lblPending.setText(lang.get("label.tasks.pending") + " " + service.countPending());
        lblCompleted.setText(lang.get("label.tasks.completed") + " " + service.countCompleted());
        for (JLabel lbl : new JLabel[]{lblTotal, lblPending, lblCompleted}) {
            lbl.setFont(theme.getFontSmall());
            lbl.setForeground(theme.getTextSecondary());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Text refresh (i18n)
    // ══════════════════════════════════════════════════════════════════════

    private void refreshTexts() {
        setTitle(lang.get("app.title"));
        headerTitle.setText(safeGet("app.header.title", "Mis Tareas"));
        lblTheme.setText(safeGet("label.theme", "Tema"));
        lblLang.setText(safeGet("label.language", "Idioma"));

        comboTheme.setItems(new String[]{
                safeGet("option.theme.light", "Claro"), safeGet("option.theme.dark", "Oscuro")
        });
        comboTheme.setSelectedIndex("dark".equals(config.getTheme()) ? 1 : 0);

        comboLang.setItems(new String[]{
                safeGet("option.lang.es", "Español"), safeGet("option.lang.en", "English")
        });
        comboLang.setSelectedIndex("en".equals(config.getLanguage()) ? 1 : 0);

        btnAdd.setText(safeGet("btn.add", "Agregar"));
        btnDelete.setText(safeGet("btn.delete", "Eliminar"));
        btnClearCompleted.setText(safeGet("btn.clear", "Limpiar"));
        btnFilterAll.setText(safeGet("filter.all", "Todas"));
        btnFilterPending.setText(safeGet("filter.pending", "Pendientes"));
        btnFilterCompleted.setText(safeGet("filter.completed", "Completadas"));

        if (inputField instanceof RoundTextField rt)
            rt.setPlaceholder(safeGet("input.placeholder", "Nueva tarea…"));

        comboTheme.setToolTipText(lang.get("tooltip.theme"));
        comboLang.setToolTipText(lang.get("tooltip.language"));
        btnAdd.setToolTipText(lang.get("tooltip.btn.add"));
        btnDelete.setToolTipText(lang.get("tooltip.btn.delete"));
        btnClearCompleted.setToolTipText(lang.get("tooltip.btn.clear"));

        updateStats();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Theme application
    // ══════════════════════════════════════════════════════════════════════

    private void applyTheme() {
        long start = System.nanoTime();

        rootPanel.setBackground(theme.getBackground());

        headerPanel.setBackground(theme.getBackgroundHeader());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, theme.getBorder()),
                new EmptyBorder(PADDING_V, PADDING_H, PADDING_V, PADDING_H)
        ));
        headerTitle.setForeground(theme.getTextHeader());
        headerTitle.setFont(theme.getFontTitle());

        if (inputField != null) {
            inputField.setBackground(theme.getBackgroundInput());
            inputField.setForeground(theme.getTextPrimary());
            inputField.setFont(theme.getFontNormal());
            inputField.setCaretColor(theme.getTextPrimary());
            inputField.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(theme.getBorder(), 1, true),
                    new EmptyBorder(0, 12, 0, 12)
            ));
        }

        if (btnAdd            != null) { btnAdd.setColors(theme.getBackgroundButton(), theme.getTextButton()); btnAdd.setFont(theme.getFontNormal()); }
        if (btnDelete         != null) { btnDelete.setColors(theme.getBackgroundButtonDanger(), theme.getTextButton()); btnDelete.setFont(theme.getFontNormal()); }
        if (btnClearCompleted != null) { btnClearCompleted.setColors(theme.getBackgroundButtonSuccess(), theme.getTextButton()); btnClearCompleted.setFont(theme.getFontNormal()); }

        styleFilterFlatButton(btnFilterAll,       "all");
        styleFilterFlatButton(btnFilterPending,   "pending");
        styleFilterFlatButton(btnFilterCompleted, "completed");

        if (comboTheme != null) styleCombo(comboTheme);
        if (comboLang  != null) styleCombo(comboLang);
        if (comboTheme != null) comboTheme.repaint();
        if (comboLang  != null) comboLang.repaint();

        if (lblTheme != null) { lblTheme.setForeground(theme.getTextSecondary()); lblTheme.setFont(theme.getFontSmall()); }
        if (lblLang  != null) { lblLang.setForeground(theme.getTextSecondary());  lblLang.setFont(theme.getFontSmall()); }
        if (filterPanel != null) filterPanel.setBackground(theme.getBackground());

        if (scrollPane != null) {
            scrollPane.setBackground(theme.getBackground());
            scrollPane.getViewport().setBackground(theme.getBackground());
        }

        applyThemeToTaskList();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > 50) logger.debug("applyTheme tomó " + elapsed + "ms");
    }

    private void applyThemeToTaskList() {
        if (taskListPanel == null) return;
        taskListPanel.setBackground(theme.getBackground());
        for (Component c : taskListPanel.getComponents()) {
            if (c instanceof TaskItemPanel tip) tip.applyTheme();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Styling helpers
    // ══════════════════════════════════════════════════════════════════════

    private void styleFilterFlatButton(FlatButton btn, String filter) {
        if (btn == null) return;
        boolean active = activeFilter.equals(filter);
        btn.setColors(
                active ? theme.getBackgroundButton()  : theme.getBackgroundPanel(),
                active ? theme.getTextButton()        : theme.getTextPrimary()
        );
        btn.setFont(active ? theme.getFontBold() : theme.getFontNormal());
        btn.setOutlined(!active, theme.getBorder());
    }

    // styleButton / styleFilterButton / addHoverEffect removed —
    // FlatButton handles its own painting (see inner class at bottom).

    private void styleCombo(FlatCombo combo) {
        combo.setColors(theme.getBackgroundInput(), theme.getTextPrimary(), theme.getBorder());
        combo.setFont(theme.getFontSmall());
    }

    private FlatButton createStyledButton(String text, String type) {
        Color bg = switch (type) {
            case "danger"  -> theme.getBackgroundButtonDanger();
            case "success" -> theme.getBackgroundButtonSuccess();
            default        -> theme.getBackgroundButton();
        };
        return new FlatButton(text, bg, theme.getTextButton(), theme.getFontNormal(), BTN_HEIGHT, RADIUS);
    }

    private FlatCombo buildStyledCombo(String[] items) {
        FlatCombo combo = new FlatCombo(items, theme.getBackgroundInput(),
                theme.getTextPrimary(), theme.getBorder(),
                theme.getFontSmall(), BTN_HEIGHT - 2, RADIUS);
        return combo;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Misc helpers
    // ══════════════════════════════════════════════════════════════════════

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, lang.get("msg.warning.title"), JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, lang.get("msg.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void animateAdd() {
        Color accent = theme.getBackgroundButton();
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accent, 2, true), new EmptyBorder(0, 12, 0, 12)
        ));
        Timer t = new Timer(220, e -> inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(theme.getBorder(), 1, true), new EmptyBorder(0, 12, 0, 12)
        )));
        t.setRepeats(false);
        t.start();
    }

    /**
     * Safe i18n lookup with hard-coded fallback.
     * Returns {@code fallback} when the key is absent or resolves to the
     * bracketed placeholder that LanguageManager emits (e.g. "[label.empty]").
     */
    private String safeGet(String key, String fallback) {
        String v = lang.get(key);
        if (v == null || v.isBlank() || v.equals("[" + key + "]")) return fallback;
        return v;
    }

    private TaskItemPanel getSelectedTaskPanel() {
        for (Component c : taskListPanel.getComponents()) {
            if (c instanceof TaskItemPanel tip && tip.isSelected()) return tip;
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inner: FlatButton  — fully custom-painted; ignores macOS Aqua L&F
    // ══════════════════════════════════════════════════════════════════════

    /**
     * A {@link JButton} subclass that paints itself entirely via
     * {@link #paintComponent}, bypassing the macOS Aqua (and any other)
     * Look-and-Feel renderer.  This guarantees that background colour,
     * foreground, border radius and hover/press states are respected on
     * every platform.
     *
     * <p>Usage mirrors a normal JButton:
     * <pre>
     *   FlatButton btn = new FlatButton("Click me", bgColor, fgColor, font, height, radius);
     *   btn.addActionListener(e -> ...);
     * </pre>
     */
    private static final class FlatButton extends JButton {

        private Color  bg;
        private Color  fg;
        private Color  borderColor;   // null → no outline, just filled
        private int    arc;
        private boolean outlined     = false;
        private boolean initialized  = false; // guard against Nimbus calling setFont before UI is ready

        // transient paint state
        private boolean hovered  = false;
        private boolean pressed  = false;

        FlatButton(String text, Color bg, Color fg, Font font, int height, int arc) {
            super(text);
            this.bg  = bg;
            this.fg  = fg;
            this.arc = arc;

            setFont(font);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);   // ← tells Swing not to fill; we do it
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // size: respect preferred width from text, fixed height
            Insets pad = new Insets(0, 16, 0, 16);
            setBorder(new EmptyBorder(pad));
            FontMetrics fm = getFontMetrics(font);
            int pref = fm.stringWidth(text) + pad.left + pad.right + 8;
            setPreferredSize(new Dimension(pref, height));
            setMinimumSize(new Dimension(pref, height));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hovered = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true;  repaint(); }
                @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
            });

            initialized = true;
            // Now that the UI is fully set up, do the first proper size measurement.
            resizeToText(getText(), getFont());
        }

        /** Change fill + text colours at runtime (theme switch). */
        void setColors(Color background, Color foreground) {
            this.bg = background;
            this.fg = foreground;
            repaint();
        }

        /**
         * When {@code outlined} is true the button renders with a transparent
         * fill and a 1-px border in {@code borderCol}.  Used for inactive
         * filter buttons.
         */
        void setOutlined(boolean outlined, Color borderCol) {
            this.outlined    = outlined;
            this.borderColor = borderCol;
            repaint();
        }

        @Override
        public void setFont(Font f) {
            super.setFont(f);
            if (initialized && f != null && getText() != null)
                resizeToText(getText(), f);
        }

        @Override
        public void setText(String text) {
            super.setText(text);
            if (initialized && text != null && getFont() != null)
                resizeToText(text, getFont());
        }

        /** Central size measurement — called only after the component is fully initialised. */
        private void resizeToText(String text, Font f) {
            FontMetrics fm = getFontMetrics(f);
            if (fm == null) return;
            int pref = fm.stringWidth(text) + 32 + 8;
            int h    = getPreferredSize().height;
            setPreferredSize(new Dimension(pref, h));
            setMinimumSize  (new Dimension(pref, h));
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

            // ── Background fill ─────────────────────────────────────────
            Color fill;
            if (outlined) {
                fill = pressed ? darken(bg, 0.08f)
                        : hovered ? darken(bg, 0.04f)
                        : bg;
            } else {
                fill = pressed ? darken(bg, 0.15f)
                        : hovered ? brighten(bg, 0.12f)
                        : bg;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc * 2, arc * 2);

            // ── Border (outline mode only) ───────────────────────────────
            if (outlined && borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc * 2, arc * 2);
            }

            // ── Label ────────────────────────────────────────────────────
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);

            g2.dispose();
        }

        // ── Colour math helpers ──────────────────────────────────────────
        private static Color brighten(Color c, float factor) {
            int r = Math.min(255, (int)(c.getRed()   + (255 - c.getRed())   * factor));
            int g = Math.min(255, (int)(c.getGreen() + (255 - c.getGreen()) * factor));
            int b = Math.min(255, (int)(c.getBlue()  + (255 - c.getBlue())  * factor));
            return new Color(r, g, b, c.getAlpha());
        }

        private static Color darken(Color c, float factor) {
            int r = Math.max(0, (int)(c.getRed()   * (1 - factor)));
            int g = Math.max(0, (int)(c.getGreen() * (1 - factor)));
            int b = Math.max(0, (int)(c.getBlue()  * (1 - factor)));
            return new Color(r, g, b, c.getAlpha());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inner: FlatCombo — custom-painted dropdown; bypasses Nimbus/Aqua
    // ══════════════════════════════════════════════════════════════════════

    /**
     * A fully custom-painted combo box that ignores the platform L&F renderer.
     * Renders as a pill with the selected item and a small chevron arrow.
     * The popup still uses the native JPopupMenu for accessibility.
     */
    private static final class FlatCombo extends JPanel {

        private String[]  items;
        private int       selectedIndex = 0;
        private Color     bg;
        private Color     fg;
        private Color     borderCol;
        private int       arc;
        private boolean   hovered = false;
        private boolean   popupOpen = false;

        private final JPopupMenu popup = new JPopupMenu();
        private final ButtonGroup group = new ButtonGroup();

        // Listeners registered externally via addActionListener
        private final java.util.List<java.awt.event.ActionListener> listeners = new java.util.ArrayList<>();

        FlatCombo(String[] items, Color bg, Color fg, Color border, Font font, int height, int arc) {
            this.items     = items;
            this.bg        = bg;
            this.fg        = fg;
            this.borderCol = border;
            this.arc       = arc;

            setOpaque(false);
            setFont(font);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(measureWidth(items, font), height));
            setMinimumSize(getPreferredSize());

            rebuildPopup();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { showPopup(); }
            });

            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e)   { popupOpen = true;  repaint(); }
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { popupOpen = false; repaint(); }
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e)            { popupOpen = false; repaint(); }
            });
        }

        // ── Public API ───────────────────────────────────────────────────

        void setSelectedIndex(int idx) {
            if (idx >= 0 && idx < items.length) {
                selectedIndex = idx;
                syncGroup();
                repaint();
            }
        }

        int getSelectedIndex() { return selectedIndex; }

        void setItems(String[] newItems) {
            this.items = newItems;
            selectedIndex = Math.min(selectedIndex, newItems.length - 1);
            setPreferredSize(new Dimension(measureWidth(newItems, getFont()), getPreferredSize().height));
            rebuildPopup();
            repaint();
        }

        void setColors(Color background, Color foreground, Color border) {
            this.bg        = background;
            this.fg        = foreground;
            this.borderCol = border;
            repaint();
        }

        void addActionListener(java.awt.event.ActionListener l) { listeners.add(l); }
        @Override public void setToolTipText(String tip) { super.setToolTipText(tip); }

        // ── Internal ─────────────────────────────────────────────────────

        private void showPopup() {
            popup.show(this, 0, getHeight() + 2);
        }

        private void rebuildPopup() {
            popup.removeAll();
            group.clearSelection();
            popup.setBackground(bg);
            popup.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(borderCol, 1, true),
                    new EmptyBorder(4, 0, 4, 0)
            ));

            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                JMenuItem mi = new JMenuItem(items[i]);
                mi.setFont(getFont());
                mi.setForeground(fg);
                mi.setBackground(bg);
                mi.setOpaque(true);
                mi.setBorder(new EmptyBorder(6, 14, 6, 14));
                mi.addActionListener(e -> {
                    selectedIndex = idx;
                    repaint();
                    fireActionPerformed();
                });
                popup.add(mi);
            }
        }

        private void syncGroup() { /* nothing — visual state drives rendering */ }

        private void fireActionPerformed() {
            java.awt.event.ActionEvent ev = new java.awt.event.ActionEvent(
                    this, java.awt.event.ActionEvent.ACTION_PERFORMED, "comboChanged");
            for (java.awt.event.ActionListener l : listeners) l.actionPerformed(ev);
        }

        private static int measureWidth(String[] items, Font font) {
            if (font == null || items == null || items.length == 0) return 80;
            // Use a temporary component to get FontMetrics
            Canvas c = new Canvas();
            FontMetrics fm = c.getFontMetrics(font);
            int maxW = 0;
            for (String s : items) maxW = Math.max(maxW, fm.stringWidth(s));
            return maxW + 16 + 16 + 20; // pad-left + pad-right + chevron area
        }

        // ── Painting ─────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // ── Fill ────────────────────────────────────────────────────
            Color fill = (hovered || popupOpen)
                    ? new Color(Math.min(255, bg.getRed()+12),
                    Math.min(255, bg.getGreen()+12),
                    Math.min(255, bg.getBlue()+20), bg.getAlpha())
                    : bg;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w-1, h-1, arc*2, arc*2);

            // ── Border ──────────────────────────────────────────────────
            Color bord = (hovered || popupOpen)
                    ? new Color(Math.min(255, borderCol.getRed()+40),
                    Math.min(255, borderCol.getGreen()+40),
                    Math.min(255, borderCol.getBlue()+60), 255)
                    : borderCol;
            g2.setColor(bord);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w-1, h-1, arc*2, arc*2);

            // ── Selected text ────────────────────────────────────────────
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            String label = (items != null && selectedIndex < items.length) ? items[selectedIndex] : "";
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, 12, ty);

            // ── Chevron ──────────────────────────────────────────────────
            int cx = w - 14, cy = h / 2;
            int[] xs = { cx - 4, cx, cx + 4 };
            int[] ys = { cy - 2, cy + 2, cy - 2 };
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 160));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(xs, ys, 3);

            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inner: RoundTextField
    // ══════════════════════════════════════════════════════════════════════

    private static final class RoundTextField extends JTextField {

        private String placeholder;
        private final int arc;

        RoundTextField(String placeholder, int arc) {
            this.placeholder = placeholder;
            this.arc = arc;
            setOpaque(false);
        }

        void setPlaceholder(String text) { this.placeholder = text; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc * 2, arc * 2));
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                g2.setColor(new Color(150, 150, 150, 160));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, getInsets().left, y);
            }
            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inner: TaskItemPanel
    // ══════════════════════════════════════════════════════════════════════

    private final class TaskItemPanel extends JPanel {

        private final Task task;
        private boolean    selected = false;

        private final JCheckBox checkbox;
        private final JLabel    titleLabel;

        TaskItemPanel(Task t) {
            this.task = t;

            // ── Outer layout: full-width row ─────────────────────────────
            setLayout(new BorderLayout(0, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(theme.getBorder(), 1, true),
                    new EmptyBorder(0, 14, 0, 14)
            ));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            setToolTipText(task.isCompleted()
                    ? safeGet("tooltip.task.completed", "Tarea completada")
                    : safeGet("tooltip.task.pending",   "Tarea pendiente"));

            // ── Checkbox ─────────────────────────────────────────────────
            checkbox = new JCheckBox();
            checkbox.setSelected(task.isCompleted());
            checkbox.setOpaque(false);
            checkbox.setFocusPainted(false);
            checkbox.setToolTipText(task.isCompleted()
                    ? safeGet("tooltip.task.uncheck", "Marcar como pendiente")
                    : safeGet("tooltip.task.check",   "Marcar como completada"));
            checkbox.addActionListener(e -> {
                logger.trace("Checkbox tarea id=" + task.getId());
                service.toggleComplete(task.getId());
                refreshTaskList();
            });

            // Wrap checkbox in a panel so it stays vertically centred
            JPanel checkWrap = new JPanel(new GridBagLayout());
            checkWrap.setOpaque(false);
            checkWrap.add(checkbox);

            // ── Title ─────────────────────────────────────────────────────
            titleLabel = new JLabel(task.getTitle());
            titleLabel.setBorder(new EmptyBorder(0, GAP_SM, 0, GAP_SM));

            // ── Badge (completed only) — centred vertically via GridBagLayout
            JPanel eastPanel = new JPanel(new GridBagLayout());
            eastPanel.setOpaque(false);
            eastPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

            if (task.isCompleted()) {
                String badgeText = safeGet("badge.done", "Listo");
                // Pill badge — fully custom-painted so radius can be large (50px = capsule)
                JComponent badge = new JComponent() {
                    {
                        setOpaque(false);
                        setFont(new Font(theme.getFontSmall().getFamily(), Font.BOLD, 10));
                        setBorder(new EmptyBorder(3, 10, 3, 10));
                    }
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        Color successCol = theme.getBackgroundButtonSuccess();
                        // Translucent fill
                        g2.setColor(new Color(successCol.getRed(), successCol.getGreen(), successCol.getBlue(), 28));
                        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                        // Border
                        g2.setColor(new Color(successCol.getRed(), successCol.getGreen(), successCol.getBlue(), 130));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                        // Text
                        g2.setFont(getFont());
                        g2.setColor(successCol);
                        FontMetrics fm = g2.getFontMetrics();
                        Insets ins = getInsets();
                        int tx = ins.left;
                        int ty = ins.top + fm.getAscent();
                        g2.drawString(badgeText, tx, ty);
                        g2.dispose();
                    }
                    @Override public Dimension getPreferredSize() {
                        FontMetrics fm = getFontMetrics(getFont());
                        Insets ins = getInsets();
                        int w = ins.left + fm.stringWidth(badgeText) + ins.right;
                        int h = ins.top + fm.getHeight() + ins.bottom;
                        return new Dimension(w, h);
                    }
                };
                eastPanel.add(badge);   // GridBagLayout centres it automatically
            }

            add(checkWrap,  BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);
            add(eastPanel,  BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (Component c : taskListPanel.getComponents()) {
                        if (c instanceof TaskItemPanel other && c != TaskItemPanel.this)
                            other.setSelected(false);
                    }
                    setSelected(!selected);
                    logger.trace("Tarea seleccionada id=" + task.getId());
                }
                @Override public void mouseEntered(MouseEvent e) { if (!selected) setHighlight(true); }
                @Override public void mouseExited (MouseEvent e) { if (!selected) setHighlight(false); }
            });

            applyTheme();
        }

        void applyTheme() {
            Color bg = task.isCompleted() ? theme.getBackgroundItemCompleted()
                    : selected           ? theme.getBackgroundInput()
                    :                      theme.getBackgroundItem();
            setBackground(bg);
            checkbox.setBackground(bg);

            // Propagate bg to wrapper panels so no colour leaks
            for (Component c : getComponents()) {
                if (c instanceof JPanel p) p.setBackground(bg);
            }

            titleLabel.setForeground(task.isCompleted()
                    ? theme.getTextCompleted()
                    : theme.getTextPrimary());
            titleLabel.setFont(task.isCompleted()
                    ? new Font(theme.getFontNormal().getFamily(), Font.ITALIC, theme.getFontNormal().getSize())
                    : theme.getFontNormal());
        }

        private void setHighlight(boolean on) {
            Color bg = on ? theme.getBackgroundInput() : theme.getBackgroundItem();
            setBackground(bg);
            for (Component c : getComponents()) {
                if (c instanceof JPanel p) p.setBackground(bg);
            }
            checkbox.setBackground(bg);
            repaint();
        }

        void    setSelected(boolean sel) { this.selected = sel; applyTheme(); repaint(); }
        boolean isSelected()             { return selected; }
        String  getTaskId()              { return task.getId(); }
    }
}
