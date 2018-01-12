//
// MouseBehaviorJ3D.java
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

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import visad.*;

import java.lang.reflect.*;
import java.awt.event.*;

import javax.media.j3d.*;
//import javax.vecmath.*;

import java.awt.*;
import java.util.*;

/**
   MouseBehaviorJ3D is the VisAD class for mouse behaviors for Java3D
*/

/*
   On MOUSE_PRESSED event with left button,
   ((InputEvent) events[i]).getModifiers() returns 0 and
   should return 16 ( = InputEvent.BUTTON1_MASK ).
   ((InputEvent) events[i]).getModifiers() correctly
   returns 16 on MOUSE_RELEASED with left button.
*/
public class MouseBehaviorA3D implements MouseBehavior {

  /** DisplayRenderer for Display */
  DisplayRendererA3D display_renderer;
  DisplayImpl display;

  MouseHelper helper = null;
  private CanvasRenderer canvasRenderer = null;

  /**
   * Default Constructor
   */
  public MouseBehaviorA3D() {
  }

  /**
   * Construct a MouseBehavior for the DisplayRenderer specified
   * @param  r  DisplayRenderer to use
   */
  public MouseBehaviorA3D(DisplayRendererA3D r) {
    this(r, MouseHelper.class);
  }

  /**
   * Construct a MouseBehavior for the DisplayRenderer specified
   * @param  r  DisplayRenderer to use
   * @param  mhClass  MouseHelper subclass to use
   */
  public MouseBehaviorA3D(DisplayRendererA3D r, Class mhClass) {
    try {
      Class[] param = new Class[] {DisplayRenderer.class, MouseBehavior.class};
      Constructor mhConstructor =
        mhClass.getConstructor(param);
      helper = (MouseHelper) mhConstructor.newInstance(new Object[] {r, this});
    }
    catch (Exception e) {
      throw new VisADError("cannot construct " + mhClass);
    }
    // helper = new MouseHelper(r, this);

    display_renderer = r;
    display = display_renderer.getDisplay();
  }

  // WLH 17 Dec 2001
  public void destroy() {
    helper = null;
    display = null;
    display_renderer = null;
  }

  /**
   * Get the helper class used by this MouseBehavior.  
   * The <CODE>MouseHelper</CODE> defines the actions taken based
   * on <CODE>MouseEvent</CODE>s.
   * @return  <CODE>MouseHelper</CODE> being used.
   */
  public MouseHelper getMouseHelper() {
    return helper;
  }
  
  public void setCanvasRenderer(CanvasRenderer cr) {
     if (canvasRenderer == null) {
        canvasRenderer = cr;
     }
  }

  /**
   * Return the VisAD ray corresponding to the component coordinates.
   * @param  screen_x  x coordinate of the component
   * @param  screen_y  y coordinate of the component
   * @return  corresponding VisADRay
   * @see visad.VisADRay
   * @see visad.LocalDisplay#getComponent()
   */
  public VisADRay findRay(int screen_x, int screen_y) {
     
    Camera camera = canvasRenderer.getCamera();
    final Vector2 pos = Vector2.fetchTempInstance().set( screen_x, screen_y);
    final Ray3 pickRay = new Ray3();
    camera.getPickRay(pos, false, pickRay);
    Vector2.releaseTempInstance(pos);
    ReadOnlyVector3 origin = pickRay.getOrigin();
    ReadOnlyVector3 direction = pickRay.getDirection();
    VisADRay ray = new VisADRay();
    ray.position[0] = origin.getXf();
    ray.position[1] = origin.getYf();
    ray.position[2] = origin.getZf();
    ray.vector[0] = direction.getXf();
    ray.vector[1] = direction.getYf();
    ray.vector[2] = direction.getZf();
    return ray;    
    
//    Canvas3D canvas = null;
//    //Canvas3D canvas = display_renderer.getCanvas();
//    Point3d position = new Point3d();
//    canvas.getPixelLocationInImagePlate(screen_x, screen_y, position);
//    Point3d eye_position = new Point3d();
//    canvas.getCenterEyeInImagePlate(eye_position);
//    Transform3D t = new Transform3D();
//    canvas.getImagePlateToVworld(t);
//    t.transform(position);
//    t.transform(eye_position);
//
//    if (display.getGraphicsModeControl().getProjectionPolicy() ==
//        View.PARALLEL_PROJECTION) {
//      eye_position = new Point3d(position.x, position.y,
//                                 position.z + 1.0f);
//    }
//
//    TransformGroup trans = display_renderer.getTrans();
//    Transform3D tt = new Transform3D();
//    trans.getTransform(tt);
//    tt.invert();
//    tt.transform(position);
//    tt.transform(eye_position);
//
//    // new eye_position = 2 * position - old eye_position
//    Vector3d vector = new Vector3d(position.x - eye_position.x,
//                                   position.y - eye_position.y,
//                                   position.z - eye_position.z);
//    vector.normalize();
//    VisADRay ray = new VisADRay();
//    ray.position[0] = position.x;
//    ray.position[1] = position.y;
//    ray.position[2] = position.z;
//    ray.vector[0] = vector.x;
//    ray.vector[1] = vector.y;
//    ray.vector[2] = vector.z;
//    // PickRay ray = new PickRay(position, vector);
//    return ray;
  }

