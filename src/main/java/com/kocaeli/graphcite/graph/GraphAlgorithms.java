package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import java.util.*;

public class GraphAlgorithms {
    private List<Makale> makaleler;
    private Map<String, Makale> makaleMap;

    public GraphAlgorithms(List<Makale> makaleler) {
        this.makaleler = makaleler;
        this.makaleMap = new HashMap<>();

        for (Makale m : makaleler) {
            makaleMap.put(m.getId(), m);
        }

        calculateAllCitationCounts();
    }

    /**
     * Tüm makalelerin kaç kez referans ALDIĞINI (In-Degree) hesaplar.
     */
    private void calculateAllCitationCounts() {
        for (Makale m : makaleler) {
            m.setCitationCount(0);
        }

        for (Makale m : makaleler) {
            for (String refId : m.getReferencedWorkIds()) {
                Makale atifAlan = makaleMap.get(refId);
                if (atifAlan != null) {
                    atifAlan.incrementCitationCount();
                }
            }
        }
    }

    /**
     * H-Index hesaplar.
     */
    public int calculateHIndex(String targetId) {
        List<Integer> citationCountsOfCiters = new ArrayList<>();

        for (Makale m : makaleler) {
            if (m.getReferencedWorkIds().contains(targetId)) {
                citationCountsOfCiters.add(m.getCitationCount());
            }
        }

        Collections.sort(citationCountsOfCiters, Collections.reverseOrder());

        int hIndex = 0;
        for (int i = 0; i < citationCountsOfCiters.size(); i++) {
            if (citationCountsOfCiters.get(i) >= (i + 1)) {
                hIndex = i + 1;
            } else {
                break;
            }
        }
        return hIndex;
    }

    /**
     * H-Core kümesini döndürür.
     */
    public List<Makale> getHCore(String targetId) {
        List<Makale> hCoreList = new ArrayList<>();
        int hIndex = calculateHIndex(targetId);

        List<Makale> citers = new ArrayList<>();
        for (Makale m : makaleler) {
            if (m.getReferencedWorkIds().contains(targetId)) {
                citers.add(m);
            }
        }

        citers.sort((m1, m2) ->
                Integer.compare(m2.getCitationCount(), m1.getCitationCount()));

        for (int i = 0; i < hIndex && i < citers.size(); i++) {
            hCoreList.add(citers.get(i));
        }

        return hCoreList;
    }

    /**
     * ✅ EKSİK OLAN METOD – H-MEDIAN
     * PDF Tanımı:
     * h-core içindeki makalelerin atıf sayılarının ortanca (median) değeri.
     */
    public int calculateHMedian(String targetId) {
        List<Makale> hCore = getHCore(targetId);

        if (hCore.isEmpty()) return 0;

        List<Integer> citationCounts = new ArrayList<>();
        for (Makale m : hCore) {
            citationCounts.add(m.getCitationCount());
        }

        Collections.sort(citationCounts);

        int size = citationCounts.size();
        if (size % 2 == 1) {
            return citationCounts.get(size / 2);
        } else {
            int mid1 = citationCounts.get(size / 2 - 1);
            int mid2 = citationCounts.get(size / 2);
            return (mid1 + mid2) / 2;
        }
    }

    public Makale getMakale(String id) {
        return makaleMap.get(id);
    }

    // ---------------- BETWEENNESS CENTRALITY ----------------

    public Map<String, Double> calculateBetweennessCentrality() {
        Map<String, List<String>> adjList = new HashMap<>();

        for (Makale m : makaleler) {
            adjList.putIfAbsent(m.getId(), new ArrayList<>());
            for (String refId : m.getReferencedWorkIds()) {
                if (makaleMap.containsKey(refId)) {
                    adjList.putIfAbsent(refId, new ArrayList<>());
                    if (!adjList.get(m.getId()).contains(refId)) adjList.get(m.getId()).add(refId);
                    if (!adjList.get(refId).contains(m.getId())) adjList.get(refId).add(m.getId());
                }
            }
        }

        Map<String, Map<String, Integer>> allDistances = new HashMap<>();
        List<String> nodeIds = new ArrayList<>(makaleMap.keySet());

        for (String id : nodeIds) {
            allDistances.put(id, runBFS(id, adjList));
        }

        Map<String, Double> centralityScores = new HashMap<>();
        for (String id : nodeIds) centralityScores.put(id, 0.0);

        for (int i = 0; i < nodeIds.size(); i++) {
            for (int j = i + 1; j < nodeIds.size(); j++) {
                String s = nodeIds.get(i);
                String t = nodeIds.get(j);

                if (allDistances.get(s).containsKey(t)) {
                    int distST = allDistances.get(s).get(t);

                    for (String v : nodeIds) {
                        if (v.equals(s) || v.equals(t)) continue;

                        if (allDistances.get(s).containsKey(v) &&
                                allDistances.get(v).containsKey(t)) {

                            int distSV = allDistances.get(s).get(v);
                            int distVT = allDistances.get(v).get(t);

                            if (distSV + distVT == distST) {
                                centralityScores.put(v,
                                        centralityScores.get(v) + 1.0);
                            }
                        }
                    }
                }
            }
        }
        return centralityScores;
    }

    private Map<String, Integer> runBFS(String startNode,
                                        Map<String, List<String>> adjList) {
        Map<String, Integer> distances = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        distances.put(startNode, 0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDist = distances.get(current);

            if (adjList.containsKey(current)) {
                for (String neighbor : adjList.get(current)) {
                    if (!distances.containsKey(neighbor)) {
                        distances.put(neighbor, currentDist + 1);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return distances;
    }

    // ---------------- K-CORE ----------------

    public List<Makale> runKCoreDecomposition(int k) {
        Map<String, Integer> currentDegrees = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        for (Makale m : makaleler) {
            adjList.putIfAbsent(m.getId(), new ArrayList<>());
            for (String refId : m.getReferencedWorkIds()) {
                if (makaleMap.containsKey(refId)) {
                    adjList.get(m.getId()).add(refId);
                    adjList.putIfAbsent(refId, new ArrayList<>());
                    adjList.get(refId).add(m.getId());
                }
            }
        }

        for (String id : makaleMap.keySet()) {
            currentDegrees.put(id,
                    adjList.containsKey(id) ? adjList.get(id).size() : 0);
        }

        boolean changed = true;
        Set<String> removed = new HashSet<>();

        while (changed) {
            changed = false;
            for (String id : makaleMap.keySet()) {
                if (removed.contains(id)) continue;
                if (currentDegrees.get(id) < k) {
                    removed.add(id);
                    changed = true;
                    for (String nb : adjList.getOrDefault(id, List.of())) {
                        if (!removed.contains(nb)) {
                            currentDegrees.put(nb,
                                    currentDegrees.get(nb) - 1);
                        }
                    }
                }
            }
        }

        List<Makale> result = new ArrayList<>();
        for (String id : makaleMap.keySet()) {
            if (!removed.contains(id)) {
                result.add(makaleMap.get(id));
            }
        }
        return result;
    }
}
