package com.noviui.treasuredungeon.editor.templates;

import java.io.File;
import java.util.Objects;

/**
 * Represents a dungeon template
 */
public class DungeonTemplate {
    
    private final String name;
    private final String description;
    private final String author;
    private final String difficulty;
    private final File file;
    
    public DungeonTemplate(String name, String description, String author, String difficulty, File file) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.difficulty = difficulty;
        this.file = file;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public File getFile() {
        return file;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DungeonTemplate that = (DungeonTemplate) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "DungeonTemplate{" +
                "name='" + name + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}