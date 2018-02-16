//
// DisplayImplA3D.java
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

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;

import visad.*;

import java.rmi.*;

import java.awt.*;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import visad.util.AnimationWidget;
import visad.util.ColorMapWidget;
import visad.util.LabeledColorWidget;
import visad.util.SelectRangeWidget;
import visad.util.VisADSlider;

/**
   DisplayImplJ3D is the VisAD class for displays that use
   Java 3D.  It is runnable.<P>

   DisplayImplJ3D is not Serializable and should not be copied
   between JVMs.<P>
*/
public class DisplayImplA3D extends DisplayImpl {

  /** distance behind for surfaces in 2-D mode */
  // WLH 25 March 2003 (at BOM)
  // public static final float BACK2D = -2.0f;
  public static final float BACK2D = -0.01f;

  /**
   * Use a parallel projection view
   * @see GraphicsModeControlJ3D#setProjectionPolicy
   */
  public static final int PARALLEL_PROJECTION =
    Camera.ProjectionMode.Parallel.ordinal();

  /**
   * Use a perspective projection view. This is the default.
   * @see GraphicsModeControlJ3D#setProjectionPolicy
   */
  public static final int PERSPECTIVE_PROJECTION =
    Camera.ProjectionMode.Parallel.ordinal();

  /** Render polygonal primitives by filling the interior of the polygon
      @see GraphicsModeControlJ3D#setPolygonMode */
  public static final int POLYGON_FILL = 0;

  /**
   * Render polygonal primitives as lines drawn between consecutive vertices
   * of the polygon.
   * @see GraphicsModeControlJ3D#setPolygonMode
   */
  public static final int POLYGON_LINE = 1;

  /**
   * Render polygonal primitives as points drawn at the vertices of
   * the polygon.
   * @see GraphicsModeControlJ3D#setPolygonMode
   */
  public static final int POLYGON_POINT = 2;
  
  /**
   * Transparency attributes. 
   */
  public static final int SCREEN_DOOR = 0;
  public static final int BLENDED = 1;
  public static final int NONE = 2;
  public static final int FASTEST = 3;
  public static final int NICEST = 4;

  /** Field for specifying unknown API type */
  public static final int UNKNOWN = 0;
  /** Field for specifying that the DisplayImpl be created in a JPanel */
  public static final int JPANEL = 1;
  /** Field for specifying that the DisplayImpl does not have a screen Component */
  public static final int OFFSCREEN = 2;
  /** Field for specifying that the DisplayImpl transforms but does not render */
  public static final int TRANSFORM_ONLY = 4;
  
  /** Ardor3D Canvas type */
  public static final int JOGL_SWING = 5;
  public static final int JOGL_AWT = 6;
  
  /** AWT-SWING Tab */
  public static boolean isTab = false;

  /** 
   * Property name for setting whether to use geometry by reference.
   * @see #GEOMETRY_BY_REF
   */
  public static final String PROP_GEOMETRY_BY_REF = "visad.java3d.geometryByRef";
  /**
   * Indicates whether to use geometry by reference when creating geometry arrays.
   * @see javax.media.j3d.GeometryArray#BY_REFERENCE
   */
  public static final boolean GEOMETRY_BY_REF;
  static {
    GEOMETRY_BY_REF = Boolean.parseBoolean(System.getProperty(PROP_GEOMETRY_BY_REF, "true"));
  }

  /**
   * Property name for enabling the use of non-power of two textures.
   * @see #TEXTURE_NPOT
   */
  public static final String PROP_TEXTURE_NPOT = "visad.java3d.textureNpot";

  /**
   * Indicates whether to allow non-power of two textures. This has been known
   * to cause some issues with Apple 32bit Macs eventhough the Canvas3D
   * properties indicate that NPOT is supported.
   * @see javax.media.j3d.Canvas3D#queryProperties()
   */
  // FIXME:
  // This works with the Java3D 1.5.2 example TextureImageNPOT but does not work
  // with the VisAD library image rednering. On initial testing it behaves as if
  // there may be threading issues.  This requires more investigation before we
  // can enable this based on the Canvas3D properties.
  public static final boolean TEXTURE_NPOT;
  static {
    TEXTURE_NPOT = Boolean.parseBoolean(System.getProperty(PROP_TEXTURE_NPOT, "false"));
    //System.err.println("TEXTURE_NPOT:"+TEXTURE_NPOT);
  }
  

  private ProjectionControlA3D projection = null;
  private GraphicsModeControlA3D mode = null;
  private int apiValue = UNKNOWN;
  
  public DisplayImplA3D(String name, Window window, Component comp, int api) 
         throws VisADException, RemoteException {
     this(name, null, window, comp, comp.getWidth(), comp.getHeight(), api);
  }
  
  public DisplayImplA3D(String name, DisplayRendererA3D dspRenderer, Window window, Component comp, int api) 
         throws VisADException, RemoteException {
     this(name, dspRenderer, window, comp, comp.getWidth(), comp.getHeight(), api);
  }  
  
  public DisplayImplA3D(String name, Window window, Component comp, int width, int height, int api) 
         throws VisADException, RemoteException {
     this(name, null, window, comp, width, height, api);
  }
  
  public DisplayImplA3D(String name, DisplayRendererA3D renderer, Window window, Component comp, int width, int height, int api) 
         throws VisADException, RemoteException {
     super(name, renderer);
     
     if (!window.isShowing()) {
        throw new VisADException("Containing window must exist on screen. For example JFrame.setVisible(true)");
     }
     
     initialize(window, comp, width, height, api);
  }
  
