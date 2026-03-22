package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class PdfToPptConverter {

    public static File convert(File inputPdf, File outputPptx) {

        if (!inputPdf.exists())
            throw new IllegalArgumentException("Input file not found");

        if (outputPptx == null)
            throw new IllegalArgumentException("Output file cannot be null");

        File outputDir = outputPptx.getParentFile();

        if (!outputDir.exists())
            outputDir.mkdirs();

        LibreOfficeBridge.convert(inputPdf, "pptx", outputDir);

        String generated =
                inputPdf.getName().replaceAll("\\.pdf$", ".pptx");

        File generatedFile = new File(outputDir, generated);

        if (!generatedFile.equals(outputPptx))
            generatedFile.renameTo(outputPptx);

        return outputPptx;
    }
}