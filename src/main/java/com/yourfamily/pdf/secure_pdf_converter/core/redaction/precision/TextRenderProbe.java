package com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;

public final class TextRenderProbe extends PDFTextStripper {

    private final List<RedactionHit> hits = new ArrayList<>();
    private final RedactionPlan plan;

    private int page = -1;
    private int charCounter = 0;

    public TextRenderProbe(RedactionPlan plan) throws IOException {
        this.plan = plan;
        setSortByPosition(true);
    }

    @Override
    protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) {
        this.page++;
        this.charCounter = 0;
    }

    @Override
    protected void processTextPosition(TextPosition text) {

        // Only inspect the target page
        if (page != plan.pageIndex()) {
            charCounter++;
            return;
        }

        float x = text.getXDirAdj();
        float y = text.getYDirAdj();
        float w = text.getWidthDirAdj();
        float h = text.getHeightDir();

        boolean intersects =
                x + w >= plan.pdfX() &&
                x <= plan.pdfX() + plan.pdfWidth() &&
                y + h >= plan.pdfY() &&
                y <= plan.pdfY() + plan.pdfHeight();

        if (intersects) {
            hits.add(new RedactionHit(
                    page,
                    charCounter,   // character index (tracked manually)
                    charCounter
            ));
        }

        charCounter++;
    }

    public static List<RedactionHit> collect(PDDocument doc, RedactionPlan plan) {
        try {
            TextRenderProbe probe = new TextRenderProbe(plan);
            probe.getText(doc);
            return probe.hits;
        } catch (IOException e) {
            throw new IllegalStateException("Render probe failed", e);
        }
    }
}
