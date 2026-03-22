package com.yourfamily.pdf.secure_pdf_converter.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.WordToPdfConverter;
import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ConversionRouter;
import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ExcelToPdfConverter;
import com.yourfamily.pdf.secure_pdf_converter.core.conversion.PdfToImageConverter;
import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ImageToPdfConverter;

public final class securepdfcli {

    public static void main(String[] args) {

        if (args.length < 1) {
            printHelp();
            return;
        }

        if (!args[0].equalsIgnoreCase("convert")) {
            printHelp();
            return;
        }

        Map<String, String> flags = parseFlags(args);

        String inputPath = flags.get("--input");
        String outputPath = flags.get("--output");
        String batchFolder = flags.get("--batch");
        String fromFlag = flags.get("--from");
        String toFlag = flags.get("--to");

        if (inputPath == null && batchFolder == null) {
            printHelp();
            return;
        }

        try {

            // 🔥 BATCH MODE
            if (batchFolder != null) {

                File folder = new File(batchFolder);

                if (!folder.exists() || !folder.isDirectory()) {
                    System.err.println("Invalid batch folder.");
                    return;
                }

                for (File file : folder.listFiles()) {
                    if (file.isFile()) {
                        try {
                            processSingleFile(file, fromFlag, toFlag);
                        } catch (Exception e) {
                            System.err.println("Failed: " + file.getName());
                        }
                    }
                }

                return;
            }

            // 🔥 SINGLE FILE MODE
            File input = new File(inputPath);

            if (!input.exists()) {
                System.err.println("Input file not found.");
                return;
            }

            long start = System.nanoTime();

            File output = ConversionRouter.convert(
                    input,
                    fromFlag,
                    toFlag,
                    outputPath
            );

            long end = System.nanoTime();
            double seconds = (end - start) / 1_000_000_000.0;

            printMetrics(input, output, seconds);

        } catch (Exception e) {
            System.err.println("Conversion failed: " + e.getMessage());
        }
    }

    // 🔥 CENTRAL ROUTER (AUTO + MANUAL)
    private static File routeConversion(
            File input,
            String fromFlag,
            String toFlag,
            String outputOverride) throws Exception {

        String detectedFrom = getExtension(input.getName()).toLowerCase();

        String from = (fromFlag != null) ? fromFlag.toLowerCase() : detectedFrom;

        String to = determineTargetExtension(from, toFlag);

        File output = buildOutputFile(input, to, outputOverride);

        return performConversion(input, output, from, to);
    }

    // 🔥 DETERMINE TARGET EXTENSION
    private static String determineTargetExtension(String from, String toFlag) {

        if (toFlag != null)
            return toFlag.toLowerCase();

        switch (from) {
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "png":
            case "jpg":
            case "jpeg":
                return "pdf";

            case "pdf":
                return "png";

            default:
                throw new IllegalArgumentException("Unsupported file type.");
        }
    }

    // 🔥 PERFORM CONVERSION
    private static File performConversion(
            File input,
            File output,
            String from,
            String to) throws Exception {

        if (from.matches("doc|docx") && to.equals("pdf"))
            return WordToPdfConverter.convert(input, output);

        if (from.matches("xls|xlsx") && to.equals("pdf"))
            return ExcelToPdfConverter.convert(input, output);

        if (from.equals("pdf") && to.equals("png"))
            return PdfToImageConverter.convert(input, output);

        if (from.matches("png|jpg|jpeg") && to.equals("pdf"))
            return ImageToPdfConverter.convert(input, output);

        throw new IllegalArgumentException(
                "Unsupported conversion combination: " + from + " -> " + to);
    }

    // 🔥 BATCH PROCESSING
    private static void processSingleFile(
            File file,
            String fromFlag,
            String toFlag) throws Exception {

        long start = System.nanoTime();

        File output = routeConversion(file, fromFlag, toFlag, null);

        long end = System.nanoTime();
        double seconds = (end - start) / 1_000_000_000.0;

        System.out.printf("Processed: %s (%.2f sec)%n",
                file.getName(), seconds);
    }

