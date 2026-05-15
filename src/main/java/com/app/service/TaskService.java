package com.app.service;

import com.app.logging.AppLogger;
import com.app.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskService {

    private static TaskService instance;
    private final AppLogger logger = AppLogger.getInstance();
    private final List<Task> tasks = new ArrayList<>();

    private TaskService() {}

    public static synchronized TaskService getInstance() {
        if (instance == null) {
            instance = new TaskService();
        }
        return instance;
    }

    public Task addTask(String title) {
        if (title == null || title.trim().isEmpty()) {
            logger.warning("[DATA] Intento de crear tarea con t\u00edtulo vac\u00edo.");
            throw new IllegalArgumentException("El t\u00edtulo de la tarea no puede estar vac\u00edo.");
        }

        Task task = new Task(title.trim());
        tasks.add(task);
        logger.logDataOperation(String.format("Tarea creada: \"%s\" (id=%s)", task.getTitle(), task.getId()));
        return task;
    }

    public void toggleComplete(String taskId) {
        Task task = findById(taskId);
        if (task == null) {
            logger.warning("[DATA] Intento de completar tarea inexistente (id=" + taskId + ")");
            return;
        }
        task.setCompleted(!task.isCompleted());
        String estado = task.isCompleted() ? "completada" : "reabierta";
        logger.logDataOperation(String.format("Tarea %s: \"%s\" (id=%s)", estado, task.getTitle(), task.getId()));
    }

    public boolean deleteTask(String taskId) {
        Task task = findById(taskId);
        if (task == null) {
            logger.warning("[DATA] Intento de eliminar tarea inexistente (id=" + taskId + ")");
            return false;
        }
        tasks.remove(task);
        logger.logDataOperation(String.format("Tarea eliminada: \"%s\" (id=%s)", task.getTitle(), task.getId()));
        return true;
    }

    public int clearCompleted() {
        List<Task> completadas = tasks.stream()
            .filter(Task::isCompleted)
            .collect(Collectors.toList());

        tasks.removeAll(completadas);
        logger.logDataOperation(String.format("Limpieza de tareas completadas: %d eliminadas.", completadas.size()));
        return completadas.size();
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getPendingTasks() {
        return tasks.stream()
            .filter(t -> !t.isCompleted())
            .collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        return tasks.stream()
            .filter(Task::isCompleted)
            .collect(Collectors.toList());
    }

    public int countTotal()     { return tasks.size(); }
    public int countPending()   { return (int) tasks.stream().filter(t -> !t.isCompleted()).count(); }
    public int countCompleted() { return (int) tasks.stream().filter(Task::isCompleted).count(); }

    private Task findById(String id) {
        return tasks.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
