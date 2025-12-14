package com.kocaeli.graphcite.ui;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

public class Main {
    public static void main(String[] args) {
        // GraphStream'in Swing arayüzünü kullanması için ayar
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("TestGraflari");

        // CSS ile stil verelim (Görselleştirme testi)
        graph.setAttribute("ui.stylesheet", "node { fill-color: red; size: 30px; text-size: 20; }");

        // Bir düğüm ekleyelim
        graph.addNode("A").setAttribute("ui.label", "Merhaba Prolab!");
        graph.addNode("B").setAttribute("ui.label", "Onur & Dilay");
        graph.addEdge("AB", "A", "B");

        // Ekrana bastır
        graph.display();
    }
}