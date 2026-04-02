package com.yourfamily.pdf.secure_pdf_converter.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ConversionRouter;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

public class ConversionView extends BorderPane {

    private final ListView<File> fileList = new ListView<>();
    private final ComboBox<String> formatBox = new ComboBox<>();
    private final TextField outputFolder = new TextField();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready to convert");
    private final VBox dropZone = new VBox();
    private final Popup conversionPopup = new Popup();
    private final VBox conversionPopupContent = new VBox(8);

    private final CheckBox batchFolderMode = new CheckBox("Batch convert entire folder");
    private final ComboBox<String> perFileFormat = new ComboBox<>();

    public ConversionView(MainWindow app, File initialFile) {

        setPadding(new Insets(30));

        try {
            getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("style.css not found.");
        }

        Button backBtn = new Button("<- Back");
        backBtn.setOnAction(e -> app.showLanding());

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        StackPane conversionGuide = buildConversionGuide();
        HBox topBar = new HBox(20, backBtn, leftSpacer, conversionGuide, rightSpacer);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Label dropTitle = new Label("Drop Files Here");
        dropTitle.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label dropSubtitle = new Label("or click to browse");
        dropSubtitle.setStyle("-fx-text-fill:#8b949e;");

        Button addBtn = new Button("+ Select Files");
        addBtn.setOnAction(e -> addFiles());

        dropZone.getChildren().addAll(dropTitle, dropSubtitle, addBtn);
        dropZone.setSpacing(15);
        dropZone.getStyleClass().add("drop-zone");

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            File selected = fileList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fileList.getItems().remove(selected);
            }
        });

        VBox leftColumn = new VBox(
                15,
                dropZone,
                batchFolderMode,
                new Label("Queued Files:"),
                fileList,
                removeBtn
        );
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        formatBox.getItems().addAll(
                "pdf",
                "png",
                "jpg",
                "jpeg",
                "docx",
                "xlsx",
                "pptx",
                "html",
                "txt",
                "tiff",
                "webp"
        );
        formatBox.setValue("pdf");
        formatBox.setMaxWidth(Double.MAX_VALUE);

        perFileFormat.getItems().addAll(
                "auto",
                "pdf",
                "png",
                "jpg",
                "jpeg",
                "docx",
                "xlsx",
                "pptx",
                "html",
                "txt",
                "tiff",
                "webp"
        );
        perFileFormat.setValue("auto");

        Button browseFolder = new Button("Browse...");
        browseFolder.setOnAction(e -> chooseFolder());

        HBox folderBox = new HBox(10, outputFolder, browseFolder);
        HBox.setHgrow(outputFolder, Priority.ALWAYS);
        outputFolder.setPromptText("Select destination...");

        Button convertBtn = new Button("START CONVERSION");
        convertBtn.getStyleClass().add("cta-button");
        convertBtn.setMaxWidth(Double.MAX_VALUE);
        convertBtn.setOnAction(e -> startConversion());

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        VBox settingsCard = new VBox(
                20,
                new Label("Global Output Format"),
                formatBox,
                new Label("Per-File Override"),
                perFileFormat,
                new Label("Save Destination"),
                folderBox,
                progressBar,
                statusLabel,
                convertBtn
        );
        settingsCard.getStyleClass().add("card");
        settingsCard.setPrefWidth(400);

        HBox centerSplit = new HBox(40, leftColumn, settingsCard);

        setTop(topBar);
        setCenter(centerSplit);

        enableDragDrop();

        if (initialFile != null) {
            fileList.getItems().add(initialFile);
        }
        Label privacy = new Label(
        	    "🔒 All processing is local. No uploads. No tracking."
        	);

        	privacy.setStyle("-fx-text-fill:#8b949e;");
        	setBottom(privacy);
        	
        	batchFolderMode.setOnAction(e -> {

        	    if(batchFolderMode.isSelected()){

        	        DirectoryChooser chooser = new DirectoryChooser();
        	        chooser.setTitle("Select Folder to Convert");

        	        File dir = chooser.showDialog(getScene().getWindow());

        	        if(dir != null){

        	            fileList.getItems().clear();

        	            File[] files = dir.listFiles(f -> f.isFile());

        	            if(files != null){
        	                fileList.getItems().addAll(files);
        	            }

        	            statusLabel.setText("📂 Loaded " + fileList.getItems().size() + " files from folder");

        	        } else {
        	            batchFolderMode.setSelected(false);
        	        }
        	    }
        	});
    }

    private void addFiles() {

        FileChooser chooser = new FileChooser();
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());

        if (files != null) {
            fileList.getItems().addAll(files);
        }
    }

    private void chooseFolder() {

        DirectoryChooser chooser = new DirectoryChooser();
        File folder = chooser.showDialog(getScene().getWindow());

        if (folder != null) {
            outputFolder.setText(folder.getAbsolutePath());
        }
    }

    private void enableDragDrop() {

        dropZone.setOnDragOver(event -> {

            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);

                if (!dropZone.getStyleClass().contains("drop-zone-active")) {
                    dropZone.getStyleClass().add("drop-zone-active");
                }
            }

            event.consume();
        });

        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drop-zone-active"));

        dropZone.setOnDragDropped(event -> {

            if (event.getDragboard().hasFiles()) {
                fileList.getItems().addAll(event.getDragboard().getFiles());
            }

            dropZone.getStyleClass().remove("drop-zone-active");
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private String buildOutputPath(File input, File outputDir, String format) {

        String name = input.getName();
        int dot = name.lastIndexOf('.');

        if (dot != -1) {
            name = name.substring(0, dot);
        }

        return new File(outputDir, name + "." + format).getAbsolutePath();
    }

    private void startConversion() {

        List<File> files = new ArrayList<>(fileList.getItems());

        if (files.isEmpty()) {
            statusLabel.setText("No files selected.");
            return;
        }

        if (outputFolder.getText().isEmpty()) {
            statusLabel.setText("Select output folder.");
            return;
        }

        String globalFormat = formatBox.getValue();
        File outputDir = new File(outputFolder.getText());

        progressBar.setVisible(true);
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                int total = files.size();
                int count = 0;

                for (File file : files) {
                    count++;

                    String format = perFileFormat.getValue().equals("auto")
                            ? globalFormat
                            : perFileFormat.getValue();

                    Platform.runLater(() ->
                            statusLabel.setText("Converting: " + file.getName()));

                    String outputPath = buildOutputPath(file, outputDir, format);

                    ConversionRouter.convert(
                            file,
                            null,
                            format,
                            outputPath
                    );

                    double progress = (double) count / total;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                    

                }

                Platform.runLater(() -> statusLabel.setText("Conversion complete"));

            } catch (Exception e) {
                Platform.runLater(() ->
                        statusLabel.setText("Failed: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private StackPane buildConversionGuide() {

    	Button trigger = new Button("⚡ Supported Conversions");
    	trigger.getStyleClass().add("button");
    	trigger.setStyle("""
    	    -fx-background-color:#21262d;
    	    -fx-text-fill:white;
    	    -fx-background-radius:8;
    	    -fx-padding:6 14;
    	""");

        conversionPopupContent.getStyleClass().add("conversion-guide-popup");
        conversionPopupContent.getChildren().setAll(buildConversionMatrix());
        conversionPopupContent.setStyle("""
        	    -fx-background-color:#0d1117;
        	    -fx-background-radius:14;
        	    -fx-border-radius:14;
        	    -fx-border-color:#30363d;
        	    -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.6), 20,0,0,4);
        	""");
        conversionPopup.getContent().setAll(conversionPopupContent);
        conversionPopup.setAutoHide(true);
        conversionPopup.setAutoFix(true);

        trigger.setOnAction(e -> {

            if(conversionPopup.isShowing()){
                conversionPopup.hide();
            } else {
                showConversionPopup(trigger);
            }
        });
        

        StackPane wrapper = new StackPane(trigger);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinWidth(220);
        return wrapper;
    }
    
    private VBox buildConversionMatrix(){

        VBox container = new VBox(18);
        container.setPadding(new Insets(16));
        container.setPrefWidth(300);

        Label header = new Label("⚡ Supported Conversions");
        header.setStyle("""
            -fx-text-fill:white;
            -fx-font-size:16px;
            -fx-font-weight:bold;
        """);

        Label sub = new Label("Click to select output format");
        sub.setStyle("-fx-text-fill:#8b949e;");

        container.getChildren().addAll(header, sub);

        // 🔥 DYNAMIC DATA FROM ROUTER
        var routes = ConversionRouter.getSupportedRoutes();

        routes.forEach((source, targets) -> {
            container.getChildren().add(createAdvancedCard(source, targets));
        });

        return container;
    }
    
    
    
    private String getIcon(String type){

        return switch(type.toLowerCase()){
            case "pdf" -> "📄";
            case "docx", "pptx", "xlsx" -> "🧾";
            case "png", "jpg", "jpeg" -> "🖼";
            case "html" -> "🌐";
            case "md", "markdown" -> "📝";
            default -> "📁";
        };
    }
    
    private VBox createAdvancedCard(String source, List<String> targets){

        Label title = new Label(getIcon(source) + " " + source.toUpperCase());
        title.setStyle("""
            -fx-text-fill:white;
            -fx-font-size:13px;
            -fx-font-weight:bold;
        """);

        HBox formatsRow = new HBox(6);

        for(String t : targets){

            Label chip = new Label(t.toUpperCase());
            chip.setStyle("""
                -fx-background-color:#21262d;
                -fx-text-fill:#c9d1d9;
                -fx-padding:3 8;
                -fx-background-radius:6;
            """);

            chip.setOnMouseEntered(e -> chip.setStyle("""
                -fx-background-color:#30363d;
                -fx-text-fill:white;
                -fx-padding:3 8;
                -fx-background-radius:6;
            """));

            chip.setOnMouseExited(e -> chip.setStyle("""
                -fx-background-color:#21262d;
                -fx-text-fill:#c9d1d9;
                -fx-padding:3 8;
                -fx-background-radius:6;
            """));

            // 🔥 CLICK = SELECT FORMAT
            chip.setOnMouseClicked(e -> {
                formatBox.setValue(t.toLowerCase());
                statusLabel.setText("Selected: " + source + " → " + t);
                conversionPopup.hide();
            });

            formatsRow.getChildren().add(chip);
        }

        VBox card = new VBox(8, title, formatsRow);
        card.setPadding(new Insets(12));

        card.setStyle("""
            -fx-background-color:#161b22;
            -fx-background-radius:12;
            -fx-border-radius:12;
            -fx-border-color:#30363d;
        """);

        card.setOnMouseEntered(e -> card.setStyle("""
            -fx-background-color:#21262d;
            -fx-background-radius:12;
            -fx-border-radius:12;
            -fx-border-color:#58a6ff;
        """));

        card.setOnMouseExited(e -> card.setStyle("""
            -fx-background-color:#161b22;
            -fx-background-radius:12;
            -fx-border-radius:12;
            -fx-border-color:#30363d;
        """));

        return card;
    }
    
    private VBox createCard(String title, String formats){

        Label titleLabel = new Label(title);
        titleLabel.setStyle("""
            -fx-text-fill:white;
            -fx-font-size:13px;
            -fx-font-weight:bold;
        """);

        Label desc = new Label(formats);
        desc.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11px;");

        VBox card = new VBox(5, titleLabel, desc);
        card.setPadding(new Insets(10));

        card.setStyle("""
            -fx-background-color:#161b22;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-border-color:#30363d;
        """);

        // 🔥 HOVER EFFECT (PREMIUM FEEL)
        card.setOnMouseEntered(e -> card.setStyle("""
            -fx-background-color:#21262d;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-border-color:#58a6ff;
        """));

        card.setOnMouseExited(e -> card.setStyle("""
            -fx-background-color:#161b22;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-border-color:#30363d;
        """));
        card.setOnMouseClicked(e -> {

            String format = extractPrimaryFormat(formats);

            formatBox.setValue(format.toLowerCase());

            statusLabel.setText("Selected format: " + format);
        });
        
        return card;
        
        
    }
    
    private String extractPrimaryFormat(String formats){
        return formats.split("•")[0].trim();
    }

    private void showConversionPopup(Button trigger) {

        if (getScene() == null || getScene().getWindow() == null) {
            return;
        }

        Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds == null) {
            return;
        }

        double x = bounds.getMinX() - 40;
        double y = bounds.getMaxY() + 10;

        if (!conversionPopup.isShowing()) {
            conversionPopup.show(getScene().getWindow(), x, y);
            return;
        }

        conversionPopup.setX(x);
        conversionPopup.setY(y);
    }

    private void hideConversionPopupIfOutside() {

        Platform.runLater(() -> {
            if (!conversionPopupContent.isHover()) {
                conversionPopup.hide();
            }
        });
    }
}
