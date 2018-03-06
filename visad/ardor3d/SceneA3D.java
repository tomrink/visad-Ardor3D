package visad.ardor3d;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.Scene;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.GameTaskQueue;
import visad.ardor3d.SwitchNode;
import java.util.Vector;
import java.util.concurrent.Callable;


public abstract class SceneA3D implements Scene
{

  // for screen locked
  private OrderedNode screen_locked = null;
  private Node locked_trans = null;

  /** root Node of scene graph for this display */
  private com.ardor3d.scenegraph.Node root = null;
  
  /** single Node for a common model Transfrom between root and Nodes holding
   * all Data depiction sub-graphs */
  private Node trans = null;
  
  private OrderedNode non_direct = null;

  /** background attached to root */
  private Object background = null;

  /** TransformGroup between trans and cursor */
  private Node cursor_trans = null;
  
  /** single Switch between cursor_trans and cursor */
  private SwitchNode cursor_switch = null;
  
  /** children of cursor_switch */
  private Node cursor_on = null, cursor_off = null;
  
  /** on / off state of cursor */
  private boolean cursorOn = false;
  
  /** on / off state of direct manipulation location display */
  private boolean directOn = false;

  /** single Switch between trans and box */
  private SwitchNode box_switch = null;
  
  /** children of box_switch */
  private Node box_on = null, box_off = null;
  
  /** on / off state of box */
  private boolean boxOn = false;

  /** single Switch between trans and scales */
  private SwitchNode scale_switch = null;
  
  /** children of scale_switch */
  private Node scale_on = null, scale_off = null;
  
  /** Vector of screen based AxisScales */
  private Vector axis_vector = new Vector();

  /** on / off state of cursor in GraphicsModeControl */

  /** cursor location */
  private float cursorX, cursorY, cursorZ;
  
  /** normalized direction perpendicular to current cursor plane */
  private float line_x, line_y, line_z;
  
  /** start value for cursor */
  private float point_x, point_y, point_z;
  
  private boolean needDraw = false;
  
  private final Object MUTEX = new Object();

  /** ModelClip stuff, done by reflection */
  private boolean[] modelClipEnables =
    {false, false, false, false, false, false};
  
  private final ClipState rootClipState;
  
  private ContextCapabilities contextCapabilities;
  
  private CanvasRenderer canvasRenderer;
  
  private DisplayManagerA3D dspManager;
  
  ReadOnlyColorRGBA boxColor = ColorRGBA.WHITE;
  ReadOnlyColorRGBA cursorColor = ColorRGBA.WHITE;
  
  
  public SceneA3D () {
    super();
      this.rootClipState = (ClipState) RenderState.createState(RenderState.StateType.Clip);
  }


  public Node getRoot() {
    return root;
  }

  public Node getTransformNode() {
     return trans;
  }

  public Node getCursorOnBranch() {
    return cursor_on;
  }

  public Node getBoxOnBranch() {
    return box_on;
  }

  public abstract Node createSceneGraph();

