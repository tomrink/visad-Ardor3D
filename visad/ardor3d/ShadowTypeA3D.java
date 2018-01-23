//
// ShadowTypeA3D.java
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
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.scenegraph.Spatial;
import visad.*;

import javax.media.j3d.*;


import java.util.Vector;

import java.rmi.*;

/**
 * The ShadowTypeA3D hierarchy shadows the MathType hierarchy, within a
 * DataDisplayLink, under Ardor3D.
 * <P>
 */
public abstract class ShadowTypeA3D extends ShadowType {

  /** This holds the cached java3d Appearances */
  private static java.util.Hashtable appearanceCache = new java.util.Hashtable();

  /** This holds the cached java3d ColoringAttributes */
  private static java.util.Hashtable coloringAttributesCache = new java.util.Hashtable();

  /** This holds the cached java3d TransparencyAttributes */
  private static java.util.Hashtable transparencyAttributesCache = new java.util.Hashtable();

  /** Do we try to cache appearances */
  private boolean cacheAppearances = false;

  /**
   * Do we try to merge Geometries into existings Shape3D scene graph components
   */
  private boolean mergeShapes = false;

  /** For logging the number of Appearance objects created */
  public static int appearanceCnt = 0;

  /** For logging the number of Shape3D objects created */
  public static int shape3DCnt = 0;

  /** basic information about this ShadowTypeJ3D */
  MathType Type; // MathType being shadowed

  /**  */
  transient DataDisplayLink Link;

  /**  */
  transient DisplayImplA3D display;

  /**  */
  transient private Data data; // from Link.getData()

  /**  */
  private ShadowTypeA3D Parent;

  // String and TextControl to pass on to children

  /**  */
  String inheritedText = null;

  /**  */
  TextControl inheritedTextControl = null;

  /**  */
  ShadowType adaptedShadowType;

  /**  */
  ProjectionControlListener projListener = null;

  /**
   * Create a new ShadowTypeJ3D
   *
   * @param type
   *          MathType of the data
   * @param link
   *          the data/display link
   * @param parent
   *          parent ShadowType
   *
   * @throws RemoteException
   *           problem creating remote instance
   * @throws VisADException
   *           problem creating local instance
   */
  public ShadowTypeA3D(MathType type, DataDisplayLink link, ShadowType parent)
      throws VisADException, RemoteException {
    super(type, link, getAdaptedParent(parent));
    Type = type;
    Link = link;
    display = (DisplayImplA3D) link.getDisplay();
    Parent = (ShadowTypeA3D) parent;
    data = link.getData();
  }

  /**
   * Get the adapted ShadowType for the parent
   *
   * @param parent
   *          the parent object to test
   *
   * @return the adapted ShadowType
   */
  public static ShadowType getAdaptedParent(ShadowType parent) {
    if (parent == null)
      return null;
    else
      return parent.getAdaptedShadowType();
  }

  /**
   * Get the adapted ShadowType for this instance
   *
   * @return the adapted ShadowType
   */
  public ShadowType getAdaptedShadowType() {
    return adaptedShadowType;
  }

  /**
   *
   *
   * @param type
   * @param doRef
   *
   * @return component array
   *
   * @throws VisADException
   */
  
  public ShadowRealType[] getComponents(ShadowType type, boolean doRef)
      throws VisADException {
    return adaptedShadowType.getComponents(type, doRef);
  }

  /**
   *
   *
   * @return inherited text
   */
  
  public String getParentText() {
    if (Parent != null && Parent.inheritedText != null
        && Parent.inheritedTextControl != null) {
      return Parent.inheritedText;
    } else {
      return null;
    }
  }

  /**
   *
   *
   * @return inherited text control
   */
  public TextControl getParentTextControl() {
    if (Parent != null && Parent.inheritedText != null
        && Parent.inheritedTextControl != null) {
      return Parent.inheritedTextControl;
    } else {
      return null;
    }
  }

  /**
   *
   *
   * @param text
   * @param control
   */
  public void setText(String text, TextControl control) {
    inheritedText = text;
    inheritedTextControl = control;
  }

  /**
   * Get the data
   *
   * @return the data
   */
  public Data getData() {
    return data;
  }

  /**
   * Get the display
   *
   * @return the display
   */
  public DisplayImpl getDisplay() {
    return display;
  }

  /**
   * Get the MathType of the Data
   *
   * @return the MathType
   */
  public MathType getType() {
    return Type;
  }

  /**
   * Get the level of difficulty for this transform
   *
   * @return the level of difficulty
   */
  public int getLevelOfDifficulty() {
    return adaptedShadowType.getLevelOfDifficulty();
  }

  /**
   *
   *
   * @return boolean TBD
   */
  public boolean getMultipleDisplayScalar() {
    return adaptedShadowType.getMultipleDisplayScalar();
  }

  /**
   *
   *
   * @return boolean TBD
   */
  public boolean getMappedDisplayScalar() {
    return adaptedShadowType.getMappedDisplayScalar();
  }

  /**
   *
   *
   * @return int array display indices
   */
  public int[] getDisplayIndices() {
    return adaptedShadowType.getDisplayIndices();
  }

  /**
   *
   *
   * @return int array value indices
   */
  public int[] getValueIndices() {
    return adaptedShadowType.getValueIndices();
  }

