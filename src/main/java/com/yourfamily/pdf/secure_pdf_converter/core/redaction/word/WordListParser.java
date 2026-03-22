package com.yourfamily.pdf.secure_pdf_converter.core.redaction.word;

import java.util.*;

public class WordListParser {

    public static List<String> parse(String input) {

        if (input == null || input.isBlank())
            return Collections.emptyList();

        String[] parts = input.split("[,\\n]");

        List<String> words = new ArrayList<>();

        for (String p : parts) {

            String cleaned = p.trim();

            if (!cleaned.isEmpty())
                words.add(cleaned);
        }

        return words;
    }
}