package com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.yourfamily.pdf.secure_pdf_converter.LoadedDocument;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;


public final class PrecisionRedactionEngine {

    public void applyAndSave(
            LoadedDocument doc,
            List<RedactionPlan> plans,
            Path output
    ) {

        try {

            if (plans == null || plans.isEmpty())
                throw new IllegalArgumentException("No redaction plans provided");

            PDDocument sourceDoc = doc.forRenderingOnly();
            PDFRenderer renderer = new PDFRenderer(sourceDoc);

            float dpi = 300f;

            /* ------------------------------------------------
               GROUP REDACTIONS BY PAGE
               ------------------------------------------------ */

            Map<Integer, List<RedactionPlan>> plansByPage = new HashMap<>();

            for (RedactionPlan plan : plans) {
                plansByPage
                        .computeIfAbsent(plan.pageIndex(), k -> new ArrayList<>())
                        .add(plan);
            }

            /* ------------------------------------------------
               BUILD CLEAN DOCUMENT
               ------------------------------------------------ */

            try (PDDocument cleanDoc = new PDDocument()) {

                int pageCount = sourceDoc.getNumberOfPages();

                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {

                    PDPage originalPage = sourceDoc.getPage(pageIndex);
                    PDRectangle mediaBox = originalPage.getCropBox();

                    BufferedImage image =
                            renderer.renderImageWithDPI(pageIndex, dpi);

                    Graphics2D g = image.createGraphics();
                    g.setColor(Color.BLACK);

                    float scaleX = image.getWidth() / mediaBox.getWidth();
                    float scaleY = image.getHeight() / mediaBox.getHeight();

                    List<RedactionPlan> pagePlans =
                            plansByPage.getOrDefault(pageIndex, Collections.emptyList());

                    for (RedactionPlan plan : pagePlans) {

                        float scaledX = (float) plan.pdfX() * scaleX;
                        float scaledWidth = (float) plan.pdfWidth() * scaleX;
                        float scaledHeight = (float) plan.pdfHeight() * scaleY;

                        float scaledY = image.getHeight()
                                - ((float) plan.pdfY() * scaleY)
                                - scaledHeight;

                        if(plan.shapeType() == RedactionPlan.ShapeType.ELLIPSE){

                            g.fillOval(
                                    Math.round(scaledX),
                                    Math.round(scaledY),
                                    Math.round(scaledWidth),
                                    Math.round(scaledHeight)
                            );

                        } else {

                            g.fill(new Rectangle2D.Float(
                                    scaledX,
                                    scaledY,
                                    scaledWidth,
                                    scaledHeight
                            ));
                        }
                    }

                    g.dispose();

                    PDPage newPage = new PDPage(mediaBox);
                    cleanDoc.addPage(newPage);

                    PDImageXObject pdImage =
                            LosslessFactory.createFromImage(cleanDoc, image);

                    try (PDPageContentStream cs =
                                 new PDPageContentStream(cleanDoc, newPage)) {

                        cs.drawImage(
                                pdImage,
                                0,
                                0,
                                mediaBox.getWidth(),
                                mediaBox.getHeight()
                        );
                    }
                }

                /* ------------------------------------------------
                   REMOVE METADATA / FORMS
                   ------------------------------------------------ */

                cleanDoc.setDocumentInformation(new PDDocumentInformation());
                cleanDoc.getDocumentCatalog().setMetadata(null);

                if (cleanDoc.getDocumentCatalog().getAcroForm() != null) {
                    cleanDoc.getDocumentCatalog().setAcroForm(null);
                }

                /* ------------------------------------------------
                   SAVE FILE
                   ------------------------------------------------ */

                File outFile = output.toFile();

                File parent = outFile.getParentFile();
                System.out.println("Saving to: " + outFile.getAbsolutePath());
                System.out.println("Parent exists: " + (parent != null && parent.exists()));

                cleanDoc.save(outFile);

                System.out.println("Saved successfully.");
                System.out.println("File size: " + outFile.length());

            }

            System.out.println("NUCLEAR REDACTION COMPLETE");

        } catch (IOException e) {
            throw new IllegalStateException("Precision redaction failed", e);
        }
    }
}