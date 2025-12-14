package com.kocaeli.graphcite.model;

import java.util.ArrayList;
import java.util.List;

public class Makale {
    private String id;
    private String title;
    private String doi;
    private int year;
    private List<String> authors;
    private List<String> referencedWorkIds; // Atıf yapılan makalelerin ID'leri
    private int citationCount; // Kaç kişi bu makaleye atıf yaptı (Hesaplanacak)

    public Makale() {
        this.authors = new ArrayList<>();
        this.referencedWorkIds = new ArrayList<>();
        this.citationCount = 0;
    }

    // --- Getter ve Setter Metodları ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public List<String> getReferencedWorkIds() { return referencedWorkIds; }
    public void setReferencedWorkIds(List<String> referencedWorkIds) { this.referencedWorkIds = referencedWorkIds; }

    public int getCitationCount() { return citationCount; }
    public void setCitationCount(int citationCount) { this.citationCount = citationCount; }

    // Alınan atıf sayısını 1 artırır
    public void incrementCitationCount() {
        this.citationCount++;
    }

    @Override
    public String toString() {
        return "Makale{" + "id='" + id + '\'' + ", title='" + title + '\'' + '}';
    }
}