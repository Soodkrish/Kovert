package com.yourfamily.pdf.secure_pdf_converter;

import java.nio.file.Path;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;

public final class LoadedDocument implements AutoCloseable {

    private final Path sourcePath;
    private final PDDocument pdDocument;
    private final int pageCount;
    private final DocumentMetadataSnapshot metadata;

    LoadedDocument(Path sourcePath, PDDocument pdDocument) {
        this.sourcePath = Objects.requireNonNull(sourcePath);
        this.pdDocument = Objects.requireNonNull(pdDocument);
        this.pageCount = pdDocument.getNumberOfPages();
        this.metadata = DocumentMetadataSnapshot.from(pdDocument);
    }

    public int pageCount() {
        return pageCount;
    }

    public LoadedPage getPage(int index) {
        if (index < 0 || index >= pageCount) {
            throw new IndexOutOfBoundsException("Invalid page index: " + index);
        }
        return new LoadedPage(index, pdDocument.getPage(index));
    }

    public DocumentMetadataSnapshot metadata() {
        return metadata;
    }

    /**
     * Rendering-only access.
     * UI / preview usage ONLY.
     */
    public PDDocument forRenderingOnly() {
        return pdDocument;
    }

    /**
     * Mutation-only access.
     * Core engines ONLY.
     */
    public PDDocument forMutationOnly() {
        return pdDocument;
    }

    /**
     * INTERNAL — same-package only.
     * Reserved for tightly-coupled core logic.
     */
    PDDocument internal() {
        return pdDocument;
    }

    @Override
    public void close() throws Exception {
        pdDocument.close();
    }
}
