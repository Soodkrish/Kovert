package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class ExcelToPdfConverter {

    public static File convert(File inputXlsx, File outputPdf) {

        if (!inputXlsx.exists()) {
            throw new IllegalArgumentException("Input file not found");
        }

        if (outputPdf == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        File outputDir = outputPdf.getParentFile();

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        LibreOfficeBridge.convert(inputXlsx,"pdf" ,outputDir);

        // LibreOffice creates file in outputDir with same base name
        String generatedName =
                inputXlsx.getName().replaceAll("\\.(xlsx|xls)$", ".pdf");

        File generatedFile = new File(outputDir, generatedName);

        if (!generatedFile.equals(outputPdf)) {
            generatedFile.renameTo(outputPdf);
        }

        return outputPdf;
    }
}