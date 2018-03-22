package visad.ardor3d;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.framework.jogl.awt.JoglAwtCanvas;
import com.ardor3d.framework.jogl.awt.JoglSwingCanvas;
import com.ardor3d.framework.jogl.awt.JoglNewtAwtCanvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.jogl.JoglNewtFocusWrapper;
import com.ardor3d.input.jogl.JoglNewtKeyboardWrapper;
import com.ardor3d.input.jogl.JoglNewtMouseManager;
import com.ardor3d.input.jogl.JoglNewtMouseWrapper;
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
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
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


public class UpdaterA3D implements Updater {
   
    private final Node root;
    
    private Component canvas;
    private final CanvasRenderer canvasRenderer;
    
    private Timer timer = null;
    private FrameHandler frameWork = null;
    private LogicalLayer logicalLayer = null;
    private RunnerA3D myRunner = null;
    private RenderContext renderContext;

    public boolean needDraw = false;
    
    private DisplayRendererA3D dspRenderer;
    
    private int canvasType = DisplayImplA3D.JOGL_AWT;
    
    
    public UpdaterA3D(Container container, DisplaySettings settings, DisplayRendererA3D dspRenderer, int canvasType) {
        System.setProperty("ardor3d.useMultipleContexts", "true");
        System.setProperty("jogl.gljpanel.noglsl", "true"); // Use OpenGL shading
        
        this.dspRenderer = dspRenderer;
        this.canvasType = canvasType;
        
        timer = Ardor3D.getTimer();
        frameWork = Ardor3D.getFrameHander();
        logicalLayer = Ardor3D.getLogicalLayer();
        Ardor3D.start();
        
        dspRenderer.createSceneGraph();
        root = dspRenderer.getRoot();
        
        SceneA3D scene = new SceneA3D(dspRenderer);
        
        canvasRenderer = new JoglCanvasRenderer(scene);

        dspRenderer.setCanvasRenderer(canvasRenderer);
        
        int width = settings.getWidth();
        int height = settings.getHeight();
        
        // Custom settings for Camera
        Camera camera = new Camera(width, height);
        
        double aspect = 1;
        if (height > 0) {
           aspect = width/height;
        }
        camera.setFrustumPerspective(60f, aspect, 1, 100);
        final Vector3 loc = new Vector3(0.0f, 0.0f, 5f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        /** Move our camera to a correct place and orientation. */
        camera.setFrame(loc, left, up, dir);
        canvasRenderer.setCamera(camera);
        

        canvas = createCanvas(settings, canvasType);
        
        addCanvas(canvas);
        container.add(canvas);
        
        scene.getRoot().attachChild(root);
        
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
        
        /* This probably need to be done AFTER adding the com.ardor3d.framework.Canvas to a visible component */
        ((Canvas)canvas).init();
        
        /* This must be done AFTER Canvas.init() */
        renderContext = canvasRenderer.getRenderContext();
        
        /* These must be done AFTER the renderContext has been obtained */
        frameWork.addUpdater(this);
        
        frameWork.addCanvas((com.ardor3d.framework.Canvas)canvas);
        
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
        
       switch (canvasType) {
          case DisplayImplA3D.JOGL_SWING:
             canvas = new JoglSwingCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
             break;
          case DisplayImplA3D.JOGL_AWT:
             canvas = new JoglAwtCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
             break;
          case DisplayImplA3D.JOGL_NEWT:
             canvas = new JoglNewtAwtCanvas(settings, (JoglCanvasRenderer)canvasRenderer);
             break;
          default:
             break;
       }
        
        return canvas;
    }
    
    public void addCanvas(Component canvas) {
        PhysicalLayer pl = null;
        if (canvas instanceof JoglAwtCanvas || canvas instanceof JoglSwingCanvas) {
           AwtMouseManager mouseManager = new AwtMouseManager(canvas);
           pl = new PhysicalLayer(new AwtKeyboardWrapper(canvas),
                   new AwtMouseWrapper(canvas, mouseManager),
                   DummyControllerWrapper.INSTANCE,
                   new AwtFocusWrapper(canvas));
        }
        else if (canvas instanceof JoglNewtAwtCanvas) {
           JoglNewtMouseManager mouseManager = new JoglNewtMouseManager((NewtWindowContainer)canvas);
           pl = new PhysicalLayer(new JoglNewtKeyboardWrapper((NewtWindowContainer)canvas),
                   new JoglNewtMouseWrapper((NewtWindowContainer)canvas, mouseManager),
                   DummyControllerWrapper.INSTANCE,
                   new JoglNewtFocusWrapper((NewtWindowContainer)canvas));
        }

        // may need to add 'deRegisterInput'
        logicalLayer.registerInput((com.ardor3d.framework.Canvas)canvas, pl);
    }
    
    public CanvasRenderer getCanvasRenderer() {
       return canvasRenderer;
    }
    
    public ContextCapabilities getCapabilities() {
       return renderContext.getCapabilities();
    }
    
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
    }
    
    public void start() {
       myRunner.start();
    }
    
    public Component getComponent() {
       return canvas;
    }
    
    public void destroy() {
       // deRegister triggers and Input
       Ardor3D.toggleRunner(false);
       frameWork.removeCanvas((com.ardor3d.framework.Canvas)canvas);
       frameWork.removeUpdater(this);
       Ardor3D.toggleRunner(true);
    }
    
}