  private void initialize(Window window, Component comp, int width, int height, int api) 
          throws VisADException, RemoteException {
     
    // a ProjectionControl always exists
    projection = new ProjectionControlA3D(this);
    addControl(projection);

    if (api == JOGL_AWT || api == JOGL_SWING) {
       if (width < 0 || height < 0) {
          throw new VisADException("DisplayImplA3D: must define Jogl canvas dimension up front");
       }
       DisplayRendererA3D dspRenderer = (DisplayRendererA3D) getDisplayRenderer();
       dspRenderer.createSceneGraph();
       DisplayManagerA3D manager = new DisplayManagerA3D((Container)comp, new Dimension(width, height), dspRenderer, api);
       Component component = manager.getCanvas();
       setComponent(component);
       apiValue = api;
    }
    else if (api == TRANSFORM_ONLY) {
      if (!(getDisplayRenderer() instanceof TransformOnlyDisplayRendererA3D)) {
        throw new DisplayException("must be TransformOnlyDisplayRendererA3D " +
                                   "for api = TRANSFORM_ONLY");
      }
      setComponent(null);
      apiValue = api;
    }
    else if (api == OFFSCREEN) {
      DisplayRendererA3D renderer = (DisplayRendererA3D) getDisplayRenderer();
      setComponent(null);
      apiValue = api;
    }
    else {
      throw new DisplayException("DisplayImplA3D: bad graphics API " + api);
    }
    
    // a GraphicsModeControl always exists
    mode = new GraphicsModeControlA3D(this);
    addControl(mode);
  }

