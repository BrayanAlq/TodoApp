package com.app.config;

import com.app.logging.AppLogger;

import java.io.*;
import java.util.Properties;

public class ConfigManager {

    private static ConfigManager instance;
    private static final String CONFIG_PATH = "resources/config.properties";

    private final Properties props = new Properties();
    private final AppLogger logger = AppLogger.getInstance();

    private static final String DEFAULT_LANGUAGE = "es";
    private static final String DEFAULT_THEME    = "light";

    private ConfigManager() {
        load();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void load() {
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
            logger.info("Configuraci\u00f3n cargada desde: " + CONFIG_PATH);
        } catch (FileNotFoundException e) {
            logger.warning("config.properties no encontrado. Usando valores por defecto.");
            setDefaults();
        } catch (IOException e) {
            logger.logError("Error al leer config.properties", e);
            setDefaults();
        }
    }

    public void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
            props.store(fos, "Configuraci\u00f3n de la app - Gestor de Tareas");
            logger.info("Configuraci\u00f3n guardada en: " + CONFIG_PATH);
        } catch (IOException e) {
            logger.logError("Error al guardar config.properties", e);
        }
    }

    private void setDefaults() {
        props.setProperty("app.language", DEFAULT_LANGUAGE);
        props.setProperty("app.theme",    DEFAULT_THEME);
        props.setProperty("app.version",  "1.0.0");
    }

    public String getLanguage() {
        return props.getProperty("app.language", DEFAULT_LANGUAGE);
    }

    public void setLanguage(String lang) {
        String previous = getLanguage();
        props.setProperty("app.language", lang);
        save();
        logger.logConfigChange(String.format("Idioma cambiado: %s \u2192 %s", previous, lang));
    }

    public String getTheme() {
        return props.getProperty("app.theme", DEFAULT_THEME);
    }

    public void setTheme(String theme) {
        String previous = getTheme();
        props.setProperty("app.theme", theme);
        save();
        logger.logConfigChange(String.format("Tema cambiado: %s \u2192 %s", previous, theme));
    }

    public String getVersion() {
        return props.getProperty("app.version", "1.0.0");
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
