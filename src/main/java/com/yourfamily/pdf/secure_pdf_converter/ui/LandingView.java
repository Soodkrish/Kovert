package com.yourfamily.pdf.secure_pdf_converter.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

public class LandingView extends StackPane {

    public LandingView(Consumer<File> fileHandler) {

        setStyle("-fx-background-color: #0e1116;");

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);

        card.setStyle("""
                -fx-background-color: rgba(30,35,45,0.8);
                -fx-background-radius: 15;
                -fx-padding: 40;
                -fx-border-radius: 15;
                -fx-border-color: #3a8bff;
                -fx-border-width: 2;
                """);

        Label title = new Label("Drop File Here");
        title.setStyle("-fx-font-size: 26px; -fx-text-fill: white;");

        Label browse = new Label("or click to browse");
        browse.setStyle("-fx-text-fill: #3a8bff; -fx-font-size: 14px;");

        card.getChildren().addAll(title, browse);

        getChildren().add(card);

        browse.setOnMouseClicked(e -> {

            FileChooser chooser = new FileChooser();

            File file = chooser.showOpenDialog(getScene().getWindow());

            if (file != null)
                fileHandler.accept(file);
        });

        setOnDragOver(e -> {

            if (e.getDragboard().hasFiles())
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);

            e.consume();
        });

        setOnDragDropped(e -> {

            var files = e.getDragboard().getFiles();

            if (!files.isEmpty())
                fileHandler.accept(files.get(0));

            e.setDropCompleted(true);
        });
    }
}