  /**
   * Return the VisAD ray corresponding to the VisAD cursor coordinates.
   * @param  cursor  array (x,y,z) of cursor location
   * @return  corresponding VisADRay
   * @see visad.VisADRay
   * @see visad.DisplayRenderer#getCursor()
   */
  public VisADRay cursorRay(double[] cursor) {
    VisADRay ray = new VisADRay();
    return ray;
//    Canvas3D canvas = null;
//    //Canvas3D canvas = display_renderer.getCanvas();
//    // note position already in Vworld coordinates
//    Point3d position = new Point3d(cursor[0], cursor[1], cursor[2]);
//    Point3d eye_position = new Point3d();
//    canvas.getCenterEyeInImagePlate(eye_position);
//    Transform3D t = new Transform3D();
//    canvas.getImagePlateToVworld(t);
//    t.transform(eye_position);
//
//    TransformGroup trans = display_renderer.getTrans();
//    Transform3D tt = new Transform3D();
//    trans.getTransform(tt);
//    tt.transform(position);
//
//    if (display.getGraphicsModeControl().getProjectionPolicy() ==
//        View.PARALLEL_PROJECTION) {
//      eye_position = new Point3d(position.x, position.y,
//                                 position.z + 1.0f);
//    }
//
//    tt.invert();
//    tt.transform(position);
//    tt.transform(eye_position);
//
//    // new eye_position = 2 * position - old eye_position
//    Vector3d vector = new Vector3d(position.x - eye_position.x,
//                                   position.y - eye_position.y,
//                                   position.z - eye_position.z);
//    vector.normalize();
//    VisADRay ray = new VisADRay();
//    ray.position[0] = eye_position.x;
//    ray.position[1] = eye_position.y;
//    ray.position[2] = eye_position.z;
//    ray.vector[0] = vector.x;
//    ray.vector[1] = vector.y;
//    ray.vector[2] = vector.z;
//    // PickRay ray = new PickRay(eye_position, vector);
//    return ray;
  }

  /**
   * Return the screen coordinates corresponding to the VisAD coordinates.
   * @param  position  array of VisAD coordinates
   * @return  corresponding (x, y) screen coordinates
   */
  public int[] getScreenCoords(double[] position) {
    Camera camera = canvasRenderer.getCamera();
    Vector3 worldPos = new Vector3(position[0], position[1], position[2]);
    Vector3 screen = camera.getScreenCoordinates(worldPos);
    return new int[] {(int) screen.getX(), (int) screen.getY()};
//    if (getPixelLocationFromImagePlateMethod == null) {
//      try {
//        Class canvas3DClass = Class.forName("javax.media.j3d.Canvas3D");
//        Class[] param =
//          {javax.vecmath.Point3d.class, javax.vecmath.Point2d.class};
//        getPixelLocationFromImagePlateMethod =
//          canvas3DClass.getMethod("getPixelLocationFromImagePlate", param);
//        if (getPixelLocationFromImagePlateMethod == null) return null;
//      }
//      catch (Exception e) {
//        return null;
//      }
//    }
//    // get transforms
//    Canvas3D canvas = null;
//    //Canvas3D canvas = display_renderer.getCanvas();
//    Transform3D t = new Transform3D();
//    canvas.getImagePlateToVworld(t);
//    TransformGroup trans = display_renderer.getTrans();
//    Transform3D tt = new Transform3D();
//    trans.getTransform(tt);
//
//    // compute image plate location
//    Point3d pos = new Point3d(position);
//    tt.transform(pos);
//    t.invert();
//    t.transform(pos);
//
//    // get screen coordinates
//    Point2d coords = new Point2d();
//    // CTR: unfortunately, the following method is Java3D 1.2 only
//    // canvas.getPixelLocationFromImagePlate(pos, coords);
//    // return new int[] {(int) coords.x, (int) coords.y};
//    try {
//      getPixelLocationFromImagePlateMethod.invoke(canvas,
//        new Object[] {pos, coords});
//    }
//    catch (Exception e) {
//      return null;
//    }
//    return new int[] {(int) coords.x, (int) coords.y};
  }

