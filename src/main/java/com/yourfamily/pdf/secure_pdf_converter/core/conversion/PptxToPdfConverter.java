package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class PptxToPdfConverter {

    public static File convert(File inputPptx, File outputPdf) {

        if (!inputPptx.exists()) {
            throw new IllegalArgumentException("Input file not found");
        }

        if (outputPdf == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        File outputDir = outputPdf.getParentFile();

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        /* LibreOffice conversion */

        LibreOfficeBridge.convert(inputPptx, "pdf", outputDir);

        /* LibreOffice generates file with same base name */

        String generatedName =
                inputPptx.getName().replaceAll("\\.(pptx|ppt)$", ".pdf");

        File generatedFile = new File(outputDir, generatedName);

        if (!generatedFile.equals(outputPdf)) {
            generatedFile.renameTo(outputPdf);
        }

        return outputPdf;
    }
}