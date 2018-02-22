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
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.ardor3d.util.GameTaskQueueManager;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


public class DisplayManagerA3D implements Updater {
   
    private final Node root;
    private final Node transform;
    
    private Component canvas;
    private final CanvasRenderer canvasRenderer;
    
    private Timer timer = null;
    private FrameHandler frameWork = null;
    private LogicalLayer logicalLayer = null;
    private RunnerA3D myRunner = null;

    public boolean frameHandlerInitialized = false;
    
    public boolean needDraw = false;
    
    private DisplayRendererA3D dspRenderer;
    
    private int canvasType = DisplayImplA3D.JOGL_AWT;
    
    public static GameTaskQueueManager queueManager = GameTaskQueueManager.getManager(new String("VisAD"));
    
    public DisplayManagerA3D(Dimension size, DisplayRendererA3D dspRenderer) {
       this(null, size, dspRenderer, DisplayImplA3D.JOGL_AWT);
    }
    
    public DisplayManagerA3D(Container container, Dimension size, DisplayRendererA3D dspRenderer, int canvasType) {
        System.setProperty("ardor3d.useMultipleContexts", "true");
        System.setProperty("jogl.gljpanel.noglsl", "true"); // Use OpenGL shading
        
        /* Only one of these per JVM (suggestion by J.Gouessej of Jogamp), but can be modified
           for one per display 
        */
        timer = Initialize.getTimer();
        frameWork = Initialize.getFrameHandler(timer);
        myRunner = Initialize.getRunner(frameWork);
        logicalLayer = Initialize.getLogicalLayer();
        
        this.dspRenderer = dspRenderer;
        this.canvasType = canvasType;
        
        root = dspRenderer.getRoot();
        transform = dspRenderer.getTransformNode();
        
        frameWork.addUpdater(this);
 
        //canvasRenderer = new JoglCanvasRenderer(dspRenderer, false, null, false);
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
        dspRenderer.setDisplayManager(this);
        

        final DisplaySettings settings = new DisplaySettings(size.width, size.height, 24, 0, 0, 16, 0, 0, false, false);        
        
        canvas = createCanvas(settings, canvasType);
        
        addCanvas(canvas);
        container.add(canvas);
        
        canvas.addComponentListener(new ComponentAdapter() {
            Dimension size = canvas.getSize();
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (size != null && !canvas.getSize().equals(size)) {
                   Camera camera = canvasRenderer.getCamera();
                   int w = canvas.getWidth();
                   int h = canvas.getHeight();
                   double r = (double)w / (double) h;
                   if (camera != null) {
                      camera.resize(w, h);
                      camera.setFrustumPerspective(camera.getFovY(), r, camera.getFrustumNear(), camera.getFrustumFar());
                   }
                   markNeedDraw();
                }
            }
        });
        
        registerInputTriggers();
        
        myRunner.start();
    }
    
    protected void registerInputTriggers() {
       
       // handle mouse event conversion to MouseHelper here. See forwardToMouse/KeyboardBehavior
        
        final TriggerAction leftPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT), leftPressedAction));
        
        final TriggerAction cntrPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);                
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.MIDDLE), cntrPressedAction));
        
        final TriggerAction rightPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_PRESSED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);

            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.RIGHT), rightPressedAction));
        
        final TriggerAction leftReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);

            }
        };
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT), leftReleasedAction));
        
        final TriggerAction cntrReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.MIDDLE), cntrReleasedAction));
        
        final TriggerAction rightReleasedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_RELEASED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);
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
                forwardToMouseHelper(source, MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON1_MASK, mouse, keyboard);
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(leftDownMouseMoved, leftDraggedAction));
        
        final TriggerAction cntrDraggedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON2_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(cntrDownMouseMoved, cntrDraggedAction));
        
        final TriggerAction rightDraggedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_DRAGGED, java.awt.event.InputEvent.BUTTON3_MASK, mouse, keyboard);
            }
        };        
        logicalLayer.registerTrigger(new InputTrigger(rghtDownMouseMoved, rightDraggedAction));
        
        final TriggerAction mouseMovedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToMouseHelper(source, MouseEvent.MOUSE_MOVED, 0, mouse, keyboard);
            }           
        };
        // Best only if component of canvas has focus? So many events otherwise.
        //logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), mouseMovedAction));
        
        final TriggerAction keyPressedAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final KeyboardState keyboard = inputStates.getCurrent().getKeyboardState();
                forwardToKeyboardBehavior(KeyEvent.KEY_PRESSED, keyboard);
            }           
        };
        logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), keyPressedAction));
    }
    
    /* These mimic an AWT Event to use the VisAD core logic as is */
    private void forwardToMouseHelper(Canvas c, int id, int button, MouseState mState, KeyboardState kbState) {
       if (c != canvas) {
          return;
       }
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
    
    /* These mimic an AWT Event to use the VisAD core logic as is */    
    private void forwardToKeyboardBehavior(int id, KeyboardState kbState) {
       int mod = 0;
       
       if (kbState.isDown(Key.LSHIFT) || kbState.isDown(Key.RSHIFT)) {
          mod |= InputEvent.SHIFT_MASK;
       }       
       if (kbState.isDown(Key.LCONTROL) || kbState.isDown(Key.RCONTROL)) {
          mod |= InputEvent.CTRL_MASK;
       }
       
       //KeyEvent ke = new KeyEvent(canvas, id, 0, mod, ,);
       //dspRenderer.getKeyboardBehavior().processKeyEvent(ke);
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
        AwtMouseManager mouseManager = new AwtMouseManager(canvas);
        PhysicalLayer pl = new PhysicalLayer(new AwtKeyboardWrapper(canvas),
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
    
//    public void start() {
//       if (myRunner == null) {
//          myRunner = new RunnerA3D(frameWork, canvasRenderer, dspRenderer, this);
//          myRunner.start();
//       }
//       markNeedDraw();
//    }
//    
//    public void stop() {
//       myRunner.exit();
//       myRunner = null;
//    }
    
    @Override
    public void update(ReadOnlyTimer rot) {
       logicalLayer.checkTriggers(rot.getTimePerFrame());
       root.updateGeometricState(rot.getTimePerFrame(), false);
    }    

//    @Override
//    public void update(ReadOnlyTimer rot) {
//       logicalLayer.checkTriggers(rot.getTimePerFrame());
//       
//        /* this resets transform dirty flag so need to do after isDirty above
//        root.updateGeometricState(rot.getTimePerFrame(), false);
//        */
//    }
    
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

class Initialize {
   private static Timer timer = null;
   private static FrameHandler frameHandler = null;
   private static RunnerA3D runner = null;
   private static LogicalLayer logicalLayer = null;
   
   public static Timer getTimer() {
      if (timer == null) {
         timer = new Timer();
      }
      return timer;
   }
   
   public static FrameHandler getFrameHandler(Timer timer) {
      if (frameHandler == null) {
         frameHandler = new FrameHandler(timer);
      }
      return frameHandler;
   }
   
   public static RunnerA3D getRunner(FrameHandler frameHandler) {
      if (runner == null) {
         runner = new RunnerA3D(frameHandler);
      }
      return runner;
   }
   
   public static LogicalLayer getLogicalLayer() {
      if (logicalLayer == null) {
         logicalLayer = new LogicalLayer();
      }
      return logicalLayer;
   }
}