  /**
   * Create a translation matrix.
   * @param  transx   x translation amount
   * @param  transy   y translation amount
   * @param  transz   z translation amount
   * @return  new translation matrix.  This can be used to translate
   *          the current matrix
   * @see #multiply_matrix(double[] a, double[] b)
   */
  public double[] make_translate(double transx, double transy, double transz) {
    return make_matrix(0.0, 0.0, 0.0, 1.0, transx, transy, transz);
  }

  /**
   * Create a translation matrix.  no translation in Z direction (useful
   * for 2D in Java 3D.
   * @param  transx   x translation amount
   * @param  transy   y translation amount
   * @return  new translation matrix.  This can be used to translate
   *          the current matrix
   * @see #multiply_matrix(double[] a, double[] b)
   */
  public double[] make_translate(double transx, double transy) {
    return make_translate(transx, transy, 0.0);
  }

  /**
   * Multiply the two matrices together.
   * @param  a  first matrix
   * @param  b  second matrix
   * @return  new resulting matrix
   * @see #static_multiply_matrix
   */
  public double[] multiply_matrix(double[] a, double[] b) {
    return static_multiply_matrix(a, b);
  }

  /**
   * Multiply the two matrices together. Static version of multiply_matrix.
   * @param  a  first matrix
   * @param  b  second matrix
   * @return  new resulting matrix
   * @see #multiply_matrix(double[] a, double[] b)
   */
  public static double[] static_multiply_matrix(double[] a, double[] b) {
    Transform3D ta = new Transform3D(a);
    Transform3D tb = new Transform3D(b);
    ta.mul(tb);
    double[] c = new double[16];
    ta.get(c);
    return c;
  }

  /**
   * Make a transformation matrix to perform the given rotation, scale and
   * translation.  This function uses the fast matrix post-concatenation
   * techniques from Graphics Gems.
   * @param rotx  x rotation
   * @param roty  y rotation
   * @param rotz  z rotation
   * @param scale  scaling factor
   * @param transx  x translation
   * @param transy  y translation
   * @param transz  z translation
   * @return  new matrix
   */
  public double[] make_matrix(double rotx, double roty, double rotz,
         double scale, double transx, double transy, double transz) {
    return make_matrix(rotx, roty, rotz, scale, scale, scale, transx, transy, transz);
  }

  /**
   * Make a transformation matrix to perform the given rotation, scale and
   * translation.  This function uses the fast matrix post-concatenation
   * techniques from Graphics Gems.
   * @param rotx  x rotation
   * @param roty  y rotation
   * @param rotz  z rotation
   * @param scalex  x scaling factor
   * @param scaley  y scaling factor
   * @param scalez  z scaling factor
   * @param transx  x translation
   * @param transy  y translation
   * @param transz  z translation
   * @return  new matrix
   */
  public double[] make_matrix(double rotx, double roty, double rotz,
                              double scalex, double scaley, double scalez, 
                              double transx, double transy, double transz) {
    return static_make_matrix(rotx, roty, rotz, scalex, scaley, scalez, transx, transy, transz);
  }

  /**
   * Make a transformation matrix to perform the given rotation, scale and
   * translation.  This function uses the fast matrix post-concatenation
   * techniques from Graphics Gems.  Static version of make_matrix.
   * @see #make_matrix(double rotx, double roty, double rotz,
   *         double scale, double transx, double transy, double transz)
   * @param rotx  x rotation
   * @param roty  y rotation
   * @param rotz  z rotation
   * @param scale  scaling factor
   * @param transx  x translation
   * @param transy  y translation
   * @param transz  z translation
   * @return  new matrix
   */
  public static double[] static_make_matrix(
         double rotx, double roty, double rotz,
         double scale, double transx, double transy, double transz) {
    return static_make_matrix(rotx, roty, rotz, scale, scale, scale, transx, transy, transz);
  }

