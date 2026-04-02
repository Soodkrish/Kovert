package com.yourfamily.pdf.secure_pdf_converter.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.*;

public final class PdfPreviewPane extends StackPane {

    public enum Tool { NONE, RECTANGLE, ELLIPSE, BRUSH }

    private final ImageView imageView = new ImageView();
    private final Pane overlay = new Pane();
    private final Group contentGroup = new Group();

    private final Scale zoomScale = new Scale(1,1);

    private Tool activeTool = Tool.NONE;

    private Image image;

    private double startX,startY;
    private Shape currentShape;

    private double panStartX,panStartY;

    private final List<Shape> shapes = new ArrayList<>();
    private final Set<Shape> selectedShapes = new HashSet<>();

    private final Stack<Shape> undoStack = new Stack<>();
    private final Stack<Shape> redoStack = new Stack<>();

    private Runnable onDrawStart;
    private Runnable onDrawEnd;

    public PdfPreviewPane(){

        setStyle("-fx-background-color:#111;");
        setCursor(Cursor.OPEN_HAND);

        imageView.setSmooth(true);

        // ✅ Prevent layout interference
        imageView.setManaged(false);
        imageView.setPickOnBounds(false);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetY(6);
        shadow.setColor(Color.rgb(0,0,0,0.6));
        imageView.setEffect(shadow);

        overlay.setManaged(false);
        overlay.setPickOnBounds(true);
        contentGroup.setManaged(false);

        contentGroup.getChildren().addAll(imageView, overlay);
        contentGroup.getTransforms().add(zoomScale);
        layoutBoundsProperty().addListener((obs, old, bounds) -> {
            if(bounds.getWidth() > 0 && bounds.getHeight() > 0){
                requestLayout();
            }
        });
        // 🔥 CRITICAL: wrap inside Pane (NOT direct StackPane child)
        Pane root = new Pane(contentGroup);

        root.setPrefSize(0, 0);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

        getChildren().add(root);

        // 🔥 FORCE POSITION
        contentGroup.setLayoutX(0);
        contentGroup.setLayoutY(0);
        setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            zoomAt(factor, e.getSceneX(), e.getSceneY());
        });
        enableInteraction();
        
        enableKeyboardDelete();

        setFocusTraversable(true);
        
    }

    /* ================= IMAGE ================= */

    public void setImage(Image image){

        if(image==null) return;

        this.image = image;

        imageView.setImage(image);

        // ✅ HARD SIZE LOCK (this actually works)
        imageView.setFitWidth(image.getWidth());
        imageView.setFitHeight(image.getHeight());
        imageView.setPreserveRatio(false);

        imageView.setLayoutX(0);
        imageView.setLayoutY(0);

        // ✅ overlay exact match
        overlay.setPrefWidth(image.getWidth());
        overlay.setPrefHeight(image.getHeight());

        overlay.setLayoutX(0);
        overlay.setLayoutY(0);

        shapes.clear();
        overlay.getChildren().clear();

        undoStack.clear();
        redoStack.clear();
        selectedShapes.clear();

        setZoom(1.0);

        // 🧪 DEBUG (keep this)
        System.out.println("IMAGE SIZE: " + image.getWidth() + " x " + image.getHeight());
        System.out.println("IMAGEVIEW SIZE: " + imageView.getBoundsInParent().getWidth() + " x " + imageView.getBoundsInParent().getHeight());
    }

    public Image getImage(){
        return image;
    }
    
    
    /* ================= PDF → SCREEN ================= */

    public Rectangle createBoxFromPdf(
            double pdfX,
            double pdfY,
            double pdfWidth,
            double pdfHeight,
            double pageWidth,
            double pageHeight
    ){

        double imgW = image.getWidth();
        double imgH = image.getHeight();

        double scaleX = imgW / pageWidth;
        double scaleY = imgH / pageHeight;

        double x = pdfX * scaleX;

        double y = (pageHeight - pdfY - pdfHeight) * scaleY;

        // ❌ REMOVED WRONG OFFSET (no longer needed)
        // UI is now perfectly aligned

        double width = pdfWidth * scaleX;
        double height = pdfHeight * scaleY;

        Rectangle r = new Rectangle(x, y, width, height);
        styleShape(r);

        return r;
    }

    public List<Shape> getShapes(){
        return shapes;
    }

    public ImageView getImageView(){
        return imageView;
    }

    /* ================= ZOOM ================= */

    public void setZoom(double zoom){
        zoomScale.setX(zoom);
        zoomScale.setY(zoom);
    }

    /* ================= TOOL ================= */

    public void setTool(Tool tool){

        this.activeTool = tool;

        if(tool==Tool.NONE)
            setCursor(Cursor.OPEN_HAND);
        else
            setCursor(Cursor.CROSSHAIR);
    }

    /* ================= DRAW ================= */

    private void enableInteraction(){

    	this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { 
    	
    	

    	    

            requestFocus();

         // 🔥 PAN MODE
            if(activeTool == Tool.NONE){
                setCursor(Cursor.CLOSED_HAND);
                panStartX = e.getSceneX();
                panStartY = e.getSceneY();
                return;
            }

            if(onDrawStart!=null) onDrawStart.run();

            startX = (e.getX() - contentGroup.getLayoutX()) / zoomScale.getX();
            startY = (e.getY() - contentGroup.getLayoutY()) / zoomScale.getY();

            switch(activeTool){

                case RECTANGLE -> {
                    Rectangle rect=new Rectangle(startX,startY,0,0);
                    styleShape(rect);
                    addResizeHandles(rect);
                    enableShapeMovement(rect);
                    currentShape=rect;
                }

                case ELLIPSE -> {
                    Ellipse ellipse=new Ellipse(startX,startY,0,0);
                    styleShape(ellipse);
                    enableShapeMovement(ellipse);
                    currentShape=ellipse;
                }

                case BRUSH -> {
                    Path brush=new Path();
                    brush.setStroke(Color.RED);
                    brush.setStrokeWidth(6);
                    brush.getElements().add(new MoveTo(startX,startY));
                    enableShapeMovement(brush);
                    currentShape=brush;
                }
            }

            if(currentShape!=null){

                overlay.getChildren().add(currentShape);
                shapes.add(currentShape);

                undoStack.push(currentShape);
                redoStack.clear();

                FadeTransition ft=new FadeTransition(Duration.millis(150),currentShape);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }
        });

    	this.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {

    	    

    		double x = (e.getX() - contentGroup.getLayoutX()) / zoomScale.getX();
    		double y = (e.getY() - contentGroup.getLayoutY()) / zoomScale.getY();
            
         // 🔥 PAN MODE
            if(activeTool == Tool.NONE){

                double dx = e.getSceneX() - panStartX;
                double dy = e.getSceneY() - panStartY;

                double smooth = 0.85;

                contentGroup.setLayoutX(contentGroup.getLayoutX() + dx * smooth);
                contentGroup.setLayoutY(contentGroup.getLayoutY() + dy * smooth);
                panStartX = e.getSceneX();
                panStartY = e.getSceneY();

                return;
            }
            
            if(currentShape instanceof Rectangle rect){

                rect.setX(Math.min(startX,x));
                rect.setY(Math.min(startY,y));
                rect.setWidth(Math.abs(x-startX));
                rect.setHeight(Math.abs(y-startY));
            }

            else if(currentShape instanceof Ellipse ellipse){

                ellipse.setCenterX((startX+x)/2);
                ellipse.setCenterY((startY+y)/2);
                ellipse.setRadiusX(Math.abs(x-startX)/2);
                ellipse.setRadiusY(Math.abs(y-startY)/2);
            }

            else if(currentShape instanceof Path brush){

                brush.getElements().add(new LineTo(x,y));
            }
        });

    	this.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> { 

            if(onDrawEnd!=null) onDrawEnd.run();
            currentShape=null;
        });
    }

    

    /* ================= MOVE ================= */

    private void enableShapeMovement(Shape shape){

        final double[] dragOffset = new double[2];

        shape.setOnMouseEntered(e -> setCursor(Cursor.HAND));
        shape.setOnMouseExited(e -> setCursor(Cursor.CROSSHAIR));

        shape.setOnMousePressed(e->{

            if(!e.isShiftDown()){
                clearSelection();
            }

            selectShape(shape);

            dragOffset[0] = e.getX();
            dragOffset[1] = e.getY();

           // e.consume();
        });

        shape.setOnMouseDragged(e->{

            double dx = e.getX() - dragOffset[0];
            double dy = e.getY() - dragOffset[1];

            // 🔥 MOVE USING REAL COORDS (NOT TRANSLATE)

            if(shape instanceof Rectangle r){
                r.setX(r.getX() + dx);
                r.setY(r.getY() + dy);
            }
            else if(shape instanceof Ellipse el){
                el.setCenterX(el.getCenterX() + dx);
                el.setCenterY(el.getCenterY() + dy);
            }
            else if(shape instanceof Path p){
                p.setLayoutX(p.getLayoutX() + dx);
                p.setLayoutY(p.getLayoutY() + dy);
            }

            e.consume();
        });
    }

    /* ================= SELECTION ================= */

    private void selectShape(Shape shape){
        selectedShapes.add(shape);
        shape.setStroke(Color.YELLOW);
    }

    private void clearSelection(){

        for(Shape s:selectedShapes){
            s.setStroke(Color.RED);
            s.setStrokeWidth(2);
        }

        selectedShapes.clear();
    }

    /* ================= DELETE ================= */

    private void enableKeyboardDelete(){

        setOnKeyPressed(e->{

            if(e.getCode()==KeyCode.DELETE){

                for(Shape s:selectedShapes){
                    overlay.getChildren().remove(s);
                    shapes.remove(s);
                }

                selectedShapes.clear();
            }
        });
    }

    /* ================= HANDLES ================= */

    private void addResizeHandles(Rectangle rect){

        Circle tl=createHandle();
        Circle tr=createHandle();
        Circle bl=createHandle();
        Circle br=createHandle();

        overlay.getChildren().addAll(tl,tr,bl,br);

        updateHandles(rect,tl,tr,bl,br);

        rect.boundsInParentProperty().addListener((obs,o,n)->
                updateHandles(rect,tl,tr,bl,br)
        );

        makeDraggable(rect,br,true,true);
        makeDraggable(rect,tr,true,false);
        makeDraggable(rect,bl,false,true);
        makeDraggable(rect,tl,false,false);
    }

    private Circle createHandle(){

        Circle c=new Circle(5);
        c.setFill(Color.WHITE);
        c.setStroke(Color.BLACK);
        return c;
    }

    private void updateHandles(Rectangle r,Circle tl,Circle tr,Circle bl,Circle br){

        tl.setCenterX(r.getX());
        tl.setCenterY(r.getY());

        tr.setCenterX(r.getX()+r.getWidth());
        tr.setCenterY(r.getY());

        bl.setCenterX(r.getX());
        bl.setCenterY(r.getY()+r.getHeight());

        br.setCenterX(r.getX()+r.getWidth());
        br.setCenterY(r.getY()+r.getHeight());
    }

    private void makeDraggable(Rectangle r,Circle handle,boolean right,boolean bottom){

        handle.setOnMouseDragged(e->{

            double x=e.getX();
            double y=e.getY();

            if(right)
                r.setWidth(x-r.getX());
            else{
                r.setWidth(r.getWidth()+(r.getX()-x));
                r.setX(x);
            }

            if(bottom)
                r.setHeight(y-r.getY());
            else{
                r.setHeight(r.getHeight()+(r.getY()-y));
                r.setY(y);
            }
        });
    }

    

   
    /* ================= STYLE ================= */

    private void styleShape(Shape shape){
        shape.setStroke(Color.RED);
        shape.setStrokeWidth(2);
        shape.setFill(Color.color(1,0,0,0.15));
    }

    public void undo(){

        if(undoStack.isEmpty()) return;

        Shape s = undoStack.pop();

        overlay.getChildren().remove(s);
        shapes.remove(s);

        redoStack.push(s);
    }

    public void redo(){

        if(redoStack.isEmpty()) return;

        Shape s = redoStack.pop();

        overlay.getChildren().add(s);
        shapes.add(s);

        undoStack.push(s);
    }

    public Pane getOverlay(){
        return overlay;
    }
    
    public void zoomAt(double factor, double sceneX, double sceneY){

        double oldZoom = zoomScale.getX();
        double newZoom = Math.max(0.5, Math.min(3.0, oldZoom * factor));

        double f = (newZoom / oldZoom) - 1;

        double dx = sceneX - contentGroup.localToScene(0,0).getX();
        double dy = sceneY - contentGroup.localToScene(0,0).getY();

        zoomScale.setX(newZoom);
        zoomScale.setY(newZoom);

        contentGroup.setLayoutX(contentGroup.getLayoutX() - f * dx);
        contentGroup.setLayoutY(contentGroup.getLayoutY() - f * dy);
    }
    
    public void setOnDrawStart(Runnable r){onDrawStart=r;}
    public void setOnDrawEnd(Runnable r){onDrawEnd=r;}
}