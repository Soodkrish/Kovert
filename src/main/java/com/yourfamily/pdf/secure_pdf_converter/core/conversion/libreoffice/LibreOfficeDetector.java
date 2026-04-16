package com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.tools.ToolPaths;

public final class LibreOfficeDetector {

    private LibreOfficeDetector() {}

    public static File detect() {

        File file = new File(ToolPaths.libreoffice());

        if (file.exists() && file.canExecute()) {
            return file;
        }

        throw new IllegalStateException(
            "LibreOffice not found in tools folder"
        );
    }
}