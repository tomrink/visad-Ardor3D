//
// ShadowFunctionOrSetTypeA3D.java
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

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.extension.SwitchNode;
import com.ardor3d.util.TextureManager;
import visad.*;
import visad.util.ThreadManager;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import java.rmi.*;

import java.awt.image.*;
import java.nio.ByteBuffer;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Texture2D;


/**
   The ShadowFunctionOrSetTypeJ3D is an abstract parent for
   ShadowFunctionTypeJ3D and ShadowSetTypeJ3D.<P>
*/
public class ShadowFunctionOrSetTypeA3D extends ShadowTypeA3D {

  ShadowRealTupleTypeA3D Domain;
  ShadowTypeA3D Range; // null for ShadowSetTypeJ3D

  private Vector AccumulationVector = new Vector();
  boolean post = false;

  boolean doTrajectory = false;
  boolean isAnimation1d = false;
  int domainLength = 0;
  Set anim1DdomainSet;
  Set domainSet;

  double trajVisibilityTimeWindow;
  FlowControl flowCntrl = null;
  ScalarMap flowMap = null;
  TrajectoryParams trajParams;
  ScalarMap altitudeToDisplayZ;
  CoordinateSystem dspCoordSys;
  
  
  List<Node> branches = null;
  SwitchNode swit = null;
  SwitchNode switB = null;
  //TrajectoryAVHandlerJ3D avHandler = null;
  AVHandler avHandler = null;

  public ShadowFunctionOrSetTypeA3D(MathType t, DataDisplayLink link,
                                    ShadowType parent)
      throws VisADException, RemoteException {
    super(t, link, parent);
    if (this instanceof ShadowFunctionTypeA3D) {
      Domain = (ShadowRealTupleTypeA3D)
               ((FunctionType) Type).getDomain().buildShadowType(link, this);
      Range = (ShadowTypeA3D)
              ((FunctionType) Type).getRange().buildShadowType(link, this);
      adaptedShadowType =
        new ShadowFunctionType(t, link, getAdaptedParent(parent),
                       (ShadowRealTupleType) Domain.getAdaptedShadowType(),
                       Range.getAdaptedShadowType());
    }
    else {
      Domain = (ShadowRealTupleTypeA3D)
               ((SetType) Type).getDomain().buildShadowType(Link, this);
      Range = null;
      adaptedShadowType =
        new ShadowSetType(t, link, getAdaptedParent(parent),
                       (ShadowRealTupleType) Domain.getAdaptedShadowType());
    }
  }

  public ShadowRealTupleTypeA3D getDomain() {
    return Domain;
  }

  public ShadowTypeA3D getRange() {
    return Range;
  }

  /** clear AccumulationVector */
  public void preProcess() throws VisADException {
    AccumulationVector.removeAllElements();
    if (this instanceof ShadowFunctionTypeA3D) {
      Range.preProcess();
    }
  }

