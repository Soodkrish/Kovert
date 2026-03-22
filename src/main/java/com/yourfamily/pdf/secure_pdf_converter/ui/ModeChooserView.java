package com.yourfamily.pdf.secure_pdf_converter.ui;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.File;

public class ModeChooserView extends VBox {

    public ModeChooserView(File file, Runnable redactAction, Runnable convertAction) {
        getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setAlignment(Pos.CENTER);
        setSpacing(60);

        Label title = new Label("CHOOSE YOUR PATH");
        title.getStyleClass().add("hyper-title");

        // Add a "pulsing" animation to the title
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(title.scaleXProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.5), new KeyValue(title.scaleXProperty(), 1.05)),
            new KeyFrame(Duration.seconds(1), new KeyValue(title.scaleXProperty(), 1.0))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        VBox redactCard = createJoJoCard("REDACT", "ERASE THE EVIDENCE", redactAction);
        VBox convertCard = createJoJoCard("CONVERT", "EVOLVE THE FORM", convertAction);

        HBox cards = new HBox(40, redactCard, convertCard);
        cards.setAlignment(Pos.CENTER);
        getChildren().addAll(title, cards);
    }

    private VBox createJoJoCard(String title, String sub, Runnable action) {
        VBox card = new VBox(10);
        card.getStyleClass().add("mode-card");
        card.setPrefSize(300, 200);
        card.setAlignment(Pos.CENTER);

        Label t = new Label(title);
        t.setStyle("-fx-font-family: 'Impact'; -fx-font-size: 30px; -fx-text-fill: white;");
        Label s = new Label(sub);
        s.setStyle("-fx-font-size: 14px; -fx-text-fill: #00f2ff; -fx-font-weight: bold;");

        card.getChildren().addAll(t, s);
        card.setOnMouseClicked(e -> action.run());

        // Shake Animation on Hover
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), card);
        shake.setFromX(-2);
        shake.setToX(2);
        shake.setCycleCount(10);
        shake.setAutoReverse(true);

        card.setOnMouseEntered(e -> shake.playFromStart());
        
        return card;
    }
}