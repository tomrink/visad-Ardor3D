package visad.ardor3d;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.awt.JoglAwtCanvas;
import com.ardor3d.framework.jogl.awt.JoglSwingCanvas;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;


public class DisplayManagerA3D implements Updater {

    private final Node root;
    private final Node transform;
    
    private Component canvas;
    private final CanvasRenderer canvasRenderer;
    
    private final Timer timer = new Timer();
    private final FrameHandler frameWork = new FrameHandler(timer);
    private final LogicalLayer logicalLayer = new LogicalLayer();
    private volatile boolean exit = false;
    private AwtMouseManager mouseManager;
    private PhysicalLayer pl;

    private MouseControlA3D mouseControl;
    
    public boolean frameHandlerInitialized = false;
    
    private RunnerA3D myRunner;
    
    public boolean needDraw = false;
    
    private DisplayRendererA3D dspRenderer;
    
    private int canvasType = DisplayImplA3D.JOGL_AWT;
    
    public DisplayManagerA3D(Dimension size, DisplayRendererA3D dspRenderer) {
       this(size, dspRenderer, DisplayImplA3D.JOGL_AWT);
    }
    
    public DisplayManagerA3D(Dimension size, DisplayRendererA3D dspRenderer, int canvasType) {
        System.setProperty("ardor3d.useMultipleContexts", "true");
        System.setProperty("jogl.gljpanel.noglsl", "true"); // Use OpenGL shading
        
        this.dspRenderer = dspRenderer;
        this.canvasType = canvasType;
        
        root = dspRenderer.getRoot();
        transform = dspRenderer.getTransformNode();
        
        
        mouseControl = new MouseControlA3D(transform);
        mouseControl.setupMouseTriggers(logicalLayer);
        
        
        canvasRenderer = new JoglCanvasRenderer(dspRenderer);
        
        final DisplaySettings settings = new DisplaySettings(size.width, size.height, 24, 0, 0, 16, 0, 0, false, false);        
        
        canvas = createCanvas(settings, canvasType);
        
        addCanvas(canvas);
        
        canvas.addComponentListener(new ComponentAdapter() {
            Dimension size = canvas.getSize();
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (size != null && !canvas.getSize().equals(size)) {
                   markNeedDraw();
                }
            }
        });
        
        frameWork.addUpdater(this);
        start();
    }
    
    public Component getCanvas() {
       return canvas;
    }
    
    public void removeCanvas() {
       frameWork.removeCanvas((com.ardor3d.framework.Canvas)canvas);
       canvas = null;
    }
    
    public Component createCanvas(final DisplaySettings settings, int canvasType) {
        Component canvas = null;
        
        if (canvasType == DisplayImplA3D.JOGL_SWING) {
           canvas = new JoglSwingCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
        }
        else if (canvasType == DisplayImplA3D.JOGL_AWT) {
           canvas = new JoglAwtCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
        }
        
        return canvas;
    }
    
    public void addCanvas(Component canvas) {
        mouseManager = new AwtMouseManager(canvas);
        pl = new PhysicalLayer(new AwtKeyboardWrapper(canvas),
                new AwtMouseWrapper(canvas, mouseManager),
                DummyControllerWrapper.INSTANCE,
                new AwtFocusWrapper(canvas));

        // may need to add 'deRegisterInput'
        logicalLayer.registerInput((com.ardor3d.framework.Canvas)canvas, pl);

        frameWork.addCanvas((com.ardor3d.framework.Canvas)canvas);      
    }
    
    public CanvasRenderer getCanvasRenderer() {
       return canvasRenderer;
    }
    
    public void start() {
       if (myRunner == null) {
          myRunner = new RunnerA3D(frameWork, canvasRenderer, dspRenderer);
       }
       // Looks like a DirtyType.Transform is the initial state of a node, so this may not be needed here.
       // More investigation needed.
       markNeedDraw();
    }
    
    public void stop() {
       myRunner.exit();
       myRunner = null;
    }
    


    @Override
    public void update(ReadOnlyTimer rot) {
       logicalLayer.checkTriggers(rot.getTimePerFrame());
       
        /* this resets transform dirty flag so need to do after isDirty above
        root.updateGeometricState(rot.getTimePerFrame(), false);
        */
    }
    
    public void markNeedDraw() {
       dspRenderer.markNeedDraw();
    }
    
    @Override
    public void init() {
        if (frameHandlerInitialized) {
           return;
        }
        frameHandlerInitialized = true;
    }
    
    
    public static DisplayManagerA3D createDisplayManager(Dimension size, DisplayRendererA3D dspRenderer, int canvasType) {
       DisplayManagerA3D display = null;
       DisplayManagerInitializer dspInitializer = new DisplayManagerInitializer(size, dspRenderer, canvasType);
       
       if (!SwingUtilities.isEventDispatchThread()) {
          try {
             SwingUtilities.invokeAndWait(dspInitializer);
             display = dspInitializer.getTheDisplay();
          }
          catch (Exception e) {
             e.printStackTrace();
          }
       }
       else {
          display = new DisplayManagerA3D(size, dspRenderer, canvasType);
       }
       
       return display;
    }  
    
    public Component getComponent() {
       return canvas;
    } 
    
    public void toggleRunner() {
       myRunner.toggle();
    }
    
    public void toggleRunner(boolean on) {
       myRunner.toggle(on);
    }
    
    public FrameHandler getFrameHandler() {
       return frameWork;
    }
    
    public boolean getFrameHandlerInitialized() {
       return frameHandlerInitialized;
    }
}

class DisplayManagerInitializer implements Runnable {
   
      private DisplayManagerA3D display;
      
      private final Dimension size;
      private final DisplayRendererA3D dspRenderer;
      private final int canvasType;
      
      
      DisplayManagerInitializer(Dimension size, DisplayRendererA3D dspRenderer, int canvasType) {
         this.size = size;
         this.dspRenderer = dspRenderer;
         this.canvasType = canvasType;
      }

      @Override
      public void run() {
         display = new DisplayManagerA3D(size, dspRenderer, canvasType);
      }
      
      DisplayManagerA3D getTheDisplay() {
         return display;
      }
}