  /** transform data into a Java3D scene graph;
      add generated scene graph components as children of group;
      value_array are inherited valueArray values;
      default_values are defaults for each display.DisplayRealTypeVector;
      return true if need post-process */
  public boolean doTransform(Object group, Data data, final float[] value_array,
                             final float[] default_values, final DataRenderer renderer)
         throws VisADException, RemoteException {

      
    boolean isTerminal = adaptedShadowType.getIsTerminal();
    
    ScalarMap timeMap = null; // used in the animation case to get control
    DataDisplayLink[] link_s = renderer.getLinks();
    DataDisplayLink link = link_s[0];
    Vector scalarMaps = link.getSelectedMapVector();

    
    // only determine if it's an animation if non-terminal. isTerminal will
    // only be determined if there are scalar maps - defaults to false
    if (!isTerminal && !scalarMaps.isEmpty()) {
      // determine if it's an animation
      MathType mtype = data.getType();
      if (mtype instanceof FunctionType) {
        int ani_map_idx = 0;
        FunctionType function = (FunctionType) mtype;
        RealTupleType functionD = function.getDomain();
        for (int kk = 0; kk < scalarMaps.size(); kk++) {
          ScalarMap scalarMap = (ScalarMap) scalarMaps.elementAt(kk);
          String scalar_name = scalarMap.getScalarName();
          if (scalar_name.equals(((RealType) functionD.getComponent(0)).getName())) {
            if (((scalarMap.getDisplayScalar()).equals(Display.Animation))
                && (functionD.getDimension() == 1)) {
              isAnimation1d = true;
              ani_map_idx = kk;
            }
          }
        }
        // animation domain
        timeMap = (ScalarMap) scalarMaps.elementAt(ani_map_idx);
      }
      
      
      // check for trajectory
      for (int kk=0; kk<scalarMaps.size(); kk++) {
        ScalarMap scalarMap = (ScalarMap) scalarMaps.elementAt(kk);
        if (scalarMap.getScalarName().equals(RealType.Altitude.getName())) {
           DisplayRealType dspType = scalarMap.getDisplayScalar();
           dspCoordSys = dspType.getTuple().getCoordinateSystem();
           RealType[] rtypes = dspCoordSys.getReference().getRealComponents();
           for (int t=0; t<rtypes.length; t++) {
              if (rtypes[t].equals(Display.ZAxis)) {
                 altitudeToDisplayZ = scalarMap;
              }
           }
        }         
        DisplayRealType dspType = scalarMap.getDisplayScalar();
        boolean isFlow = false;
        if (dspType.equals(Display.Flow1X) || dspType.equals(Display.Flow1Y) || dspType.equals(Display.Flow1Z)) {
          isFlow = true;
        }
        else if (dspType.equals(Display.Flow2X) || dspType.equals(Display.Flow2Y) || dspType.equals(Display.Flow2Z)) {
          isFlow = true;
        }
      
        if (isFlow) {
          flowCntrl = (FlowControl) scalarMap.getControl();
          flowMap = scalarMap;
          if (flowCntrl.trajectoryEnabled()) {
            doTrajectory = true;
            trajParams = flowCntrl.getTrajectoryParams();
            trajVisibilityTimeWindow = trajParams.getTrajVisibilityTimeWindow();
            break;
          }
          else {
            doTrajectory = false;
          }

        }
      }
    }

    // animation logic
    if (isAnimation1d) {
      
      // analyze data's domain (its a Field)
      domainSet = ((Field) data).getDomainSet();
      anim1DdomainSet = domainSet;

      // create and add switch with nodes for animation images
      domainLength = domainSet.getLength(); // num of domain nodes
      swit = (SwitchNode) makeSwitch(domainLength);
      AnimationControlA3D control = (AnimationControlA3D)timeMap.getControl();
      
      if (!doTrajectory) {
        addSwitch(group, swit, control, domainSet, renderer);
      }
      else {
        double[] times = TrajectoryManager.getTimes((Gridded1DSet)anim1DdomainSet);
        java.util.Arrays.sort(times);
        int len = times.length;
        double avgTimeStep = (times[len-1] - times[0])/(len-1);
        int numNodes = (int) (trajVisibilityTimeWindow/avgTimeStep);
        int[] whichVisible = new int[numNodes];
        for (int i=0; i<numNodes; i++) whichVisible[i] = -((numNodes-1) - i);

        //avHandler = new TrajectoryAVHandlerJ3D(swit, domainLength, whichVisible, trajParams.getDirection());
        avHandler = null;
        ((AVControlA3D) control).addPair(swit, domainSet, renderer, avHandler);
        ((AVControlA3D) control).init();
        
        Node branch = new Node();
        branch.attachChild((SwitchNode) swit);
        ((Node) group).attachChild(branch);

        // this node holds the trajectory tracer display geometry
        switB = (SwitchNode) makeSwitch(domainLength);
        addSwitch(group, switB, control, domainSet, renderer);
      }

      branches = new ArrayList<Node>();
      for (int i=0; i<domainLength; i++) {
          Node branch = (Node) makeBranch();
          branches.add(branch);
      }      

      ThreadManager threadManager = new ThreadManager("animation rendering");
      for (int i=0; i<domainLength; i++) {
          final Node branch = (Node) branches.get(i);
          final Data sample  = ((Field) data).getSample(i);
          final Node node = (Node) swit.getChild(i);
          threadManager.addRunnable(new ThreadManager.MyRunnable() {
                  public void run()  throws Exception {
                      recurseRange(branch, sample,
                                   value_array, default_values, renderer);
                      if (!doTrajectory) {
                        node.attachChild(branch);          
                      }
                  }
              });
      }

      if (doTrajectory) {
        post = true;
        threadManager.runSequentially();
      }
      else {
        post = false;
        threadManager.runInParallel();
      }
    }
    else {
      ShadowFunctionOrSetType shadow = (ShadowFunctionOrSetType)adaptedShadowType;
      post = shadow.doTransform(group, data, value_array, default_values, renderer, this); 
    }
    
    return post;
  }

  /**
   * Get the possibly adjusted texture width.
   * @param data_width The initial texture width.
   * @return If <code>DisplayImplJ3D.TEXTURE_NPOT</code> then return
   *  <code>data_width</code>, otherwise return the minimum power of two greater
   *  than <code>data_width</code>.
   * @see DisplayImplJ3D#TEXTURE_NPOT
   */
  public int textureWidth(int data_width) {
    if (DisplayImplA3D.TEXTURE_NPOT) return data_width;
    // must be a power of 2 in Java3D
    int texture_width = 1;
    while (texture_width < data_width) texture_width *= 2;
    return texture_width;
  }

  /**
   * Get the possibly adjusted texture height.
   * @param data_height The initial texture height.
   * @return If <code>DisplayImplJ3D.TEXTURE_NPOT</code> then return
   *  <code>data_height</code>, otherwise return the minimum power of two greater
   *  than <code>data_height</code>.
   * @see DisplayImplJ3D#TEXTURE_NPOT
   */
  public int textureHeight(int data_height) {
    if (DisplayImplA3D.TEXTURE_NPOT) return data_height;
    // must be a power of 2 in Java3D
    int texture_height = 1;
    while (texture_height < data_height) texture_height *= 2;
    return texture_height;
  }

  /**
   * Get the possibly adjusted texture depth.
   * @param data_depth The initial texture depth.
   * @return If <code>DisplayImplJ3D.TEXTURE_NPOT</code> then return
   *  <code>data_depth</code>, otherwise return the minimum power of two greater
   *  than <code>data_depth</code>.
   * @see DisplayImplJ3D#TEXTURE_NPOT
   */
  public int textureDepth(int data_depth) {
    if (DisplayImplA3D.TEXTURE_NPOT) return data_depth;
    // must be a power of 2 in Java3D
    int texture_depth = 1;
    while (texture_depth < data_depth) texture_depth *= 2;
    return texture_depth;
  }

  public void adjustZ(float[] coordinates) {
    if (display.getDisplayRenderer().getMode2D()) {
      for (int i=2; i<coordinates.length; i+=3) {
        coordinates[i] = DisplayImplA3D.BACK2D;
      }
    }
  }

  public int getImageComponentType(int buffImgType) {
    if (buffImgType == BufferedImage.TYPE_4BYTE_ABGR) {
      return ImageComponent2D.FORMAT_RGBA8;
    }
    else if (buffImgType == BufferedImage.TYPE_3BYTE_BGR) {
      return ImageComponent2D.FORMAT_RGB8;
    }
    else if (buffImgType == BufferedImage.TYPE_BYTE_GRAY) {
      return ImageComponent2D.FORMAT_CHANNEL8;
    }
    return ImageComponent2D.FORMAT_RGBA8;
  }

