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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

public class MainWindow extends JFrame {

    private final AppLogger      logger   = AppLogger.getInstance();
    private final ConfigManager  config   = ConfigManager.getInstance();
    private final ThemeManager   theme    = ThemeManager.getInstance();
    private final LanguageManager lang    = LanguageManager.getInstance();
    private final TaskService    service  = TaskService.getInstance();

    private JPanel     rootPanel;
    private JPanel     headerPanel;
    private JLabel     headerTitle;
    private JTextField inputField;
    private JButton    btnAdd;
    private JButton    btnDelete;
    private JButton    btnClearCompleted;
    private JButton    btnExport;
    private JButton    btnImport;
    private JPanel     filterPanel;
    private JButton    btnFilterAll;
    private JButton    btnFilterPending;
    private JButton    btnFilterCompleted;
    private JPanel     taskListPanel;
    private JScrollPane scrollPane;
    private JLabel     lblTotal;
    private JLabel     lblPending;
    private JLabel     lblCompleted;
    private JComboBox<String> comboTheme;
    private JComboBox<String> comboLang;
    private JLabel     lblTheme;
    private JLabel     lblLang;
    private JComboBox<String> comboFontFamily;
    private JComboBox<String> comboFontSize;
    private JLabel     lblFont;

    private List<String> themeCodes = new ArrayList<>();
    private String activeFilter = "all";

    private static final String[] LANGUAGE_CODES = {"es", "en", "fr", "pt", "de"};

    private boolean updatingControls = false;

    public MainWindow() {
        initFrame();
        buildUI();
        applyTheme();
        refreshTaskList();
        setVisible(true);

        logger.info("Aplicaci\u00f3n iniciada. Versi\u00f3n: " + config.getVersion());
    }

