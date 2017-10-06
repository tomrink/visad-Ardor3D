package visad.ardor3d;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.awt.JoglAwtCanvas;
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class DisplayA3D implements Updater {

    private final Node root;
    private final Node transform;
    private Node branch = null;
    
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
    
    private SceneA3D scene;
    
    public DisplayA3D() {
       this(null);
    }
    
    public DisplayA3D(Dimension size) {
        System.setProperty("ardor3d.useMultipleContexts", "true");
        System.setProperty("jogl.gljpanel.noglsl", "true"); // Use OpenGL shading
        
        scene = new SceneA3D();
        
        root = scene.getRootNode();
        transform = scene.getTransformNode();
        
        
        mouseControl = new MouseControlA3D(transform);
        mouseControl.setupMouseTriggers(logicalLayer);
        
        
        canvasRenderer = new JoglCanvasRenderer(scene);
        
        final DisplaySettings settings = new DisplaySettings(size.width, size.height, 24, 0, 0, 16, 0, 0, false, false);        
        
        canvas = createCanvas(settings);
        
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
    
    public Component createCanvas(final DisplaySettings settings) {
        //canvas = new JoglSwingCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
        canvas = new JoglAwtCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
        
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
          myRunner = new RunnerA3D(frameWork);
       }
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
    
    public synchronized void markNeedDraw() {
       scene.markNeedDraw();
    }
    
    @Override
    public void init() {
        if (frameHandlerInitialized) {
           return;
        }
        frameHandlerInitialized = true;
    }
    
    
    public static DisplayA3D createDisplay() {
       return createDisplay(null);
    }
    
    public static DisplayA3D createDisplay(Dimension size) {
       DisplayA3D display = null;
       DisplayInit dspInitializer = new DisplayInit(size);
       
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
          display = new DisplayA3D(size);
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
    
    private static void createAndShowGUI() {
       final DisplayA3D dspA3d = DisplayA3D.createDisplay();
       final JFrame frame = new JFrame();
       frame.setPreferredSize(new Dimension(500, 500));
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
       final JComponent outerComp = new JPanel(new BorderLayout());
       JPanel cntrlPanel = new JPanel(new FlowLayout());
       
       
       JPanel panel = new JPanel();
       final Component comp = dspA3d.getComponent();
       
       
       outerComp.add(comp, BorderLayout.CENTER);
       outerComp.add(cntrlPanel, BorderLayout.SOUTH);
       frame.getContentPane().add(outerComp);  
       

       //Display the window.
       frame.pack();
       frame.setVisible(true);
       
    }
    
    public static void main(String[] args) {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });      
    }
}

class DisplayInit implements Runnable {
   
      private DisplayA3D display;
      
      private Dimension size;
      
      DisplayInit() {
         this.size = null;
      }
      
      DisplayInit(Dimension size) {
         this.size = size;
      }

      @Override
      public void run() {
         display = new DisplayA3D(size);
      }
      
      DisplayA3D getTheDisplay() {
         return display;
      }
}