  public int getTextureType(int buffImgType) {
    if (buffImgType == BufferedImage.TYPE_4BYTE_ABGR) {
      return Texture2D.RGBA;
    }
    else if (buffImgType == BufferedImage.TYPE_3BYTE_BGR) {
      return Texture2D.RGB;
    }
    else if (buffImgType == BufferedImage.TYPE_BYTE_GRAY) {
      return Texture2D.LUMINANCE;
    }
    return Texture2D.RGBA;
  }

  public void setTexCoords(float[] texCoords, float ratiow, float ratioh) {
    setTexCoords(texCoords, ratiow, ratioh, false);
  }

  public void setTexCoords(float[] texCoords, float ratiow, float ratioh, boolean yUp) {
    if (!yUp) { // the default
      // corner 0
      texCoords[0] = 0.0f;
      texCoords[1] = 1.0f;
      // corner 1
      texCoords[2] = ratiow;
      texCoords[3] = 1.0f;
      // corner 2
      texCoords[4] = ratiow;
      texCoords[5] = 1.0f - ratioh;
      // corner 3
      texCoords[6] = 0.0f;
      texCoords[7] = 1.0f - ratioh;
    }
    else {  // yUp = true, for imageByReference=true
      // corner 0
      texCoords[0] = 0.0f;
      texCoords[1] = 0.0f;
      // corner 1
      texCoords[2] = 0.0f;
      texCoords[3] = ratioh;
      // corner 2
      texCoords[4] = ratiow;
      texCoords[5] = ratioh;
      // corner 3
      texCoords[6] = ratiow;
      texCoords[7] = 0.0f;
    }
  }

  public float[] setTex3DCoords(int length, int axis, float ratiow,
                                float ratioh, float ratiod) {
    // need to flip Y and Z in X and Y views?
    float[] texCoords = new float[12 * length];
    if (axis == 2) {
      for (int i=0; i<length; i++) {
        int i12 = i * 12;
        float depth = 0.0f + (ratiod - 0.0f) * i / (length - 1.0f);
        // corner 0
        texCoords[i12] = 0.0f;
        texCoords[i12 + 1] = 1.0f;
        texCoords[i12 + 2] = depth;
        // corner 1
        texCoords[i12 + 3] = ratiow;
        texCoords[i12 + 4] = 1.0f;
        texCoords[i12 + 5] = depth;
        // corner 2
        texCoords[i12 + 6] = ratiow;
        texCoords[i12 + 7] = 1.0f - ratioh;
        texCoords[i12 + 8] = depth;
        // corner 3
        texCoords[i12 + 9] = 0.0f;
        texCoords[i12 + 10] = 1.0f - ratioh;
        texCoords[i12 + 11] = depth;
      }
    }
    else if (axis == 1) {
      for (int i=0; i<length; i++) {
        int i12 = i * 12;
        float height = 1.0f - ratioh * i / (length - 1.0f);
        // corner 0
        texCoords[i12] = 0.0f;
        texCoords[i12 + 1] = height;
        texCoords[i12 + 2] = 0.0f;
        // corner 1
        texCoords[i12 + 3] = ratiow;
        texCoords[i12 + 4] = height;
        texCoords[i12 + 5] = 0.0f;
        // corner 2
        texCoords[i12 + 6] = ratiow;
        texCoords[i12 + 7] = height;
        texCoords[i12 + 8] = ratiod;
        // corner 3
        texCoords[i12 + 9] = 0.0f;
        texCoords[i12 + 10] = height;
        texCoords[i12 + 11] = ratiod;
      }
    }
    else if (axis == 0) {
      for (int i=0; i<length; i++) {
        int i12 = i * 12;
        float width = 0.0f + (ratiow - 0.0f) * i / (length - 1.0f);
        // corner 0
        texCoords[i12] = width;
        texCoords[i12 + 1] = 1.0f;
        texCoords[i12 + 2] = 0.0f;
        // corner 1
        texCoords[i12 + 3] = width;
        texCoords[i12 + 4] = 1.0f - ratioh;
        texCoords[i12 + 5] = 0.0f;
        // corner 2
        texCoords[i12 + 6] = width;
        texCoords[i12 + 7] = 1.0f - ratioh;
        texCoords[i12 + 8] = ratiod;
        // corner 3
        texCoords[i12 + 9] = width;
        texCoords[i12 + 10] = 1.0f;
        texCoords[i12 + 11] = ratiod;
      }
    }
    return texCoords;
  }

  // WLH 17 March 2000
  private static float EPS = 0.00f;

