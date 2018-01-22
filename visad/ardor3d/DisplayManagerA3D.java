package visad.ardor3d;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.awt.JoglAwtCanvas;
import com.ardor3d.framework.jogl.awt.JoglSwingCanvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.MouseMovedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.jogamp.newt.event.InputEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 
        canvasRenderer = new JoglCanvasRenderer(dspRenderer);
        
        // Custom settings for Camera
        Camera camera = new Camera(size.width, size.height);
        camera.setFrustumPerspective(60f, size.width/size.height, 1, 100);
        final Vector3 loc = new Vector3(0.0f, 0.0f, 5f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        /** Move our camera to a correct place and orientation. */
        camera.setFrame(loc, left, up, dir);
        canvasRenderer.setCamera(camera);
        
        dspRenderer.setCanvasRenderer(canvasRenderer);
        ((MouseBehaviorA3D)dspRenderer.getMouseBehavior()).setCanvasRenderer(canvasRenderer);
        
        
        final DisplaySettings settings = new DisplaySettings(size.width, size.height, 24, 0, 0, 16, 0, 0, false, false);        
        
        canvas = createCanvas(settings, canvasType);
        
        addCanvas(canvas);
        
        canvas.addComponentListener(new ComponentAdapter() {
            Dimension size = canvas.getSize();
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (size != null && !canvas.getSize().equals(size)) {
                   canvasRenderer.getCamera().resize(canvas.getWidth(), canvas.getHeight());
                   markNeedDraw();
                }
            }
        });
        
        
        
        // handle mouse event conversion to MouseHelper here
        
        final TriggerAction leftPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT), leftPressedAction));
        
        final TriggerAction cntrPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);                
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.MIDDLE), cntrPressedAction));
        
        final TriggerAction rightPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);

            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.RIGHT), rightPressedAction));
        
        final TriggerAction leftReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);

            }
        };
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT), leftReleasedAction));
        
        final TriggerAction cntrReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.MIDDLE), cntrReleasedAction));
        
        final TriggerAction rightReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.RIGHT), rightReleasedAction));        
        
        final Predicate<TwoInputStates> leftDownMouseMoved = Predicates.and(TriggerConditions.leftButtonDown(), TriggerConditions.mouseMoved());
        final Predicate<TwoInputStates> cntrDownMouseMoved = Predicates.and(TriggerConditions.middleButtonDown(), TriggerConditions.mouseMoved());
        final Predicate<TwoInputStates> rghtDownMouseMoved = Predicates.and(TriggerConditions.rightButtonDown(), TriggerConditions.mouseMoved());        

        final TriggerAction leftDraggedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(leftDownMouseMoved, leftDraggedAction));
        
        final TriggerAction cntrDraggedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(cntrDownMouseMoved, cntrDraggedAction));
        
        final TriggerAction rightDraggedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(rghtDownMouseMoved, rightDraggedAction));
        
        final TriggerAction mouseMovedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(MouseEvent.MOUSE_MOVED, 0, mouse, keyboard);
            }           
        };
        // Best only if component of canvas has focus? So many events otherwise.
        //logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), mouseMovedAction));
        
        
        
        frameWork.addUpdater(this);
        start();
    }
    
    private void forwardToMouseHelper(int id, int button, MouseState mState, KeyboardState kbState) {
       int mod = 0;
       
       mod |= button;
       
       if (kbState.isDown(Key.LSHIFT) || kbState.isDown(Key.RSHIFT)) {
          mod |= InputEvent.SHIFT_MASK;
       }       
       if (kbState.isDown(Key.LCONTROL) || kbState.isDown(Key.RCONTROL)) {
          mod |= InputEvent.CTRL_MASK;
       }
       
       Dimension size = canvas.getSize();
       
       MouseEvent me = new MouseEvent(canvas, id, 0, mod, mState.getX(), (size.height-mState.getY()), 0, false);
       dspRenderer.getMouseBehavior().getMouseHelper().processEvent(me);
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