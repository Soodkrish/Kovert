package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class PdfToExcelConverter {

    public static File convert(File inputPdf, File outputXlsx) {

        if (!inputPdf.exists())
            throw new IllegalArgumentException("Input file not found");

        if (outputXlsx == null)
            throw new IllegalArgumentException("Output file cannot be null");

        File outputDir = outputXlsx.getParentFile();

        if (!outputDir.exists())
            outputDir.mkdirs();

        LibreOfficeBridge.convert(inputPdf, "xlsx", outputDir);

        String generated =
                inputPdf.getName().replaceAll("\\.pdf$", ".xlsx");

        File generatedFile = new File(outputDir, generated);

        if (!generatedFile.equals(outputXlsx))
            generatedFile.renameTo(outputXlsx);

        return outputXlsx;
    }
}