  public float[] setTexStackCoords(int length, int axis, float ratiow,
                                   float ratioh, float ratiod) {
    float[] texCoords = new float[8 * length];
    if (axis == 2) {
      for (int i=0; i<length; i++) {
        int i8 = i * 8;
        // corner 0
        texCoords[i8] = 0.0f + EPS;
        texCoords[i8 + 1] = 1.0f - EPS;
        // corner 1
        texCoords[i8 + 2] = ratiow - EPS;
        texCoords[i8 + 3] = 1.0f - EPS;
        // corner 2
        texCoords[i8 + 4] = ratiow - EPS;
        texCoords[i8 + 5] = 1.0f - ratioh + EPS;
        // corner 3
        texCoords[i8 + 6] = 0.0f + EPS;
        texCoords[i8 + 7] = 1.0f - ratioh + EPS;
      }
    }
    else if (axis == 1) {
      // WLH 23 Feb 2000 - flip Z
      for (int i=0; i<length; i++) {
        int i8 = i * 8;
        // corner 0
        texCoords[i8] = 0.0f + EPS;
        texCoords[i8 + 1] = 1.0f - EPS;
        // corner 1
        texCoords[i8 + 2] = ratiow - EPS;
        texCoords[i8 + 3] = 1.0f - EPS;
        // corner 2
        texCoords[i8 + 4] = ratiow - EPS;
        texCoords[i8 + 5] = 1.0f - ratiod + EPS;
        // corner 3
        texCoords[i8 + 6] = 0.0f + EPS;
        texCoords[i8 + 7] = 1.0f - ratiod + EPS;
      }
    }
    else if (axis == 0) {
      // WLH 23 Feb 2000 - flip Y and Z
      for (int i=0; i<length; i++) {
        int i8 = i * 8;
        // corner 0
        texCoords[i8] = 0.0f + EPS;
        texCoords[i8 + 1] = 1.0f - EPS;
        // corner 1
        texCoords[i8 + 2] = ratioh - EPS;
        texCoords[i8 + 3] = 1.0f - EPS;
        // corner 2
        texCoords[i8 + 4] = ratioh - EPS;
        texCoords[i8 + 5] = 1.0f - ratiod + EPS;
        // corner 3
        texCoords[i8 + 6] = 0.0f + EPS;
        texCoords[i8 + 7] = 1.0f - ratiod + EPS;
      }
    }

    return texCoords;
  }

  public Vector getTextMaps(int i, int[] textIndices) {
    if (i < 0) {
      return ((ShadowTextTypeA3D) Range).getSelectedMapVector();
    }
    else {
      ShadowTextTypeA3D text = (ShadowTextTypeA3D)
        ((ShadowTupleTypeA3D) Range).getComponent(textIndices[i]);
      return text.getSelectedMapVector();
    }
  }
  
  public Object createImage(int data_width, int data_height, int textureWidth, int textureHeight, byte[][] color_values) {
     
    int clrDim = color_values.length;
    byte[] byteData = new byte[clrDim*textureWidth*textureHeight];
    ImageDataFormat imgFrmt = null;
    
    if (clrDim == 4) {
      imgFrmt = ImageDataFormat.BGRA;

      int k = 0;
      int m = 0;
      byte r, g, b, a;
      for (int j=0; j<data_height; j++) {
        for (int i=0; i<data_width; i++) {
          r = color_values[0][k];
          g = color_values[1][k];
          b = color_values[2][k];
          a = color_values[3][k];
          
          byteData[m++] = b;
          byteData[m++] = g;
          byteData[m++] = r;
          byteData[m++] = a;
          k++;
        }
        for (int i=data_width; i<textureWidth; i++) {
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
        }
      }
      for (int j=data_height; j<textureHeight; j++) {
        for (int i=0; i<textureWidth; i++) {
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
        }
      }
      
    }
    else { // must be 3
      imgFrmt = ImageDataFormat.BGR;
      
      int k = 0;
      int m = 0;
      byte r, g, b;
      for (int j=0; j<data_height; j++) {
        for (int i=0; i<data_width; i++) {
          r = color_values[0][k];
          g = color_values[1][k];
          b = color_values[2][k];

          byteData[m++] = b;
          byteData[m++] = g;
          byteData[m++] = r;
          k++;
        }
        for (int i=data_width; i<textureWidth; i++) {
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
        }
      }
      for (int j=data_height; j<textureHeight; j++) {
        for (int i=0; i<textureWidth; i++) {
          byteData[m++] = 0;
          byteData[m++] = 0;
          byteData[m++] = 0;
        }
      }
      
    }
    
    ByteBuffer bbuf = ByteBuffer.wrap(byteData);
    Image image = new Image(imgFrmt, PixelDataType.UnsignedByte, textureWidth, textureHeight, bbuf, null);
      
    return image;
  }  

  public void textureToGroup(Object group, VisADGeometryArray array,
                            Object image, GraphicsModeControl mode,
                            float constant_alpha, float[] constant_color,
                            int texture_width, int texture_height, boolean byReference, boolean yUp, VisADImageTileA3D tile) throws VisADException {
    textureToGroup(group, array, image, mode, constant_alpha, constant_color, texture_width, texture_height, byReference, yUp, tile, false);
  }

  public void textureToGroup(Object group, VisADGeometryArray array,
                            Object image, GraphicsModeControl mode,
                            float constant_alpha, float[] constant_color,
                            int texture_width, int texture_height) throws VisADException {
    textureToGroup(group, array, image, mode, constant_alpha, constant_color, texture_width, texture_height, false, false, null, false);
  }

  public void textureToGroup(Object group, VisADGeometryArray array,
                            Object img, GraphicsModeControl mode,
                            float constant_alpha, float[] constant_color,
                            int textureWidth, int textureHeight, 
                            boolean byReference, boolean yUp, VisADImageTileA3D tile, boolean smoothen) throws VisADException {
    // Note: constant_color did not appear to be used in the Java3D graphics dependent API version, but keep for now.
    
    Image aImage = (Image) img;
    
    if (constant_alpha == 1.0f) {
      // constant opaque alpha = NONE
    }
    else if (constant_alpha == constant_alpha) {
    }
    else { // constant_alpha == NaN
    }    
       
    TextureState ts = new TextureState();
    ts.setCorrectionType(TextureState.CorrectionType.Affine);
    ts.setEnabled(true);
    
    com.ardor3d.image.Texture.MinificationFilter minFilter;
    com.ardor3d.image.Texture.ApplyMode applyMode = com.ardor3d.image.Texture.ApplyMode.Replace;
    if (smoothen) {
       minFilter = com.ardor3d.image.Texture.MinificationFilter.BilinearNoMipMaps;
    }
    else {
       minFilter = com.ardor3d.image.Texture.MinificationFilter.NearestNeighborNoMipMaps;
    }
        
    com.ardor3d.image.Texture2D texture = (com.ardor3d.image.Texture2D) TextureManager.loadFromImage(aImage, minFilter);
    texture.setMagnificationFilter(com.ardor3d.image.Texture.MagnificationFilter.NearestNeighbor);
    texture.setApply(applyMode);
    ts.setTexture(texture);
    
    /* this may only be necessary if array is filled */
    OffsetState offset = (OffsetState) RenderState.createState(RenderState.StateType.Offset);
    offset.setFactor(1f);
    offset.setUnits(1f);
    offset.setTypeEnabled(OffsetState.OffsetType.Fill, true);
        
    MaterialState material = new MaterialState();
    material.setColorMaterial(MaterialState.ColorMaterial.Emissive);
    
    Spatial geom = display.makeGeometry(array);
    
    Node branch = new Node();
        
    branch.attachChild(geom);
    branch.setRenderState(material);
    branch.setRenderState(ts);
    branch.setRenderState(offset);
    
    ((Node)group).attachChildAt(branch, 0);
    
    if (tile != null) {
       tile.setTexture(texture);
    }
     
  }
  