  /**
   * checkIndices: check for rendering difficulty, etc
   *
   * @param indices
   * @param display_indices
   * @param value_indices
   * @param isTransform
   * @param levelOfDifficulty
   *
   * @return num occcurrences RealType and DisplayRealType
   *
   * @throws RemoteException
   * @throws VisADException
   */
  public int checkIndices(int[] indices, int[] display_indices,
      int[] value_indices, boolean[] isTransform, int levelOfDifficulty)
      throws VisADException, RemoteException {
    return adaptedShadowType.checkIndices(indices, display_indices,
        value_indices, isTransform, levelOfDifficulty);
  }

  /**
   * clear AccumulationVector
   *
   * @throws VisADException
   */
  public void preProcess() throws VisADException {
  }

  /**
   * transform data into a Java3D scene graph; add generated scene graph
   * components as children of group; value_array are inherited valueArray
   * values; default_values are defaults for each display.DisplayRealTypeVector;
   * return true if need post-process; this is default (for ShadowTextType)
   *
   * @param group
   *          group to add to
   * @param data
   *          the data to transform
   * @param value_array
   *          the values
   * @param default_values
   *          the default values
   * @param renderer
   *          the renderer
   *
   * @return false
   *
   * @throws RemoteException
   * @throws VisADException
   */
  public boolean doTransform(Object group, Data data, float[] value_array,
      float[] default_values, DataRenderer renderer) throws VisADException,
      RemoteException {
    return false;
  }

  /**
   * render accumulated Vector of value_array-s to and add to group; then clear
   * AccumulationVector
   *
   * @param group
   *
   * @throws VisADException
   */
  public void postProcess(Object group) throws VisADException {
  }
  
  /* helpers for doTransform */

  /**
   * map values into display_values according to ScalarMap-s in reals
   *
   * @param display_values
   * @param values
   * @param reals
   *
   * @throws VisADException
   */
  public static void mapValues(float[][] display_values, double[][] values,
      ShadowRealType[] reals) throws VisADException {
    ShadowType.mapValues(display_values, values, reals);
  }

  /**
   * map values into display_values according to ScalarMap-s in reals
   *
   * @param display_values
   * @param values
   * @param reals
   *
   * @throws VisADException
   */
  public static void mapValues(float[][] display_values, float[][] values,
      ShadowRealType[] reals) throws VisADException {
    mapValues(display_values, values, reals, true);
  }

  /**
   * map values into display_values according to ScalarMap-s in reals
   *
   * @param display_values
   * @param values
   * @param reals
   * @param copy
   *
   * @throws VisADException
   */
  public static void mapValues(float[][] display_values, float[][] values,
      ShadowRealType[] reals, boolean copy) throws VisADException {
    ShadowType.mapValues(display_values, values, reals, copy);
  }

  /**
   *
   *
   * @param spatial_values
   * @param color_values
   *
   * @return point geometry array
   *
   * @throws VisADException
   */
  public static VisADGeometryArray makePointGeometry(float[][] spatial_values,
      byte[][] color_values) throws VisADException {
    return ShadowType.makePointGeometry(spatial_values, color_values);
  }

  /**
   * Make an Appearance object for this display
   *
   * @param mode
   *          GraphicsModeControl
   * @param constant_alpha
   *          the constant alpha value
   * @param constant_color
   *          the constant color value
   * @param geometry
   *          the GeometryArray
   * @param no_material
   *          flag for material
   *
   * @return appearance object constructed from geometry array
   */
  
  public Appearance makeAppearance(GraphicsModeControl mode,
      TransparencyAttributes constant_alpha, ColoringAttributes constant_color,
      GeometryArray geometry, boolean no_material) {
    return ShadowTypeA3D.staticMakeCachedAppearance(mode, constant_alpha,
        constant_color, geometry, no_material, false);
  }

  /**
   * Make an Appearance that may be cached or not, depending on the okToCache
   * and the cache flag
   *
   * @param mode
   *          GraphicsModeControl
   * @param constant_alpha
   *          the constant alpha value
   * @param constant_color
   *          the constant color value
   * @param geometry
   *          the GeometryArray
   * @param no_material
   *          flag for material
   * @param okToCache
   *          flag for caching checked with mode.getCacheAppearances
   *
   * @return appearance object constructed from geometry array
   */
  
  private Appearance makeCachedAppearance(GraphicsModeControl mode,
      TransparencyAttributes constant_alpha, ColoringAttributes constant_color,
      GeometryArray geometry, boolean no_material, boolean okToCache) {
    return ShadowTypeA3D.staticMakeCachedAppearance(mode, constant_alpha,
        constant_color, geometry, no_material, okToCache);
  }

  /**
   * A utility method to add the given geometry into the given group with the
   * given appearance. If the mergeShapes flag is true then this will look for
   * an existing child Shape3D node of the group that has the same Appearance
   * and contains geometries of the same type. If found it will add the geometry
   * to that Shape3D node. If not found or if mergeShapes is false then this
   * behaves normally, creating a new Shape3D node and adding it to the group.
   *
   * @param group
   *          group to add to
   * @param geometry
   *          geometry to add
   * @param appearance
   *          appearance
   */
  private void addToShape(Group group, GeometryArray geometry,
      Appearance appearance) {
    Shape3D shape = null;
    if (mergeShapes) {
      int cnt = group.numChildren();
      for (int i = 0; i < cnt; i++) {
        Node node = group.getChild(i);
        if (!(node instanceof Shape3D))
          continue;
        Shape3D s = (Shape3D) node;
        // Make sure the geometries are the same
        int subcnt = s.numGeometries();
        if (subcnt > 0) {
          if (!(s.getGeometry(0).getClass().equals(geometry.getClass()))) {
            continue;
          }
        }
        // Make sure the appearance is the same
        if (s.getAppearance().equals(appearance)) {
          shape = s;
          break;
        }
      }
    }

    if (shape == null) {
      shape = new Shape3D(geometry, appearance);
      shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      group.addChild(shape);
      shape3DCnt++;
    } else {
      shape.addGeometry(geometry);
    }
  }

