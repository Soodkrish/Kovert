package com.yourfamily.pdf.secure_pdf_converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public final class DocumentValidator {

    public void validate(LoadedDocument doc) {
        try {
            PDDocument pd = doc.internal();

            // 1️⃣ Document must have pages
            int pageCount = pd.getNumberOfPages();
            if (pageCount <= 0) {
                throw new IllegalStateException("PDF has no pages");
            }

            // 2️⃣ Page count consistency
            int declaredPages = doc.metadata().pageCount();
            if (declaredPages != pageCount) {
                throw new IllegalStateException(
                        "Page count mismatch: declared="
                                + declaredPages + ", actual=" + pageCount
                );
            }

            // 3️⃣ Structural page validation (not content enforcement)
            for (PDPage page : pd.getPages()) {
                if (page.getCOSObject() == null) {
                    throw new IllegalStateException("Page missing COS dictionary");
                }

                // Accessing contents must not throw
                try {
                    page.getContents();
                } catch (Exception ex) {
                    throw new IllegalStateException("Unreadable page content stream", ex);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("PDF validation failed", e);
        }
    }
}
