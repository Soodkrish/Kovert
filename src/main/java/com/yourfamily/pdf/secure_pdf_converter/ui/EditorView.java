package com.yourfamily.pdf.secure_pdf_converter.ui;

import com.yourfamily.pdf.secure_pdf_converter.DocumentLoader;
import com.yourfamily.pdf.secure_pdf_converter.LoadedDocument;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlanner;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionRequest;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision.PrecisionRedactionEngine;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.word.RedactionPlanMerger;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.word.WordRedactionEngine;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.word.TesseractWordEngine;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;

import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

public class EditorView extends BorderPane {

    private final PdfPreviewPane preview = new PdfPreviewPane();
    private LoadedDocument document;
    private PDFRenderer renderer;
    private final MainWindow app;

    private ThumbnailSidebar sidebar;

    private int currentPage = 0;
    private int pageCount = 0;

    private final Map<Integer,List<Shape>> pageShapes = new HashMap<>();

    private final List<String> wordList = new ArrayList<>();
    private List<RedactionPlan> wordPlans = new ArrayList<>();

    private final Label pageLabel = new Label();
    private final Map<Integer, List<com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord>> ocrCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final TextField wordInput = new TextField();
    private final ListView<String> wordListView = new ListView<>();
    private final Label detectionLabel = new Label();
    private final com.yourfamily.pdf.secure_pdf_converter.core.redaction.engine.PdfTextEngine pdfTextEngine
    = new com.yourfamily.pdf.secure_pdf_converter.core.redaction.engine.PdfTextEngine();
    
    int DPI = 150;
    public EditorView(File file, boolean redactionMode, MainWindow app) {

        this.app = app;

        SplitPane mainSplit = new SplitPane();

        /* ---------------- LEFT SIDEBAR ---------------- */

        VBox wordPanel = buildWordPanel();

        /* ---------------- TOOLBAR ---------------- */

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER);

        Button backBtn = new Button("← Back");
        Button prevPage = new Button("◀");
        Button nextPage = new Button("▶");

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");

        ToggleGroup toolGroup = new ToggleGroup();

        ToggleButton rectBtn = new ToggleButton("Box");
        ToggleButton circleBtn = new ToggleButton("Circle");
        ToggleButton brushBtn = new ToggleButton("Brush");
        
        toolbar.getChildren().add(detectionLabel);

        rectBtn.setToggleGroup(toolGroup);
        circleBtn.setToggleGroup(toolGroup);
        brushBtn.setToggleGroup(toolGroup);

        toolGroup.selectedToggleProperty().addListener((obs,o,n)->{

            if(n==rectBtn)
                preview.setTool(PdfPreviewPane.Tool.RECTANGLE);

            else if(n==circleBtn)
                preview.setTool(PdfPreviewPane.Tool.ELLIPSE);

            else if(n==brushBtn)
                preview.setTool(PdfPreviewPane.Tool.BRUSH);
        });

        Button applyBtn = new Button("APPLY REDACTION");

        toolbar.getChildren().addAll(
                backBtn,
                prevPage,pageLabel,nextPage,
                undoBtn,redoBtn,
                rectBtn,circleBtn,brushBtn,
                applyBtn
        );

        backBtn.setOnAction(e -> app.showLanding());
        undoBtn.setOnAction(e -> preview.undo());
        redoBtn.setOnAction(e -> preview.redo());

        prevPage.setOnAction(e -> previousPage());
        nextPage.setOnAction(e -> nextPage());

        applyBtn.setOnAction(e -> performRedaction());

        /* ---------------- CANVAS ---------------- */

