package com.app.config;

import com.app.logging.AppLogger;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ThemeManager {

    private static ThemeManager instance;
    private static final String THEMES_DIR = "resources/themes/";

    private final AppLogger logger = AppLogger.getInstance();
    private Properties themeProps = new Properties();
    private List<String> availableThemes;
    private Map<String, String> themeDisplayNames;

    private ThemeManager() {
        reload();
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void reload() {
        String themeName = ConfigManager.getInstance().getTheme();
        String path = THEMES_DIR + "theme-" + themeName + ".properties";

        Properties newProps = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            newProps.load(fis);
        } catch (IOException e) {
            logger.logError("Error al cargar tema: " + path, e);
            return;
        }

        ConfigManager config = ConfigManager.getInstance();
        String fontFamily = config.getFontFamily();
        if (!fontFamily.isEmpty()) {
            newProps.setProperty("font.family", fontFamily);
        }
        int fontSize = config.getFontSize();
        if (fontSize > 0) {
            newProps.setProperty("font.size.normal", String.valueOf(fontSize));
            newProps.setProperty("font.size.title", String.valueOf(fontSize + 6));
            newProps.setProperty("font.size.small", String.valueOf(fontSize - 2));
        }

        themeProps = newProps;
        logger.info("Tema cargado: " + path);
    }

    public List<String> getAvailableThemes() {
        if (availableThemes == null) {
            discoverThemes();
        }
        return availableThemes;
    }

    public String getThemeDisplay(String themeKey) {
        if (themeDisplayNames == null) {
            discoverThemes();
        }
        return themeDisplayNames.getOrDefault(themeKey, themeKey);
    }

    private void discoverThemes() {
        availableThemes = new ArrayList<>();
        themeDisplayNames = new HashMap<>();

        File dir = new File(THEMES_DIR);
        File[] files = dir.listFiles((d, name) -> name.matches("theme-(\\w+)\\.properties"));
        if (files == null || files.length == 0) {
            availableThemes.add("light");
            themeDisplayNames.put("light", "Light");
            return;
        }

        for (File f : files) {
            String themeName = f.getName().replaceAll("theme-(\\w+)\\.properties", "$1");
            Properties temp = new Properties();
            try (FileInputStream fis = new FileInputStream(f)) {
                temp.load(fis);
            } catch (IOException e) {
                continue;
            }
            String display = temp.getProperty("theme.display", themeName);
            availableThemes.add(themeName);
            themeDisplayNames.put(themeName, display);
        }

        availableThemes.sort((a, b) -> {
            String da = themeDisplayNames.get(a);
            String db = themeDisplayNames.get(b);
            return da.compareToIgnoreCase(db);
        });
    }

    public Color getBackground()             { return color("color.background", "#F5F5F5"); }
    public Color getBackgroundPanel()        { return color("color.background.panel", "#FFFFFF"); }
    public Color getBackgroundInput()        { return color("color.background.input", "#FFFFFF"); }
    public Color getBackgroundButton()       { return color("color.background.button", "#4A90D9"); }
    public Color getBackgroundButtonDanger() { return color("color.background.button.danger", "#E05C5C"); }
    public Color getBackgroundButtonSuccess(){ return color("color.background.button.success", "#5CB85C"); }
    public Color getBackgroundItem()         { return color("color.background.item", "#FFFFFF"); }
    public Color getBackgroundItemCompleted(){ return color("color.background.item.completed", "#F0F0F0"); }
    public Color getBackgroundHeader()       { return color("color.background.header", "#4A90D9"); }

    public Color getTextPrimary()     { return color("color.text.primary", "#1A1A2E"); }
    public Color getTextSecondary()   { return color("color.text.secondary", "#555555"); }
    public Color getTextButton()      { return color("color.text.button", "#FFFFFF"); }
    public Color getTextHeader()      { return color("color.text.header", "#FFFFFF"); }
    public Color getTextCompleted()   { return color("color.text.completed", "#999999"); }
    public Color getTextPlaceholder() { return color("color.text.placeholder", "#AAAAAA"); }

    public Color getBorder()          { return color("color.border", "#DDDDDD"); }
    public Color getBorderFocus()     { return color("color.border.focus", "#4A90D9"); }

    public Font getFontNormal() {
        return new Font(themeProps.getProperty("font.family", "Segoe UI"), Font.PLAIN, intProp("font.size.normal", 14));
    }

    public Font getFontTitle() {
        return new Font(themeProps.getProperty("font.family", "Segoe UI"), Font.BOLD, intProp("font.size.title", 20));
    }

    public Font getFontSmall() {
        return new Font(themeProps.getProperty("font.family", "Segoe UI"), Font.PLAIN, intProp("font.size.small", 12));
    }

    public Font getFontBold() {
        return new Font(themeProps.getProperty("font.family", "Segoe UI"), Font.BOLD, intProp("font.size.normal", 14));
    }

    private Color color(String key, String fallback) {
        String hex = themeProps.getProperty(key, fallback);
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            logger.warning("Color inv\u00e1lido para clave '" + key + "': " + hex);
            return Color.decode(fallback);
        }
    }

    private int intProp(String key, int fallback) {
        try {
            return Integer.parseInt(themeProps.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