  /**
   * Make a transformation matrix to perform the given rotation, scale and
   * translation.  This function uses the fast matrix post-concatenation
   * techniques from Graphics Gems.  Static version of make_matrix.
   * @see #make_matrix(double rotx, double roty, double rotz,
   *         double scale, double transx, double transy, double transz)
   * @param rotx  x rotation
   * @param roty  y rotation
   * @param rotz  z rotation
   * @param scalex  x scaling factor
   * @param scaley  y scaling factor
   * @param scalez  z scaling factor
   * @param transx  x translation
   * @param transy  y translation
   * @param transz  z translation
   * @return  new matrix
   */
  public static double[] static_make_matrix(
         double rotx, double roty, double rotz,
         double scalex, double scaley, double scalez, 
         double transx, double transy, double transz) {
    double sx, sy, sz, cx, cy, cz, t;
    int i, j, k;
    double deg2rad = 1.0 / 57.2957;
    double[] matrix = new double[16];
    double[][] mat = new double[4][4];

    /* Get sin and cosine values */
    sx = Math.sin(rotx * deg2rad);
    cx = Math.cos(rotx * deg2rad);
    sy = Math.sin(roty * deg2rad);
    cy = Math.cos(roty * deg2rad);
    sz = Math.sin(rotz * deg2rad);
    cz = Math.cos(rotz * deg2rad);

    /* Start with identity matrix */
    mat[0][0] = 1.0;  mat[0][1] = 0.0;  mat[0][2] = 0.0;  mat[0][3] = 0.0;
    mat[1][0] = 0.0;  mat[1][1] = 1.0;  mat[1][2] = 0.0;  mat[1][3] = 0.0;
    mat[2][0] = 0.0;  mat[2][1] = 0.0;  mat[2][2] = 1.0;  mat[2][3] = 0.0;
    mat[3][0] = 0.0;  mat[3][1] = 0.0;  mat[3][2] = 0.0;  mat[3][3] = 1.0;

    /* Z Rotation */
    for (i=0;i<4;i++) {
      t = mat[i][0];
      mat[i][0] = t*cz - mat[i][1]*sz;
      mat[i][1] = t*sz + mat[i][1]*cz;
    }

    /* X rotation */
    for (i=0;i<4;i++) {
      t = mat[i][1];
      mat[i][1] = t*cx - mat[i][2]*sx;
      mat[i][2] = t*sx + mat[i][2]*cx;
    }

    /* Y Rotation */
    for (i=0;i<4;i++) {
      t = mat[i][0];
      mat[i][0] = mat[i][2]*sy + t*cy;
      mat[i][2] = mat[i][2]*cy - t*sy;
    }

    /* Scale */
    for (i=0;i<3;i++) {
      mat[i][0] *= scalex;
      mat[i][1] *= scaley;
      mat[i][2] *= scalez;
    }

    /* Translation */
/* should be mat[0][3], mat[1][3], mat[2][3]
   WLH 22 Dec 97 */
    mat[0][3] = transx;
    mat[1][3] = transy;
    mat[2][3] = transz;
/*
    mat[3][0] = transx;
    mat[3][1] = transy;
    mat[3][2] = transz;
*/
    k = 0;
    for (i=0; i<4; i++) {
      for (j=0; j<4; j++) {
        matrix[k] = mat[i][j];
        k++;
      }
    }
    return matrix;
  }

  static final double EPS = 0.000001;

  /**
   * Get the rotation, scale and translation parameters for the specified
   * matrix.  Results are not valid for non-uniform aspect (scale).
   * @param  rot  array to hold x,y,z rotation values
   * @param  scale  array to hold scale value
   * @param  trans  array to hold x,y,z translation values
   */
  public void instance_unmake_matrix(double[] rot, double[] scale,
                            double[] trans, double[] matrix) {
    unmake_matrix(rot, scale, trans, matrix);
  }