  /**
   * A utility method to create a ColoringAttribute with the given colors If the
   * cacheAppearances flag is true then this will try to find the
   * ColoringAttribute in the cache. If false it just creates a new one. Note:
   * If caching is on then the result object should be treated as being
   * immutable
   *
   * @param red
   *          red value
   * @param green
   *          green value
   * @param blue
   *          blue value
   *
   * @return the ColoringAttributes
   */
  private ColoringAttributes getColoringAttributes(float red, float green,
      float blue) {
    ColoringAttributes ca = null;
    String key = null;
    if (cacheAppearances) {
      key = red + "," + green + "," + blue;
      ca = (ColoringAttributes) coloringAttributesCache.get(key);
    }
    if (ca == null) {
      ca = new ColoringAttributes(red, green, blue,
          ColoringAttributes.SHADE_GOURAUD);
      ca.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
      ca.setCapability(ColoringAttributes.ALLOW_SHADE_MODEL_READ);
      if (cacheAppearances) {
        coloringAttributesCache.put(key, ca);
      }
    }
    return ca;
  }

  /**
   * A utility method to create a TransparencyAttributes with the given mode and
   * value. If the flag cacheAppearances is true then this will try to find the
   * TransparencyAttributes in the cache. If false it just creates a new one.
   * Note: If caching is on then the result object should be treated as being
   * immutable
   *
   * @param mode
   *          Transparency mode
   * @param value
   *          transparancy value
   *
   * @return cached or new TransparencyAttributes object
   */
  
  private TransparencyAttributes getTransparencyAttributes(int mode, float value) {
    String key = null;
    TransparencyAttributes ta = null;
    if (cacheAppearances) {
      key = mode + "_" + value;
      ta = (TransparencyAttributes) transparencyAttributesCache.get(key);
    }
    if (ta == null) {
      ta = new TransparencyAttributes(mode, value);
      ta.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
      ta.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
      if (cacheAppearances) {
        transparencyAttributesCache.put(key, ta);
      }
    }
    return ta;
  }

  /**
   * Construct an Appearance object from a GeometryArray
   *
   * @param mode
   *          GraphicsModeControl
   * @param constant_alpha
   *          transparency attributes
   * @param constant_color
   *          color to use
   * @param geometry
   *          geometry to use for the appearance
   * @param no_material
   *          true to not use a Material for illumination, false to use it for
   *          2-D geometries
   *
   * @return The new appearance
   */
  public static Appearance staticMakeAppearance(GraphicsModeControl mode,
      TransparencyAttributes constant_alpha, ColoringAttributes constant_color,
      GeometryArray geometry, boolean no_material) {

    return staticMakeCachedAppearance(mode, constant_alpha, constant_color,
        geometry, no_material, false);
  }

