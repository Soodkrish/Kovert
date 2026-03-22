package com.yourfamily.pdf.secure_pdf_converter.core.redaction.word;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import com.yourfamily.pdf.secure_pdf_converter.LoadedDocument;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;

public class WordRedactionEngine extends PDFTextStripper {

    private final Set<String> targetWords = new HashSet<>();
    private final List<RedactionPlan> plans = new ArrayList<>();

    private int pageRotation = 0;
    private float pageWidth;
    private float pageHeight;

    private int currentPage = -1;

    public WordRedactionEngine(Collection<String> words) throws IOException {

        setSortByPosition(true);

        for (String w : words) {
            if (w != null && !w.isBlank()) {
                targetWords.add(w.trim().toLowerCase());
            }
        }
    }

    public List<RedactionPlan> findWords(LoadedDocument doc) throws IOException {

        plans.clear();
        currentPage = -1;

        getText(doc.forRenderingOnly());

        return plans;
    }

    @Override
    protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) {

        currentPage++;

        pageRotation = page.getRotation();

        var box = page.getCropBox();
        pageWidth = box.getWidth();
        pageHeight = box.getHeight();
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {

        if (positions == null || positions.isEmpty())
            return;

        StringBuilder word = new StringBuilder();
        List<TextPosition> wordPositions = new ArrayList<>();

        for (int i = 0; i < positions.size(); i++) {

            TextPosition tp = positions.get(i);
            String ch = tp.getUnicode();

            if (ch == null || ch.trim().isEmpty()) {

                processWord(word, wordPositions);

                word.setLength(0);
                wordPositions.clear();
                continue;
            }

            word.append(ch);
            wordPositions.add(tp);
        }

        // process last word
        processWord(word, wordPositions);
    }

    private void processWord(StringBuilder word, List<TextPosition> positions) {

        if (word.length() == 0)
            return;

        String cleaned = word.toString()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        if (cleaned.isEmpty())
            return;

        if (!targetWords.contains(cleaned))
            return;

        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        for (TextPosition tp : positions) {

        	float x = tp.getTextMatrix().getTranslateX();
        	float y = tp.getTextMatrix().getTranslateY();
        	y = pageHeight - y;
            float w = tp.getWidthDirAdj();
            float h = tp.getHeightDir();

            float finalX = x;
            float finalY = y;

            // 🔥 ROTATION SAFE COORDINATES
            switch (pageRotation) {

                case 90 -> {
                    finalX = y;
                    finalY = pageWidth - x - w;
                }

                case 180 -> {
                    finalX = pageWidth - x - w;
                    finalY = pageHeight - y - h;
                }

                case 270 -> {
                    finalX = pageHeight - y - h;
                    finalY = x;
                }

                default -> {
                    // 0° → no change
                }
            }

            // 🔥 UPDATE BOUNDS WITH ROTATION FIX
            minX = Math.min(minX, finalX);
            maxX = Math.max(maxX, finalX + w);

            minY = Math.min(minY, finalY);
            maxY = Math.max(maxY, finalY + h);
        }

        float padding = 1.5f;

        plans.add(new RedactionPlan(
                currentPage,
                minX - padding,
                minY - padding,
                (maxX - minX) + padding * 2,
                (maxY - minY) + padding * 2,
                RedactionPlan.ShapeType.RECTANGLE
        ));
    }
}