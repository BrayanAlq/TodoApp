package com.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.app.logging.AppLogger;
import com.app.ui.MainWindow;

public class Main {


    public static void main(String[] args) {
        AppLogger logger = AppLogger.getInstance();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.logError("Excepci\u00f3n no controlada en hilo: " + thread.getName(), throwable);
        });

        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                logger.warning("No se pudo aplicar Nimbus LookAndFeel: " + e.getMessage());
            }

            new MainWindow();
        });
    }
}