  /**
   * Construct an Appearance object from a GeometryArray
   *
   * @param mode
   *          GraphicsModeControl
   * @param constant_alpha
   *          transparency attributes
   * @param constant_color
   *          color to use
   * @param geometry
   *          geometry to use for the appearance
   * @param no_material
   *          true to not use a Material for illumination, false to use it for
   *          2-D geometries
   * @param okToCache
   *          If true and if the mode's cacheAppearances flag is true then we
   *          will use the appearance cache.
   *
   * @return The new appearance or, if available a previously cached one
   */
  private static Appearance staticMakeCachedAppearance(
      GraphicsModeControl mode, TransparencyAttributes constant_alpha,
      ColoringAttributes constant_color, GeometryArray geometry,
      boolean no_material, boolean okToCache) {

    boolean doMaterial = false;
    if (!(geometry instanceof LineArray || geometry instanceof PointArray
        || geometry instanceof IndexedLineArray
        || geometry instanceof IndexedPointArray
        || geometry instanceof IndexedLineStripArray || geometry instanceof LineStripArray)) {
      if (!no_material) {
        doMaterial = true;
      }
    }

    Object cacheKey = null;
    Appearance appearance = null;

    if (mode.getCacheAppearances() && okToCache) {
	cacheKey = mode.getSaveString() + "_" + (constant_alpha==null?"null" :(constant_alpha.getTransparency()+"_" + constant_alpha.getTransparencyMode()))  + "_"
          + constant_color + "_" + new Boolean(doMaterial);
      appearance = (Appearance) appearanceCache.get(cacheKey);
      if (appearance != null) {
        return appearance;
      }
    }
    appearanceCnt++;
    appearance = new Appearance();
    appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
    appearance.setCapability(Appearance.ALLOW_POINT_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_TEXGEN_READ);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
    /* TDR (2013-10-16): Can possibly cause problems on ATI graphics
    appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
    */
    // appearance.setCapability(Appearance.ALLOW_TEXTURE_UNIT_STATE_READ);
    appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

    LineAttributes line = new LineAttributes();
    line.setCapability(LineAttributes.ALLOW_ANTIALIASING_READ);
    line.setCapability(LineAttributes.ALLOW_PATTERN_READ);
    line.setCapability(LineAttributes.ALLOW_WIDTH_READ);
    line.setLineWidth(mode.getLineWidth());
    int pattern = GraphicsModeControlA3D.LINE_PATTERN[mode.getLineStyle()];
    line.setLinePattern(pattern);
    appearance.setLineAttributes(line);

    PointAttributes point = new PointAttributes();
    point.setCapability(PointAttributes.ALLOW_ANTIALIASING_READ);
    point.setCapability(PointAttributes.ALLOW_SIZE_READ);
    point.setPointSize(mode.getPointSize());
    appearance.setPointAttributes(point);

    PolygonAttributes polygon = new PolygonAttributes();
    polygon.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
    polygon.setCapability(PolygonAttributes.ALLOW_MODE_READ);
    polygon.setCapability(PolygonAttributes.ALLOW_NORMAL_FLIP_READ);
    polygon.setCapability(PolygonAttributes.ALLOW_OFFSET_READ);
    polygon.setCullFace(PolygonAttributes.CULL_NONE);
    polygon.setPolygonMode(mode.getPolygonMode());

    try {
      float polygonOffset = mode.getPolygonOffset();
      if (polygonOffset == polygonOffset)
        polygon.setPolygonOffset(polygonOffset);
    } catch (Exception e) {
    }

    // - TDR, use reflection since setPolygonOffsetFactor is not available in
    // earlier versions of Java3D.

    try {
      java.lang.reflect.Method method = polygon.getClass().getMethod(
          "setPolygonOffsetFactor", new Class[] { float.class });
      float polygonOffsetFactor = mode.getPolygonOffsetFactor();
      if (polygonOffsetFactor == polygonOffsetFactor) {
        method.invoke(polygon, new Object[] {polygonOffsetFactor});
      }
    } catch (Exception e) {
    }

    appearance.setPolygonAttributes(polygon);

    RenderingAttributes rendering = new RenderingAttributes();
    rendering.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_FUNCTION_READ);
    rendering.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_READ);
    rendering.setCapability(RenderingAttributes.ALLOW_DEPTH_ENABLE_READ);
    /* TDR (2013-10-16): Can possibly cause problems on ATI graphics
    rendering.setCapability (RenderingAttributes.ALLOW_VISIBLE_WRITE);
    */

    //rendering.setCapability(RenderingAttributes.ALLOW_IGNORE_VERTEX_COLORS_READ
    // );
    // rendering.setCapability(RenderingAttributes.ALLOW_RASTER_OP_READ);
    // rendering.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
    rendering.setDepthBufferEnable(((GraphicsModeControlA3D)mode).getDepthBufferEnable());
    appearance.setRenderingAttributes(rendering);

    if (constant_color != null) {
      // constant_color.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
      //constant_color.setCapability(ColoringAttributes.ALLOW_SHADE_MODEL_READ);
      appearance.setColoringAttributes(constant_color);
    }
    // only do Material if geometry is 2-D (not 0-D points or 1-D lines)
    if (doMaterial) {
      Material material = new Material();
      material.setCapability(Material.ALLOW_COMPONENT_READ);
      material.setSpecularColor(0.0f, 0.0f, 0.0f);

      // no lighting in 2-D mode
      if (!mode.getMode2D())
        material.setLightingEnable(true);
      appearance.setMaterial(material);
    }
    if (constant_alpha != null) {
      // constant_alpha.setCapability(TransparencyAttributes.
      // ALLOW_BLEND_FUNCTION_READ);
      // constant_alpha.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
      // constant_alpha.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
      appearance.setTransparencyAttributes(constant_alpha);
    }
    if (cacheKey != null) {
      appearanceCache.put((Object) cacheKey, appearance);
    }
    return appearance;
  }

  /**
   * collect and transform Shape DisplayRealType values from display_values;
   * offset by spatial_values, selected by range_select
   *
   * @param display_values
   * @param valueArrayLength
   * @param valueToMap
   * @param MapVector
   * @param valueToScalar
   * @param display
   * @param default_values
   * @param inherited_values
   * @param spatial_values
   * @param color_values
   * @param range_select
   * @param index
   * @param shadow_api
   *
   * @return transformed Shape values
   *
   * @throws RemoteException
   * @throws VisADException
   */
  
  public VisADGeometryArray[] assembleShape(float[][] display_values,
      int valueArrayLength, int[] valueToMap, Vector MapVector,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      int[] inherited_values, float[][] spatial_values, byte[][] color_values,
      boolean[][] range_select, int index, ShadowType shadow_api)
      throws VisADException, RemoteException {
    return adaptedShadowType.assembleShape(display_values, valueArrayLength,
        valueToMap, MapVector, valueToScalar, display, default_values,
        inherited_values, spatial_values, color_values, range_select, index,
        shadow_api);
  }

  /**
   * collect and transform spatial DisplayRealType values from display_values;
   * add spatial offset DisplayRealType values; adjust flow1_values and
   * flow2_values for any coordinate transform; if needed, return a spatial Set
   * from spatial_values, with the same topology as domain_set (or an
   * appropriate Irregular topology); domain_set = null and allSpatial = false
   * if not called from ShadowFunctionType
   *
   * @param spatial_values
   * @param display_values
   * @param valueArrayLength
   * @param valueToScalar
   * @param display
   * @param default_values
   * @param inherited_values
   * @param domain_set
   * @param allSpatial
   * @param set_for_shape
   * @param spatialDimensions
   * @param range_select
   * @param flow1_values
   * @param flow2_values
   * @param flowScale
   * @param swap
   * @param renderer
   * @param shadow_api
   *
   * @return spatial Set from spatial values
   *
   * @throws RemoteException
   * @throws VisADException
   */
  
  public Set assembleSpatial(float[][] spatial_values,
      float[][] display_values, int valueArrayLength, int[] valueToScalar,
      DisplayImpl display, float[] default_values, int[] inherited_values,
      Set domain_set, boolean allSpatial, boolean set_for_shape,
      int[] spatialDimensions, boolean[][] range_select,
      float[][] flow1_values, float[][] flow2_values, float[] flowScale,
      boolean[] swap, DataRenderer renderer, ShadowType shadow_api)
      throws VisADException, RemoteException {
    return adaptedShadowType.assembleSpatial(spatial_values, display_values,
        valueArrayLength, valueToScalar, display, default_values,
        inherited_values, domain_set, allSpatial, set_for_shape,
        spatialDimensions, range_select, flow1_values, flow2_values, flowScale,
        swap, renderer, shadow_api);
  }

  /**
   * assemble Flow components; Flow components are 'single', so no compositing
   * is required
   *
   * @param flow1_values
   * @param flow2_values
   * @param flowScale
   * @param display_values
   * @param valueArrayLength
   * @param valueToScalar
   * @param display
   * @param default_values
   * @param range_select
   * @param renderer
   * @param shadow_api
   *
   * @throws RemoteException
   * @throws VisADException
   */
  public void assembleFlow(float[][] flow1_values, float[][] flow2_values,
      float[] flowScale, float[] arrowScale, float[][] display_values, int valueArrayLength,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      boolean[][] range_select, DataRenderer renderer, ShadowType shadow_api)
      throws VisADException, RemoteException {
    adaptedShadowType.assembleFlow(flow1_values, flow2_values, flowScale, arrowScale,
        display_values, valueArrayLength, valueToScalar, display,
        default_values, range_select, renderer, shadow_api);
  }

  /**
   *
   *
   * @param which
   * @param flow_values
   * @param flowScale
   * @param spatial_values
   * @param color_values
   * @param range_select
   *
   * @return flow geometry array
   *
   * @throws VisADException
   */
  
  public VisADGeometryArray[] makeFlow(int which, float[][] flow_values,
      float flowScale, float arrowScale, float[][] spatial_values, byte[][] color_values,
      boolean[][] range_select) throws VisADException {
    return adaptedShadowType.makeFlow(which, flow_values, flowScale, arrowScale,
        spatial_values, color_values, range_select);
  }

  /**
   *
   *
   * @param which
   * @param flow_values
   * @param flowScale
   * @param spatial_values
   * @param spatial_set
   * @param spatialManifoldDimension
   * @param color_values
   * @param range_select
   * @param valueArrayLength
   * @param valueToMap
   * @param MapVector
   *
   * @return streamline geometry array
   *
   * @throws VisADException
   */
  
  public VisADGeometryArray[] makeStreamline(int which, float[][] flow_values,
      float flowScale, float[][] spatial_values, Set spatial_set,
      int spatialManifoldDimension, byte[][] color_values,
      boolean[][] range_select, int valueArrayLength, int[] valueToMap,
      Vector MapVector) throws VisADException {
    return adaptedShadowType.makeStreamline(which, flow_values, flowScale,
        spatial_values, spatial_set, spatialManifoldDimension, color_values,
        range_select, valueArrayLength, valueToMap, MapVector);
  }

  /**
   *
   *
   * @param valueArrayLength
   * @param valueToScalar
   * @param display_values
   * @param inherited_values
   * @param MapVector
   * @param valueToMap
   * @param domain_length
   * @param range_select
   * @param spatialManifoldDimension
   * @param spatial_set
   * @param color_values
   * @param indexed
   * @param group
   * @param mode
   * @param swap
   * @param constant_alpha
   * @param constant_color
   * @param shadow_api
   * @param Domain
   * @param DomainReferenceComponents
   * @param domain_set
   * @param domain_units
   * @param dataCoordinateSystem
   *
   * @return true if any contours created
   *
   * @throws VisADException
   */
  
  public boolean makeContour(int valueArrayLength, int[] valueToScalar,
      float[][] display_values, int[] inherited_values, Vector MapVector,
      int[] valueToMap, int domain_length, boolean[][] range_select,
      int spatialManifoldDimension, Set spatial_set, byte[][] color_values,
      boolean indexed, Object group, GraphicsModeControl mode, boolean[] swap,
      float constant_alpha, float[] constant_color, ShadowType shadow_api,
      ShadowRealTupleType Domain, ShadowRealType[] DomainReferenceComponents,
      Set domain_set, Unit[] domain_units, CoordinateSystem dataCoordinateSystem)
      throws VisADException {
    return adaptedShadowType.makeContour(valueArrayLength, valueToScalar,
        display_values, inherited_values, MapVector, valueToMap, domain_length,
        range_select, spatialManifoldDimension, spatial_set, color_values,
        indexed, group, mode, swap, constant_alpha, constant_color, shadow_api,
        Domain, DomainReferenceComponents, domain_set, domain_units,
        dataCoordinateSystem);
  }

  private Object MUTEX = new Object();

  public void addLabelsToGroup(Object group, VisADGeometryArray[] arrays,
      GraphicsModeControl mode, ContourControl control,
      ProjectionControl p_cntrl, int[] cnt_a, float constant_alpha,
      float[] constant_color) throws VisADException {

    synchronized(MUTEX) {
       int cnt = cnt_a[0];

       if (cnt == 0) {
         projListener = new ProjectionControlListener(p_cntrl, control);
       }

       int n_labels = arrays.length;

       // add the stretchy line segments if we are not filling
       if (!control.contourFilled() && arrays != null) {
         projListener.LT_array[cnt] = new LabelTransform[3][n_labels];

         GraphicsModeControl styledMode = (GraphicsModeControl) mode.clone();
         styledMode.setLineStyle(control.getDashedStyle(), false);

         for (int ii = 0; ii < n_labels; ii++) {
           ContourLabelGeometry array = (ContourLabelGeometry) arrays[ii];

           TransformNode segL_trans_group = new TransformNode();
           TransformNode segR_trans_group = new TransformNode();

           if (control.getAutoSizeLabels()) {

             LabelTransform lbl_trans = new LabelTransform(segL_trans_group, p_cntrl,
                 new VisADGeometryArray[] { array.expSegLeft, array.segLeftAnchor },
                 array.segLeftScaleInfo, 1);
             projListener.LT_array[cnt][1][ii] = lbl_trans;

             lbl_trans = new LabelTransform(segR_trans_group, p_cntrl,
                 new VisADGeometryArray[] { array.expSegRight, array.segRightAnchor },
                 array.segRightScaleInfo, 1);
             projListener.LT_array[cnt][2][ii] = lbl_trans;
           }
           ((com.ardor3d.scenegraph.Node) group).attachChild(segL_trans_group);
           ((com.ardor3d.scenegraph.Node) group).attachChild(segR_trans_group);

           if (array.isStyled) {
             addToGroup(segL_trans_group, array.expSegLeft, styledMode,
                        constant_alpha, constant_color);
             addToGroup(segR_trans_group, array.expSegRight, styledMode,
                        constant_alpha, constant_color);
           }
           else {
             addToGroup(segL_trans_group, array.expSegLeft, mode,
                        constant_alpha, constant_color);
             addToGroup(segR_trans_group, array.expSegRight, mode,
                        constant_alpha, constant_color);
           }
         }

       } else {
         projListener.LT_array[cnt] = new LabelTransform[1][n_labels];
       }

       cnt = cnt_a[0];

       for (int ii = 0; ii < n_labels; ii++) {
         TransformNode lbl_trans_group = new TransformNode();

         ContourLabelGeometry array = (ContourLabelGeometry) arrays[ii];

         if (control.getAutoSizeLabels()) {
           LabelTransform lbl_trans = new LabelTransform(lbl_trans_group, p_cntrl,
               new VisADGeometryArray[] { array.label, array.labelAnchor }, null, 0);
           projListener.LT_array[cnt][0][ii] = lbl_trans;
         }

         ((com.ardor3d.scenegraph.Node) group).attachChild(lbl_trans_group);

         addToGroup(lbl_trans_group, array.label, mode, constant_alpha,
             constant_color);
       }
       cnt++;
       projListener.cnt = cnt;
       cnt_a[0] = cnt;
    }
  }


  /**
   *
   *
   * @param text_values
   * @param text_control
   * @param spatial_values
   * @param color_values
   * @param range_select
   *
   * @return text geometry array
   *
   * @throws VisADException
   */
  
  public VisADGeometryArray makeText(String[] text_values,
      TextControl text_control, float[][] spatial_values,
      byte[][] color_values, boolean[][] range_select) throws VisADException {
    return adaptedShadowType.makeText(text_values, text_control,
        spatial_values, color_values, range_select);
  }

  /**
   * composite and transform color and Alpha DisplayRealType values from
   * display_values, and return as (Red, Green, Blue, Alpha)
   *
   * @param display_values
   * @param valueArrayLength
   * @param valueToScalar
   * @param display
   * @param default_values
   * @param range_select
   * @param single_missing
   * @param shadow_api
   *
   * @return byte color array
   *
   * @throws RemoteException
   * @throws VisADException
   */
  
  public byte[][] assembleColor(float[][] display_values, int valueArrayLength,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      boolean[][] range_select, boolean[] single_missing, ShadowType shadow_api)
      throws VisADException, RemoteException {
    return adaptedShadowType.assembleColor(display_values, valueArrayLength,
        valueToScalar, display, default_values, range_select, single_missing,
        shadow_api);
  }

  /**
   * return a composite of SelectRange DisplayRealType values from
   * display_values, as 0.0 for select and Double.Nan for no select (these
   * values can be added to other DisplayRealType values)
   *
   * @param display_values
   * @param domain_length
   * @param valueArrayLength
   * @param valueToScalar
   * @param display
   * @param shadow_api
   *
   * @return array of booleans as described above
   *
   * @throws VisADException
   */
  
  public boolean[][] assembleSelect(float[][] display_values,
      int domain_length, int valueArrayLength, int[] valueToScalar,
      DisplayImpl display, ShadowType shadow_api) throws VisADException {
    return adaptedShadowType.assembleSelect(display_values, domain_length,
        valueArrayLength, valueToScalar, display, shadow_api);
  }

  /**
   *
   *
   * @param group
   * @param display_values
   * @param text_value
   * @param text_control
   * @param valueArrayLength
   * @param valueToScalar
   * @param default_values
   * @param inherited_values
   * @param renderer
   *
   * @return true if post-processing needed
   *
   * @throws RemoteException
   * @throws VisADException
   */
  
  public boolean terminalTupleOrScalar(Object group, float[][] display_values,
      String text_value, TextControl text_control, int valueArrayLength,
      int[] valueToScalar, float[] default_values, int[] inherited_values,
      DataRenderer renderer) throws VisADException, RemoteException {

    boolean post = adaptedShadowType.terminalTupleOrScalar(group,
        display_values, text_value, text_control, valueArrayLength,
        valueToScalar, default_values, inherited_values, renderer, this);
    return post;
  }

  /**
   * Add the GeometryArray to the group
   *
   *
   * @param group
   *          Group
   * @param array
   *          array to add
   * @param mode
   *          GraphicsModeControl
   * @param constant_alpha
   *          constant alpha value
   * @param constant_color
   *          constant color value
   *
   * @return true if successful
   *
   * @throws VisADException
   *           unable to add the array to the group
   */
  public boolean addToGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color)
      throws VisADException {
     
    if (array == null) {
       return false;
    }
    
    if (array != null && array.vertexCount <= 0) {
       return false;
    }
    
    float af = 0.0f;
    if (constant_alpha == 1.0f) {
       af = 1.0f;
    } 
    else if (!Float.isNaN(constant_alpha)) {
       af = constant_alpha;
    }
      
    ColorRGBA defaultColor ;
      
    if (constant_color != null && constant_color.length == 3) {
      if (constant_color.length == 4) {
        af = constant_color[3];
        if (Float.isNaN(af)) {
           af = 0f;
        }
      }
      defaultColor = new ColorRGBA(constant_color[0], constant_color[1], constant_color[2], af);        
    }
    else {
      defaultColor = new ColorRGBA(0, 0, 0, af);
    }
    
    Spatial geometry = display.makeGeometry(array, defaultColor, mode);
       
    MaterialState material = new MaterialState();
    material.setColorMaterial(MaterialState.ColorMaterial.Diffuse);
    //material.setColorMaterial(MaterialState.ColorMaterial.AmbientAndDiffuse);          
    geometry.setRenderState(material);    
      
//      Leave here for reference for now.      
//      Appearance appearance = makeCachedAppearance(mode, c_alpha, c_color, geometry, false, true);
//      addToShape((Group) group, geometry, appearance);

    ((com.ardor3d.scenegraph.Node)group).attachChild(geometry);

    return true;
  }
  
  /** 
   *   Convenience method that returns a Branch node, which may be parented,
   *   and whose only child can be detached. Acts also as an id since its parent
   *   may have multiple children. 
   */
  public BranchGroup addToDetachableGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color) throws VisADException {
     
    BranchGroup branch = (BranchGroup) makeBranch();
    branch.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
    branch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

    /* For example, this branch is detached from 'branch' during auto resizing. 
     * New resized trcrArray is then added back to 'branch' 
     */
    BranchGroup trcrBranch = (BranchGroup) makeBranch();

    addToGroup(trcrBranch, array, mode, constant_alpha, constant_color);
    branch.addChild(trcrBranch);
    
    ((BranchGroup)group).addChild(branch);
    
    return branch;     
  }
  
  /** 
   *   Convenience method that returns a Branch node, which may be parented,
   *   and whose only child can be detached. Acts also as an id since its parent
   *   may have multiple children. 
   */