  /**
   * Get the rotation, scale and translation parameters for the specified
   * matrix.  Results are not valid for non-uniform aspect (scale).
   * Static version of <CODE>instance_unmake_matrix</CODE>.
   * @param  rot  array to hold x,y,z rotation values
   * @param  scale  array to hold scale value(s). If length == 1, assumes
   *                uniform scaling.
   * @param  trans  array to hold x,y,z translation values
   */
  public static void unmake_matrix(double[] rot, double[] scale,
                                   double[] trans, double[] matrix) {
    double  sx, sy, sz, cx, cy, cz;
    int i, j;
    double[][] mat = new double[4][4];
    double[][] nat = new double[4][4];

    double scalex, scaley, scalez, cxa, cxb, cxinv;
    double[] scaleinv = new double[3];

    if (rot == null || rot.length != 3) return;
    if (scale == null || !(scale.length == 1 || scale.length == 3)) return;
    if (trans == null || trans.length != 3) return;
    if (matrix == null || matrix.length != 16) return;

    int k = 0;
    for (i=0; i<4; i++) {
      for (j=0; j<4; j++) {
        mat[i][j] = matrix[k];
        k++;
      }
    }

    trans[0] = mat[0][3];
    trans[1] = mat[1][3];
    trans[2] = mat[2][3];

    /* scale */
    scalex = scaley = scalez = 0.0;
    for (i=0; i<3; i++) {
      scalex += mat[0][i] * mat[0][i];
      scaley += mat[1][i] * mat[1][i];
      scalez += mat[2][i] * mat[2][i];
    }
    if (Math.abs(scalex - scaley) > EPS || Math.abs(scalex - scalez) > EPS) {
      // System.out.println("problem " + scalex + " " + scaley + " " + scalez);
    }
    if (scale.length == 1) {
      scale[0] = Math.sqrt((scalex + scaley + scalez)/3.0);
      scaleinv[0] = Math.abs(scale[0]) > EPS ? 1.0 / scale[0] : 1.0 / EPS;
      scaleinv[1] = scaleinv[2] = scaleinv[0];
    } else {
      scale[0] = Math.sqrt(scalex);
      scale[1] = Math.sqrt(scaley);
      scale[2] = Math.sqrt(scalez);
      for (i=0; i<3; i++) {
        scaleinv[i] = Math.abs(scale[i]) > EPS ? 1.0 / scale[i] : 1.0 / EPS;
      }
    }
        

    for (i=0; i<3; i++) {
      for (j=0; j<3; j++) {
        nat[j][i] = scaleinv[j] * mat[j][i];
      }
    }

    /* rotation */
    sx = -nat[2][1];

    cxa = Math.sqrt(nat[2][0]*nat[2][0] + nat[2][2]*nat[2][2]);
    cxb = Math.sqrt(nat[0][1]*nat[0][1] + nat[1][1]*nat[1][1]);

    if (Math.abs(cxa - cxb) > EPS) {
      // System.out.println("problem2 " + cxa + " " + cxb);
    }
    /* the sign of cx does not matter;
       it is an ambiguity in 3-D rotations:
       (rotz, rotx, roty) = (180+rotz, 180-rotx, 180+roty) */
    cx = (cxa + cxb) / 2.0;
    if (Math.abs(cx) > EPS) {
      cxinv = 1.0 / cx;
      sy = nat[2][0] * cxinv;
      cy = nat[2][2] * cxinv;
      sz = nat[0][1] * cxinv;
      cz = nat[1][1] * cxinv;
    }
    else {
      /* if cx == 0 then roty and rotz are ambiguous:
         assume rotx = 0.0 */
      sy = 0.0;
      cy = 1.0;
      sz = nat[0][2];
      cz = nat[1][2];
    }

    rot[0] = 57.2957 * Math.atan2(sx, cx);
    rot[1] = 57.2957 * Math.atan2(sy, cy);
    rot[2] = 57.2957 * Math.atan2(sz, cz);
    return;
  }


/*
   homogeneous coordinates:
   point (x, y, z) is represented as (w*x, w*y, w*z, w) for w != 0
   point (x, y, z, 1) transforms to
         x' = x*mat[0]  + y*mat[1]  + z*mat[2]  + mat[3]
         y' = x*mat[4]  + y*mat[5]  + z*mat[6]  + mat[7]
         z' = x*mat[8]  + y*mat[9]  + z*mat[10] + mat[11]
         w' = x*mat[12] + y*mat[13] + z*mat[14] + mat[15]
*/


}

