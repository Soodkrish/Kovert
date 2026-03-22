package com.yourfamily.pdf.secure_pdf_converter.core.redaction.engine;

import com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class PdfTextEngine {

    public List<OcrWord> extractWords(PDDocument doc, int pageIndex) throws Exception {

        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);

        // whole page region
        Rectangle rect = new Rectangle(0, 0, 2000, 2000);
        stripper.addRegion("page", rect);

        stripper.extractRegions(doc.getPage(pageIndex));

        String text = stripper.getTextForRegion("page");

        if(text == null || text.isEmpty()){
            throw new RuntimeException("No text found");
        }

        List<OcrWord> words = new ArrayList<>();

        String[] split = text.split("\\s+");

        // ⚠️ TEMP: no coordinates (fallback behavior)
        // This still speeds up detection logic

        for(String w : split){
            words.add(new OcrWord(w, 0, 0, 0, 0));
        }

        return words;
    }
}