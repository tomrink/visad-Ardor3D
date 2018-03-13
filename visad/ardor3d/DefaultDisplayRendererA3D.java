//
// DefaultDisplayRendererA3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2017 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
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
 * <CODE>DefaultDisplayRendererA3D</CODE> is the VisAD class for the
 * default background and metadata rendering algorithm under Jogamp's Ardor3D.<P>
 */
public class DefaultDisplayRendererA3D extends DisplayRendererA3D {

  private Object not_destroyed = new Object();

  /** color of box and cursor */
  

  /** for box cursor line settings */  
  private WireframeState boxWfState = null;
  private WireframeState cursorWfState = null;

  private Class mouseBehaviorA3DClass = null;

  private MouseBehaviorA3D mouse = null; // Behavior for mouse interactions

  /**
   * This is the default <CODE>DisplayRenderer</CODE> used by the
   * <CODE>DisplayImplJ3D</CODE> constructor.
   * It draws a 3-D cube around the scene.<P>
   * The left mouse button controls the projection as follows:
   * <UL>
   *  <LI>mouse drag rotates in 3-D
   *  <LI>mouse drag with Shift down zooms the scene
   *  <LI>mouse drag with Ctrl translates the scene sideways
   * </UL>
   * The center mouse button activates and controls the
   * 3-D cursor as follows:
   * <UL>
   *  <LI>mouse drag translates the cursor sideways
   *  <LI>mouse drag with Shift translates the cursor in and out
   *  <LI>mouse drag with Ctrl rotates scene in 3-D with cursor on
   * </UL>
   * The right mouse button is used for direct manipulation by clicking on
   * the depiction of a <CODE>Data</CODE> object and dragging or re-drawing
   * it.<P>
   * Cursor and direct manipulation locations are displayed in RealType
   * values.<P>
   * <CODE>BadMappingExceptions</CODE> and
   * <CODE>UnimplementedExceptions</CODE> are displayed<P>
   */
  public DefaultDisplayRendererA3D () {
    super();
    mouseBehaviorA3DClass = MouseBehaviorA3D.class;
  }

  /**
   * @param mbj3dClass - sub Class of MouseBehaviorJ3D
   */
  
  public DefaultDisplayRendererA3D (Class mbj3dClass) {
    super();
    mouseBehaviorA3DClass = mbj3dClass;
  }

  public void destroy() {
    not_destroyed = null;
    mouse = null; 
    super.destroy();
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
    cursorWfState = (WireframeState) RenderState.createState(RenderState.StateType.Wireframe);
    cursor.setRenderState(cursorWfState);
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

  // WLH 2 Dec 2002 in response to qomo2.txt
  public void setLineWidth(float width) {
    boxWfState.setLineWidth(width);
    cursorWfState.setLineWidth(width);
    
    markNeedDraw();
  }

  private static final float[] box_verts = {
     // front face
         -1.0f, -1.0f,  1.0f,                       -1.0f,  1.0f,  1.0f,
         -1.0f,  1.0f,  1.0f,                        1.0f,  1.0f,  1.0f,
          1.0f,  1.0f,  1.0f,                        1.0f, -1.0f,  1.0f,
          1.0f, -1.0f,  1.0f,                       -1.0f, -1.0f,  1.0f,
     // back face
         -1.0f, -1.0f, -1.0f,                       -1.0f,  1.0f, -1.0f,
         -1.0f,  1.0f, -1.0f,                        1.0f,  1.0f, -1.0f,
          1.0f,  1.0f, -1.0f,                        1.0f, -1.0f, -1.0f,
          1.0f, -1.0f, -1.0f,                       -1.0f, -1.0f, -1.0f,
     // connectors
         -1.0f, -1.0f,  1.0f,                       -1.0f, -1.0f, -1.0f,
         -1.0f,  1.0f,  1.0f,                       -1.0f,  1.0f, -1.0f,
          1.0f,  1.0f,  1.0f,                        1.0f,  1.0f, -1.0f,
          1.0f, -1.0f,  1.0f,                        1.0f, -1.0f, -1.0f
  };

  private static final float[] cursor_verts = {
          0.0f,  0.0f,  0.1f,                        0.0f,  0.0f, -0.1f,
          0.0f,  0.1f,  0.0f,                        0.0f, -0.1f,  0.0f,
          0.1f,  0.0f,  0.0f,                       -0.1f,  0.0f,  0.0f
  };

}