    private void initFrame() {
        setTitle(lang.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(520, 640));
        setPreferredSize(new Dimension(580, 700));
        setLocationRelativeTo(null);
        pack();
    }

    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout(0, 0));
        setContentPane(rootPanel);

        rootPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        rootPanel.add(buildCenterPanel(), BorderLayout.CENTER);
        rootPanel.add(buildStatsPanel(), BorderLayout.SOUTH);
    }

    private String[] getLanguageOptions() {
        return new String[]{
                lang.get("option.lang.es"),
                lang.get("option.lang.en"),
                lang.get("option.lang.fr"),
                lang.get("option.lang.pt"),
                lang.get("option.lang.de")
        };
    }

    private int getLanguageIndex(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) {
                return i;
            }
        }
        return 0;
    }

    private String getSelectedLanguageCode() {
        int index = comboLang.getSelectedIndex();

        if (index < 0 || index >= LANGUAGE_CODES.length) {
            return "es";
        }

        return LANGUAGE_CODES[index];
    }

    private String[] getThemeOptions() {
        return new String[]{
                lang.get("option.theme.light"),
                lang.get("option.theme.dark")
        };
    }

    private JPanel buildHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        headerTitle = new JLabel(lang.get("app.header.title"));
        headerPanel.add(headerTitle, BorderLayout.WEST);

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        configPanel.setOpaque(false);

        lblTheme = new JLabel(lang.get("label.theme"));
        comboTheme = new JComboBox<>();
        themeCodes = theme.getAvailableThemes();
        themeCodes.forEach(code -> comboTheme.addItem(theme.getThemeDisplay(code)));
        int themeIdx = themeCodes.indexOf(config.getTheme());
        if (themeIdx >= 0) comboTheme.setSelectedIndex(themeIdx);
        comboTheme.addActionListener(e -> onThemeChanged());

        lblLang = new JLabel(lang.get("label.language"));
        comboLang = new JComboBox<>(getLanguageOptions());
        comboLang.setSelectedIndex(getLanguageIndex(config.getLanguage()));
        comboLang.addActionListener(e -> onLanguageChanged());

        lblFont = new JLabel(lang.get("label.font"));
        comboFontFamily = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames());
        comboFontFamily.setSelectedItem(theme.getFontNormal().getFamily());
        comboFontFamily.addActionListener(e -> onFontChanged());

        comboFontSize = new JComboBox<>();
        for (int i = 10; i <= 24; i++) {
            comboFontSize.addItem(String.valueOf(i));
        }
        comboFontSize.setSelectedItem(String.valueOf(theme.getFontNormal().getSize()));
        comboFontSize.addActionListener(e -> onFontChanged());

        configPanel.add(lblTheme);
        configPanel.add(comboTheme);
        configPanel.add(Box.createHorizontalStrut(10));
        configPanel.add(lblLang);
        configPanel.add(comboLang);
        configPanel.add(Box.createHorizontalStrut(10));
        configPanel.add(lblFont);
        configPanel.add(comboFontFamily);
        configPanel.add(comboFontSize);

        headerPanel.add(configPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel buildCenterPanel() {
        JPanel below = new JPanel(new BorderLayout(0, 8));
        below.setOpaque(false);
        below.add(buildFilterPanel(), BorderLayout.NORTH);
        below.add(buildTaskArea(), BorderLayout.CENTER);

        JPanel full = new JPanel(new BorderLayout(0, 12));
        full.setOpaque(false);
        full.setBorder(new EmptyBorder(12, 20, 10, 20));
        full.add(buildInputPanel(), BorderLayout.NORTH);
        full.add(below, BorderLayout.CENTER);

        return full;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        inputField = new JTextField();
        inputField.putClientProperty("hint", lang.get("input.placeholder"));

        btnAdd = createButton(lang.get("btn.add"), "primary");
        btnAdd.setPreferredSize(new Dimension(100, 38));
        btnAdd.addActionListener(e -> onAddTask());

        inputField.addActionListener(e -> onAddTask());

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(btnAdd,     BorderLayout.EAST);
        return panel;
    }

    private JPanel buildFilterPanel() {
        filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        btnFilterAll       = createFilterButton(lang.get("filter.all"),       "all");
        btnFilterPending   = createFilterButton(lang.get("filter.pending"),   "pending");
        btnFilterCompleted = createFilterButton(lang.get("filter.completed"), "completed");

        btnDelete         = createButton(lang.get("btn.delete"),          "danger");
        btnClearCompleted = createButton(lang.get("btn.clear.completed"), "success");

        btnDelete.addActionListener(e -> onDeleteTask());
        btnClearCompleted.addActionListener(e -> onClearCompleted());

        btnExport = createButton(lang.get("btn.export"), "primary");
        btnImport = createButton(lang.get("btn.import"), "primary");
        btnExport.addActionListener(e -> onExportTasks());
        btnImport.addActionListener(e -> onImportTasks());

        filterPanel.add(btnFilterAll);
        filterPanel.add(btnFilterPending);
        filterPanel.add(btnFilterCompleted);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(btnDelete);
        filterPanel.add(btnClearCompleted);
        filterPanel.add(Box.createHorizontalStrut(12));
        filterPanel.add(btnExport);
        filterPanel.add(btnImport);

        return filterPanel;
    }

    private JButton createFilterButton(String text, String filter) {
        JButton btn = new JButton(text);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            activeFilter = filter;
            logger.trace("Filtro seleccionado: " + filter.toUpperCase());
            refreshTaskList();
        });
        return btn;
    }

    private JScrollPane buildTaskArea() {
        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(taskListPanel);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 20, 12, 20));

        lblTotal     = new JLabel();
        lblPending   = new JLabel();
        lblCompleted = new JLabel();

        panel.add(lblTotal);
        panel.add(new JLabel("|"));
        panel.add(lblPending);
        panel.add(new JLabel("|"));
        panel.add(lblCompleted);

        return panel;
    }

    private void onAddTask() {
        String title = inputField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, lang.get("msg.empty.task"), lang.get("msg.warning.title"), JOptionPane.WARNING_MESSAGE);
            logger.warning("[USER_ACTION] Intento de agregar tarea vac\u00eda.");
            return;
        }

        try {
            service.addTask(title);
            logger.info(lang.format("msg.task.added", title));
            inputField.setText("");
            refreshTaskList();
        } catch (IllegalArgumentException e) {
            logger.logError("Error al agregar tarea", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), lang.get("msg.error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteTask() {
        TaskItemPanel selected = getSelectedTaskPanel();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, lang.get("msg.no.selection"), lang.get("msg.warning.title"), JOptionPane.WARNING_MESSAGE);
            logger.warning("[USER_ACTION] Intento de eliminar sin selecci\u00f3n.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            lang.get("msg.confirm.delete"),
            lang.get("msg.confirm.title"),
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String deletedTitle = selected.getTaskTitle();

            service.deleteTask(selected.getTaskId());
            logger.info(lang.format("msg.task.deleted", deletedTitle));

            refreshTaskList();
        }
    }

    private void onClearCompleted() {
        int removed = service.clearCompleted();
        if (removed == 0) {
            logger.info("[USER_ACTION] No hab\u00eda tareas completadas para limpiar.");
        }
        refreshTaskList();
    }

    private void onExportTasks() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(lang.get("dialog.export.title"));
        chooser.setSelectedFile(new java.io.File("tasks-export.json"));
        javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter("JSON (*.json)", "json");
        chooser.setFileFilter(filter);
        int r = chooser.showSaveDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File f = chooser.getSelectedFile();
        String path = f.getPath();
        if (!path.toLowerCase().endsWith(".json")) {
            f = new java.io.File(path + ".json");
        }
        try {
            Path p = f.toPath();
            service.exportTo(p);
            JOptionPane.showMessageDialog(this,
                lang.format("msg.export.done", service.countTotal()),
                lang.get("dialog.export.title"),
                JOptionPane.INFORMATION_MESSAGE);
            logger.info("[USER_ACTION] Tareas exportadas a " + p.toAbsolutePath());
        } catch (IOException ex) {
            logger.logError("Exportación fallida", ex);
            JOptionPane.showMessageDialog(this,
                lang.format("msg.data.io.error", ex.getMessage()),
                lang.get("msg.error.title"),
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onImportTasks() {
        String[] modes = { lang.get("import.replace"), lang.get("import.merge") };
        int modeIdx = JOptionPane.showOptionDialog(
            this,
            lang.get("msg.import.choose.mode"),
            lang.get("msg.import.title"),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            modes,
            modes[0]
        );
        if (modeIdx < 0) {
            return;
        }
        TaskService.ImportMode mode = modeIdx == 0
            ? TaskService.ImportMode.REPLACE
            : TaskService.ImportMode.MERGE;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(lang.get("dialog.import.title"));
        javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter("JSON (*.json)", "json");
        chooser.setFileFilter(filter);
        int r = chooser.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path p = chooser.getSelectedFile().toPath();
            service.importFrom(p, mode);
            refreshTaskList();
            JOptionPane.showMessageDialog(this,
                lang.format("msg.import.done", service.countTotal()),
                lang.get("msg.import.title"),
                JOptionPane.INFORMATION_MESSAGE);
            logger.info("[USER_ACTION] Tareas importadas desde " + p.toAbsolutePath());
        } catch (IOException ex) {
            logger.logError("Importación fallida", ex);
            JOptionPane.showMessageDialog(this,
                lang.format("msg.data.io.error", ex.getMessage()),
                lang.get("msg.error.title"),
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onThemeChanged() {
        if (updatingControls) return;
        int idx = comboTheme.getSelectedIndex();
        if (idx < 0 || idx >= themeCodes.size()) return;
        String themeKey = themeCodes.get(idx);
        if (themeKey.equals(config.getTheme())) return;
        config.setTheme(themeKey);
        theme.reload();
        applyTheme();
        repaint();
    }

    private void onFontChanged() {
        if (updatingControls) return;
        config.setFontFamily((String) comboFontFamily.getSelectedItem());
        String sizeStr = (String) comboFontSize.getSelectedItem();
        if (sizeStr != null) {
            config.setFontSize(Integer.parseInt(sizeStr));
        }
        theme.reload();
        applyTheme();
    }

    private void onLanguageChanged() {
        if (updatingControls) return;

        String langKey = getSelectedLanguageCode();

        config.setLanguage(langKey);
        lang.reload();

        refreshTexts();
        refreshTaskList();
    }

    private void refreshTaskList() {
        long start = System.nanoTime();
        taskListPanel.removeAll();

        List<Task> tasks;
        tasks = switch (activeFilter) {
            case "pending" -> service.getPendingTasks();
            case "completed" -> service.getCompletedTasks();
            default -> service.getAllTasks();
        };

        if (tasks.isEmpty()) {
            JLabel empty = new JLabel("\u2014", SwingConstants.CENTER);
            empty.setFont(theme.getFontSmall());
            empty.setForeground(theme.getTextSecondary());
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(40, 0, 0, 0));
            taskListPanel.add(empty);
        } else {
            for (Task task : tasks) {
                TaskItemPanel item = new TaskItemPanel(task);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                taskListPanel.add(item);
                taskListPanel.add(Box.createVerticalStrut(4));
            }
        }

        updateStats();
        applyThemeToTaskList();
        taskListPanel.revalidate();
        taskListPanel.repaint();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > 50) logger.debug("refreshTaskList tom\u00f3 " + elapsed + "ms (" + tasks.size() + " tareas)");
    }

    private void updateStats() {
        lblTotal.setText(lang.get("label.tasks.total") + " " + service.countTotal());
        lblPending.setText(lang.get("label.tasks.pending") + " " + service.countPending());
        lblCompleted.setText(lang.get("label.tasks.completed") + " " + service.countCompleted());

        lblTotal.setFont(theme.getFontSmall());
        lblPending.setFont(theme.getFontSmall());
        lblCompleted.setFont(theme.getFontSmall());

        lblTotal.setForeground(theme.getTextSecondary());
        lblPending.setForeground(theme.getTextSecondary());
        lblCompleted.setForeground(theme.getTextSecondary());
    }

    private void refreshTexts() {
        setTitle(lang.get("app.title"));
        headerTitle.setText(lang.get("app.header.title"));

        lblTheme.setText(lang.get("label.theme"));
        lblLang.setText(lang.get("label.language"));
        lblFont.setText(lang.get("label.font"));

        updatingControls = true;

        try {
            String currentTheme = config.getTheme();
            comboTheme.removeAllItems();
            themeCodes = theme.getAvailableThemes();
            themeCodes.forEach(code -> comboTheme.addItem(theme.getThemeDisplay(code)));
            int themeIdx = themeCodes.indexOf(currentTheme);
            if (themeIdx >= 0) comboTheme.setSelectedIndex(themeIdx);

            comboLang.setModel(new DefaultComboBoxModel<>(getLanguageOptions()));
            comboLang.setSelectedIndex(getLanguageIndex(config.getLanguage()));
        } finally {
            updatingControls = false;
        }

        comboFontFamily.setSelectedItem(theme.getFontNormal().getFamily());
        comboFontSize.setSelectedItem(String.valueOf(theme.getFontNormal().getSize()));

        btnAdd.setText(lang.get("btn.add"));
        btnDelete.setText(lang.get("btn.delete"));
        btnClearCompleted.setText(lang.get("btn.clear.completed"));
        if (btnExport != null) btnExport.setText(lang.get("btn.export"));
        if (btnImport != null) btnImport.setText(lang.get("btn.import"));

        btnFilterAll.setText(lang.get("filter.all"));
        btnFilterPending.setText(lang.get("filter.pending"));
        btnFilterCompleted.setText(lang.get("filter.completed"));

        inputField.putClientProperty("hint", lang.get("input.placeholder"));

        updateStats();
    }

    private void applyTheme() {
        long start = System.nanoTime();
        rootPanel.setBackground(theme.getBackground());

        headerPanel.setBackground(theme.getBackgroundHeader());
        headerTitle.setForeground(theme.getTextHeader());
        headerTitle.setFont(theme.getFontTitle());

        if (inputField != null) {
            inputField.setBackground(theme.getBackgroundInput());
            inputField.setForeground(theme.getTextPrimary());
            inputField.setFont(theme.getFontNormal());
            inputField.setCaretColor(theme.getTextPrimary());
            inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(6, 12, 6, 12)
            ));
        }

        if (btnAdd != null)            styleButton(btnAdd,            theme.getBackgroundButton());
        if (btnDelete != null)         styleButton(btnDelete,         theme.getBackgroundButtonDanger());
        if (btnClearCompleted != null) styleButton(btnClearCompleted, theme.getBackgroundButtonSuccess());
        if (btnExport != null)         styleButton(btnExport,         theme.getBackgroundButton());
        if (btnImport != null)         styleButton(btnImport,         theme.getBackgroundButton());

        styleFilterButton(btnFilterAll,       "all");
        styleFilterButton(btnFilterPending,   "pending");
        styleFilterButton(btnFilterCompleted, "completed");

        if (lblTheme != null) styleLabel(lblTheme);
        if (lblLang  != null) styleLabel(lblLang);
        if (lblFont  != null) styleLabel(lblFont);

        if (comboTheme != null) styleCombo(comboTheme);
        if (comboLang  != null) styleCombo(comboLang);
        if (comboFontFamily != null) styleCombo(comboFontFamily);
        if (comboFontSize   != null) styleCombo(comboFontSize);

        if (filterPanel != null) filterPanel.setBackground(theme.getBackground());

        if (scrollPane != null) {
            scrollPane.setBackground(theme.getBackground());
            scrollPane.getViewport().setBackground(theme.getBackground());
        }

        applyThemeToTaskList();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > 50) logger.debug("applyTheme tom\u00f3 " + elapsed + "ms");
    }

    private void applyThemeToTaskList() {
        if (taskListPanel == null) return;
        taskListPanel.setBackground(theme.getBackground());
        for (Component c : taskListPanel.getComponents()) {
            if (c instanceof TaskItemPanel taskItemPanel) {
                taskItemPanel.applyTheme();
            }
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(theme.getTextButton());
        btn.setFont(theme.getFontNormal());
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
    }

    private void styleFilterButton(JButton btn, String filter) {
        if (btn == null) return;
        boolean active = activeFilter.equals(filter);
        btn.setBackground(active ? theme.getBackgroundButton() : theme.getBackgroundPanel());
        btn.setForeground(active ? theme.getTextButton() : theme.getTextPrimary());
        btn.setFont(active ? theme.getFontBold() : theme.getFontNormal());
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.getBorder(), 1, true),
            new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
    }

    private void styleLabel(JLabel label) {
        label.setForeground(theme.getTextPrimary());
        label.setFont(theme.getFontNormal());
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(theme.getBackgroundInput());
        combo.setForeground(theme.getTextPrimary());
        combo.setFont(theme.getFontSmall());
    }

    private JButton createButton(String text, String type) {
        JButton btn = new JButton(text);
        Color bg;
        bg = switch (type) {
            case "danger" -> theme.getBackgroundButtonDanger();
            case "success" -> theme.getBackgroundButtonSuccess();
            default -> theme.getBackgroundButton();
        };
        styleButton(btn, bg);
        return btn;
    }

    private TaskItemPanel getSelectedTaskPanel() {
        for (Component c : taskListPanel.getComponents()) {
            if (c instanceof TaskItemPanel && ((TaskItemPanel) c).isSelected()) {
                return (TaskItemPanel) c;
            }
        }
        return null;
    }

    private final class TaskItemPanel extends JPanel {

        private final Task task;
        private boolean selected = false;

        private final JCheckBox checkbox;
        private final JLabel    titleLabel;

        TaskItemPanel(Task t) {
            this.task = t;
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(8, 14, 8, 14)
            ));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            checkbox = new JCheckBox();
            checkbox.setSelected(task.isCompleted());
            checkbox.setOpaque(false);
            checkbox.addActionListener(e -> {
                logger.trace("Clic en checkbox de tarea (id=" + task.getId() + ")");

                boolean wasCompleted = task.isCompleted();

                service.toggleComplete(task.getId());

                if (!wasCompleted) {
                    logger.info(lang.format("msg.task.completed", task.getTitle()));
                }

                refreshTaskList();
            });

            titleLabel = new JLabel(task.getTitle());

            add(checkbox,   BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    logger.trace("Tarea seleccionada (id=" + task.getId() + ")");
                    for (Component c : taskListPanel.getComponents()) {
                        if (c instanceof TaskItemPanel && c != TaskItemPanel.this) {
                            ((TaskItemPanel) c).setSelected(false);
                        }
                    }
                    setSelected(!selected);
                }
            });

            applyTheme();
        }

        void applyTheme() {
            Color bg = task.isCompleted()
                ? theme.getBackgroundItemCompleted()
                : (selected ? theme.getBackgroundInput() : theme.getBackgroundItem());
            setBackground(bg);

            titleLabel.setFont(task.isCompleted()
                ? new Font(theme.getFontNormal().getFamily(), Font.ITALIC, theme.getFontNormal().getSize())
                : theme.getFontNormal());
            titleLabel.setForeground(task.isCompleted()
                ? theme.getTextCompleted()
                : theme.getTextPrimary());
            checkbox.setBackground(bg);
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            applyTheme();
            repaint();
        }

        boolean isSelected() { return selected; }
        String  getTaskId()  { return task.getId(); }
        String  getTaskTitle(){ return task.getTitle(); }
    }
}
