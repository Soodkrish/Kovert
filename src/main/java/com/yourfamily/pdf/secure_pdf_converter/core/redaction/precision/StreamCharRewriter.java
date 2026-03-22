package com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;

public final class StreamCharRewriter {

    public static void rewritePage(
            PDDocument doc,
            PDPage page,
            int pageIndex,
            RedactionHitMap hitMap
    ) {
        try {
            // 🔹 1. Fix: PDFBox 3.x getContentStreams() returns an Iterator
            Iterator<PDStream> streamIter = page.getContentStreams();
            while (streamIter.hasNext()) {
                PDStream contentStream = streamIter.next();
                
                // Fix: PDFStreamParser constructor in 3.x
                PDFStreamParser parser = new PDFStreamParser(page); 
                List<Object> tokens = parser.parse(); // parse() returns tokens in 3.0.1

                List<Object> rewritten = rewriteTokens(tokens, pageIndex, hitMap);

                PDStream newStream = new PDStream(doc);
                try (OutputStream os = newStream.createOutputStream()) {
                    new ContentStreamWriter(os).writeTokens(rewritten);
                }

                // 🔹 Fix: PDFBox 3.x doesn't use "DATA". Use setContents or transfer stream data.
                page.setContents(newStream);
            }

            // 🔹 2. Rewrite Form XObjects (Recursive)
            var resources = page.getResources();
            if (resources == null) return;

            for (COSName name : resources.getXObjectNames()) {
                var xObject = resources.getXObject(name);
                if (xObject instanceof PDFormXObject form) {
                    rewriteForm(doc, form, pageIndex, hitMap);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Rewrite failed", e);
        }
    }

    private static void rewriteForm(
            PDDocument doc,
            PDFormXObject form,
            int pageIndex,
            RedactionHitMap hitMap
    ) throws IOException {

        PDFStreamParser parser = new PDFStreamParser(form);
        List<Object> tokens = parser.parse();

        List<Object> rewritten = rewriteTokens(tokens, pageIndex, hitMap);

        PDStream newStream = new PDStream(doc);
        try (OutputStream os = newStream.createOutputStream()) {
            new ContentStreamWriter(os).writeTokens(rewritten);
        }

        // 🔹 Fix: For Form XObjects in 3.x, use replaceStream
        try (OutputStream os = form.getStream().createOutputStream()) {
            new ContentStreamWriter(os).writeTokens(rewritten);
        }

        var resources = form.getResources();
        if (resources == null) return;

        for (COSName name : resources.getXObjectNames()) {
            var nested = resources.getXObject(name);
            if (nested instanceof PDFormXObject nestedForm) {
                rewriteForm(doc, nestedForm, pageIndex, hitMap);
            }
        }
    }

    private static List<Object> rewriteTokens(
            List<Object> tokens,
            int pageIndex,
            RedactionHitMap hitMap
    ) {
        List<Object> rewritten = new ArrayList<>();
        int charCursor = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);

            if (token instanceof COSArray array &&
                i + 1 < tokens.size() &&
                tokens.get(i + 1) instanceof Operator op &&
                op.getName().equals("TJ")) {

                if (shouldRemoveText(array, pageIndex, charCursor, hitMap)) {
                    i++; 
                    continue;
                }
                charCursor += getTextLength(array);
            }
            else if (token instanceof COSString str &&
                i + 1 < tokens.size() &&
                tokens.get(i + 1) instanceof Operator op &&
                isTextShowOperator(op.getName())) {

                if (shouldRemoveText(str, pageIndex, charCursor, hitMap)) {
                    i++; 
                    continue;
                }
                charCursor += str.getString().length();
            }

            rewritten.add(token);
        }
        return rewritten;
    }

    private static boolean shouldRemoveText(Object textObj, int pageIndex, int cursor, RedactionHitMap hitMap) {
        if (textObj instanceof COSString str) {
            String text = str.getString();
            for (int c = 0; c < text.length(); c++) {
                if (hitMap.isRedacted(pageIndex, cursor + c)) return true;
            }
        } else if (textObj instanceof COSArray array) {
            int localCursor = cursor;
            for (Object item : array) {
                if (item instanceof COSString str) {
                    if (shouldRemoveText(str, pageIndex, localCursor, hitMap)) return true;
                    localCursor += str.getString().length();
                }
            }
        }
        return false;
    }

    private static int getTextLength(COSArray array) {
        int length = 0;
        for (Object item : array) {
            if (item instanceof COSString str) length += str.getString().length();
        }
        return length;
    }

    private static boolean isTextShowOperator(String name) {
        return name.equals("Tj") || name.equals("'") || name.equals("\"");
    }
}