        ScrollPane scroll = new ScrollPane(preview);
        scroll.setPannable(true);
        scroll.setOnScroll(e -> {

            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;

            double currentZoom = preview.getScaleX();

            double newZoom = Math.max(0.5, Math.min(3.0, currentZoom * zoomFactor));

            preview.setZoom(newZoom);

            e.consume();
        });
        // ❌ DO NOT FIT (this was breaking everything)
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);

        preview.setOnDrawStart(() -> scroll.setPannable(false));
        preview.setOnDrawEnd(() -> scroll.setPannable(true));

        VBox center = new VBox(toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        /* ---------------- LOAD FILE ---------------- */

        loadFile(file);

        mainSplit.getItems().addAll(sidebar,center,wordPanel);

        mainSplit.setDividerPositions(0.15,0.80);

        setCenter(mainSplit);
        
        
    }

    /* ================= WORD PANEL ================= */

    private VBox buildWordPanel(){

        VBox panel = new VBox(10);

        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);

        Label title = new Label("Words to redact");

        Button addWordBtn = new Button("Add");
        Button scanBtn = new Button("Scan Words");

        HBox inputRow = new HBox(5,wordInput,addWordBtn);

        wordListView.setPrefHeight(200);

        addWordBtn.setOnAction(e -> addWord());

        scanBtn.setOnAction(e -> scanWords());
        
     // ✅ LIVE SEARCH
      /*  wordInput.textProperty().addListener((obs, old, val) -> {

            if(val == null || val.trim().isEmpty()){
                wordPlans.clear();
                renderPage(currentPage);
                return;
            }

            // 🔥 ONLY live preview (DO NOT TOUCH wordList)
            scanWordsLive(val.trim());
        });
*/        panel.getChildren().addAll(
                title,
                inputRow,
                wordListView,
                scanBtn
        );

        return panel;
    }

    /* ================= ADD WORD ================= */

    private void addWord(){

        String word = wordInput.getText().trim();

        if(word.isEmpty()) return;

        if(!wordList.contains(word)){
            wordList.add(word);
            wordListView.getItems().add(word);
        }

        wordInput.clear();

        // 🔥 OPTIONAL: auto-scan after adding
        scanWords();
    }

    /* ================= SCAN WORDS ================= */

    private void scanWords(){

        if(wordList.isEmpty()) return;

        runScan(new ArrayList<>(wordList));
    }
    
    
    private void scanWordsLive(String input){

        if(input == null || input.trim().length() < 2){
            wordPlans.clear();
            renderPage(currentPage);
            return;
        }

        runScan(List.of(input.trim()));
    }
    
    private int levenshteinDistance(String a, String b) {

        int[][] dp = new int[a.length()+1][b.length()+1];

        for(int i=0;i<=a.length();i++) dp[i][0]=i;
        for(int j=0;j<=b.length();j++) dp[0][j]=j;

        for(int i=1;i<=a.length();i++){
            for(int j=1;j<=b.length();j++){

                int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;

                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j]+1, dp[i][j-1]+1),
                        dp[i-1][j-1]+cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
    
    private void runScan(List<String> targets){

        try{

            long startTime = System.currentTimeMillis();

            int pages = document.forRenderingOnly().getNumberOfPages();

            List<String> cleanTargets = targets.stream()
                    .map(this::cleanText)
                    .filter(t -> t.length() >= 2)
                    .toList();

            if(cleanTargets.isEmpty()){
                wordPlans.clear();
                return;
            }

            List<RedactionPlan> tempPlans = Collections.synchronizedList(new ArrayList<>());

            IntStream.range(0, pages).parallel().forEach(page -> {

                try{

                    List<com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord> words =
                            ocrCache.computeIfAbsent(page, p -> {

                                try{

                                    List<com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord> textWords = Collections.emptyList();

                                    // 🔥 TRY TEXT ENGINE
                                    try{
                                        textWords = pdfTextEngine.extractWords(document.forRenderingOnly(), p);
                                    }catch(Exception ignored){}

                                    // 🔥 ALWAYS GET OCR ALSO
                                    BufferedImage image;
                                    synchronized (renderer){
                                        image = renderer.renderImageWithDPI(p, DPI);
                                    }

                                    var ocr = new com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWordEngine();
                                    List<com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord> ocrWords =
                                            ocr.extractWords(image);

                                    // 🔥 MERGE BOTH (CRITICAL FIX)
                                    if(textWords == null || textWords.size() < ocrWords.size() * 0.5){
                                        return ocrWords; // OCR wins
                                    }

                                    // combine both
                                    List<com.yourfamily.pdf.secure_pdf_converter.core.redaction.ocr.OcrWord> merged =
                                            new ArrayList<>(ocrWords);

                                    merged.addAll(textWords);

                                    return merged;

                                }catch(Exception e){
                                    e.printStackTrace();
                                    return Collections.emptyList();
                                }
                            });

                    Set<Long> seen = new HashSet<>();

                    for(var w : words){

                        String raw = w.text();
                        if(raw == null) continue;

                        String text = raw.trim();
                        if(text.length() < 2) continue;

                        if(!containsLetterOrDigit(text)) continue;

                        String cleanWord = cleanText(text);
                        if(cleanWord.isEmpty()) continue;

                        for(String cleanTarget : cleanTargets){

                            // 🔥 STRONG MATCH LOGIC (RESTORED)
                            if(cleanWord.contains(cleanTarget)){

                                long key =
                                        (((long) page) << 32) |
                                        (((long) (w.x() * 1000)) << 16) |
                                        (long) (w.y() * 1000);

                                if(!seen.add(key)) break;

                                tempPlans.add(new RedactionPlan(
                                        page,
                                        w.x(),
                                        w.y(),
                                        w.width(),
                                        w.height(),
                                        RedactionPlan.ShapeType.RECTANGLE
                                ));

                                break;
                            }
                        }
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
            });

            wordPlans = new ArrayList<>(tempPlans);

            Map<Integer,Integer> counts = new HashMap<>();
            for(RedactionPlan p : wordPlans){
                counts.merge(p.pageIndex(),1,Integer::sum);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("🔥 Scan completed in: " + totalTime + " ms");
            System.out.println("🔥 Total matches: " + wordPlans.size());

            javafx.application.Platform.runLater(() -> {
                sidebar.showMatches(counts);
                detectionLabel.setText("Matches: " + wordPlans.size());
                renderPage(currentPage);
            });

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    private String cleanText(String input){

        StringBuilder sb = new StringBuilder(input.length());

        for(int i=0;i<input.length();i++){
            char c = input.charAt(i);

            if(Character.isLetterOrDigit(c)){
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }
    
    private boolean containsLetterOrDigit(String text){

        for(int i=0;i<text.length();i++){
            char c = text.charAt(i);
            if(Character.isLetterOrDigit(c)){
                return true;
            }
        }

        return false;
    }
    /* ================= LOAD FILE ================= */

    private void loadFile(File file){

        try{

            DocumentLoader loader = new DocumentLoader();

            document = loader.load(file.toPath());

            renderer = new PDFRenderer(document.forRenderingOnly());

            pageCount = document.forRenderingOnly().getNumberOfPages();

            sidebar = new ThumbnailSidebar(renderer,pageCount,this::renderPage);

            renderPage(0);

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    /* ================= PAGE NAVIGATION ================= */

    private void nextPage(){

        if(currentPage < pageCount-1)
            renderPage(currentPage+1);
    }

    private void previousPage(){

        if(currentPage > 0)
            renderPage(currentPage-1);
    }

    /* ================= RENDER PAGE ================= */

    private void renderPage(int pageIndex){

        try{

            saveCurrentPageShapes();

            BufferedImage image;

            // 🔥 PRIMARY RENDER (DPI + RGB)
            try {
                synchronized (renderer) {
                    image = renderer.renderImageWithDPI(
                            pageIndex,
                            DPI,
                            org.apache.pdfbox.rendering.ImageType.RGB
                    );
                }
            } catch (Exception e) {

                System.out.println("⚠️ DPI render failed, fallback -> page " + pageIndex);

                // 🔥 FALLBACK 1 — basic render
                try {
                    synchronized (renderer) {
                        image = renderer.renderImage(pageIndex);
                    }
                } catch (Exception ex) {

                    System.out.println("⚠️ Basic render failed, retrying lower DPI -> page " + pageIndex);

                    // 🔥 FALLBACK 2 — lower DPI safe mode
                    try {
                        synchronized (renderer) {
                            image = renderer.renderImageWithDPI(
                                    pageIndex,
                                    120,
                                    org.apache.pdfbox.rendering.ImageType.RGB
                            );
                        }
                    } catch (Exception finalEx) {
                        System.out.println("❌ Page failed completely: " + pageIndex);
                        finalEx.printStackTrace();
                        return;
                    }
                }
            }

            // 🔥 SAFETY CHECK
            if(image == null){
                System.out.println("❌ Render returned NULL: " + pageIndex);
                return;
            }

            // 🔥 SET IMAGE (OCR + preview uses SAME image)
            preview.setImage(SwingFXUtils.toFXImage(image,null));

            // DEBUG
            System.out.println("PREVIEW SIZE: " + preview.getWidth() + " x " + preview.getHeight());
            System.out.println("IMAGE SIZE: " + image.getWidth() + " x " + image.getHeight());

            // 🔥 CLEAR + RELOAD
            preview.getOverlay().getChildren().clear();

            loadPageShapes(pageIndex);

            highlightWordMatches(pageIndex);

            currentPage = pageIndex;

            pageLabel.setText("Page "+(pageIndex+1)+" / "+pageCount);

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    /* ================= SHAPE STORAGE ================= */

    private void saveCurrentPageShapes(){

        pageShapes.put(currentPage,new ArrayList<>(preview.getShapes()));
    }

    private void loadPageShapes(int page){

        preview.getShapes().clear();

        List<Shape> shapes = pageShapes.get(page);

        if(shapes == null)
            return;

        for(Shape s : shapes){

            preview.getShapes().add(s);

            preview.getOverlay().getChildren().add(s);
        }
    }

    /* ================= WORD HIGHLIGHTS ================= */

    private void highlightWordMatches(int page) {

        Pane overlay = preview.getOverlay();

        overlay.getChildren().removeIf(n ->
            n instanceof Shape s &&
            Color.YELLOW.equals(s.getStroke())
        );

        for (RedactionPlan p : wordPlans) {

            if (p.pageIndex() != page)
                continue;

            Rectangle r = new Rectangle(
                    p.pdfX(),
                    p.pdfY(),
                    p.pdfWidth(),
                    p.pdfHeight()
            );

            r.setStroke(Color.YELLOW);
            r.setFill(Color.color(1, 1, 0, 0.25));
            r.setStrokeWidth(2);
            r.setMouseTransparent(false);
            r.setPickOnBounds(false); // 🔥 THIS IS THE MAGIC LINE

            r.setOnMouseClicked(e -> {
                overlay.getChildren().remove(r);
                e.consume();
            });

            // ✅ HOVER UX
            r.setOnMouseEntered(e -> r.setStroke(Color.ORANGE));
            r.setOnMouseExited(e -> r.setStroke(Color.YELLOW));

            overlay.getChildren().add(r);
        }
    }

    /* ================= APPLY REDACTION ================= */

    private void performRedaction(){

        try{

            saveCurrentPageShapes();

            // 🔥 STEP 1 — start with OCR plans (IMAGE SPACE)
            List<RedactionPlan> plans = new ArrayList<>(wordPlans);

            // 🔥 STEP 2 — add manual shapes (ALSO IMAGE SPACE)
            for(var entry : pageShapes.entrySet()){

                int pageIndex = entry.getKey();

                for(Shape shape : entry.getValue()){

                    double x=0,y=0,w=0,h=0;
                    RedactionPlan.ShapeType type = RedactionPlan.ShapeType.RECTANGLE;

                    if(shape instanceof Rectangle r){
                        x=r.getX();
                        y=r.getY();
                        w=r.getWidth();
                        h=r.getHeight();
                        type = RedactionPlan.ShapeType.RECTANGLE;
                    }
                    else if(shape instanceof Ellipse e){
                        x=e.getCenterX()-e.getRadiusX();
                        y=e.getCenterY()-e.getRadiusY();
                        w=e.getRadiusX()*2;
                        h=e.getRadiusY()*2;
                        type = RedactionPlan.ShapeType.ELLIPSE;
                    }
                    else if(shape instanceof Path p){

                        var b = p.getBoundsInLocal();

                        x = b.getMinX() + p.getLayoutX();
                        y = b.getMinY() + p.getLayoutY();
                        w = b.getWidth();
                        h = b.getHeight();

                        type = RedactionPlan.ShapeType.PATH;
                    }

                    // ✅ NO conversion here
                    plans.add(new RedactionPlan(
                            pageIndex,
                            x,
                            y,
                            w,
                            h,
                            type
                    ));
                }
            }

            // 🔥 STEP 3 — SINGLE CONVERSION (CRITICAL)
            Image img = preview.getImage();

            double scale = 72.0 / DPI;

            List<RedactionPlan> converted = new ArrayList<>();

            for(RedactionPlan p : plans){

                double pdfX = p.pdfX() * scale;

                double pdfY = (img.getHeight() - p.pdfY() - p.pdfHeight()) * scale;

                double pdfW = p.pdfWidth() * scale;
                double pdfH = p.pdfHeight() * scale;

                converted.add(new RedactionPlan(
                        p.pageIndex(),
                        pdfX,
                        pdfY,
                        pdfW,
                        pdfH,
                        p.shapeType()
                ));
            }

            // 🔥 STEP 4 — merge AFTER conversion
            converted = RedactionPlanMerger.merge(converted);

            // 🔥 SAVE
            FileChooser saver = new FileChooser();

            saver.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF","*.pdf")
            );

            File out = saver.showSaveDialog(getScene().getWindow());

            if(out == null)
                return;

            PrecisionRedactionEngine engine = new PrecisionRedactionEngine();

            engine.applyAndSave(document, converted, out.toPath());

            new Alert(Alert.AlertType.INFORMATION,"Redaction complete").showAndWait();

        }catch(Exception ex){

            ex.printStackTrace();

            new Alert(Alert.AlertType.ERROR,"Redaction failed").showAndWait();
        }
    }
}