//  public com.ardor3d.scenegraph.Node addToDetachableGroup(Object group, VisADGeometryArray array,
//      GraphicsModeControl mode, float constant_alpha, float[] constant_color) throws VisADException {
//     
//    com.ardor3d.scenegraph.Node branch = new com.ardor3d.scenegraph.Node();
//
//    /* For example, this branch is detached from 'branch' during auto resizing. 
//     * New resized trcrArray is then added back to 'branch' 
//     */
//    com.ardor3d.scenegraph.Node trcrBranch = new com.ardor3d.scenegraph.Node();
//
//    addToGroup(trcrBranch, array, mode, constant_alpha, constant_color);
//    branch.attachChild(trcrBranch);
//    
//    ((com.ardor3d.scenegraph.Node)group).attachChild(branch);
//    
//    return branch;     
//  }
  
  /**
   *
   *
   * @param group
   * @param array
   * @param mode
   * @param constant_alpha
   * @param constant_color
   *
   * @return true if added
   *
   * @throws VisADException
   */
  
  public boolean addTextToGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color)
      throws VisADException {
     
    if (array != null && array.vertexCount > 0) {

      addToGroup(group, array, mode, constant_alpha, constant_color);
      
      if (array instanceof VisADTriangleArray) {
        /* Keep for reference, what is this?
        GeometryArray geometry2 = display.makeGeometry(array);
        // Don't cache the appearance
        Appearance appearance2 = makeCachedAppearance(mode, c_alpha, c_color,
            geometry2, false, false);
        // LineAttributes la = appearance2.getLineAttributes();
        // better without anti-aliasing
        // la.setLineAntialiasingEnable(true);
        PolygonAttributes pa = appearance2.getPolygonAttributes();
        pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        Shape3D shape2 = new Shape3D(geometry2, appearance2);
        shape2.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        shape2.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        ((Group) group).addChild(shape2);
        */
      }

      return true;
    } 
    else {
      return false;
    }
  }

  /**
   * Do we allo constant color surfaces?
   *
   * @return false
   */
  public boolean allowConstantColorSurfaces() {
    return false;
  }

  /**
   * Get a String representation of this object
   *
   * @return the String representation of this object
   */
  public String toString() {
    return adaptedShadowType.toString();
  }

}

