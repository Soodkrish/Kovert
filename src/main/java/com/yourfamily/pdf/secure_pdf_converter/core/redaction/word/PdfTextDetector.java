package com.yourfamily.pdf.secure_pdf_converter.core.redaction.word;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfTextDetector {

    public static boolean isPageTextReliable(PDDocument doc, int pageIndex) {

        try {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);

            String text = stripper.getText(doc);

            if (text == null || text.trim().length() < 10)
                return false;

            int letters = text.replaceAll("[^A-Za-z]", "").length();

            return letters > text.length() * 0.5;

        } catch (Exception e) {
            return false;
        }
    }
}