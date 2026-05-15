package com.app.i18n;

import com.app.config.ConfigManager;
import com.app.logging.AppLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

public class LanguageManager {

    private static LanguageManager instance;
    private static final String I18N_DIR = "resources/i18n/";

    private final AppLogger logger = AppLogger.getInstance();

    private Properties messages = new Properties();
    private Locale currentLocale = new Locale("es");

    private LanguageManager() {
        reload();
    }

    public static synchronized LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void reload() {
        String lang = ConfigManager.getInstance().getLanguage();

        if (lang == null || lang.isBlank()) {
            lang = "es";
        }

        currentLocale = Locale.forLanguageTag(lang);

        String path = I18N_DIR + "messages_" + lang + ".properties";

        Properties newMessages = new Properties();

        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {

            newMessages.load(isr);
            messages = newMessages;

            logger.info("Idioma cargado: " + path);

        } catch (IOException e) {
            logger.logError("Error al cargar idioma: " + path, e);

            if (!"es".equals(lang)) {
                logger.warning("Intentando fallback a español...");
                loadFallback();
            }
        }
    }

    private void loadFallback() {
        String path = I18N_DIR + "messages_es.properties";

        Properties fallbackMessages = new Properties();

        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {

            fallbackMessages.load(isr);
            messages = fallbackMessages;
            currentLocale = new Locale("es");

            logger.info("Idioma fallback cargado: " + path);

        } catch (IOException e) {
            logger.logError("Error al cargar idioma fallback (es)", e);
        }
    }

    public String get(String key) {
        return messages.getProperty(key, "[" + key + "]");
    }

    public String get(String key, String defaultValue) {
        return messages.getProperty(key, defaultValue);
    }

    public String format(String key, Object... args) {
        String pattern = get(key);
        MessageFormat formatter = new MessageFormat(pattern, currentLocale);
        return formatter.format(args);
    }

    public String formatWithDefault(String key, String defaultValue, Object... args) {
        String pattern = get(key, defaultValue);
        MessageFormat formatter = new MessageFormat(pattern, currentLocale);
        return formatter.format(args);
    }
}