/**
 * Class ProjectionControlListener
 */
class ProjectionControlListener implements ControlListener {

  /**  */
  LabelTransform[][][] LT_array = null;

  /**  */
  ProjectionControl p_cntrl = null;

  /**  */
  ContourControl c_cntrl = null;

  /**  */
  double last_scale;

  /**  */
  double first_scale;

  /**  */
  int cnt = 0;

  /**  */
  double last_time;

  /**
   *
   *
   */
  ProjectionControlListener(ProjectionControl p_cntrl, ContourControl c_cntrl) {
    this.p_cntrl = p_cntrl;
    this.c_cntrl = c_cntrl;
    double[] matrix = p_cntrl.getMatrix();
    double[] rot_a = new double[3];
    double[] trans_a = new double[3];
    double[] scale_a = new double[1];
    MouseBehaviorA3D.unmake_matrix(rot_a, scale_a, trans_a, matrix);
    last_scale = scale_a[0];
    first_scale = last_scale;
    LT_array = new LabelTransform[1000][][];
    last_time = System.currentTimeMillis();
    p_cntrl.addControlListener(this);
    c_cntrl.addProjectionControlListener(this, p_cntrl);
  }

  /**
   *
   *
   * @param e
   *
   * @throws RemoteException
   * @throws VisADException
   */
  public synchronized void controlChanged(ControlEvent e)
      throws VisADException, RemoteException {
    double[] matrix = p_cntrl.getMatrix();
    double[] rot_a = new double[3];
    double[] trans_a = new double[3];
    double[] scale_a = new double[1];

    MouseBehaviorA3D.unmake_matrix(rot_a, scale_a, trans_a, matrix);

    // - identify scale change events.
    if (!visad.util.Util.isApproximatelyEqual(scale_a[0], last_scale)) {
      double current_time = System.currentTimeMillis();
      if (scale_a[0] / last_scale > 1.15 || scale_a[0] / last_scale < 1 / 1.15) {
        // BMF 2006-10-11 added loop for iterating controlChanged(...) calls to
        // avoid null pointer exceptions
        if (current_time - last_time < 3000) {
          if (LT_array != null) {
            for (int ii = 0; ii < cnt; ii++) {
              for (int kk = 0; kk < LT_array[ii][0].length; kk++) {
                for (int jj = 0; jj < LT_array[0].length; jj++)
                  if (LT_array[ii][jj][kk] != null)
                    LT_array[ii][jj][kk].controlChanged(first_scale, scale_a);
              }
            }
          }
        } else {
          if (LT_array != null) {
            for (int ii = 0; ii < cnt; ii++) {
              for (int kk = 0; kk < LT_array[ii][0].length; kk++) {
                for (int jj = 0; jj < LT_array[0].length; jj++) {
                  if (LT_array[ii][jj][kk] != null)
                    LT_array[ii][jj][kk].controlChanged(first_scale, scale_a);
                }
              }
            }
          }
          c_cntrl.reLabel();
        }
        last_scale = scale_a[0];
      }
      last_time = current_time;
    }
  }
}

