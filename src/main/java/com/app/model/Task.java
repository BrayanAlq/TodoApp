package com.app.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Task {

    private final String id;
    private String title;
    private boolean completed;
    private final LocalDateTime createdAt;

    public Task(String title) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.title = title;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Reconstruye una tarea desde persistencia (archivo JSON) conservando id y fecha de creación.
     */
    public static Task restore(String id, String title, boolean completed, LocalDateTime createdAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
        return new Task(id, title, completed, createdAt);
    }

    private Task(String id, String title, boolean completed, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public String getId()                  { return id; }
    public String getTitle()               { return title; }
    public void   setTitle(String title)   { this.title = title; }
    public boolean isCompleted()           { return completed; }
    public void   setCompleted(boolean c)  { this.completed = c; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    @Override
    public String toString() {
        return String.format("Task{id='%s', title='%s', completed=%b}", id, title, completed);
    }
}
