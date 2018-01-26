/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package visad.ardor3d;

import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.geom.BufferUtils;
import java.lang.reflect.Constructor;
import visad.VisADError;

/**
 *
 * @author rink
 */
public class TwoDDisplayRendererA3D extends DisplayRendererA3D {
  private Object not_destroyed = new Object();
 
  private Class mouseBehaviorA3DClass = null;

  private MouseBehaviorA3D mouse = null; // Behavior for mouse interactions
  
  /** for box and cursor line settings */  
  private WireframeState boxWfState = null;
  private WireframeState cursorWfState = null;

  /**
   * This <CODE>DisplayRenderer</CODE> supports 2-D only rendering.
   * It is easiest to describe in terms of differences from
   * <CODE>DefaultDisplayRendererJ3D</CODE>.  The cursor and box
   * around the scene are 2-D, the scene cannot be rotated,
   * the cursor cannot be translated in and out, and the
   * scene can be translated sideways with the left mouse
   * button with or without pressing the Ctrl key.<P>
   * No RealType may be mapped to ZAxis or Latitude.
   */
  public TwoDDisplayRendererA3D () {
    super();
    mouseBehaviorA3DClass = MouseBehaviorA3D.class;
  }

  /**
   * @param mbj3dClass - sub Class of MouseBehaviorJ3D
   */
  
  public TwoDDisplayRendererA3D (Class mbj3dClass) {
    super();
    mouseBehaviorA3DClass = mbj3dClass;
  }
  
  public Node createSceneGraph() {
    if (not_destroyed == null) return null;
    Node root = getRoot();
    if (root != null) return root;

    // create MouseBehaviorJ3D for mouse interactions
    try {
      Class[] param = new Class[] {DisplayRendererA3D.class};
      Constructor mbConstructor =
        mouseBehaviorA3DClass.getConstructor(param);
      mouse = (MouseBehaviorA3D) mbConstructor.newInstance(new Object[] {this});
    }
    catch (Exception e) {
      throw new VisADError("cannot construct " + mouseBehaviorA3DClass);
    }
    mouse = new MouseBehaviorA3D(this);

    getDisplay().setMouseBehavior(mouse);
    root = createBasicSceneGraph(mouse);
    TransformNode trans = getTransformNode();

    /* create box containing data depictions */
    Mesh box = new Mesh();
    boxWfState = (WireframeState) RenderState.createState(RenderState.StateType.Wireframe);
    box.setRenderState(boxWfState);
    MeshData meshData = new MeshData();
    meshData.setIndexMode(IndexMode.Lines);
       
    meshData.setVertexBuffer(BufferUtils.createFloatBuffer(box_verts));
       
    box.setMeshData(meshData);
    box.setDefaultColor(boxColor);
       
    MaterialState material = new MaterialState();
    material.setColorMaterial(MaterialState.ColorMaterial.Emissive);
    box.setRenderState(material);
       
    Node box_on = getBoxOnBranch();
    box_on.attachChild(box);


    Mesh cursor = new Mesh();
    meshData = new MeshData();
    meshData.setIndexMode(IndexMode.Lines);
    meshData.setVertexBuffer(BufferUtils.createFloatBuffer(cursor_verts));
    
    cursor.setMeshData(meshData);
    cursor.setDefaultColor(cursorColor);
    material = new MaterialState();
    material.setColorMaterial(MaterialState.ColorMaterial.Emissive);
    cursor.setRenderState(material);    
    
    Node cursor_on = getCursorOnBranch();
    cursor_on.attachChild(cursor);
    

    // insert MouseBehaviorA3D into scene graph. This is a place holder until we figure out best for Ardor3D
    trans.setMouseBehavior(mouse);

    
    /**
     * Create a ZBuffer to display pixels closest to the camera above
     * farther ones.
     */
    final ZBufferState buf = new ZBufferState();
    buf.setEnabled(true);
    buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
    root.setRenderState(buf);

    /**
     * Set up a basic, default lights.
     */
    PointLight light = new PointLight();
    light.setDiffuse(new ColorRGBA(1f, 1f, 1f, 1.0f));
    //light.setAmbient(new ColorRGBA(0.05f, 0.05f, 0.05f, 1.0f));
    light.setLocation(new Vector3(100, 100, 100));
    light.setEnabled(true);      
      
    PointLight light2 = new PointLight();
    light2.setDiffuse(new ColorRGBA(1f, 1f, 1f, 1.0f));
    //light2.setAmbient(new ColorRGBA(0.05f, 0.05f, 0.05f, 1.0f));
    light2.setLocation(new Vector3(-100, -100, -100));
    light2.setEnabled(true);    
    
    
    /**
     * Attach the lights to a lightState and the lightState to rootNode.
     */
    LightState lightState = new LightState();
    lightState.setEnabled(true);
    lightState.setTwoSidedLighting(false);
    lightState.attach(light);
    lightState.attach(light2);
    root.setRenderState(lightState);

    root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
    
    final BlendState as = new BlendState();
    as.setEnabled(true);
    as.setBlendEnabled(true);
    as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
      
    root.setRenderState(as);
    
    markNeedDraw();
    return root;
  }
  
    /**
   * set the aspect for the containing box
   * aspect double[3] array used to scale x, y and z box sizes
   */
  public void setBoxAspect(double[] aspect) {
    if (not_destroyed == null) return;
    float[] new_verts = new float[box_verts.length];
    for (int i=0; i<box_verts.length; i+=3) {
      new_verts[i] = (float) (box_verts[i] * aspect[0]);
      new_verts[i+1] = (float) (box_verts[i+1] * aspect[1]);
      new_verts[i+2] = (float) (box_verts[i+2] * aspect[2]);
    }
    Node box_on = getBoxOnBranch();
    Mesh box = (Mesh) box_on.getChild(0);
    MeshData meshData = new MeshData();
    meshData.setIndexMode(IndexMode.Lines);
       
    meshData.setVertexBuffer(BufferUtils.createFloatBuffer(new_verts));
       
    box.setMeshData(meshData);
    markNeedDraw();
  }
  
    private static final float[] box_verts = {
     // front face
         -1.0f, -1.0f,  0.0f,                       -1.0f,  1.0f,  0.0f,
         -1.0f,  1.0f,  0.0f,                        1.0f,  1.0f,  0.0f,
          1.0f,  1.0f,  0.0f,                        1.0f, -1.0f,  0.0f,
          1.0f, -1.0f,  0.0f,                       -1.0f, -1.0f,  0.0f
  };

  private static final float[] cursor_verts = {
          0.0f,  0.1f,  0.0f,                        0.0f, -0.1f,  0.0f,
          0.1f,  0.0f,  0.0f,                       -0.1f,  0.0f,  0.0f
  };

}