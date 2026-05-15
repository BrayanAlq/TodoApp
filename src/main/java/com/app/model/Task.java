package com.app.model;

import java.time.LocalDateTime;
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
