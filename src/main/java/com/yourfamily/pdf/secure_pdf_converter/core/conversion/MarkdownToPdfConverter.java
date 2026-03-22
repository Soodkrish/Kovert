package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.pandoc.PandocBridge;

public final class MarkdownToPdfConverter {

    public static File convert(File input, File output) {

        PandocBridge.convert(input, output);

        return output;
    }
}