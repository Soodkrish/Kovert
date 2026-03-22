package com.yourfamily.pdf.secure_pdf_converter.core.redaction;

public record RedactionPlan(
        int pageIndex,
        double pdfX,
        double pdfY,
        double pdfWidth,
        double pdfHeight,
        ShapeType shapeType
) {

    public enum ShapeType {
        RECTANGLE,
        ELLIPSE,
        PATH
    }
}