  /** construct a DisplayImpl for Java3D with the
      default DisplayRenderer, in a JFC JPanel */
  public DisplayImplA3D(String name)
         throws VisADException, RemoteException {
    this(name, null, JOGL_AWT, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer, in a JFC JPanel */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer)
         throws VisADException, RemoteException {
    this(name, renderer, JOGL_AWT, null);
  }

  /** constructor with default DisplayRenderer */
  public DisplayImplA3D(String name, int api)
         throws VisADException, RemoteException {
    this(name, null, api, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      GraphicsConfiguration, in a JFC JPanel */
  public DisplayImplA3D(String name, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, null, JOGL_AWT, config);
  }
  
  public DisplayImplA3D(String name, int width, int height, int api)
         throws VisADException, RemoteException {
     super(name, null);
     initialize(api, null, width, height);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer;
      in a JFC JPanel if api == DisplayImplJ3D.JPANEL and
      in an AppletFrame if api == DisplayImplJ3D.APPLETFRAME */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api)
         throws VisADException, RemoteException {
    this(name, renderer, api, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer and GraphicsConfiguration, in a JFC JPanel */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, renderer, JOGL_AWT, config);
  }

  /** constructor with default DisplayRenderer and a non-default
      GraphicsConfiguration */
  public DisplayImplA3D(String name, int api, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, null, api, config);
  }

  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, renderer, api, config, null);
  }

  /** the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then api must
      be JPANEL and its super() constructor for VisADCanvasJ3D
      must be 'super(renderer, config)' */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api,
                        GraphicsConfiguration config, Object c)
         throws VisADException, RemoteException {
    super(name, renderer);

    initialize(api, config, c);
  }

  /** constructor for off screen */
  public DisplayImplA3D(String name, int width, int height)
         throws VisADException, RemoteException {
    this(name, null, width, height);
  }

  /** constructor for off screen */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        int width, int height)
         throws VisADException, RemoteException {
    this(name, renderer, width, height, null);
  }

  /** constructor for off screen;
      the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then its super()
      constructor for VisADCanvasJ3D must be
      'super(renderer, width, height)' */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        int width, int height, Object c)
         throws VisADException, RemoteException {
    super(name, renderer);

    initialize(OFFSCREEN, null, width, height, c);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy)
         throws VisADException, RemoteException {
    this(rmtDpy, null, rmtDpy.getDisplayAPI(), null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, rmtDpy.getDisplayAPI(), null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, int api)
         throws VisADException, RemoteException {
    this(rmtDpy, null, api, null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, null, rmtDpy.getDisplayAPI(), config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
			int api)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, api, null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, rmtDpy.getDisplayAPI(), config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, int api,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, null, api, config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        int api, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, api, config, null);
  }

  /** the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then api must
      be JPANEL and its super() constructor for VisADCanvasJ3D
      must be 'super(renderer, config)' */
  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        int api, GraphicsConfiguration config,
                        Object c)
         throws VisADException, RemoteException {
    super(rmtDpy,
          ((renderer == null && api == TRANSFORM_ONLY) ?
             new TransformOnlyDisplayRendererA3D() : renderer));

    initialize(api, config, c);

    syncRemoteData(rmtDpy);
  }

  private void initialize(int api, GraphicsConfiguration config)
          throws VisADException, RemoteException {
    initialize(api, config, -1, -1, null);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          Object c)
          throws VisADException, RemoteException {
    initialize(api, config, -1, -1, c);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          int width, int height)
          throws VisADException, RemoteException {
    initialize(api, config, width, height, null);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          int width, int height, Object c)
          throws VisADException, RemoteException {
     
    // a ProjectionControl always exists
    projection = new ProjectionControlA3D(this);
    addControl(projection);

    if (api == JOGL_AWT || api == JOGL_SWING) {
       if (width < 0 || height < 0) {
          throw new VisADException("DisplayImplA3D: must define Jogl canvas dimension up front");
       }
       DisplayRendererA3D dspRenderer = (DisplayRendererA3D) getDisplayRenderer();
       dspRenderer.createSceneGraph();
       //DisplayManagerA3D manager = DisplayManagerA3D.createDisplayManager(new Dimension(width, height), dspRenderer, api);
       //Component component = manager.getCanvas();
       //setComponent(component);
       apiValue = api;
    }
    else if (api == TRANSFORM_ONLY) {
      if (!(getDisplayRenderer() instanceof TransformOnlyDisplayRendererA3D)) {
        throw new DisplayException("must be TransformOnlyDisplayRendererA3D " +
                                   "for api = TRANSFORM_ONLY");
      }
      setComponent(null);
      apiValue = api;
    }
    else if (api == OFFSCREEN) {
      DisplayRendererA3D renderer = (DisplayRendererA3D) getDisplayRenderer();
      setComponent(null);
      apiValue = api;
    }
    else {
      throw new DisplayException("DisplayImplA3D: bad graphics API " + api);
    }
    
    // a GraphicsModeControl always exists
    mode = new GraphicsModeControlA3D(this);
    addControl(mode);
  }

  /** return a DefaultDisplayRendererJ3D */
  protected DisplayRenderer getDefaultDisplayRenderer() {
    return new DefaultDisplayRendererA3D();
  }

  public void setScreenAspect(double height, double width) {
    DisplayRendererA3D dr = (DisplayRendererA3D) getDisplayRenderer();
//    Screen3D screen = dr.getCanvas().getScreen3D();
//    screen.setPhysicalScreenHeight(height);
//    screen.setPhysicalScreenWidth(width);
  }

  /**
   * Get the projection control associated with this display
   * @see ProjectionControlJ3D
   *
   * @return  this display's projection control
   */
  public ProjectionControl getProjectionControl() {
    return projection;
  }

  /**
   * Get the graphics mode control associated with this display
   * @see GraphicsModeControlJ3D
   *
   * @return  this display's graphics mode control
   */
  public GraphicsModeControl getGraphicsModeControl() {
    return mode;
  }

  /**
   * Return the API used for this display
   *
   * @return  the mode being used (UNKNOWN, JPANEL, APPLETFRAME,
   *                               OFFSCREEN, TRANSFORM_ONLY)
   * @throws  VisADException
   */
  public int getAPI()
	throws VisADException
  {
    return apiValue;
  }

  public Spatial makeGeometry(VisADGeometryArray vga) throws VisADException {
     return makeGeometry(vga, null);
  }
  
  public Spatial makeGeometry(VisADGeometryArray vga, ColorRGBA defaultColor) throws VisADException {
     return makeGeometry(vga, defaultColor, null);
  }
  
  public Spatial makeGeometry(VisADGeometryArray vga, ColorRGBA defaultColor, GraphicsModeControl mode) throws VisADException {
    if (vga == null) return null;
    
    Mesh mesh = new Mesh();
    MeshData meshData = new MeshData();
    
    if (defaultColor != null) {
       mesh.setDefaultColor(defaultColor);
    }
    
    if (mode == null) {
       mode = getGraphicsModeControl();
    }
    
    boolean mode2d = getDisplayRenderer().getMode2D();
    
    int polygonMode = mode.getPolygonMode();
    
    if (polygonMode == DisplayImplA3D.POLYGON_POINT) {
      meshData.setIndexMode(IndexMode.Points);
      basicGeometry(vga, meshData, false);
      mesh = new com.ardor3d.scenegraph.Point();
      ((com.ardor3d.scenegraph.Point)mesh).setPointSize(mode.getPointSize());      
      mesh.setMeshData(meshData);
      
      return mesh;       
    }
    else if (polygonMode == DisplayImplA3D.POLYGON_LINE) {
      WireframeState wfState = (WireframeState) RenderState.createState(RenderState.StateType.Wireframe);
      wfState.setLineWidth(mode.getLineWidth());
      mesh.setRenderState(wfState);
    }

    if (vga instanceof VisADIndexedTriangleStripArray) {
      VisADIndexedTriangleStripArray vgb = (VisADIndexedTriangleStripArray) vga;
      if (vga.vertexCount == 0) return null;

      return mesh;
    }
    if (vga instanceof VisADTriangleStripArray) {
      VisADTriangleStripArray vgb = (VisADTriangleStripArray) vga;
      if (vga.vertexCount == 0) return null;

      
      meshData.setIndexMode(IndexMode.TriangleStrip);
      meshData.setIndexLengths(vgb.stripVertexCounts);
      basicGeometry(vgb, meshData, mode2d);
      mesh.setMeshData(meshData);
      
      return mesh;
    }
    else if (vga instanceof VisADLineArray) {
      if (vga.vertexCount == 0) return null;

      meshData.setIndexMode(IndexMode.Lines);
      WireframeState wfState = (WireframeState) RenderState.createState(RenderState.StateType.Wireframe);
      wfState.setLineWidth(mode.getLineWidth());
      mesh.setRenderState(wfState);
      
      basicGeometry(vga, meshData, false);
      mesh.setMeshData(meshData);      
      return mesh;
    }
    else if (vga instanceof VisADLineStripArray) {
      if (vga.vertexCount == 0) return null;
      VisADLineStripArray vgb = (VisADLineStripArray) vga;
      
      WireframeState wfState = (WireframeState) RenderState.createState(RenderState.StateType.Wireframe);
      wfState.setLineWidth(mode.getLineWidth());
      mesh.setRenderState(wfState);

      meshData.setIndexMode(IndexMode.LineStrip);
      meshData.setIndexLengths(vgb.stripVertexCounts);
      
      basicGeometry(vga, meshData, false);
       
      mesh.setMeshData(meshData);
       
      return mesh;
    }
    else if (vga instanceof VisADPointArray) {
      if (vga.vertexCount == 0) return null;
      
      meshData.setIndexMode(IndexMode.Points);
      basicGeometry(vga, meshData, false);
      mesh = new com.ardor3d.scenegraph.Point();
      ((com.ardor3d.scenegraph.Point)mesh).setPointSize(mode.getPointSize());      
      mesh.setMeshData(meshData);
      
      return mesh;
    }
    else if (vga instanceof VisADTriangleArray) {
      if (vga.vertexCount == 0) return null;
      
      meshData.setIndexMode(IndexMode.Triangles);
      basicGeometry(vga, meshData, mode2d);
      mesh.setMeshData(meshData);
      
      return mesh;
    }
    else if (vga instanceof VisADQuadArray) {
      if (vga.vertexCount == 0) return null;

      meshData.setIndexMode(IndexMode.Quads);
      basicGeometry(vga, meshData, mode2d);
      mesh.setMeshData(meshData);
      
      return mesh;
    }
    else {
      throw new DisplayException("DisplayImplJ3D.makeGeometry");
    }
  }
  
  private void basicGeometry(VisADGeometryArray vga, MeshData meshData, boolean mode2d) {
     if (mode2d) {
       if (vga.coordinates != null) {
        int len = vga.coordinates.length;
        float[] coords = new float[len];
        System.arraycopy(vga.coordinates, 0, coords, 0, len);
        for (int i=2; i<len; i+=3) coords[i] = BACK2D;
        meshData.setVertexBuffer(BufferUtils.createFloatBuffer(coords));
       }
     }
     else {
       if (vga.coordinates != null) {
         meshData.setVertexBuffer(BufferUtils.createFloatBuffer(vga.coordinates));
       }        
     }
     if (vga.colors != null) {
        float[] fltClrs;
        int numVerts = vga.vertexCount;
        if (numVerts == vga.colors.length/3) { // Always expand to 4 component color
          fltClrs = new float[numVerts*4];
          for (int k=0; k<numVerts; k++) {
            int idx = k*3;
            int idxA = k*4;
            fltClrs[idxA] = ((float)Byte.toUnsignedInt(vga.colors[idx]))/255f;
            fltClrs[idxA+1] = ((float)Byte.toUnsignedInt(vga.colors[idx+1]))/255f;
            fltClrs[idxA+2] = ((float)Byte.toUnsignedInt(vga.colors[idx+2]))/255f;
            fltClrs[idxA+3] = 1f; 
          }
        }
        else { // Must be 4 component
          fltClrs = new float[vga.colors.length];
          for (int i=0; i<fltClrs.length; i++) {
            fltClrs[i] = ((float)Byte.toUnsignedInt(vga.colors[i]))/255f;
          }
        }
        meshData.setColorBuffer(BufferUtils.createFloatBuffer(fltClrs));
     }
     if (vga.normals != null) {
        meshData.setNormalBuffer(BufferUtils.createFloatBuffer(vga.normals));
     }
     if (vga.texCoords != null) {
        meshData.setTextureBuffer(BufferUtils.createFloatBuffer(vga.texCoords), 0);
     }
  }
  
  
  public void destroy() throws VisADException, RemoteException {
    /** TDR, keep temporarily for reference
    if(isDestroyed())return;

    ((DisplayRendererA3D) getDisplayRenderer()).destroy();
    if (apiValue == OFFSCREEN) {
      destroyUniverse();
    }
    MouseBehavior mouse =  getMouseBehavior();
    if(mouse!=null && mouse instanceof MouseBehaviorJ3D) {
        ((MouseBehaviorJ3D) mouse).destroy();
    }
    super.destroy();
    applet = null;
    projection = null;
    mode = null;
    */
  }
  
  float getOffsetDepthMinimum(float depthOffsetMax) {
    Vector rendVec = getRendererVector();
    Iterator<DataRenderer> iter = rendVec.iterator();
    float offsetMin = depthOffsetMax;
    while (iter.hasNext()) {
      DataRenderer rend = iter.next();
        if (rend.hasPolygonOffset()) {
          if (rend.getPolygonOffset() < offsetMin) {
            offsetMin = rend.getPolygonOffset();  
          }
        }
    }
    return offsetMin;
  }
    
  int getNumRenderersWithZoffset() {
     Vector rendVec = getRendererVector();
     Iterator<DataRenderer> iter = rendVec.iterator();
     int num = 0;
     while (iter.hasNext()) {
       DataRenderer rend = iter.next();
       if (rend.hasPolygonOffset()) {
         num++;
       }
     }
     return num;
   }
   
   /**
    * Sets the depth buffer offset when autoDepthOffset is enabled for this display.
    * @param renderer
    * @param mode 
    */
  public void setDepthBufferOffset(DataRenderer renderer, GraphicsModeControl mode) {
     GraphicsModeControlA3D mode3d = (GraphicsModeControlA3D) mode;
     if (mode3d.getAutoDepthOffsetEnable()) {
       float depthOffsetInc = mode3d.getDepthOffsetIncrement();
       int numLayers = mode3d.getNumRenderersWithDepthOffset();
       float maxDepthOffset = numLayers*(-depthOffsetInc);
       
       if (!renderer.hasPolygonOffset()) {
         int cnt = getNumRenderersWithZoffset();
         if (cnt < numLayers) {
           renderer.setPolygonOffset(getOffsetDepthMinimum(maxDepthOffset) + depthOffsetInc);
           renderer.setPolygonOffsetFactor(0f);
           renderer.setHasPolygonOffset(true);  
         }
         else {
           renderer.setPolygonOffset(0f);  
           renderer.setPolygonOffsetFactor(0f);
           renderer.setHasPolygonOffset(false);
         }
       }
       mode3d.setPolygonOffset(renderer.getPolygonOffset(), false);
       mode3d.setPolygonOffsetFactor(renderer.getPolygonOffsetFactor(), false);
     }
   }
   
   public void resetDepthBufferOffsets() {
     Vector rendVec = getRendererVector();
     Iterator<DataRenderer> iter = rendVec.iterator();
     while (iter.hasNext()) {
       DataRenderer rend = iter.next();
       if (rend.hasPolygonOffset()) {
         rend.setHasPolygonOffset(false);
         rend.setPolygonOffset(0f);
         rend.setPolygonOffsetFactor(0f);
       }
     }
   }
   
    private static void createAndShowGUI() throws VisADException, RemoteException {
       int width = 500;
       int height = 500;
       
       Component widget = null;
       
       final JFrame frame = new JFrame();
       frame.setSize(width, height);
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       frame.setVisible(true);

       final DisplayImplA3D display = new DisplayImplA3D("Display", frame, frame.getContentPane(), JOGL_AWT);
       //display.disableAction();
       //frame.pack();
       
       //final visad.java3d.DisplayImplJ3D display = new visad.java3d.DisplayImplJ3D("Display");
       
//       GraphicsModeControl modeCtrl = display.getGraphicsModeControl();
//       modeCtrl.setCurvedSize(2);
//       modeCtrl.setTextureEnable(false);
//       modeCtrl.setPointMode(true);
//       modeCtrl.setPointSize(2);
       
       /* Simple Test 1 */
//       FieldImpl dataFld;
//       FunctionType fncType = new FunctionType(RealTupleType.SpatialEarth2DTuple, RealType.Generic);
//       dataFld = FlatField.makeField(fncType, 2048, false);
//       
//       ScalarMap xmap = new ScalarMap(RealType.Longitude, Display.XAxis);
//       ScalarMap ymap = new ScalarMap(RealType.Latitude, Display.YAxis);
//       ScalarMap cmap = new ScalarMap(RealType.Generic, Display.RGBA);
//       
//       display.addMap(xmap);
//       display.addMap(ymap);
//       display.addMap(cmap);
//       widget = new ColorMapWidget(cmap);
//       
//       /* test 2 */
//       FunctionType fldType = new FunctionType(RealType.Time, fncType);
//       Integer1DSet outerDom = new Integer1DSet(RealType.Time, 10);
//       dataFld = new FieldImpl(fldType, outerDom);
//       int len = outerDom.getLength();
//       for (int k=0; k<len; k++) {
//          FlatField vFld = FlatField.makeField(fncType, 2048, false);
//          fillField(vFld, 1, k*(2048/len));
//          dataFld.setSample(k, vFld);
//       }
//       
//       ScalarMap tmap = new ScalarMap(RealType.Time, Display.Animation);
//       
//       display.addMap(tmap);
//       AnimationControl acntrl = (AnimationControl) tmap.getControl();
//       acntrl.setOn(true);
//       widget = new AnimationWidget(tmap);

//       
       /* Simple test 4 */
//       ScalarMap zmap = new ScalarMap(RealType.Generic, Display.ZAxis);
//       display.addMap(zmap);
//       ScalarMap rmap = new ScalarMap(RealType.Generic, Display.SelectRange);
//       display.addMap(rmap);
//       widget = new SelectRangeWidget(rmap);
//       display.addMap(new ScalarMap(RealType.Generic, Display.Green));
//       display.addMap(new ConstantMap(0.5f, Display.Red));
//       display.addMap(new ConstantMap(0.5f, Display.Blue));
//       //display.addMap(new ConstantMap(0.5, Display.Alpha));
       
//         DataReferenceImpl ref = new DataReferenceImpl("vfld");
//         ref.setData(dataFld);
//         display.addReference(ref);
         //display.enableAction();
       
 
       /* Simple test 3 */
//       RealType[] types3d = {RealType.Latitude, RealType.Longitude, RealType.Radius};
//       RealTupleType earth_location3d = new RealTupleType(types3d);
//       RealType vis_radiance = RealType.getRealType("vis_radiance", CommonUnit.degree);
//       RealType ir_radiance = RealType.getRealType("ir_radiance", CommonUnit.degree);
//       //RealType[] types2 = {vis_radiance, ir_radiance};
//       RealType[] types2 = {vis_radiance};
//       RealTupleType radiance = new RealTupleType(types2);
//       FunctionType grid_tuple = new FunctionType(earth_location3d, radiance);
//
//       FlatField grid3d = FlatField.makeField(grid_tuple, 64, false);
//
//       ScalarMap lat_map = new ScalarMap(RealType.Latitude, Display.YAxis);
//       display.addMap(lat_map);
//       lat_map.setOverrideUnit(CommonUnit.radian);
//       display.addMap(new ScalarMap(RealType.Longitude, Display.XAxis));
//       display.addMap(new ScalarMap(RealType.Radius, Display.ZAxis));
//       ScalarMap map1color = new ScalarMap(vis_radiance, Display.RGBA);
//       display.addMap(map1color);
//       map1color.setOverrideUnit(CommonUnit.radian);
//       ScalarMap map1contour = new ScalarMap(vis_radiance, Display.IsoContour);
//       display.addMap(map1contour);
//       map1contour.setOverrideUnit(CommonUnit.radian);
//       widget = new ContourWidget(map1contour);
//
//       GraphicsModeControl mode = display.getGraphicsModeControl();
//
//       ContourControl cntrl = (ContourControl)map1contour.getControl();
//       cntrl.setSurfaceValue(0.4f, false);
//
//       DataReferenceImpl ref_grid3d = new DataReferenceImpl("ref_grid3d");
//       ref_grid3d.setData(grid3d);
//       display.addReference(ref_grid3d, new ConstantMap[] {new ConstantMap(0.8, Display.Red),
//               new ConstantMap(0.2, Display.Green), new ConstantMap(0.8, Display.Blue), new ConstantMap(1.0, Display.Alpha)});
       
 
       /* simple test 37 */
//    RealType[] types = {RealType.Latitude, RealType.Longitude, RealType.Altitude};
//    RealTupleType earth_location = new RealTupleType(types);
//    RealType vis_radiance = new RealType("vis_radiance", null, null);
//    RealType ir_radiance = new RealType("ir_radiance", null, null);
//    RealType[] types2 = {vis_radiance, ir_radiance};
//    RealTupleType radiance = new RealTupleType(types2);
//    FunctionType image_tuple = new FunctionType(earth_location, radiance);
//    RealType[] typesxx = {RealType.Longitude, RealType.Latitude};
//    RealTupleType earth_locationxx = new RealTupleType(typesxx);
//    FunctionType image_tuplexx = new FunctionType(earth_locationxx, radiance);
//
//    int size = 64;
//    FlatField imaget1;
//    boolean reverse = true;
//    if (!reverse) {
//      imaget1 = FlatField.makeField(image_tuple, size, false);
//    }
//    else {
//      imaget1 = FlatField.makeField(image_tuplexx, size, false);
//    }
//
//    double first = 0.0;
//    double last = size - 1.0;
//    double step = 1.0;
//    double half = 0.5 * last;
//
//    int nr = size;
//    int nc = size;
//    double ang = 2*Math.PI/nr;
//    float[][] locs = new float[3][nr*nc];
//    for ( int jj = 0; jj < nc; jj++ ) {
//      for ( int ii = 0; ii < nr; ii++ ) {
//        int idx = jj*nr + ii;
//        locs[0][idx] = ii;
//        locs[1][idx] = jj;
//        locs[2][idx] =
//           2f*((float)Math.sin(2*ang*ii)) + 2f*((float)Math.sin(2*ang*jj));
//      }
//    }
//    Gridded3DSet d_set =
//      new Gridded3DSet(RealTupleType.SpatialCartesian3DTuple, locs, nr, nc);
//    imaget1 = new FlatField(image_tuple, d_set);
//    FlatField.fillField(imaget1, step, half);
//
//
//    ScalarMap xmap = new ScalarMap(RealType.Longitude, Display.XAxis);
//    display.addMap(xmap);
//    ScalarMap ymap = new ScalarMap(RealType.Latitude, Display.YAxis);
//    display.addMap(ymap);
//    ScalarMap zmap = new ScalarMap(RealType.Altitude, Display.ZAxis);
//    display.addMap(zmap);
//    ScalarMap rgbaMap = new ScalarMap(vis_radiance, Display.RGBA);
//    display.addMap(rgbaMap);
//    zmap.setRange(-20, 20);
//   
//    ScalarMap map1contour = new ScalarMap(vis_radiance, Display.IsoContour);
//    display.addMap(map1contour);
//    ContourControl ctr_cntrl = (ContourControl) map1contour.getControl();
//    ctr_cntrl.setLabelSize(3.0);
//    widget = new ContourWidget(map1contour);
//
//    GraphicsModeControl mode = display.getGraphicsModeControl();
//    mode.setScaleEnable(true);
//    mode.setPointSize(2);
//
//    DataReferenceImpl ref_imaget1 = new DataReferenceImpl("ref_imaget1");
//    ref_imaget1.setData(imaget1);
//    display.addReference(ref_imaget1, null);       

       /* simple test 19 */
//          RealType[] types = {RealType.Latitude, RealType.Longitude};
//          RealTupleType earth_location = new RealTupleType(types);
//          RealType vis_radiance = RealType.getRealType("vis_radiance");
//          RealType ir_radiance = RealType.getRealType("ir_radiance");
//          RealType[] types2 = {vis_radiance, ir_radiance};
//          RealTupleType radiance = new RealTupleType(types2);
//          FunctionType image_tuple = new FunctionType(earth_location, radiance);
//          RealType[] types4 = {ir_radiance, vis_radiance};
//          RealTupleType ecnaidar = new RealTupleType(types4);
//          FunctionType image_bumble = new FunctionType(earth_location, ecnaidar);
//          RealType[] time = {RealType.Time};
//          RealTupleType time_type = new RealTupleType(time);
//          FunctionType time_images = new FunctionType(time_type, image_tuple);
//          FunctionType time_bee = new FunctionType(time_type, image_bumble);
//
//          int size = 64;
//          FlatField imaget1 = FlatField.makeField(image_tuple, size, false);
//          FlatField wasp = FlatField.makeField(image_bumble, size, false);
//
//          int ntimes1 = 4;
//          int ntimes2 = 6;
//          // different time resolutions for test
//          Set time_set =
//            new Linear1DSet(time_type, 0.0, 1.0, ntimes1);
//          Set time_hornet =
//            new Linear1DSet(time_type, 0.0, 1.0, ntimes2);
//
//          FieldImpl image_sequence = new FieldImpl(time_images, time_set);
//          FieldImpl image_stinger = new FieldImpl(time_bee, time_hornet);
//          FlatField temp = imaget1;
//          FlatField tempw = wasp;
//          Real[] reals19 = {new Real(vis_radiance, (float) size / 4.0f),
//                            new Real(ir_radiance, (float) size / 8.0f)};
//          RealTuple val = new RealTuple(reals19);
//          for (int i=0; i<ntimes1; i++) {
//            image_sequence.setSample(i, temp);
//            temp = (FlatField) temp.add(val);
//          }
//          for (int i=0; i<ntimes2; i++) {
//            image_stinger.setSample(i, tempw);
//            tempw = (FlatField) tempw.add(val);
//          }
//          FieldImpl[] images19 = {image_sequence, image_stinger};
//          Tuple big_tuple = new Tuple(images19);
//
//          display.addMap(new ScalarMap(RealType.Latitude, Display.YAxis));
//          display.addMap(new ScalarMap(RealType.Longitude, Display.XAxis));
//          display.addMap(new ScalarMap(vis_radiance, Display.ZAxis));
//          display.addMap(new ScalarMap(ir_radiance, Display.Green));
//          display.addMap(new ConstantMap(0.5, Display.Blue));
//          display.addMap(new ConstantMap(0.5, Display.Red));
//          ScalarMap map1value = new ScalarMap(RealType.Time, Display.SelectValue);
//          display.addMap(map1value);
//          
//          DataReferenceImpl ref_big_tuple;
//          ref_big_tuple = new DataReferenceImpl("ref_big_tuple");
//          ref_big_tuple.setData(big_tuple);
//          display.addReference(ref_big_tuple, null);
//          
//          final ValueControl value1control =
//           (ValueControl) map1value.getControl();
//          DataReferenceImpl value_ref = new DataReferenceImpl("value");
//
//          widget = new VisADSlider("value", 0, 100, 0, 0.01, value_ref, RealType.Generic);
//
//          final DataReference cell_ref = value_ref;
//
//          CellImpl cell = new CellImpl() {
//           public void doAction() throws RemoteException, VisADException {
//            value1control.setValue(((Real) cell_ref.getData()).getValue());
//           }
//          };
//         cell.addReference(cell_ref);

//    final RealType ir_radiance =
//      RealType.getRealType("ir_radiance", CommonUnit.degree);
//    Unit cycles = CommonUnit.dimensionless.divide(CommonUnit.second);
//    Unit hz = cycles.clone("Hz");
//    final RealType count = RealType.getRealType("count", hz);
//    FunctionType ir_histogram = new FunctionType(ir_radiance, count);
//    final RealType vis_radiance = RealType.getRealType("vis_radiance");
//
//    int size = 64;
//    FlatField histogram1 = FlatField.makeField(ir_histogram, size, false);
//    Real direct = new Real(ir_radiance, 2.0);
//    Real[] reals3 = {new Real(count, 1.0), new Real(ir_radiance, 2.0),
//                     new Real(vis_radiance, 1.0)};
//    RealTuple direct_tuple = new RealTuple(reals3);
//
//    //dpys[0].addMap(new ScalarMap(vis_radiance, Display.ZAxis));
//    ScalarMap irmap = new ScalarMap(ir_radiance, Display.XAxis);
//    //dpys[0].addMap(irmap);
//    irmap.setOverrideUnit(CommonUnit.radian);
//    //dpys[0].addMap(new ScalarMap(count, Display.YAxis));
//    //dpys[0].addMap(new ScalarMap(count, Display.Green));
//
//    //GraphicsModeControl mode = dpys[0].getGraphicsModeControl();
//    //mode.setPointSize(5.0f);
//    //mode.setPointMode(false);
//    //mode.setScaleEnable(true);
//
//    DataReferenceImpl ref_direct = new DataReferenceImpl("ref_direct");
//    ref_direct.setData(direct);
//    DataReference[] refs1 = {ref_direct};
//    //dpys[0].addReferences(new DirectManipulationRendererA3D(), refs1, null);
//
//    DataReferenceImpl ref_direct_tuple =
//      new DataReferenceImpl("ref_direct_tuple");
//    ref_direct_tuple.setData(direct_tuple);
//    DataReference[] refs2 = {ref_direct_tuple};
//    //dpys[0].addReferences(new DirectManipulationRendererA3D(), refs2, null);
//
//    DataReferenceImpl ref_histogram1 = new DataReferenceImpl("ref_histogram1");
//    ref_histogram1.setData(histogram1);
//    DataReference[] refs3 = {ref_histogram1};
//    //dpys[0].addReferences(new DirectManipulationRendererA3D(), refs3, null);
//
//    display.addMap(new ScalarMap(vis_radiance, Display.ZAxis));
//    display.addMap(new ScalarMap(ir_radiance, Display.XAxis));
//    display.addMap(new ScalarMap(count, Display.YAxis));
//    display.addMap(new ScalarMap(count, Display.Green));
//    //final DisplayRenderer dr0 = dpys[0].getDisplayRenderer();
//    final DisplayRenderer dr1 = display.getDisplayRenderer();
//    //dr0.setCursorStringOn(true);
//    //dr1.setCursorStringOn(false);
//
//    GraphicsModeControl mode = display.getGraphicsModeControl();
//    mode.setPointSize(8.0f);
//    mode.setPointMode(false);
//    mode.setScaleEnable(true);
//
//    display.addReferences(new DirectManipulationRendererA3D(), refs1, null);
//    display.addReferences(new DirectManipulationRendererA3D(), refs2, null);
//    display.addReferences(new DirectManipulationRendererA3D(), refs3, new ConstantMap[][] {{new ConstantMap(2f, Display.LineWidth)}});
//
//    MouseHelper helper = dr1.getMouseBehavior().getMouseHelper();
//    helper.setFunctionMap(new int[][][]
//      {{{MouseHelper.DIRECT, MouseHelper.DIRECT},
//        {MouseHelper.DIRECT, MouseHelper.DIRECT}},
//       {{MouseHelper.ROTATE, MouseHelper.NONE},
//        {MouseHelper.NONE, MouseHelper.NONE}},
//       {{MouseHelper.TRANSLATE, MouseHelper.ZOOM},
//        {MouseHelper.NONE, MouseHelper.TRANSLATE}}});


      /* Test61: Volume Rendering */
     
    RealType xr = RealType.getRealType("xr");
    RealType yr = RealType.getRealType("yr");
    RealType zr = RealType.getRealType("zr");
    RealType wr = RealType.getRealType("wr");
    RealType[] types3d = {xr, yr, zr};
    RealTupleType earth_location3d = new RealTupleType(types3d);
    FunctionType grid_tuple = new FunctionType(earth_location3d, wr);

    int NX = 256;
    int NY = 256;
    int NZ = 256;
    Integer3DSet set = new Integer3DSet(NX, NY, NZ);
    FlatField grid3d = new FlatField(grid_tuple, set);

    float[][] values = new float[1][NX * NY * NZ];
    int k = 0;
    for (int iz=0; iz<NZ; iz++) {
      // double z = Math.PI * (-1.0 + 2.0 * iz / (NZ - 1.0));
      double z = Math.PI * (-1.0 + 2.0 * iz * iz / ((NZ - 1.0)*(NZ - 1.0)) );
      for (int iy=0; iy<NY; iy++) {
        double y = -1.0 + 2.0 * iy / (NY - 1.0);
        for (int ix=0; ix<NX; ix++) {
          double x = -1.0 + 2.0 * ix / (NX - 1.0);
          double r = x - 0.5 * Math.cos(z);
          double s = y - 0.5 * Math.sin(z);
          double dist = Math.sqrt(r * r + s * s);
          values[0][k] = (float) ((dist < 0.1) ? 10.0 : 1.0 / dist);
          k++;
        }
      }
    }
    grid3d.setSamples(values);

    display.addMap(new ScalarMap(xr, Display.XAxis));
    display.addMap(new ScalarMap(yr, Display.YAxis));
    display.addMap(new ScalarMap(zr, Display.ZAxis));

    ScalarMap xrange = new ScalarMap(xr, Display.SelectRange);
    ScalarMap yrange = new ScalarMap(yr, Display.SelectRange);
    ScalarMap zrange = new ScalarMap(zr, Display.SelectRange);
    display.addMap(xrange);
    display.addMap(yrange);
    display.addMap(zrange);

    GraphicsModeControl mode = display.getGraphicsModeControl();
    mode.setScaleEnable(true);

    mode.setTexture3DMode(GraphicsModeControl.STACK2D);

    // new
    RealType duh = RealType.getRealType("duh");
    int NT = 32;
    Linear2DSet set2 = new Linear2DSet(0.0, (double) NX, NT,
                                       0.0, (double) NY, NT);
    RealType[] types2d = {xr, yr};
    RealTupleType domain2 = new RealTupleType(types2d);
    FunctionType ftype2 = new FunctionType(domain2, duh);
    float[][] v2 = new float[1][NT * NT];
    for (int i=0; i<NT*NT; i++) {
      v2[0][i] = (i * i) % (NT/2 +3);
    }
    // float[][] v2 = {{1.0f,2.0f,3.0f,4.0f}};
    FlatField field2 = new FlatField(ftype2,set2);
    field2.setSamples(v2);
    display.addMap(new ScalarMap(duh, Display.RGB));

    ScalarMap map1color = new ScalarMap(wr, Display.RGBA);
    display.addMap(map1color);

    ColorAlphaControl control = (ColorAlphaControl) map1color.getControl();
    control.setTable(buildTable(control.getTable()));

    DataReferenceImpl ref_grid3d = new DataReferenceImpl("ref_grid3d");
    ref_grid3d.setData(grid3d);

    DataReferenceImpl ref2 = new DataReferenceImpl("ref2");
    ref2.setData(field2);

    ConstantMap[] cmaps = {new ConstantMap(0.0, Display.TextureEnable)};
    display.addReference(ref2, cmaps);

    display.addReference(ref_grid3d, null);

    widget = getSpecialComponent(display);


       /* Main display window */
//       final JComponent outerComp = new JPanel(new BorderLayout());
//       JPanel cntrlPanel = new JPanel(new FlowLayout());
//       JPanel panel = new JPanel();
//       final Component comp = display.getComponent();
//       outerComp.add(comp, BorderLayout.CENTER);
//       outerComp.add(cntrlPanel, BorderLayout.SOUTH);
//       frame.getContentPane().add(outerComp);  
       
      /* If we have a control widget */
       if (widget != null) {
          JPanel panel2 = new JPanel();
          panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
          panel2.setAlignmentY(JPanel.TOP_ALIGNMENT);
          panel2.setAlignmentX(JPanel.LEFT_ALIGNMENT);
          panel2.add(widget);

          final JFrame frame2 = new JFrame();
          frame2.setPreferredSize(new Dimension(width, height));
          frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame2.getContentPane().add(panel2);

          frame2.pack();
          frame2.setVisible(true);
       }
       
       //mode.setProjectionPolicy(1);
       
    }
    