  public void texture3DToGroup(Object group, VisADGeometryArray arrayX,
                    VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
                    VisADGeometryArray arrayXrev,
                    VisADGeometryArray arrayYrev,
                    VisADGeometryArray arrayZrev,
                    BufferedImage[] images, GraphicsModeControl mode,
                    float constant_alpha, float[] constant_color,
                    int texture_width, int texture_height,
                    int texture_depth, DataRenderer renderer)
         throws VisADException {
    throw new VisADException("texture3DtoGroup: not yet implemented for Ardor3D");

    /*
    GeometryArray geometryX = display.makeGeometry(arrayX);
    GeometryArray geometryY = display.makeGeometry(arrayY);
    GeometryArray geometryZ = display.makeGeometry(arrayZ);
    GeometryArray geometryXrev = display.makeGeometry(arrayXrev);
    GeometryArray geometryYrev = display.makeGeometry(arrayYrev);
    GeometryArray geometryZrev = display.makeGeometry(arrayZrev);
    // System.out.println("texture geometry");
    // create basic Appearance
    TransparencyAttributes c_alpha = null;

    if (constant_alpha == 1.0f) {
      // constant opaque alpha = NONE
      c_alpha = null;
    }
    else if (constant_alpha == constant_alpha) {
      // c_alpha = new TransparencyAttributes(mode.getTransparencyMode(),
      c_alpha = new TransparencyAttributes(TransparencyAttributes.BLENDED,
                                           constant_alpha);
    }
    else {
      c_alpha = new TransparencyAttributes();
      c_alpha.setTransparencyMode(TransparencyAttributes.BLENDED);
    }
    ColoringAttributes c_color = null;
    if (constant_color != null && constant_color.length == 3) {
      c_color = new ColoringAttributes();
      c_color.setColor(constant_color[0], constant_color[1], constant_color[2]);
    }
    Appearance appearance =
      makeAppearance(mode, c_alpha, null, geometryX, true);
    // create TextureAttributes
    TextureAttributes texture_attributes = new TextureAttributes();
    // texture_attributes.setTextureMode(TextureAttributes.REPLACE);
    texture_attributes.setTextureMode(TextureAttributes.MODULATE);
    texture_attributes.setPerspectiveCorrectionMode(
                          TextureAttributes.NICEST);
    appearance.setTextureAttributes(texture_attributes);
    // create Texture2D
// TextureLoader uses 1st argument = 1
    Texture3D texture = new Texture3D(Texture.BASE_LEVEL, Texture.RGBA,
                          texture_width, texture_height, texture_depth);
    texture.setCapability(Texture.ALLOW_IMAGE_READ);
    ImageComponent3D image3d =
      new ImageComponent3D(ImageComponent.FORMAT_RGBA, texture_width,
                           texture_height, texture_depth);
    image3d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
    for (int i=0; i<texture_depth; i++) {
      image3d.set(i, images[i]);
      images[i] = null; // take out the garbage
    }
    texture.setImage(0, image3d);
    //
    // from TextureLoader
    // TextureLoader uses 3 for both setMinFilter and setMagFilter
    //for interpolation:
    texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
    texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
    //for sampling:
    //texture.setMinFilter(Texture.BASE_LEVEL_POINT);
    //texture.setMagFilter(Texture.BASE_LEVEL_POINT);
    texture.setEnable(true);
    // end of from TextureLoader

    // OK to share appearance ??
    Shape3D shapeX = new Shape3D(geometryX, appearance);
    shapeX.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    Shape3D shapeY = new Shape3D(geometryY, appearance);
    shapeY.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    Shape3D shapeZ = new Shape3D(geometryZ, appearance);
    shapeZ.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    Shape3D shapeXrev = new Shape3D(geometryXrev, appearance);
    Shape3D shapeYrev = new Shape3D(geometryYrev, appearance);
    Shape3D shapeZrev = new Shape3D(geometryZrev, appearance);
    appearance.setTexture(texture);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);

    shapeX.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeX.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    shapeY.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeY.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    shapeZ.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeZ.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    shapeXrev.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeXrev.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    shapeYrev.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeYrev.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    shapeZrev.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shapeZrev.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    
    Switch swit = (Switch) makeSwitch();
    swit.addChild(shapeX);
    swit.addChild(shapeY);
    swit.addChild(shapeZ);
    swit.addChild(shapeXrev);
    swit.addChild(shapeYrev);
    swit.addChild(shapeZrev);

    // WLH 6 April 2000
    // ((Group) group).addChild(swit);
    BranchGroup branch = new BranchGroup();
    branch.setCapability(BranchGroup.ALLOW_DETACH);
    branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    branch.addChild(swit);
    if (((Group) group).numChildren() > 0) {
      ((Group) group).setChild(branch, 0);
    }
    else {
      ((Group) group).addChild(branch);
    }

    ProjectionControlA3D control =
      (ProjectionControlA3D) display.getProjectionControl();
    control.addPair(swit, renderer);
    */
  }

