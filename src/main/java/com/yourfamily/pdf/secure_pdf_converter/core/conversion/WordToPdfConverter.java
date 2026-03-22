package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class WordToPdfConverter {

    public static File convert(File inputDocx, File outputPdf) {

        if (!inputDocx.exists()) {
            throw new IllegalArgumentException("Input file not found");
        }

        if (outputPdf == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        File outputDir = outputPdf.getParentFile();

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        LibreOfficeBridge.convert(inputDocx, "pdf" ,outputDir);

        String generatedName =
                inputDocx.getName().replaceAll("\\.(docx|doc)$", ".pdf");

        File generatedFile = new File(outputDir, generatedName);

        if (!generatedFile.equals(outputPdf)) {
            generatedFile.renameTo(outputPdf);
        }

        return outputPdf;
    }
}