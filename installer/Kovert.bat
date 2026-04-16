@echo off

set ROOT=%~dp0

"%ROOT%runtime\bin\java.exe" ^
  -cp "%ROOT%app\app.jar" ^
  com.yourfamily.pdf.secure_pdf_converter.cli.securepdfcli %*