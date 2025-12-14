package com.kocaeli.graphcite.parser;

import com.kocaeli.graphcite.model.Makale;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonParser {

    /**
     * Verilen dosya yolundaki JSON'ı okur ve Makale listesine çevirir.
     */
    public static List<Makale> parse(String filePath) throws IOException {
        List<Makale> makaleler = new ArrayList<>();

        // 1. Dosyayı tek bir String olarak oku
        String content = Files.readString(Path.of(filePath));

        // 2. Her bir süslü parantezli { ... } bloğunu bul (Regex ile)
        // Bu regex, en dıştaki obje bloklarını yakalamaya çalışır.
        Pattern objectPattern = Pattern.compile("\\{[\\s\\S]*?\\}(?=\\s*,|\\s*\\])", Pattern.MULTILINE);
        Matcher objectMatcher = objectPattern.matcher(content);

        while (objectMatcher.find()) {
            String jsonObject = objectMatcher.group();
            // Basit bir kontrol: İçinde "id" yoksa boş bir bloktur, geç.
            if (!jsonObject.contains("\"id\"")) continue;

            Makale makale = new Makale();

            // --- ID Çekme ---
            makale.setId(extractString(jsonObject, "id"));

            // --- Title Çekme ---
            makale.setTitle(extractString(jsonObject, "title"));

            // --- DOI Çekme ---
            makale.setDoi(extractString(jsonObject, "doi"));

            // --- Year Çekme (Sayısal) ---
            makale.setYear(extractInt(jsonObject, "year"));

            // --- Authors (Liste) Çekme ---
            makale.setAuthors(extractList(jsonObject, "authors"));

            // --- Referenced Works (Liste) Çekme ---
            makale.setReferencedWorkIds(extractList(jsonObject, "referenced_works"));

            makaleler.add(makale);
        }

        return makaleler;
    }

    // Helper: "key": "value" formatındaki string değeri çeker
    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // Helper: "key": 2024 formatındaki int değeri çeker
    private static int extractInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // Helper: "key": [ "a", "b", "c" ] formatındaki listeyi çeker
    private static List<String> extractList(String json, String key) {
        List<String> list = new ArrayList<>();

        // Önce köşeli parantez içini bul: "authors": [ ... ]
        Pattern listPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher listMatcher = listPattern.matcher(json);

        if (listMatcher.find()) {
            String contentInsideBrackets = listMatcher.group(1);

            // Tırnak içindeki her şeyi yakala: "Ali Veli"
            Pattern itemPattern = Pattern.compile("\"(.*?)\"");
            Matcher itemMatcher = itemPattern.matcher(contentInsideBrackets);

            while (itemMatcher.find()) {
                list.add(itemMatcher.group(1));
            }
        }
        return list;
    }
}