package com.yourfamily.pdf.secure_pdf_converter.core.redaction;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.yourfamily.pdf.secure_pdf_converter.LoadedDocument;

public final class RedactionPlanner {

    public RedactionPlan plan(LoadedDocument doc, RedactionRequest req, RedactionPlan.ShapeType shapeType) {

        PDPage page =
                doc.getPage(req.pageIndex())
                        .forPlanningOnly();

        PDRectangle mediaBox = page.getCropBox();

        double scaleX = mediaBox.getWidth() / req.screenPageWidth();
        double scaleY = mediaBox.getHeight() / req.screenPageHeight();

        double pdfX = req.screenX() * scaleX;
        double pdfHeight = req.screenHeight() * scaleY;

        // PDF origin is bottom-left, UI is top-left
        double pdfY = mediaBox.getHeight()
                - ((req.screenY() + req.screenHeight()) * scaleY);

        double pdfWidth = req.screenWidth() * scaleX;

        return new RedactionPlan(
                req.pageIndex(),
                pdfX,
                pdfY,
                pdfWidth,
                pdfHeight,
                shapeType
        );
    }
}