  public void textureStackToGroup(Object group, VisADGeometryArray arrayX,
                    VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
                    VisADGeometryArray arrayXrev,
                    VisADGeometryArray arrayYrev,
                    VisADGeometryArray arrayZrev,
                    BufferedImage[] imagesX,
                    BufferedImage[] imagesY,
                    BufferedImage[] imagesZ,
                    GraphicsModeControl mode,
                    float constant_alpha, float[] constant_color,
                    int texture_width, int texture_height,
                    int texture_depth, DataRenderer renderer)
         throws VisADException {
     throw new VisADException("textureStackToGroup: Not yet implemented for Ardor3D");
     
    /*
    GeometryArray[] geometryX = makeGeometrys(arrayX);
    GeometryArray[] geometryY = makeGeometrys(arrayY);
    GeometryArray[] geometryZ = makeGeometrys(arrayZ);
    //not needed ??
    //GeometryArray[] geometryXrev = makeGeometrys(arrayXrev);
    //GeometryArray[] geometryYrev = makeGeometrys(arrayYrev);
    //GeometryArray[] geometryZrev = makeGeometrys(arrayZrev);

    int nx = arrayX.coordinates.length;
    boolean flipX = (arrayX.coordinates[0] > arrayX.coordinates[nx-3]);
    int ny = arrayY.coordinates.length;
    boolean flipY = (arrayY.coordinates[1] > arrayY.coordinates[ny-2]);
    int nz = arrayZ.coordinates.length;
    boolean flipZ = (arrayZ.coordinates[2] > arrayZ.coordinates[nz-1]);
    // System.out.println("flipX = " + flipX + " flipY = " + flipY +
    //                    " flipZ = " + flipZ);

    // create Attributes for Appearances
    TransparencyAttributes c_alpha = null;
    if (constant_alpha == 1.0f) {
      // constant opaque alpha = NONE
      c_alpha = null;
    }
    else if (constant_alpha == constant_alpha) {
      // c_alpha = new TransparencyAttributes(mode.getTransparencyMode(),
      c_alpha = new TransparencyAttributes(TransparencyAttributes.BLENDED,
                                           constant_alpha);
    }
    else {
      c_alpha = new TransparencyAttributes();
      c_alpha.setTransparencyMode(TransparencyAttributes.BLENDED);
    }
    ColoringAttributes c_color = null;
    if (constant_color != null && constant_color.length == 3) {
      c_color = new ColoringAttributes();
      c_color.setColor(constant_color[0], constant_color[1], constant_color[2]);
    }
    TextureAttributes texture_attributes = new TextureAttributes();

    // WLH 17 March 2000
    // texture_attributes.setTextureMode(TextureAttributes.MODULATE);
    texture_attributes.setTextureMode(TextureAttributes.REPLACE);

    texture_attributes.setPerspectiveCorrectionMode(
                          TextureAttributes.NICEST);

    int transparencyMode = mode.getTransparencyMode();

    OrderedGroup branchX = new OrderedGroup();
    branchX.setCapability(Group.ALLOW_CHILDREN_READ);
    int data_depth = geometryX.length;
    Shape3D[] shapeX = new Shape3D[data_depth];
    for (int ii=0; ii<data_depth; ii++) {
      int i = flipX ? data_depth-1-ii : ii;
      int width = imagesX[i].getWidth();
      int height = imagesX[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesX[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryX[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeX[i] = new Shape3D(geometryX[i], appearance);
      shapeX[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeX[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchX.addChild(shapeX[i]);
    }
    OrderedGroup branchXrev = new OrderedGroup();
    branchXrev.setCapability(Group.ALLOW_CHILDREN_READ);
    for (int ii=data_depth-1; ii>=0; ii--) {
      int i = flipX ? data_depth-1-ii : ii;
      int width = imagesX[i].getWidth();
      int height = imagesX[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesX[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryX[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeX[i] = new Shape3D(geometryX[i], appearance);
      shapeX[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeX[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchXrev.addChild(shapeX[i]);
    }
    shapeX = null;

    OrderedGroup branchY = new OrderedGroup();
    branchY.setCapability(Group.ALLOW_CHILDREN_READ);
    int data_height = geometryY.length;
    Shape3D[] shapeY = new Shape3D[data_height];
    for (int ii=0; ii<data_height; ii++) {
      int i = flipY ? data_height-1-ii : ii;
      int width = imagesY[i].getWidth();
      int height = imagesY[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      // flip texture on Y axis
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesY[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryY[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeY[i] = new Shape3D(geometryY[i], appearance);
      shapeY[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeY[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchY.addChild(shapeY[i]);
    }
    OrderedGroup branchYrev = new OrderedGroup();
    branchYrev.setCapability(Group.ALLOW_CHILDREN_READ);
    for (int ii=data_height-1; ii>=0; ii--) {
      int i = flipY ? data_height-1-ii : ii;
      int width = imagesY[i].getWidth();
      int height = imagesY[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      // flip texture on Y axis
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesY[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryY[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeY[i] = new Shape3D(geometryY[i], appearance);
      shapeY[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeY[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchYrev.addChild(shapeY[i]);
    }
    shapeY = null;

    OrderedGroup branchZ = new OrderedGroup();
    branchZ.setCapability(Group.ALLOW_CHILDREN_READ);
    int data_width = geometryZ.length;
    Shape3D[] shapeZ = new Shape3D[data_width];
    for (int ii=0; ii<data_width; ii++) {
      int i = flipZ ? data_width-1-ii : ii;
      int width = imagesZ[i].getWidth();
      int height = imagesZ[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesZ[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryZ[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeZ[i] = new Shape3D(geometryZ[i], appearance);
      shapeZ[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeZ[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchZ.addChild(shapeZ[i]);
    }
    OrderedGroup branchZrev = new OrderedGroup();
    branchZrev.setCapability(Group.ALLOW_CHILDREN_READ);
    for (int ii=data_width-1; ii>=0; ii--) {
      int i = flipZ ? data_width-1-ii : ii;
      int width = imagesZ[i].getWidth();
      int height = imagesZ[i].getHeight();
      Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        width, height);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent.FORMAT_RGBA, imagesZ[i]);
      image2d.setCapability(ImageComponent.ALLOW_IMAGE_READ);
      texture.setImage(0, image2d);
      Appearance appearance =
        makeAppearance(mode, c_alpha, null, geometryZ[i], true);
      appearance.setTextureAttributes(texture_attributes);
      // WLH 17 March 2000
      if (transparencyMode == TransparencyAttributes.FASTEST) {
        texture.setMinFilter(Texture.BASE_LEVEL_POINT);
        texture.setMagFilter(Texture.BASE_LEVEL_POINT);
      }
      else {
        texture.setBoundaryModeS(Texture.CLAMP);
        texture.setBoundaryModeT(Texture.CLAMP);
        texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
        texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
      }
      texture.setEnable(true);
      appearance.setTexture(texture);
      appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      shapeZ[i] = new Shape3D(geometryZ[i], appearance);
      shapeZ[i].setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      shapeZ[i].setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      branchZrev.addChild(shapeZ[i]);
    }
    shapeZ = null;

    Switch swit = (Switch) makeSwitch();
    swit.addChild(branchX);
    swit.addChild(branchY);
    swit.addChild(branchZ);
    swit.addChild(branchXrev);
    swit.addChild(branchYrev);
    swit.addChild(branchZrev);

    // WLH 6 April 2000
    // ((Group) group).addChild(swit);
    BranchGroup branch = new BranchGroup();
    branch.setCapability(BranchGroup.ALLOW_DETACH);
    branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    branch.addChild(swit);
    if (((Group) group).numChildren() > 0) {
      ((Group) group).setChild(branch, 0);
    }
    else {
      ((Group) group).addChild(branch);
    }

    ProjectionControlA3D control =
      (ProjectionControlA3D) display.getProjectionControl();
    control.addPair(swit, renderer);
    */
  }


