package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice.LibreOfficeBridge;

public final class PdfToExcelConverter {

    public static File convert(File inputPdf, File outputXlsx) {

        if (inputPdf == null || !inputPdf.exists()) {
            throw new IllegalArgumentException("Input file not found");
        }

        if (outputXlsx == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        File outputDir = outputXlsx.getParentFile();

        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 🔥 Convert using LibreOffice
        LibreOfficeBridge.convert(inputPdf, "xlsx", outputDir);

        // Expected generated file name
        String generatedName = inputPdf.getName().replaceAll("(?i)\\.pdf$", ".xlsx");
        File generatedFile = new File(outputDir, generatedName);

        if (!generatedFile.exists()) {
            throw new RuntimeException("Conversion failed: output file not generated");
        }

        try {
            // 🔥 Move/rename safely
            Files.move(
                generatedFile.toPath(),
                outputXlsx.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to move converted file", e);
        }

        // ✅ Logging AFTER everything
        System.out.println("WRITING FILE TO: " + outputXlsx.getAbsolutePath());
        System.out.println("EXISTS AFTER WRITE: " + outputXlsx.exists());

        return outputXlsx;
    }
}