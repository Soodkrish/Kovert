package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class PdfToWordConverter {

    public static File convert(File inputPdf, File outputDocx) {

        if (!inputPdf.exists())
            throw new IllegalArgumentException("Input file not found");

        if (outputDocx == null)
            throw new IllegalArgumentException("Output file cannot be null");

        File outputDir = outputDocx.getParentFile();

        if (!outputDir.exists())
            outputDir.mkdirs();

        LibreOfficeBridge.convert(inputPdf, "docx", outputDir);

        String generated =
                inputPdf.getName().replaceAll("\\.pdf$", ".docx");

        File generatedFile = new File(outputDir, generated);

        if (!generatedFile.equals(outputDocx))
            generatedFile.renameTo(outputDocx);

        return outputDocx;
    }
}