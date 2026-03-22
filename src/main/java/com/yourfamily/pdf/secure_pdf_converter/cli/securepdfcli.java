package com.yourfamily.pdf.secure_pdf_converter.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ConversionRouter;

public final class securepdfcli {

    public static void main(String[] args) {

        if (args.length < 1) {
            printHelp();
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            printSupportedConversions();
            return;
        }

        if (args[0].equalsIgnoreCase("--help")
                || args[0].equalsIgnoreCase("-h")) {
            printHelp();
            return;
        }

        if (!args[0].equalsIgnoreCase("convert")) {
            printHelp();
            return;
        }

        Map<String, String> flags = parseFlags(args);

        if (flags.containsKey("--list")) {
            printSupportedConversions();
            return;
        }

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

            if (batchFolder != null) {

                File folder = new File(batchFolder);

                if (!folder.exists() || !folder.isDirectory()) {
                    System.err.println("Invalid batch folder.");
                    return;
                }

                File[] files = folder.listFiles(File::isFile);
                if (files == null || files.length == 0) {
                    System.err.println("No files found in batch folder.");
                    return;
                }

                for (File file : files) {
                    try {
                        processSingleFile(file, fromFlag, toFlag, outputPath);
                    } catch (Exception e) {
                        System.err.println("Failed: " + file.getName()
                                + " -> " + e.getMessage());
                    }
                }

                return;
            }

            File input = new File(inputPath);

            if (!input.exists()) {
                System.err.println("Input file not found.");
                return;
            }

            long start = System.nanoTime();
            File output = routeConversion(input, fromFlag, toFlag, outputPath);
            long end = System.nanoTime();

            double seconds = (end - start) / 1_000_000_000.0;
            printMetrics(input, output, seconds);

        } catch (Exception e) {
            System.err.println("Conversion failed: " + e.getMessage());
        }
    }

    private static File routeConversion(
            File input,
            String fromFlag,
            String toFlag,
            String outputOverride) throws Exception {

        String detectedFrom = getExtension(input.getName()).toLowerCase();
        String from = fromFlag != null ? fromFlag.toLowerCase() : detectedFrom;
        String to = determineTargetExtension(from, toFlag);

        return ConversionRouter.convert(input, from, to, outputOverride);
    }

    private static String determineTargetExtension(String from, String toFlag) {

        if (toFlag != null && !toFlag.isBlank()) {
            return toFlag.toLowerCase();
        }

        List<String> targets = ConversionRouter.getSupportedTargets(from);

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Unsupported file type: " + from);
        }

        if (targets.contains("pdf")) {
            return "pdf";
        }

        return targets.get(0);
    }

    private static void processSingleFile(
            File file,
            String fromFlag,
            String toFlag,
            String outputPath) throws Exception {

        long start = System.nanoTime();
        File output = routeConversion(
                file,
                fromFlag,
                toFlag,
                resolveBatchOutput(file, outputPath, fromFlag, toFlag));
        long end = System.nanoTime();

        double seconds = (end - start) / 1_000_000_000.0;

        System.out.printf("Processed: %s -> %s (%.2f sec)%n",
                file.getName(), output.getName(), seconds);
    }

    private static String resolveBatchOutput(
            File input,
            String outputPath,
            String fromFlag,
            String toFlag) {

        if (outputPath == null || outputPath.isBlank()) {
            return null;
        }

        File outputTarget = new File(outputPath);

        if (outputTarget.exists() && outputTarget.isFile()) {
            return outputTarget.getAbsolutePath();
        }

        String detectedFrom = getExtension(input.getName()).toLowerCase();
        String from = fromFlag != null ? fromFlag.toLowerCase() : detectedFrom;
        String to = determineTargetExtension(from, toFlag);

        String name = input.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;

        return new File(outputTarget, base + "." + to).getAbsolutePath();
    }

    private static void printMetrics(
            File input,
            File output,
            double seconds) {

        Runtime runtime = Runtime.getRuntime();
        long usedMemory =
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        System.out.println();
        System.out.println("SECURE PDF CONVERTER");
        System.out.println("------------------------------------");
        System.out.println("Input:  " + input.getName());
        System.out.println("Output: " + output.getName());
        System.out.printf("Conversion time: %.2f seconds%n", seconds);
        System.out.println("Memory used: " + usedMemory + " MB");
        System.out.println("Status: SUCCESS");
        System.out.println("------------------------------------");
        System.out.println();
    }

    private static void printSupportedConversions() {

        List<String> routes = new ArrayList<>(ConversionRouter.getSupportedRoutes());

        System.out.println();
        System.out.println("SUPPORTED CONVERSIONS");
        System.out.println("---------------------");

        for (String route : routes) {
            System.out.println("  " + route);
        }

        System.out.println();
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }

    private static Map<String, String> parseFlags(String[] args) {

        Map<String, String> map = new HashMap<>();

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(args[i], args[i + 1]);
                    i++;
                } else {
                    map.put(args[i], "true");
                }
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
   _____                             ____  ____  ______
  / ___/___  _______  ________     / __ \\/ __ \\/ ____/
  \\__ \\/ _ \\/ ___/ / / / ___/ _   / /_/ / / / / /_
 ___/ /  __/ /__/ /_/ / /  /  __// ____/ /_/ / __/
/____/\\___/\\___/\\__,_/_/   \\___//_/   /_____/_/

  Secure PDF Converter
  Local-first private document conversion
  =======================================

Usage:
  securepdf convert --input <file>
  securepdf convert --input <file> --output <file>
  securepdf convert --input <file> --to <format>
  securepdf convert --input <file> --from <format> --to <format>
  securepdf convert --batch <folder>
  securepdf list

Default behavior:
  Picks a preferred target automatically when --to is omitted.
  Use `securepdf list` or `securepdf convert --list` to see every route.

--from and --to are optional.
If omitted, format is auto-detected.
""");
    }
}
