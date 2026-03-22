package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.imagemagick.ImageMagickBridge;

public final class WebpToPngConverter {

    public static File convert(File input, File output) {

        ImageMagickBridge.convert(input, "png", output);

        return output;
    }
}