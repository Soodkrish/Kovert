package com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public final class LibreOfficeBridge {

    private static final long TIMEOUT_SECONDS = 60;

    private LibreOfficeBridge() {}

    public static void convert(File input, String format, File outputDir) {

        if (!input.exists())
            throw new IllegalArgumentException("Input file does not exist");

        if (!outputDir.exists())
            outputDir.mkdirs();

        File soffice = LibreOfficeDetector.detect();

        ProcessBuilder builder = new ProcessBuilder(
                soffice.getAbsolutePath(),
                "--headless",
                "--convert-to", format,
                "--outdir", outputDir.getAbsolutePath(),
                input.getAbsolutePath()
        );

        builder.redirectErrorStream(true);

        try {

            Process process = builder.start();

            ExecutorService executor = Executors.newSingleThreadExecutor();

            Future<Integer> future = executor.submit(() -> {
                try {
                    return process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            int exitCode;

            try {
                exitCode = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e) {

                process.destroyForcibly();

                throw new LibreOfficeConversionException(
                        "LibreOffice conversion timed out after "
                        + TIMEOUT_SECONDS + " seconds"
                );
            }

            executor.shutdown();

            if (exitCode != 0) {

                throw new LibreOfficeConversionException(
                        "LibreOffice conversion failed. Exit code: "
                        + exitCode
                );
            }

        }
        catch (IOException | InterruptedException | ExecutionException e) {

            throw new LibreOfficeConversionException(
                    "LibreOffice conversion error",
                    e
            );
        }
    }

    /* Convenience method for existing converters */

    public static void convertToPdf(File input, File outputDir) {
        convert(input, "pdf", outputDir);
    }
}