package com.yourfamily.pdf.secure_pdf_converter;

import java.nio.file.Path;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class App {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=== PHASE 1 CHECK START ===");

        // 1️⃣ Path to test PDF
        Path pdfPath = Path.of("Cover letter.pdf");

        // 2️⃣ Load document
        DocumentLoader loader = new DocumentLoader();
        LoadedDocument doc = loader.load(pdfPath);
        System.out.println("PDF loaded");

        // 3️⃣ Validate document
        DocumentValidator validator = new DocumentValidator();
        validator.validate(doc);
        System.out.println("PDF validated");

        // 4️⃣ Generate fingerprint
        DocumentFingerprint.Fingerprint fp =
                DocumentFingerprint.generate(doc);

        System.out.println("Fingerprint:");
        System.out.println(fp.hex());

        // 5️⃣ Cleanup
        doc.close();

        System.out.println("=== PHASE 1 CHECK DONE ===");
    }
}
