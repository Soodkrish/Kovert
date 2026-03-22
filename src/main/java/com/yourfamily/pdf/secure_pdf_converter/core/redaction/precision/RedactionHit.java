package com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision;

public record RedactionHit(
        int pageIndex,
        int streamIndex,
        int charIndex
) {}