  public Node createBasicSceneGraph() {
    if (root != null) return root;

    // Create the root of the branch graph
    root = new Node();
    
    locked_trans = new Node();
    screen_locked = new OrderedNode();
    locked_trans.attachChild(screen_locked);
    Node node = new Node();
    node.attachChild(locked_trans);
    root.attachChild(node);
    
    
    // create the TransformNode that is the parent of Data object Group objects
    setTransform(null);
    root.attachChild(trans);

    // create background
    /*
    background = new Background();
    float[] ctlBg = getRendererControl().getBackgroundColor();
    background.setColor(ctlBg[0], ctlBg[1], ctlBg[2]);
    BoundingSphere bound2 = new BoundingSphere(new Point3d(0.0,0.0,0.0),2000000.0);
    background.setApplicationBounds(bound2);
    root.addChild(background);
    */

    // WLH 10 March 2000
    non_direct = new OrderedNode();
    trans.attachChild(non_direct);

    cursor_trans = new Node();
    trans.attachChild(cursor_trans);
    cursor_switch = new SwitchNode();
    cursor_trans.attachChild(cursor_switch);
    cursor_on = new Node();
    cursor_off = new Node();
    cursor_switch.attachChild(cursor_off);
    cursor_switch.attachChild(cursor_on);
    cursor_switch.setSingleVisible(0); // initially off
    cursorOn = false;

    box_switch = new SwitchNode();
    trans.attachChild(box_switch);
    box_on = new Node();
    box_off = new Node();
    box_switch.attachChild(box_off);
    box_switch.attachChild(box_on);
    box_switch.setSingleVisible(1); // initially on

    scale_switch = new SwitchNode();
    trans.attachChild(scale_switch);
    scale_on = new Node();
    scale_off = new Node();
    scale_switch.attachChild(scale_off);
    scale_switch.attachChild(scale_on);
    scale_switch.setSingleVisible(0); // initially off

    root.setRenderState(rootClipState);

    return root;
  }  

  public void setClip(int plane, boolean enable, float a, float b, float c, float d) {
    rootClipState.setClipPlaneEquation(plane, a, b, c, d);
    rootClipState.setEnabled(enable);
  }

  public void addSceneGraphComponent(Object group) {
       Callable updateCallable = new Callable() {
          public Object call() {
             canvasRenderer.makeCurrentContext();
             non_direct.attachChild((Node)group);
             canvasRenderer.releaseCurrentContext();
             return null;
          }
       };
       GameTaskQueue uQueue = ManagerA3D.queueManager.getQueue(GameTaskQueue.RENDER);
       uQueue.enqueue(updateCallable);
       uQueue.execute();    
    //non_direct.attachChild((Node)group);
  }

  public void addLockedSceneGraphComponent(Object node) {
    if (screen_locked == null) return;
    screen_locked.attachChild((Node)node);
  }

  public void updateLockedTrans(double[] matrix) {
    if (locked_trans != null) {
      //locked_trans.setTransform(new Transform3D(matrix));
    }
  }

  public void clearScene(Object group) {
    if (group != null) {
      non_direct.detachChild((Spatial)group);
    }
  }

  public void setTransform(Transform t) {
    if (trans == null) {
      trans = new Node();
    }
    if (t != null) {
      Callable updateCallable = new Callable() {
          public Object call() {
             canvasRenderer.makeCurrentContext();
             trans.setTransform(t);
             canvasRenderer.releaseCurrentContext();
             return null;
          }
      };
      //GameTaskQueue uQueue = DisplayManagerA3D.queueManager.getQueue(GameTaskQueue.RENDER);
      //uQueue.enqueue(updateCallable);
      //uQueue.execute();
      trans.setTransform(t);
    }
  }

  public void setLineWidth(float width) {
  }

  public int getTextureWidthMax() {
//    return contextCapabilities.getMaxTextureSize();
      return 8192;
  }

  public int getTextureHeightMax() {
//    return contextCapabilities.getMaxTextureSize();
      return 8192;
  }
  
  public void setCapabilities(ContextCapabilities obj) {
     this.contextCapabilities = obj;
  }

   @Override
   public boolean renderUnto(Renderer renderer) {
      root.onDraw(renderer);
      return true;
   }
      
   @Override
   public PickResults doPick(Ray3 pickRay) {
      final PickResults pickResults = new PrimitivePickResults();
      pickResults.setCheckDistance(true);
      PickingUtil.findPick(getRoot(), pickRay, pickResults);
      return null;
   }
   
   void setCanvasRenderer(CanvasRenderer canvasRenderer) {
      this.canvasRenderer = canvasRenderer;
   }
   
   public CanvasRenderer getCanvasRenderer() {
      return canvasRenderer;
   }

   void setDisplayManager(DisplayManagerA3D aThis) {
      this.dspManager = aThis;
   }
   
   public DisplayManagerA3D getDisplayManager() {
      return this.dspManager;
   }

 }