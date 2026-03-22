package com.yourfamily.pdf.secure_pdf_converter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public record DocumentMetadataSnapshot(
        String pdfVersion,
        int pageCount,
        boolean hasXmpMetadata
) {

    static DocumentMetadataSnapshot from(PDDocument doc) {
        PDDocumentInformation info = doc.getDocumentInformation();
        return new DocumentMetadataSnapshot(
                String.valueOf(doc.getVersion()),
                doc.getNumberOfPages(),
                info != null && info.getCOSObject() != null
        );
    }
}

