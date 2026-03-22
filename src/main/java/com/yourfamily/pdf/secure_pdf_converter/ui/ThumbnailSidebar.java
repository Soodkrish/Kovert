package com.yourfamily.pdf.secure_pdf_converter.ui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

public class ThumbnailSidebar extends ScrollPane {

    private final VBox container = new VBox(12);

    private final Map<Integer, Label> matchLabels = new HashMap<>();
    private final Map<Integer, StackPane> pageNodes = new HashMap<>();

    public ThumbnailSidebar(
            PDFRenderer renderer,
            int pageCount,
            IntConsumer onPageSelected
    ){

        setPrefWidth(150);
        setStyle("-fx-background:#1b1f26;");

        container.setPadding(new Insets(10));
        container.setAlignment(Pos.TOP_CENTER);

        setContent(container);
        setFitToWidth(true);

        loadThumbnails(renderer,pageCount,onPageSelected);
    }

    private void loadThumbnails(
            PDFRenderer renderer,
            int pageCount,
            IntConsumer onPageSelected
    ){

        Thread loader = new Thread(() -> {

            try{

                for(int i=0;i<pageCount;i++){

                    BufferedImage image;

                    synchronized(renderer){
                        image = renderer.renderImageWithDPI(i,40);
                    }

                    ImageView thumb =
                            new ImageView(SwingFXUtils.toFXImage(image,null));

                    thumb.setFitWidth(100);
                    thumb.setPreserveRatio(true);
                    thumb.setCursor(Cursor.HAND);

                    Label pageLabel =
                            new Label("Page " + (i+1));

                    pageLabel.setStyle(
                            "-fx-text-fill:white;" +
                            "-fx-font-size:11;"
                    );

                    Label matchBadge =
                            new Label("");

                    matchBadge.setStyle(
                            "-fx-background-color:#ff5555;" +
                            "-fx-text-fill:white;" +
                            "-fx-padding:2 6;" +
                            "-fx-background-radius:8;" +
                            "-fx-font-size:10;"
                    );

                    matchBadge.setVisible(false);

                    StackPane wrapper = new StackPane();

                    VBox inner = new VBox(4,thumb,pageLabel);
                    inner.setAlignment(Pos.CENTER);

                    StackPane.setAlignment(matchBadge,Pos.TOP_RIGHT);
                    wrapper.getChildren().addAll(inner,matchBadge);

                    int pageIndex=i;

                    thumb.setOnMouseClicked(e ->
                            onPageSelected.accept(pageIndex)
                    );

                    matchLabels.put(pageIndex,matchBadge);
                    pageNodes.put(pageIndex,wrapper);

                    Platform.runLater(() ->
                            container.getChildren().add(wrapper)
                    );

                    // Small pause so JavaFX UI thread doesn't get flooded
                    Thread.sleep(10);
                }

            }
            catch(Exception ex){
                ex.printStackTrace();
            }

        });

        loader.setDaemon(true);
        loader.start();
    }

    /* =========================================
       SHOW MATCH COUNTS
       ========================================= */

    public void showMatches(Map<Integer,Integer> matches){

        Platform.runLater(() -> {

            matchLabels.values().forEach(l -> l.setVisible(false));

            for(var entry : matches.entrySet()){

                int page = entry.getKey();
                int count = entry.getValue();

                Label badge = matchLabels.get(page);

                if(badge!=null){

                    badge.setText(String.valueOf(count));
                    badge.setVisible(true);
                }

                StackPane node = pageNodes.get(page);

                if(node!=null){

                    node.setStyle(
                            "-fx-border-color:#ffaa00;" +
                            "-fx-border-width:2;" +
                            "-fx-border-radius:6;"
                    );
                }
            }
        });
    }

    /* =========================================
       CLEAR MATCH HIGHLIGHTS
       ========================================= */

    public void clearMatches(){

        Platform.runLater(() -> {

            matchLabels.values().forEach(l -> l.setVisible(false));

            pageNodes.values().forEach(n ->
                    n.setStyle("")
            );
        });
    }
}