  public Spatial[] makeGeometrys(VisADGeometryArray array)
                  throws VisADException {
    int count = array.vertexCount;
    int depth = count / 4;
    VisADGeometryArray[] qarrays = makeVisADGeometrys(array);
    Spatial[] geometrys = new Spatial[depth];
    for (int d=0; d<depth; d++) {
      geometrys[d] = display.makeGeometry(qarrays[d]);
    }
    return geometrys;
  }

  public VisADGeometryArray[] makeVisADGeometrys(VisADGeometryArray array)
                  throws VisADException {
    int count = array.vertexCount;
    int depth = count / 4;
    int color_length = array.colors.length / count;
    int tex_length = array.texCoords.length / count;

    VisADGeometryArray[] geometrys = new VisADGeometryArray[depth];
    for (int d=0; d<depth; d++) {
      int i12 = d * 4 * 3;
      int i4c = d * 4 * color_length;
      int i4t = d * 4 * tex_length;
      VisADQuadArray qarray = new VisADQuadArray();
      qarray.vertexCount = 4;
      qarray.coordinates = new float[12];
      qarray.texCoords = new float[tex_length * 4];
      qarray.colors = new byte[color_length * 4];
      qarray.normals = new float[12];
      for (int i=0; i<12; i++) {
        qarray.coordinates[i] = array.coordinates[i12 + i];
        qarray.normals[i] = array.normals[i12 + i];
      }
      for (int i=0; i<4*color_length; i++) {
        qarray.colors[i] = array.colors[i4c + i];
      }
      for (int i=0; i<4*tex_length; i++) {
        qarray.texCoords[i] = array.texCoords[i4t + i];
      }
      geometrys[d] = qarray;
    }
    return geometrys;
  }

  public Object makeSwitch() {
    SwitchNode swit = new SwitchNode();
    return swit;
  }

  public Object makeSwitch(int length) throws VisADException {
    SwitchNode swit = (SwitchNode) makeSwitch();

    for (int i=0; i<length; i++) {
      Node node = new Node();
      addToSwitch(swit, node);
    }
    return swit;
  }
  
  public Object makeBranch() {
    Node branch = new Node();
    return branch;
  }

  public void addToGroup(Object group, Object branch)
         throws VisADException {
    ((Node) group).attachChild((Node) branch);
  }

  public void addToSwitch(Object swit, Object branch)
         throws VisADException {
    ((SwitchNode) swit).attachChild((Node) branch);
  }

  public void addSwitch(Object group, Object swit, Control control,
                        Set domain_set, DataRenderer renderer)
         throws VisADException {
    ((AVControlA3D) control).addPair((SwitchNode) swit, domain_set, renderer);
    ((AVControlA3D) control).init();
    // WLH 06 Feb 06 - fix problem adding a new switch to an existing group
    // TDR Oct 17 - something might depend extra Node so keep this for now.
    // ((Group) group).addChild((Switch) swit);
    Node branch = new Node();
    branch.attachChild((SwitchNode) swit);
    ((Node) group).attachChild(branch);
  }

