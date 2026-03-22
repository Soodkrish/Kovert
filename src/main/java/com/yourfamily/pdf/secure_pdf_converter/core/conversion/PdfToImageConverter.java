package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class PdfToImageConverter {

    public static File convert(File inputPdf, File outputImage) throws Exception {

        if (!inputPdf.exists())
            throw new IllegalArgumentException("Input PDF not found");

        File parent = outputImage.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        try (PDDocument doc = Loader.loadPDF(inputPdf)) {

            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image =
                    renderer.renderImageWithDPI(0, 300);

            ImageIO.write(image, "png", outputImage);
        }

        return outputImage;
    }
}