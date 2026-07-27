package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public final class Book extends Item {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String readingMemoryId;

    public String getReadingMemoryId() {
        return readingMemoryId;
    }

    public void setReadingMemoryId(String readingMemoryId) {
        this.readingMemoryId = readingMemoryId;
    }
}
