package com.yourfamily.pdf.secure_pdf_converter.core.conversion.pandoc;

import java.io.*;

public final class PandocBridge {

    private PandocBridge() {}

    public static void convert(File input, File output) {

        ProcessBuilder builder = new ProcessBuilder(
                "pandoc",
                input.getAbsolutePath(),
                "-o",
                output.getAbsolutePath()
        );

        builder.redirectErrorStream(true);

        try {

            Process process = builder.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Pandoc] " + line);
            }

            int exit = process.waitFor();

            if (exit != 0) {
                throw new RuntimeException("Pandoc conversion failed");
            }

        } catch (IOException | InterruptedException e) {

            throw new RuntimeException("Pandoc conversion error", e);
        }
    }
}