/**
 * Class LabelTransform
 */
class LabelTransform {

  /**  */
  TransformNode trans;

  /**  */
  Transform t3d;

  /**  */
  ProjectionControl proj;

  /**  */
  VisADGeometryArray label_array;

  /**  */
  VisADGeometryArray anchr_array;

  /**  */
  double[] matrix;

  /**  */
  double last_scale;

  /**  */
  double first_scale;

  /**  */
  float[] vertex;

  /**  */
  float[] anchr_vertex;

  /**  */
  double[] rot_a;

  /**  */
  double[] trans_a;

  /**  */
  double[] scale_a;

  /**  */
  int flag;

  /**  */
  float[] f_array;

  /**
   *
   *
   * @param trans
   * @param proj
   * @param label_array
   * @param f_array
   * @param flag
   */
  LabelTransform(TransformNode trans, ProjectionControl proj,
      VisADGeometryArray[] label_array, float[] f_array, int flag) {
    this.trans = trans;
    this.proj = proj;
    this.label_array = label_array[0];
    this.anchr_array = label_array[1];
    this.flag = flag;
    this.f_array = f_array;

    t3d = new Transform();
    matrix = proj.getMatrix();
    rot_a = new double[3];
    trans_a = new double[3];
    scale_a = new double[1];
    MouseBehaviorA3D.unmake_matrix(rot_a, scale_a, trans_a, matrix);
    last_scale = scale_a[0];
    first_scale = last_scale;

    vertex = this.label_array.coordinates;
    anchr_vertex = this.anchr_array.coordinates;
    int vertexCount = this.label_array.vertexCount;
  }

