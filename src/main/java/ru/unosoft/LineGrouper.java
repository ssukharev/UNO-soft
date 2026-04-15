package ru.unosoft;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LineGrouper {

    private int[] parent;
    private int[] rank;

    private int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private void union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra == rb) return;
        if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) rank[ra]++;
    }

    public void run(String inputPath) throws IOException {
        long startTime = System.currentTimeMillis();

        // Read and deduplicate lines
        List<String[]> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int maxCols = 0;

        try (BufferedReader br = Files.newBufferedReader(Path.of(inputPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!isValidLine(line)) continue;
                if (!seen.add(line)) continue;

                String[] parts = parseLine(line);
                if (parts == null) continue;
                rows.add(parts);
                if (parts.length > maxCols) maxCols = parts.length;
            }
        }

        int n = rows.size();
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        // Group using Union-Find: for each column, map value -> first row index
        for (int col = 0; col < maxCols; col++) {
            Map<String, Integer> valToRow = new HashMap<>();
            for (int i = 0; i < n; i++) {
                String[] parts = rows.get(i);
                if (col >= parts.length) continue;
                String val = parts[col];
                if (val.isEmpty()) continue;

                Integer prev = valToRow.get(val);
                if (prev != null) {
                    union(i, prev);
                } else {
                    valToRow.put(val, i);
                }
            }
        }


        //Collect groups
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(find(i), k -> new ArrayList<>()).add(i);
        }

        // Sort groups by size descending
        List<List<Integer>> sortedGroups = new ArrayList<>(groups.values());
        sortedGroups.sort((a, b) -> Integer.compare(b.size(), a.size()));

        long multiGroupCount = sortedGroups.stream().filter(g -> g.size() > 1).count();

        // output
        try (BufferedWriter bw = Files.newBufferedWriter(Path.of("output.txt"))) {
            bw.write("Количество групп с более чем одним элементом: " + multiGroupCount);
            bw.newLine();
            bw.newLine();

            int groupNum = 1;
            for (List<Integer> group : sortedGroups) {
                bw.write("Группа " + groupNum);
                bw.newLine();
                for (int idx : group) {
                    bw.write(rebuildLine(rows.get(idx)));
                    bw.newLine();
                }
                bw.newLine();
                groupNum++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Количество групп с более чем одним элементом: " + multiGroupCount);
        System.out.println("Общее количество групп: " + sortedGroups.size());
        System.out.println("Время выполнения: " + elapsed + " мс");
    }

    private static boolean isValidLine(String line) {
        if (line == null || line.isBlank()) return false;
        int i = 0;
        int len = line.length();
        while (i < len) {
            if (line.charAt(i) == '"') {
                i++;
                int closeQuote = line.indexOf('"', i);
                if (closeQuote == -1) return false;
                i = closeQuote + 1;
                if (i < len && line.charAt(i) != ';') return false;
                if (i < len) i++; 
            } else if (line.charAt(i) == ';') {
                i++; 
            } else {
                int semi = line.indexOf(';', i);
                if (semi == -1) {
                    i = len;
                } else {
                    i = semi + 1;
                }
            }
        }
        return true;
    }

    private static String[] parseLine(String line) {
        List<String> parts = new ArrayList<>();
        int i = 0;
        int len = line.length();
        while (i <= len) {
            if (i == len) {
                break;
            }
            if (line.charAt(i) == '"') {
                i++;
                int closeQuote = line.indexOf('"', i);
                parts.add(line.substring(i, closeQuote));
                i = closeQuote + 1;
                if (i < len) i++; 
            } else if (line.charAt(i) == ';') {
                parts.add("");
                i++;
            } else {
                int semi = line.indexOf(';', i);
                if (semi == -1) {
                    parts.add(line.substring(i));
                    i = len;
                } else {
                    parts.add(line.substring(i, semi));
                    i = semi + 1;
                }
            }
        }
        return parts.toArray(new String[0]);
    }

    private static String rebuildLine(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(';');
            sb.append('"').append(parts[i]).append('"');
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java -jar line-grouper.jar <path-to-file>");
            System.exit(1);
        }
        new LineGrouper().run(args[0]);
    }
}
