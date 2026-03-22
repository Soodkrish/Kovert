package com.yourfamily.pdf.secure_pdf_converter;

import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public final class LoadedPage {

    private final int index;
    private final PDPage page;

    LoadedPage(int index, PDPage page) {
        this.index = index;
        this.page = Objects.requireNonNull(page);
    }

    public int index() {
        return index;
    }

    public PDRectangle mediaBox() {
        return page.getMediaBox();
    }

    /**
     * Planning-only access.
     * Used by redaction planners to read page geometry.
     */
    public PDPage forPlanningOnly() {
        return page;
    }

    // INTERNAL — engine only
    PDPage internal() {
        return page;
    }
}