    // 🔥 BUILD OUTPUT PATH
    private static File buildOutputFile(
            File input,
            String newExtension,
            String override) {

        if (override != null)
            return new File(override);

        String name = input.getName();
        int dot = name.lastIndexOf(".");
        String base = (dot > 0) ? name.substring(0, dot) : name;

        return new File(input.getParent(), base + "." + newExtension);
    }

    // 🔥 METRICS
    private static void printMetrics(
            File input,
            File output,
            double seconds) {

        Runtime runtime = Runtime.getRuntime();
        long usedMemory =
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        System.out.println("\n🔥 SECURE PDF CONVERTER 🔥");
        System.out.println("------------------------------------");
        System.out.println("Input:  " + input.getName());
        System.out.println("Output: " + output.getName());
        System.out.printf("Conversion time: %.2f seconds%n", seconds);
        System.out.println("Memory used: " + usedMemory + " MB");
        System.out.println("Status: SUCCESS");
        System.out.println("------------------------------------\n");
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }

    private static Map<String, String> parseFlags(String[] args) {

        Map<String, String> map = new HashMap<>();

        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                map.put(args[i], args[i + 1]);
                i++;
            }
        }

        return map;
    }

    private static void printHelp() {

        System.out.println("""
⣿⣿⣿⣿⣿⣿⣿⡿⡛⠟⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⠿⠨⡀⠄⠄⡘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⠿⢁⠼⠊⣱⡃⠄⠈⠹⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⡿⠛⡧⠁⡴⣦⣔⣶⣄⢠⠄⠄⠹⣿⣿⣿⣿⣿⣿⣿⣤⠭⠏⠙⢿⣿⣿⣿⣿⣿
⣿⡧⠠⠠⢠⣾⣾⣟⠝⠉⠉⠻⡒⡂⠄⠙⠻⣿⣿⣿⣿⣿⡪⠘⠄⠉⡄⢹⣿⣿⣿⣿
⣿⠃⠁⢐⣷⠉⠿⠐⠑⠠⠠⠄⣈⣿⣄⣱⣠⢻⣿⣿⣿⣿⣯⠷⠈⠉⢀⣾⣿⣿⣿⣿
⣿⣴⠤⣬⣭⣴⠂⠇⡔⠚⠍⠄⠄⠁⠘⢿⣷⢈⣿⣿⣿⣿⡧⠂⣠⠄⠸⡜⡿⣿⣿⣿
⣿⣇⠄⡙⣿⣷⣭⣷⠃⣠⠄⠄⡄⠄⠄⠄⢻⣿⣿⣿⣿⣿⣧⣁⣿⡄⠼⡿⣦⣬⣰⣿
⣿⣷⣥⣴⣿⣿⣿⣿⠷⠲⠄⢠⠄⡆⠄⠄⠄⡨⢿⣿⣿⣿⣿⣿⣎⠐⠄⠈⣙⣩⣿⣿
⣿⣿⣿⣿⣿⣿⢟⠕⠁⠈⢠⢃⢸⣿⣿⣶⡘⠑⠄⠸⣿⣿⣿⣿⣿⣦⡀⡉⢿⣧⣿⣿
⣿⣿⣿⣿⡿⠋⠄⠄⢀⠄⠐⢩⣿⣿⣿⣿⣦⡀⠄⠄⠉⠿⣿⣿⣿⣿⣿⣷⣨⣿⣿⣿
⣿⣿⣿⡟⠄⠄⠄⠄⠄⠋⢀⣼⣿⣿⣿⣿⣿⣿⣿⣶⣦⣀⢟⣻⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⡆⠆⠄⠠⡀⡀⠄⣽⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⡿⡅⠄⠄⢀⡰⠂⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
🔥 SECURE PDF CONVERTER 🔥
Stand Proud.

Usage:
  securepdf convert --input <file>
  securepdf convert --input <file> --output <file>
  securepdf convert --input <file> --to <format>
  securepdf convert --input <file> --from <format> --to <format>
  securepdf convert --batch <folder>

Default behavior:
  doc/docx -> pdf
  xls/xlsx -> pdf
  pdf -> png
  png/jpg/jpeg -> pdf

--from and --to are optional.
If omitted, format is auto-detected.
""");
    }
}
    
    