public static void fillField(FlatField image, double step, double half)
         throws VisADException, RemoteException {
    Random random = new Random();
    FunctionType type = (FunctionType) image.getType();
    RealTupleType dtype = type.getDomain();
    RealTupleType rtype = type.getFlatRange();
    int domain_dim = dtype.getDimension();
    int range_dim = rtype.getDimension();
    SampledSet domain_set = (SampledSet) image.getDomainSet();
    int dsize = domain_set.getLength();

    double[][] data = new double[range_dim][dsize];
    float[][] samples = domain_set.getSamples();
    for (int k=0; k<range_dim; k++) {
      if (domain_dim == 1) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          if (k == 0) {
            data[k][i] = (float) Math.abs(step * (x - half));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
      else if (domain_dim == 2) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          float y = samples[1][i];
          if (k == 0) {
            data[k][i] = (float) (step * Math.sqrt(
              (x - half) * (x - half) +
              (y - half) * (y - half)));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else if (k == 2) {
            data[k][i] = y;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
      else if (domain_dim == 3) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          float y = samples[1][i];
          float z = samples[2][i];
          if (k == 0) {
            data[k][i] = (float) (step * Math.sqrt(
              (x - half) * (x - half) +
              (y - half) * (y - half) +
              (z - half) * (z - half)));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else if (k == 2) {
            data[k][i] = y;
          }
          else if (k == 3) {
            data[k][i] = z;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
    }
    image.setSamples(data);
  }

  private static float[][] buildTable(float[][] table)
  {
    int length = table[0].length;
    for (int i=0; i<length; i++) {
      float a = ((float) i) / ((float) (table[3].length - 1));
      table[3][i] = a;
    }
    return table;
  }
  
  private static Component getSpecialComponent(DisplayImpl display)
    throws RemoteException, VisADException
  {
    java.util.Vector mapVector = display.getMapVector();
    final int numMaps = mapVector.size();

    ScalarMap xrange = (ScalarMap )mapVector.elementAt(numMaps-5);
    ScalarMap yrange = (ScalarMap )mapVector.elementAt(numMaps-4);
    ScalarMap zrange = (ScalarMap )mapVector.elementAt(numMaps-3);

    ScalarMap map1color = (ScalarMap )mapVector.elementAt(numMaps-1);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(new LabeledColorWidget(map1color));
    panel.add(new SelectRangeWidget(xrange));
    panel.add(new SelectRangeWidget(yrange));
    panel.add(new SelectRangeWidget(zrange));
    return panel;
  }
   
   public static void main(String[] args) throws VisADException, RemoteException {
      createAndShowGUI();
//      try {
//         java.lang.Thread.sleep(5000);
//      }
//      catch (Exception e) {
//         
//      }
//      createAndShowGUI();
//      createAndShowGUI();
//         SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                //Turn off metal's use of bold fonts
//                UIManager.put("swing.boldMetal", Boolean.FALSE);
//                try {
//                   createAndShowGUI();
//                }
//                catch (Exception e) {
//                   e.printStackTrace();
//                }
//            }
//        });            
      
   }
  
}