  /**
   *
   *
   * @param first_scale
   * @param scale_a
   */
  public void controlChanged(double first_scale, double[] scale_a) {
    //t3d = trans.getTransform();

    double factor = 0;
    float f_scale = 0;

    if (flag == 0) { // -- label
      double k = first_scale; // - final scale
      factor = k / scale_a[0];
      f_scale = (float) ((scale_a[0] - k) / scale_a[0]);
    } else { // -- expanding line segments
      double k = (f_array[0] / (f_array[1] - f_array[0]))
          * (f_array[1] / f_array[0] - first_scale / scale_a[0]) * scale_a[0];
      factor = k / scale_a[0];
      f_scale = (float) ((scale_a[0] - k) / scale_a[0]);
    }

    Vector3 trans_vec = new Vector3(f_scale*anchr_vertex[0], f_scale*anchr_vertex[1], f_scale*anchr_vertex[2]);

    //  These can't all be zero: non-affine transform
    if (!(factor == 0.0 && (trans_vec.getX() == 0.0 && trans_vec.getY() == 0.0 && trans_vec.getZ() == 0.0))) {
      //t3d.set((float) factor, trans_vec);
      //trans.setTransform(t3d);
      trans.setScale(factor);
      trans.setTranslation(trans_vec.getX(), trans_vec.getY(), trans_vec.getZ());
    }
  }
}
