package com.yourfamily.pdf.secure_pdf_converter.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.yourfamily.pdf.secure_pdf_converter.core.conversion.ConversionRouter;
import com.yourfamily.pdf.secure_pdf_converter.core.tools.ToolHealthChecker;
import com.yourfamily.pdf.secure_pdf_converter.core.tools.ToolSettings;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ConversionView extends BorderPane {

    private final ListView<File> fileList = new ListView<>();
    private final ComboBox<String> formatBox = new ComboBox<>();
    private final TextField outputFolder = new TextField();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready to convert");
    private final VBox dropZone = new VBox();
    private final Popup conversionPopup = new Popup();
    private final VBox conversionPopupContent = new VBox(8);
    private final Map<File, String> perFileMap = new HashMap<>();
    private final CheckBox batchFolderMode = new CheckBox("Batch convert entire folder");
    private final CheckBox perFileMode = new CheckBox("Per-file format selection");

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
        Button toolsStatusBtn = new Button("🔧 Tools");

	     // 🎨 COLOR LOGIC
	     String overallStatus = ToolHealthChecker.getOverallStatus();
	
	     switch (overallStatus) {
	         case "GOOD" -> toolsStatusBtn.setStyle("-fx-background-color: green;");
	         case "WARN" -> toolsStatusBtn.setStyle("-fx-background-color: orange;");
	         case "BAD"  -> toolsStatusBtn.setStyle("-fx-background-color: red;");
	     }
	
	     // 👉 CLICK HANDLER
	     toolsStatusBtn.setOnAction(e -> showToolsPopup());
	
	     HBox topBar = new HBox(20, backBtn, leftSpacer, conversionGuide, toolsStatusBtn, rightSpacer);
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
                "webp"
        );
        formatBox.setValue("pdf");
        formatBox.setOnAction(e -> {

            if (!perFileMode.isSelected()) return;

            String global = formatBox.getValue();

            fileList.getItems().forEach(file -> {
                if (!perFileMap.containsKey(file)) {
                    perFileMap.put(file, global);
                }
            });
            fileList.setStyle("""
            	    -fx-control-inner-background: #0d1117;
            	    -fx-background-color: #0d1117;
            	""");
            fileList.refresh();
        });
        formatBox.setMaxWidth(Double.MAX_VALUE);

                
      
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
                perFileMode,
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
        
        fileList.setCellFactory(list -> new ListCell<>() {

            private final HBox row = new HBox(10);
            private final Label name = new Label();
            
            private final ComboBox<String> formatSelector = new ComboBox<>();

            {
                formatSelector.getItems().addAll(
                    "pdf","png","jpg","jpeg","docx","xlsx","pptx","html","txt","webp"
                );
                
                name.setStyle("""
                	    -fx-text-fill: #e6edf3;
                	    -fx-font-size: 13px;
                	""");
                
                
                formatSelector.setPrefWidth(110);

                row.getChildren().addAll(name, formatSelector);
                HBox.setHgrow(name, Priority.ALWAYS);

                row.setStyle("""
                	    -fx-padding:10;
                	    -fx-background-color:#0d1117;
                	    -fx-border-color:#30363d;
                	    -fx-border-width:0 0 1 0;
                	""");
            }
            
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);

                if (empty || file == null) {
                    setGraphic(null);
                    return;
                }

                name.setText(file.getName());
                name.setStyle("""
                	    -fx-text-fill: #e6edf3;
                	    -fx-font-size: 13px;
                	    -fx-font-weight: 500;
                	""");
                boolean enabled = perFileMode.isSelected();

                row.getChildren().clear();

                if (enabled) {
                    row.getChildren().addAll(name, formatSelector);
                } else {
                    row.getChildren().add(name);
                }

                if (enabled) {

                    String saved = perFileMap.get(file);

                    if (saved != null) {
                        formatSelector.setValue(saved);
                    } else {
                        String global = ConversionView.this.formatBox.getValue();
                        formatSelector.setValue(global);
                        perFileMap.put(file, global);
                    }
                }

                formatSelector.setPrefWidth(110);

             // ✅ Define the action ONCE per cell, not every time it updates.
             formatSelector.setOnAction(e -> {
                 File currentFile = getItem();
                 if (currentFile != null) {
                     perFileMap.put(currentFile, formatSelector.getValue());
                 }
             });
          // Store the default style so we can restore it accurately
             String defaultStyle = "-fx-padding:10; -fx-background-color:#0d1117; -fx-border-color:#30363d; -fx-border-width:0 0 1 0;";
             String hoverStyle = "-fx-padding:10; -fx-background-color:#21262d; -fx-border-color:#30363d; -fx-border-width:0 0 1 0;";

             row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
             row.setOnMouseExited(e -> row.setStyle(defaultStyle));
                setGraphic(row);
            }
            
        }); 
        
        perFileMode.setOnAction(e -> {
            fileList.refresh();
        });
        
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

        // 1. Snapshot data
        List<File> files = new ArrayList<>(fileList.getItems());
        String globalFormat = formatBox.getValue();
        String outputDirPath = outputFolder.getText();

        if (files.isEmpty() || outputDirPath.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #ff7b72;");
            statusLabel.setText("❌ Error: Select files and a destination.");
            return;
        }

        File outputDir = new File(outputDirPath);

        // 2. Task
        Task<Void> conversionTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                int total = files.size();
                int count = 0;
                List<String> skippedFiles = new ArrayList<>();

                for (File file : files) {

                    if (isCancelled()) break;
                    count++;

                    String format = perFileMode.isSelected()
                            ? perFileMap.getOrDefault(file, globalFormat)
                            : globalFormat;

                    String from = safeExt(file);

                    // ⚠ Unknown extension
                    if (from.isEmpty()) {
                        updateMessage("⚠ Skipping (No Ext): " + file.getName());
                        updateProgress(count, total);
                        continue;
                    }

                    List<String> path = ConversionRouter.findConversionPath(from, format);

                    // 🔍 Multi-step path
                    if (path.size() > 2) {
                        updateMessage("🔍 Complex Path: " + String.join(" → ", path));
                        Thread.sleep(300);

                        for (int i = 0; i < path.size() - 1; i++) {
                            updateMessage("🛠 Step " + (i + 1) + "/" + (path.size() - 1)
                                    + ": " + path.get(i).toUpperCase());
                            Thread.sleep(200);
                        }
                    }

                    // ⚡ Skip same format
                    if (from.equals(format)) {
                        skippedFiles.add(file.getName());
                        updateProgress(count, total);
                        continue;
                    }

                    // ⏳ Processing
                    updateMessage("⏳ Processing: " + file.getName());

                    String outputPath = buildOutputPath(file, outputDir, format);

                    File result = ConversionRouter.smartConvert(file, from, format, outputPath);

                    updateMessage("✅ Saved: " + result.getName());

                    updateProgress(count, total);
                }

                // 🔥 FINAL MESSAGE (THIS IS THE ONE THAT MATTERS)
                if (!skippedFiles.isEmpty()) {

                    if (skippedFiles.size() == 1) {
                        updateMessage("🤨 " + skippedFiles.get(0) + " was already perfect. I did nothing.");
                    } else {
                        updateMessage("😂 " + skippedFiles.size() + " files were already perfect... I respect the confidence.");
                    }

                } else {
                    updateMessage("🎉 All " + total + " files converted successfully.");
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                // 🔥 DO NOT override message — ONLY style it
                statusLabel.textProperty().unbind();
                statusLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
            }

            @Override
            protected void failed() {
                super.failed();

                Throwable e = getException();

                statusLabel.textProperty().unbind();
                statusLabel.setStyle("-fx-text-fill: #ff7b72;");
                statusLabel.setText("❌ Error: " + e.getMessage());

                e.printStackTrace();
            }
        };

        // 3. Bind UI
        progressBar.setVisible(true);

        statusLabel.textProperty().unbind();
        statusLabel.textProperty().bind(conversionTask.messageProperty());

        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(conversionTask.progressProperty());

        // 4. Cancel handling
        conversionTask.setOnCancelled(e -> {
            statusLabel.textProperty().unbind();
            statusLabel.setStyle("-fx-text-fill: #f2cc60;");
            statusLabel.setText("⛔ Conversion cancelled");
        });

        // 5. Run
        Thread thread = new Thread(conversionTask);
        thread.setDaemon(true);
        thread.start();
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
    
    private VBox buildConversionMatrix() {

        VBox container = new VBox(16);
        container.setPadding(new Insets(16));
        container.setPrefWidth(700);
        container.setPrefHeight(500);

        // =========================
        // 🔥 DETECT INPUT FORMATS
        // =========================
        Set<String> detected = fileList.getItems()
                .stream()
                .map(this::safeExt)
                .collect(Collectors.toSet());

        // =========================
        // 🔥 HEADER (SMART UI)
        // =========================
        VBox header = new VBox(6);

        Label title = new Label("⚡ Smart Conversions");
        title.setStyle("-fx-text-fill:white; -fx-font-size:18px; -fx-font-weight:bold;");

        Label subtitle = new Label("Based on your selected files");
        subtitle.setStyle("-fx-text-fill:#8b949e;");

        HBox detectedRow = new HBox(6);

        for (String d : detected) {
            Label chip = new Label(d.toUpperCase());
            chip.setStyle("""
                -fx-background-color:#30363d;
                -fx-text-fill:white;
                -fx-padding:4 10;
                -fx-background-radius:20;
            """);
            detectedRow.getChildren().add(chip);
        }

        header.getChildren().addAll(title, subtitle, detectedRow);

        // 🔥 SMART MODE BANNER
        if (detected.size() > 1) {
            Label smart = new Label("⚡ Mixed files detected — choosee formats per file");
            smart.setStyle("-fx-text-fill:#58a6ff;");
            header.getChildren().add(smart);
        }

        // =========================
        // 🔍 SEARCH BAR
        // =========================
        TextField search = new TextField();
        search.setPromptText("Search conversions (pdf, docx...)");

        // =========================
        // 🧠 GRID (MAIN UI)
        // =========================
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(20);

        // Build initial grid
        buildGridContent(grid, detected, "");

        // 🔍 SEARCH FILTER LOGIC
        search.textProperty().addListener((obs, oldVal, val) -> {
            buildGridContent(grid, detected, val.toLowerCase());
        });

        container.getChildren().addAll(header, search, grid);

        return container;
    }
    	
    private void buildGridContent(GridPane grid, Set<String> detected, String filter) {

        grid.getChildren().clear();

        // 📄 DOCUMENTS
        VBox documents = createCategory("📄 Documents",
                Map.of(
                        "pdf", List.of("docx", "xlsx", "pptx"),
                        "docx", List.of("pdf", "html", "txt"),
                        "xlsx", List.of("pdf"),
                        "pptx", List.of("pdf")
                ),
                detected,
                filter
        );

        // 🖼 IMAGES
        VBox images = createCategory("🖼 Images",
                Map.of(
                        "pdf", List.of("png", "jpg"),
                        "png", List.of("pdf", "jpg", "webp", "svg"),
                        "jpg", List.of("pdf", "png"),
                        "webp", List.of("png")
                ),
                detected,
                filter
        );

        // 🌐 WEB / TEXT
        VBox web = createCategory("🌐 Web & Text",
                Map.of(
                        "html", List.of("pdf", "docx"),
                        "md", List.of("pdf"),
                        "docx", List.of("html", "txt")
                ),
                detected,
                filter
        );

        grid.add(documents, 0, 0);
        grid.add(images, 1, 0);
        grid.add(web, 2, 0);
    }
    
    private VBox createCategory(String titleText,
            Map<String, List<String>> data,
            Set<String> detected,
            String filter) {

	Label title = new Label(titleText);
	title.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");
	
	VBox box = new VBox(10);
	box.getChildren().add(title);
	
	data.forEach((source, targets) -> {
	
	// 🔍 FILTER LOGIC
	boolean matches = filter.isEmpty()
	|| source.contains(filter)
	|| targets.stream().anyMatch(t -> t.contains(filter));
	
	if (matches) {
	box.getChildren().add(createRow(source, targets, detected));
	}
	});
	
	return box;
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
    
    private VBox createRow(String source, List<String> targets, Set<String> detected) {

        Label title = new Label(getIcon(source) + " " + source.toUpperCase());
        title.setStyle("-fx-text-fill:white; -fx-font-weight:bold;");

        HBox outputs = new HBox(6);

        for (String t : targets) {

            Label chip = new Label(t.toUpperCase());
            chip.getStyleClass().add("chip");

            chip.setOnMouseClicked(e -> {
                formatBox.setValue(t);
                statusLabel.setText("Convert " + source + " → " + t);
                conversionPopup.hide();
            });

            outputs.getChildren().add(chip);
        }

        VBox row = new VBox(6, title, outputs);
        row.setPadding(new Insets(10));

        // 🔥 SMART HIGHLIGHT
        if (detected.contains(source)) {
            row.setStyle("""
                -fx-background-color:#1f2a35;
                -fx-border-color:#58a6ff;
                -fx-border-radius:10;
                -fx-background-radius:10;
            """);
        } else {
            row.setStyle("""
                -fx-background-color:#161b22;
                -fx-border-color:#30363d;
                -fx-border-radius:10;
                -fx-background-radius:10;
            """);
        }

        // ✨ HOVER EFFECT
             
        row.setOnMouseEntered(e ->
        row.setStyle("""
            -fx-background-color:#21262d;
            -fx-padding:10;
            -fx-background-radius:8;
        """)
    );

    
        
        return row;
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

        if (getScene() == null || getScene().getWindow() == null) return;

        Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds == null) return;

        double x = bounds.getMinX() - 40;
        double y = bounds.getMaxY() + 10;

        if (!conversionPopup.isShowing()) {

            conversionPopupContent.setOpacity(0);
            conversionPopupContent.setScaleX(0.95);
            conversionPopupContent.setScaleY(0.95);

            conversionPopup.show(getScene().getWindow(), x, y);

            // 🔥 FADE IN
            FadeTransition fade = new FadeTransition(Duration.millis(180), conversionPopupContent);
            fade.setFromValue(0);
            fade.setToValue(1);

            // 🔥 SCALE IN (premium feel)
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), conversionPopupContent);
            scale.setFromX(0.95);
            scale.setFromY(0.95);
            scale.setToX(1);
            scale.setToY(1);

            fade.play();
            scale.play();

            return;
        }

        conversionPopup.hide();
    }
    private void hideConversionPopupIfOutside() {

        Platform.runLater(() -> {
            if (!conversionPopupContent.isHover()) {
                conversionPopup.hide();
            }
        });
    }
    
    private void showToolsPopup() {

    	Stage popup = new Stage();
    	popup.setTitle("Tools Health Check");
    	// ✅ Forces the user to close this popup before interacting with the main app again
    	popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15;");

        var status = ToolHealthChecker.checkAllDetailed();

        status.forEach((tool, result) -> {

            HBox row = new HBox(10);

            CheckBox box = new CheckBox(tool + " - " + result);

            if (result.startsWith("✅")) {
                box.setStyle("-fx-text-fill: green;");
                box.setSelected(true);
            } else if (result.startsWith("⚠")) {
                box.setStyle("-fx-text-fill: orange;");
            } else {
                box.setStyle("-fx-text-fill: red;");
            }

            box.setDisable(true);

            row.getChildren().add(box);

            // 🔥 ADD FIX BUTTON IF NOT OK
            if (!result.startsWith("✅")) {

                Button fixBtn = new Button("Fix");

                fixBtn.setOnAction(e -> {

                    FileChooser fc = new FileChooser();
                    fc.setTitle("Locate " + tool);

                    File file = fc.showOpenDialog(null);

                    if (file != null) {

                        // 🔥 save path (you must have ToolSettings class)
                        ToolSettings.set(tool.toLowerCase(), file.getAbsolutePath());

                        popup.close();
                        showToolsPopup(); // refresh
                    }
                });

                row.getChildren().add(fixBtn);
            }

            root.getChildren().add(row);
        });

        // 🔥 REFRESH BUTTON
        Button refresh = new Button("Refresh");

        refresh.setOnAction(e -> {
            popup.close();
            showToolsPopup();
        });

        root.getChildren().add(refresh);

        Scene scene = new Scene(root, 400, 300);
        popup.setScene(scene);
        popup.show();
    }
    
    private String safeExt(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
