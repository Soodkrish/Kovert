package com.yourfamily.pdf.secure_pdf_converter.core.conversion;

import java.io.File;

public interface ConversionHandler {

    File convert(File input, File output) throws Exception;

}