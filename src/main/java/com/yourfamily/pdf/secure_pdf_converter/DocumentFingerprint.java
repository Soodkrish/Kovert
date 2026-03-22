package com.yourfamily.pdf.secure_pdf_converter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public final class DocumentFingerprint {

    private DocumentFingerprint() {}

    public static Fingerprint generate(LoadedDocument doc) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-512");
            ByteArrayOutputStream canonical = new ByteArrayOutputStream();

            PDDocument pd = doc.internal();

            // Stable traversal: logical page order
            for (PDPage page : pd.getPages()) {
                COSDictionary pageDict = page.getCOSObject();

                // Normalize ONLY stable, semantic keys
                normalizeSelected(pageDict, canonical,
                        COSName.CONTENTS,
                        COSName.RESOURCES,
                        COSName.ANNOTS
                );
            }

            byte[] hash = digest.digest(canonical.toByteArray());
            return new Fingerprint(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Fingerprint generation failed", e);
        }
    }

    // ---------------- NORMALIZATION ----------------

    private static void normalizeSelected(
            COSDictionary dict,
            ByteArrayOutputStream out,
            COSName... keys
    ) {
        for (COSName key : keys) {
            COSBase value = dict.getDictionaryObject(key);
            normalize(value, out);
        }
    }

    private static void normalize(COSBase base, ByteArrayOutputStream out) {
        if (base == null) return;

        if (base instanceof COSString s) {
            out.writeBytes(s.getString().getBytes(StandardCharsets.UTF_8));
        }
        else if (base instanceof COSNumber n) {
            out.writeBytes(n.toString().getBytes(StandardCharsets.UTF_8));
        }
        else if (base instanceof COSName name) {
            out.writeBytes(name.getName().getBytes(StandardCharsets.UTF_8));
        }
        else if (base instanceof COSArray array) {
            for (COSBase el : array) {
                normalize(el, out);
            }
        }
        else if (base instanceof COSDictionary dict) {
            dict.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getName()))
                    .forEach(e -> {
                        normalize(e.getKey(), out);
                        normalize(e.getValue(), out);
                    });
        }
        // Intentionally ignore:
        // - object numbers
        // - indirect references
        // - timestamps
        // - compression artifacts
    }

    // ---------------- VALUE OBJECT ----------------

    public record Fingerprint(byte[] hash) {
        public String hex() {
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
}
