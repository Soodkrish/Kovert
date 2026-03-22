package com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr;

public record OcrWord(
        String text,
        int x,
        int y,
        int width,
        int height
) {}