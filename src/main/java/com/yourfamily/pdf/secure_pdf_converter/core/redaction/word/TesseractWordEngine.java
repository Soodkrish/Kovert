package com.yourfamily.pdf.secure_pdf_converter.core.redaction.word;

import net.sourceforge.tess4j.*;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.util.*;

import com.yourfamily.pdf.secure_pdf_converter.LoadedDocument;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;

public class TesseractWordEngine {

    private final List<String> words;

    public TesseractWordEngine(List<String> words) {
        this.words = words;
    }

    public List<RedactionPlan> findWords(LoadedDocument doc) throws Exception {

        List<RedactionPlan> plans = new ArrayList<>();

        PDFRenderer renderer = new PDFRenderer(doc.forRenderingOnly());

        ITesseract tesseract = new Tesseract();

        // 🔥 IMPORTANT — CHANGE THIS PATH
        tesseract.setDatapath("C:/Users/soodk/AppData/Local/Programs/Tesseract-OCR/tessdata");

        int pageCount = doc.forRenderingOnly().getNumberOfPages();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {

            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);

            List<Word> ocrWords =
                    tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            for (Word w : ocrWords) {

                String text = w.getText().trim().toLowerCase();

                for (String target : words) {

                    if (text.equals(target.toLowerCase())) {

                        java.awt.Rectangle b = w.getBoundingBox();

                        plans.add(new RedactionPlan(
                                pageIndex,
                                b.getX(),
                                b.getY(),
                                b.getWidth(),
                                b.getHeight(),
                                RedactionPlan.ShapeType.RECTANGLE
                        ));
                    }
                }
            }
        }

        return plans;
    }
}