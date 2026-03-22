package com.yourfamily.pdf.secure_pdf_converter;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader; // The new 3.0 Entry Point
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class DocumentLoader {

    public LoadedDocument load(Path pdfPath) {
        try {
            // PDFBox 3.0.x approach
            var fileSource = new RandomAccessReadBufferedFile(pdfPath);
            PDDocument document = Loader.loadPDF(fileSource);

            // Check encryption status
            if (document.isEncrypted()) {
                // In 3.0, we close immediately if we don't support it
                document.close();
                throw new IllegalStateException("Encrypted PDFs are not supported in Stage 1");
            }

            return new LoadedDocument(pdfPath, document);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load PDF: " + pdfPath, e);
        }
    }
}
