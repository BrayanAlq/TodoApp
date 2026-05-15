package com.app.service;

import com.app.config.ConfigManager;
import com.app.logging.AppLogger;
import com.app.model.Task;
import com.app.persistence.TaskJsonStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskService {

    public enum ImportMode {
        /** Sustituye la lista actual por el contenido del archivo. */
        REPLACE,
        /** Añade tareas del archivo; si hay ids duplicados se generan nuevos. */
        MERGE
    }

    private static TaskService instance;
    private final AppLogger logger = AppLogger.getInstance();
    private final List<Task> tasks = new ArrayList<>();
    private final TaskJsonStorage storage = new TaskJsonStorage();
    private final Path dataPath;
    private volatile boolean shutdownHookRegistered;

    private TaskService() {
        this.dataPath = Paths.get(ConfigManager.getInstance().getTasksDataPath());
        loadFromDisk();
        registerShutdownHook();
    }

    public static synchronized TaskService getInstance() {
        if (instance == null) {
            instance = new TaskService();
        }
        return instance;
    }

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        synchronized (this) {
            if (shutdownHookRegistered) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    persist();
                } catch (Exception e) {
                    logger.logError("No se pudo guardar tareas al cerrar la aplicación", e);
                }
            }, "task-persist-shutdown"));
            shutdownHookRegistered = true;
        }
    }

    private void loadFromDisk() {
        try {
            List<Task> loaded = storage.load(dataPath);
            synchronized (tasks) {
                tasks.clear();
                tasks.addAll(loaded);
            }
            logger.info(String.format("[DATA] Tareas cargadas desde %s (%d)", dataPath.toAbsolutePath(), loaded.size()));
        } catch (IOException e) {
            logger.logError("No se pudieron cargar las tareas; se inicia con lista vacía.", e);
        }
    }

    private void persist() {
        List<Task> snapshot;
        synchronized (tasks) {
            snapshot = new ArrayList<>(tasks);
        }
        try {
            storage.save(dataPath, snapshot);
        } catch (IOException e) {
            logger.logError("Error al guardar tareas en " + dataPath.toAbsolutePath(), e);
        }
    }

    /**
     * Exporta un instantánea actual a la ruta indicada (no cambia la ruta de auto-guardado).
     */
    public void exportTo(Path path) throws IOException {
        List<Task> snapshot;
        synchronized (tasks) {
            snapshot = new ArrayList<>(tasks);
        }
        storage.save(path, snapshot);
        logger.logDataOperation(String.format("Exportadas %d tareas a %s", snapshot.size(), path.toAbsolutePath()));
    }

    /**
     * Importa tareas desde un archivo JSON generado por esta app o compatible.
     */
    public void importFrom(Path path, ImportMode mode) throws IOException {
        List<Task> incoming = storage.load(path);
        synchronized (tasks) {
            if (mode == ImportMode.REPLACE) {
                tasks.clear();
                tasks.addAll(incoming);
            } else {
                Set<String> used = tasks.stream().map(Task::getId).collect(Collectors.toCollection(HashSet::new));
                for (Task t : incoming) {
                    String id = t.getId();
                    while (used.contains(id)) {
                        id = UUID.randomUUID().toString().substring(0, 8);
                    }
                    used.add(id);
                    tasks.add(Task.restore(id, t.getTitle(), t.isCompleted(), t.getCreatedAt()));
                }
            }
        }
        persist();
        logger.logDataOperation(String.format(
            "Importación (%s): %d tareas desde %s",
            mode.name(),
            incoming.size(),
            path.toAbsolutePath()
        ));
    }

    public Task addTask(String title) {
        if (title == null || title.trim().isEmpty()) {
            logger.warning("[DATA] Intento de crear tarea con t\u00edtulo vac\u00edo.");
            throw new IllegalArgumentException("El t\u00edtulo de la tarea no puede estar vac\u00edo.");
        }

        Task task = new Task(title.trim());
        synchronized (tasks) {
            tasks.add(task);
        }
        persist();
        logger.logDataOperation(String.format("Tarea creada: \"%s\" (id=%s)", task.getTitle(), task.getId()));
        return task;
    }

    public void toggleComplete(String taskId) {
        Task task;
        synchronized (tasks) {
            task = findById(taskId);
            if (task == null) {
                logger.warning("[DATA] Intento de completar tarea inexistente (id=" + taskId + ")");
                return;
            }
            task.setCompleted(!task.isCompleted());
        }
        persist();
        String estado = task.isCompleted() ? "completada" : "reabierta";
        logger.logDataOperation(String.format("Tarea %s: \"%s\" (id=%s)", estado, task.getTitle(), task.getId()));
    }

    public boolean deleteTask(String taskId) {
        Task task;
        synchronized (tasks) {
            task = findById(taskId);
            if (task == null) {
                logger.warning("[DATA] Intento de eliminar tarea inexistente (id=" + taskId + ")");
                return false;
            }
            tasks.remove(task);
        }
        persist();
        logger.logDataOperation(String.format("Tarea eliminada: \"%s\" (id=%s)", task.getTitle(), task.getId()));
        return true;
    }

    public int clearCompleted() {
        int n;
        synchronized (tasks) {
            List<Task> completadas = tasks.stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
            tasks.removeAll(completadas);
            n = completadas.size();
        }
        persist();
        logger.logDataOperation(String.format("Limpieza de tareas completadas: %d eliminadas.", n));
        return n;
    }

    public List<Task> getAllTasks() {
        synchronized (tasks) {
            return new ArrayList<>(tasks);
        }
    }

    public List<Task> getPendingTasks() {
        synchronized (tasks) {
            return tasks.stream()
                .filter(t -> !t.isCompleted())
                .collect(Collectors.toList());
        }
    }

    public List<Task> getCompletedTasks() {
        synchronized (tasks) {
            return tasks.stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
        }
    }

    public int countTotal() {
        synchronized (tasks) {
            return tasks.size();
        }
    }

    public int countPending() {
        synchronized (tasks) {
            return (int) tasks.stream().filter(t -> !t.isCompleted()).count();
        }
    }

    public int countCompleted() {
        synchronized (tasks) {
            return (int) tasks.stream().filter(Task::isCompleted).count();
        }
    }

    private Task findById(String id) {
        return tasks.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
