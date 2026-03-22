package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

public final class ImageToPdfConverter {

    public static File convert(File inputImage, File outputPdf) throws Exception {

        if (!inputImage.exists())
            throw new IllegalArgumentException("Input image not found");

        File parent = outputPdf.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        BufferedImage image = ImageIO.read(inputImage);

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage();
            doc.addPage(page);

            var pdImage = LosslessFactory.createFromImage(doc, image);

            try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page)) {

                cs.drawImage(pdImage, 0, 0,
                        page.getMediaBox().getWidth(),
                        page.getMediaBox().getHeight());
            }

            doc.save(outputPdf);
        }

        return outputPdf;
    }
}