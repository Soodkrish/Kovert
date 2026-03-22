package com.yourfamily.pdf.secure_pdf_converter.core.redaction;

public record RedactionRequest(
        int pageIndex,
        double screenX,
        double screenY,
        double screenWidth,
        double screenHeight,
        double screenPageWidth,
        double screenPageHeight
) {}
