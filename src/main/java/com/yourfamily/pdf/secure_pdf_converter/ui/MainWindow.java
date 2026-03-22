package com.yourfamily.pdf.secure_pdf_converter.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class MainWindow extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {

        this.primaryStage = stage;

        showLanding();

        stage.setTitle("Secure PDF Studio");
        stage.setWidth(980);
        stage.setHeight(720);
        stage.show();
    }

    /* LANDING SCREEN */

    public void showLanding() {

        LandingView landing = new LandingView(file -> {

            // Always go to ModeChooser
            showModeChooser(file);

        });

        primaryStage.setScene(new Scene(landing));
    }

    /* MODE CHOOSER */

    private void showModeChooser(File file) {

        ModeChooserView chooser = new ModeChooserView(
                file,

                // Redact action
                () -> showEditor(file, true),

                // Convert action
                () -> showConversion(file)
        );

        primaryStage.setScene(new Scene(chooser));
    }

    /* EDITOR */

    private void showEditor(File file, boolean redactionMode) {

        EditorView editor = new EditorView(file, redactionMode, this);

        primaryStage.setScene(new Scene(editor));
    }

    /* CONVERSION VIEW */

    public void showConversion(File file) {

        ConversionView view = new ConversionView(this, file);

        primaryStage.setScene(new Scene(view));
    }

    public static void main(String[] args) {
        launch(args);
    }
}