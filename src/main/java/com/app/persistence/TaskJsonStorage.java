package com.app.persistence;

import com.app.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Serializa y deserializa la lista de tareas en un único archivo JSON UTF-8.
 */
public final class TaskJsonStorage {

    public static final int FORMAT_VERSION = 1;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<Task> load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new ArrayList<>();
        }
        String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }

        Object root;
        try {
            root = JsonMin.parse(raw);
        } catch (RuntimeException e) {
            throw new IOException("JSON inválido en " + path + ": " + e.getMessage(), e);
        }

        if (!(root instanceof Map)) {
            throw new IOException("La raíz del JSON debe ser un objeto");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;

        Object fmt = map.get("format");
        if (fmt instanceof Number fn && fn.intValue() > FORMAT_VERSION) {
            throw new IOException("Versión de formato no soportada: " + fmt);
        }

        Object arr = map.get("tasks");
        if (arr == null) {
            return new ArrayList<>();
        }
        if (!(arr instanceof List<?> list)) {
            throw new IOException("Campo 'tasks' debe ser un array");
        }

        List<Task> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Task task = taskFromJsonObject((Map<String, Object>) o);
            if (task != null) {
                out.add(task);
            }
        }
        return out;
    }

    public void save(Path path, List<Task> tasks) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(tasks);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String json = toJson(tasks);
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private static Task taskFromJsonObject(Map<String, Object> o) {
        try {
            String id = asNonBlankString(o.get("id"));
            String title = asNonBlankString(o.get("title"));
            if (id == null || title == null) {
                return null;
            }
            boolean completed = Boolean.TRUE.equals(o.get("completed"));

            Object ca = o.get("createdAt");
            if (!(ca instanceof String)) {
                return null;
            }
            LocalDateTime created;
            try {
                created = LocalDateTime.parse((String) ca, ISO);
            } catch (DateTimeParseException e) {
                return null;
            }
            return Task.restore(id, title, completed, created);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String asNonBlankString(Object v) {
        if (!(v instanceof String s)) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    public String toJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"format\":").append(FORMAT_VERSION).append(",\"tasks\":[");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Task t = tasks.get(i);
            sb.append("{\"id\":").append(jsonString(t.getId()))
                .append(",\"title\":").append(jsonString(t.getTitle()))
                .append(",\"completed\":").append(t.isCompleted() ? "true" : "false")
                .append(",\"createdAt\":").append(jsonString(t.getCreatedAt().format(ISO)))
                .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
