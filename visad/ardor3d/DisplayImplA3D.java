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
import visad.util.ContourWidget;

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
       DisplayManagerA3D manager = DisplayManagerA3D.createDisplayManager(new Dimension(width, height), dspRenderer, api);
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
    if (vga == null) return null;
    
    Mesh mesh = new Mesh();
    MeshData meshData = new MeshData();
    
    if (defaultColor != null) {
       mesh.setDefaultColor(defaultColor);
    }
    
    boolean mode2d = getDisplayRenderer().getMode2D();

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
      
      basicGeometry(vga, meshData, false);
      mesh.setMeshData(meshData);      
      return mesh;
    }
    else if (vga instanceof VisADLineStripArray) {
      if (vga.vertexCount == 0) return null;
      VisADLineStripArray vgb = (VisADLineStripArray) vga;


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
        float[] fltClrs = new float[vga.colors.length];
        for (int i=0; i<fltClrs.length; i++) {
          fltClrs[i] = ((float)Byte.toUnsignedInt(vga.colors[i]))/255f;
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
       
       JPanel widget = null;
       
       final JFrame frame = new JFrame();
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       final DisplayImplA3D display = new DisplayImplA3D("Display", width, height, JOGL_AWT);
       //final visad.java3d.DisplayImplJ3D display = new visad.java3d.DisplayImplJ3D("Display");
       
       /* Simple Test 1 */
       FieldImpl dataFld;
       FunctionType fncType = new FunctionType(RealTupleType.SpatialEarth2DTuple, RealType.Generic);
       dataFld = FlatField.makeField(fncType, 2048, false);
       
       ScalarMap xmap = new ScalarMap(RealType.Longitude, Display.XAxis);
       ScalarMap ymap = new ScalarMap(RealType.Latitude, Display.YAxis);
       ScalarMap cmap = new ScalarMap(RealType.Generic, Display.RGB);
       
       display.addMap(xmap);
       display.addMap(ymap);
       display.addMap(cmap);
       
       widget = new ColorMapWidget(cmap);
       
       /* test 2 */
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
//       
         DataReferenceImpl ref = new DataReferenceImpl("vfld");
         ref.setData(dataFld);
         display.addReference(ref);
       
 
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
//       ScalarMap map1color = new ScalarMap(vis_radiance, Display.RGB);
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
//       display.addReference(ref_grid3d, new ConstantMap[] {new ConstantMap(0.0, Display.Red),
//               new ConstantMap(1.0, Display.Green), new ConstantMap(0.0, Display.Blue)});
       
       
       final JComponent outerComp = new JPanel(new BorderLayout());
       JPanel cntrlPanel = new JPanel(new FlowLayout());
       JPanel panel = new JPanel();
       final Component comp = display.getComponent();
       outerComp.add(comp, BorderLayout.CENTER);
       outerComp.add(cntrlPanel, BorderLayout.SOUTH);
       frame.getContentPane().add(outerComp);  
       //Display the window.
       frame.pack();
       frame.setVisible(true);
 
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
   
   public static void main(String[] args) throws VisADException, RemoteException {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                try {
                   createAndShowGUI();
                }
                catch (Exception e) {
                   e.printStackTrace();
                }
            }
        });            
      
   }
  
}

