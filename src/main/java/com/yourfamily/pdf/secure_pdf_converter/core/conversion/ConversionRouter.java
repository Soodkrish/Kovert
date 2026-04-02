package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ConversionRouter {

    private static final Map<String, ConversionHandler> ROUTES = new HashMap<>();

    static {

        ROUTES.put("doc->pdf", WordToPdfConverter::convert);
        ROUTES.put("docx->pdf", WordToPdfConverter::convert);

        ROUTES.put("xls->pdf", ExcelToPdfConverter::convert);
        ROUTES.put("xlsx->pdf", ExcelToPdfConverter::convert);

        ROUTES.put("pdf->png", PdfToImageConverter::convert);
        ROUTES.put("pdf->jpg", PdfToImageConverter::convert);
        ROUTES.put("pdf->jpeg", PdfToImageConverter::convert);
        ROUTES.put("pdf->tiff", PdfToImageConverter::convert);

        ROUTES.put("png->pdf", ImageToPdfConverter::convert);
        ROUTES.put("jpg->pdf", ImageToPdfConverter::convert);
        ROUTES.put("jpeg->pdf", ImageToPdfConverter::convert);

        ROUTES.put("pdf->docx", PdfToWordConverter::convert);
        ROUTES.put("pdf->xlsx", PdfToExcelConverter::convert);
        ROUTES.put("pdf->pptx", PdfToPptConverter::convert);
        ROUTES.put("pptx->pdf", PptxToPdfConverter::convert);

        ROUTES.put("png->jpg", PngToJpgConverter::convert);
        ROUTES.put("jpg->png", JpgToPngConverter::convert);
        ROUTES.put("png->webp", PngToWebpConverter::convert);
        ROUTES.put("webp->png", WebpToPngConverter::convert);

        ROUTES.put("docx->html", DocxToHtmlConverter::convert);
        ROUTES.put("docx->txt", DocxToTxtConverter::convert);
        ROUTES.put("html->pdf", HtmlToPdfConverter::convert);
        ROUTES.put("html->docx", HtmlToDocxConverter::convert);
        ROUTES.put("md->pdf", MarkdownToPdfConverter::convert);
        ROUTES.put("markdown->pdf", MarkdownToPdfConverter::convert);
    }

    private ConversionRouter() {}

    public static File convert(
            File input,
            String fromFlag,
            String toFlag,
            String outputOverride) throws Exception {

        String detectedFrom = getExtension(input.getName()).toLowerCase();
        String from = fromFlag != null ? fromFlag.toLowerCase() : detectedFrom;

        if (toFlag == null) {
            throw new IllegalArgumentException("Target format must be specified");
        }

        String to = toFlag.toLowerCase();
        String key = from + "->" + to;
        ConversionHandler handler = ROUTES.get(key);

        if (handler == null) {
            throw new IllegalArgumentException("Unsupported conversion: " + key);
        }

        File output = buildOutputFile(input, to, outputOverride);
        return handler.convert(input, output);
    }

    private static final Map<String, Map<String, ConversionHandler>> GRAPH = new HashMap<>();

    static {
        for (var entry : ROUTES.entrySet()) {
            String[] parts = entry.getKey().split("->", 2);
            if (parts.length != 2) continue;

            String from = parts[0];
            String to = parts[1];

            GRAPH
                .computeIfAbsent(from, k -> new HashMap<>())
                .put(to, entry.getValue());
        }
    }
    
    public static Map<String, List<String>> getSupportedRoutes() {

        Map<String, List<String>> result = new TreeMap<>();

        for (String route : ROUTES.keySet()) {

            String[] parts = route.split("->", 2);

            if (parts.length != 2) continue;

            String source = parts[0];
            String target = parts[1];

            result
                .computeIfAbsent(source, k -> new ArrayList<>())
                .add(target);
        }

        result.forEach((k, v) -> v.sort(String::compareTo));

        return result;
    }
    
    public static List<String> getSupportedTargets(String from) {

        if (from == null || from.isBlank()) {
            return List.of();
        }

        String normalizedFrom = from.toLowerCase();
        Set<String> targets = new TreeSet<>();

        for (String route : ROUTES.keySet()) {
            String[] parts = route.split("->", 2);
            if (parts.length == 2 && parts[0].equals(normalizedFrom)) {
                targets.add(parts[1]);
            }
        }

        return new ArrayList<>(targets);
    }

    private static File buildOutputFile(
            File input,
            String newExtension,
            String override) {

        if (override != null) {
            return new File(override);
        }

        String name = input.getName();
        int dot = name.lastIndexOf(".");
        String base = dot > 0 ? name.substring(0, dot) : name;

        return new File(input.getParent(), base + "." + newExtension);
    }

    private static String getExtension(String name) {

        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }
}
