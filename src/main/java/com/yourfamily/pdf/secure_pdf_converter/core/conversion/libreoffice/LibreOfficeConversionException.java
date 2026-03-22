package com.yourfamily.pdf.secure_pdf_converter.core.conversion.libreoffice;

public class LibreOfficeConversionException extends RuntimeException {

    public LibreOfficeConversionException(String message) {
        super(message);
    }

    public LibreOfficeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}