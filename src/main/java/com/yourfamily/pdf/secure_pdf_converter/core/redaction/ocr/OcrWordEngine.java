package com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr;

import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.Word;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.yourfamily.pdf.secure_pdf_converter.core.tools.AppPaths;
import com.yourfamily.pdf.secure_pdf_converter.core.tools.TesseractFactory;

public class OcrWordEngine {

    private final ITesseract tesseract;

    public OcrWordEngine() {
        tesseract = TesseractFactory.create();
    }

    public List<OcrWord> extractWords(BufferedImage image) throws Exception {

        List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

        List<OcrWord> result = new ArrayList<>();

        for (Word w : words) {

            String text = w.getText();

            if (text == null || text.isBlank())
                continue;

            var box = w.getBoundingBox();

            result.add(new OcrWord(
                    text,
                    box.x,
                    box.y,
                    box.width,
                    box.height
            ));
        }

        return result;
    }
}