  public boolean recurseRange(Object group, Data data, float[] value_array,
                             float[] default_values, DataRenderer renderer)
         throws VisADException, RemoteException {
    return Range.doTransform(group, data, value_array,
                             default_values, renderer);
  }

  public boolean wantIndexed() {
    return false;
  }

  public void postProcessTraj() throws VisADException {
    try {
       doTrajectory();
    } catch (Exception e) {
       e.printStackTrace();
    }
  }
  
  /** render accumulated Vector of value_array-s to
      and add to group; then clear AccumulationVector */
  public void postProcess(Object group) throws VisADException {
    if (doTrajectory) {
      postProcessTraj();
      return;
    }
    
    if (((ShadowFunctionOrSetType) adaptedShadowType).getFlat()) {
      int LevelOfDifficulty = getLevelOfDifficulty();
      if (LevelOfDifficulty == LEGAL) {
        throw new UnimplementedException("terminal LEGAL unimplemented: " +
                                         "ShadowFunctionOrSetTypeJ3D.postProcess");
      }
      else {
        // includes !isTerminal
        // nothing to do
      }
    }
    else {
      if (this instanceof ShadowFunctionTypeA3D) {
        Range.postProcess(group);
      }
    }
    AccumulationVector.removeAllElements();
  }

  private void doTrajectory() throws VisADException, RemoteException {
    ArrayList<FlowInfo> flowInfoList = Range.getAdaptedShadowType().getFlowInfo();
    int dataDomainLength = anim1DdomainSet.getLength();
    boolean trcrEnabled = trajParams.getMarkerEnabled();
    int trajForm = trajParams.getTrajectoryForm();
    boolean autoSizeTrcr = true;
    float trcrSize = trajParams.getMarkerSize();
    double trajRefreshInterval = trajParams.getTrajRefreshInterval();
    int direction = trajParams.getDirection();
    
    DataRenderer renderer = getLink().getRenderer();
    ProjectionControl pCntrl = renderer.getDisplay().getProjectionControl();
    MouseBehavior mouseBehav = renderer.getDisplay().getMouseBehavior();
    FixGeomSizeAppearance listener = null;
    
    double[] times = TrajectoryManager.getTimes((Gridded1DSet)anim1DdomainSet);
    double[] timeSteps = TrajectoryManager.getTimeSteps((Gridded1DSet)anim1DdomainSet);
    
    TrajectoryManager trajMan = new TrajectoryManager(renderer, trajParams, flowInfoList, dataDomainLength, times[0], altitudeToDisplayZ, dspCoordSys);
    
    trcrEnabled = (trcrEnabled && (trajForm == TrajectoryManager.LINE)) && trajForm != TrajectoryManager.POINT;
    
    if (autoSizeTrcr && trcrEnabled) {
      //listener = new FixGeomSizeAppearanceJ3D(pCntrl, this, mouseBehav);
      listener = null;
      trajMan.setListener(pCntrl, listener, flowCntrl);
      listener.lock();
    }
    double[] dspScale = TrajectoryManager.getScale(mouseBehav, pCntrl); // current dispaly scale
    double scale = dspScale[0];
    
    trajMan.initCleanUp(flowMap, flowCntrl, pCntrl, display);
    
    double trcrSizeRatio = 1;

    double timeAccum = 0;

    VisADGeometryArray[] arrays;
    VisADGeometryArray trcrArray = null;
    VisADGeometryArray[] auxArray = new VisADGeometryArray[2];
    ArrayList<float[]> achrArrays = null;
    
    for (int k=0; k<dataDomainLength; k++) {
      int i = (direction < 0) ? ((dataDomainLength-1) - k) : k;
      
      FlowInfo info = flowInfoList.get(i);
      
      arrays = trajMan.computeTrajectories(k, timeAccum, times, timeSteps);
      if (trajMan.getNumberOfTrajectories() > 0) {
        achrArrays = new ArrayList<float[]>();
        trcrArray = trajMan.makeTracerGeometry(achrArrays, direction, trcrSize, dspScale, true);
        trcrArray = TrajectoryManager.scaleGeometry(trcrArray, achrArrays, (float)(1.0/scale));      
      }
      
      GraphicsModeControl mode = (GraphicsModeControl) info.mode.clone();

      if ((k==0) || (timeAccum >= trajRefreshInterval)) { // for non steady state trajectories (refresh frequency)
        //avHandler.setNoneVisibleIndex(i);
        timeAccum = 0.0;
      }
      timeAccum += timeSteps[i];

      if (trcrEnabled) {
        Object group = switB.getChild(i);
        BranchGroup trcrBG = addToDetachableGroup(group, trcrArray, mode, info.constant_alpha, info.constant_color);
        if (listener != null && trcrArray != null) {
          listener.add(trcrBG, trcrArray, achrArrays, mode, info.constant_alpha, info.constant_color);
        }
      }

      Node branch = (Node) branches.get(i);
      addToGroup(branch, arrays[0], mode, info.constant_alpha, info.constant_color);
      if (trajForm == TrajectoryManager.CYLINDER) {
        // cylinder elbows
        addToGroup(branch, arrays[2], mode, info.constant_alpha, info.constant_color);                  
      }
      Node node = (Node) swit.getChild(i);
      node.attachChild(branch);
      
      if (trajForm == TrajectoryManager.CYLINDER) {
        Node auxBrnch = (Node) makeBranch();
        // cylinder cone
        addToGroup(auxBrnch, arrays[1], mode, info.constant_alpha, info.constant_color);  
        ((Node)switB.getChild(i)).attachChild(auxBrnch);
      }      
      
    } //---  domain length (time steps) outer time loop  -------------------------
        
    if (listener != null) {
       if (listener.isLocked()) {
          listener.update();
          listener.unlock();
       }
    }
  }
}
