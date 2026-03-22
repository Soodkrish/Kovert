package com.yourfamily.pdf.secure_pdf_converter.core.conversion.imagemagick;

import java.io.File;
import java.io.IOException;

public final class ImageMagickBridge {

    private ImageMagickBridge() {}

    public static void convert(File input, String format, File output) {

        ProcessBuilder builder = new ProcessBuilder(
                "magick",
                input.getAbsolutePath(),
                output.getAbsolutePath()
        );

        builder.redirectErrorStream(true);

        try {

            Process process = builder.start();

            int exit = process.waitFor();

            if (exit != 0) {
                throw new RuntimeException("ImageMagick conversion failed");
            }

        } catch (IOException | InterruptedException e) {

            throw new RuntimeException("ImageMagick conversion error", e);
        }
    }
}