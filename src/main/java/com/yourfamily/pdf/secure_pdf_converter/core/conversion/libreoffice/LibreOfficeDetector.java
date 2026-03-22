package com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class LibreOfficeDetector {

    private static final List<String> COMMON_PATHS = List.of(
        "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
        "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
    );

    private LibreOfficeDetector() {}

    public static File detect() {

        for (String path : COMMON_PATHS) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        throw new IllegalStateException(
            "LibreOffice not found. Please install LibreOffice 7.6.x"
        );
    }
}