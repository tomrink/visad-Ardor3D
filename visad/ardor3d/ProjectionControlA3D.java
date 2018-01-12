//
// ProjectionControlA3D.java
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

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import visad.*;

import java.rmi.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.Vector;
import java.util.Enumeration;

import java.lang.reflect.*;

/**
   ProjectionControlJ3D is the VisAD class for controlling the Projection
   from 3-D to 2-D.  It manipulates a TransformGroup node in the
   scene graph.<P>
*/
/* WLH 17 June 98
public class ProjectionControlJ3D extends Control
       implements ProjectionControl {
*/
public class ProjectionControlA3D extends ProjectionControl {

  private transient Transform3D Matrix;
  
  private transient Transform trans;

  // Vector of Switch nodes for volume rendering
  transient Vector switches = new Vector();
  int which_child = 2; // initial view along Z axis (?)

  // DRM 6 Nov 2000
  /** View of the postive X face of the display cube */
  public static final int X_PLUS = 0;
  /** View of the negative X face of the display cube */
  public static final int X_MINUS = 1;
  /** View of the postive Y face of the display cube */
  public static final int Y_PLUS = 2;
  /** View of the negative Y face of the display cube */
  public static final int Y_MINUS = 3;
  /** View of the postive Z face of the display cube */
  public static final int Z_PLUS = 4;
  /** View of the negative Z face of the display cube */
  public static final int Z_MINUS = 5;

  /**
   * Construct a new ProjectionControl for the display.  The initial
   * projection is saved so it can be reset with resetProjection().
   * @see #resetProjection().
   * @param  d  display whose projection will be controlled by this
   * @throws VisADException
   */
  public ProjectionControlA3D(DisplayImpl d) throws VisADException {
    super(d);
    trans = init();
    matrix = new double[MATRIX3D_LENGTH];
    Matrix4 mat4 = trans.getHomogeneousMatrix(null);
    matrix = mat4.toArray(null);
    saveProjection();   // DRM 6 Nov 2000
  }

  /**
   * Set the projection matrix.
   * @param m new projection matrix
   * @throws VisADException  VisAD error
   * @throws RemoteException  remote error
   */
  public void setMatrix(double[] m)
         throws VisADException, RemoteException {
    super.setMatrix(m);
    Matrix = new Transform3D(matrix);
    
    Matrix4 mat4 = new Matrix4(m[0],  m[1],  m[2],  m[3],
                               m[4],  m[5],  m[6],  m[7],
                               m[8],  m[9],  m[10], m[11],
                               m[12], m[13], m[14], m[15]);
    trans = new Transform();
    
    double[] rot = new double[3];
    double[] scale = new double[3];
    double[] trns = new double[3];
    MouseBehaviorA3D.unmake_matrix(rot, scale, trns, m);
    double[] rotm = MouseBehaviorA3D.static_make_matrix(rot[0], rot[1], rot[2], 1, 1, 1, 0, 0, 0);
    Matrix3 rotm3 = new Matrix3(rotm[0], rotm[1], rotm[2], rotm[4], rotm[5], rotm[6], rotm[8], rotm[9], rotm[10]);
    
    trans.setRotation(rotm3);
    trans.setScale(scale[0], scale[1], scale[2]);
    trans.setTranslation(trns[0], trns[1], trns[2]);


    ((DisplayRendererA3D) getDisplayRenderer()).setTransform(trans);
    if (!switches.isEmpty()) selectSwitches();
    changeControl(false);
  }

  /**
   * Set the aspect for the axes.  Default upon initialization is 
   * 1.0, 1.0, 1.0.  Invokes saveProjection to set this as the new default.
   * @see #saveProjection()
   * @param aspect  ratios (dimension 3) for the X, Y, and Z axes
   * @throws VisADException  aspect is null or wrong dimension or other error
   * @throws RemoteException  remote error
   */
  public void setAspect(double[] aspect)
         throws VisADException, RemoteException {
    if (aspect == null || aspect.length != 3) {
      throw new DisplayException("aspect array must be length = 3");
    }
    double[] m = new double[MATRIX3D_LENGTH];
    m[0] = aspect[0];
    m[5] = aspect[1];
    m[10] = aspect[2];
    setMatrix(m);
    saveProjection();
  }

  private Transform init() {
    Transform trans = new Transform();
    if (getDisplayRenderer().getMode2D()){
       double scale = ProjectionControl.SCALE2D;
       trans.setScale(scale);
    }
    return trans;
  }

  public void addPair(Switch sw, DataRenderer re) {
    switches.addElement(new SwitchProjection(sw, re));
    sw.setWhichChild(which_child);
  }

  private void selectSwitches() {
    int old_which_child = which_child;
    // calculate which axis is most parallel to eye direction
    Transform3D tt = new Transform3D(Matrix);
    tt.invert();
    Point3d origin = new Point3d(0.0, 0.0, 0.0);
    Point3d eye = new Point3d(0.0, 0.0, 1.0);
    tt.transform(origin);
    tt.transform(eye);
    double dx = eye.x - origin.x;
    double dy = eye.y - origin.y;
    double dz = eye.z - origin.z;
    double ax = Math.abs(dx);
    double ay = Math.abs(dy);
    double az = Math.abs(dz);
    if (az >= ay && az >= ax) {
      which_child = (dz > 0) ? 2 : 5;
    }
    else if (ay >= ax) {
      which_child = (dy > 0) ? 1 : 4;
    }
    else {
      which_child = (dx > 0) ? 0 : 3;
    }

    // axis did not change, so no need to change Switches
    if (old_which_child == which_child) return;
/*
System.out.println("which_child = " + which_child + "  " + dx +
                  " " + dy + " " + dz);
*/
    // axis changed, so change Switches
    Enumeration pairs = ((Vector) switches.clone()).elements();
    while (pairs.hasMoreElements()) {
      SwitchProjection ss = (SwitchProjection) pairs.nextElement();
      ss.swit.setWhichChild(which_child);
    }
  }

  /** clear all 'pairs' in switches that involve re */
  public void clearSwitches(DataRenderer re) {
    Enumeration pairs = ((Vector) switches.clone()).elements();
    while (pairs.hasMoreElements()) {
      SwitchProjection ss = (SwitchProjection) pairs.nextElement();
      if (ss.renderer.equals(re)) {
        switches.removeElement(ss);
      }
    }
  }

  /**
   * Set the projection so the requested view is displayed.
   * @param  view  one of the static view fields (X_PLUS, X_MINUS, etc).  This
   *               will set the view so the selected face is orthogonal to
   *               the display.
   * @throws VisADException   VisAD failure.
   * @throws RemoteException  Java RMI failure.
   */
  public void setOrthoView(int view)
    throws VisADException, RemoteException 
  {
    double[] viewMatrix;
    if (getDisplayRenderer().getMode2D()) return;
    switch (view)
    {
      case Z_PLUS: // Top
        viewMatrix = 
          getDisplay().make_matrix(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        break;
      case Z_MINUS: // Bottom
        viewMatrix = 
          getDisplay().make_matrix(0.0, 180.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        break;
      case Y_PLUS: // North
        viewMatrix = 
          getDisplay().make_matrix(-90.0, 180.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        break;
      case Y_MINUS: // South
        viewMatrix = 
          getDisplay().make_matrix(90.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        break;
      case X_PLUS: // East
        viewMatrix = 
          getDisplay().make_matrix(0.0, 90.0, 90.0, 1.0, 0.0, 0.0, 0.0);
        break;
      case X_MINUS: // West
        viewMatrix = 
          getDisplay().make_matrix(0.0, -90.0, -90.0, 1.0, 0.0, 0.0, 0.0);
        break;
      default:   // no change
        viewMatrix = 
          getDisplay().make_matrix(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        break;
   }
   setMatrix(
     getDisplay().multiply_matrix(viewMatrix, getSavedProjectionMatrix()));
 }

  /** SwitchProjection is an inner class of ProjectionControlJ3D for
      (Switch, DataRenderer) structures */
  private class SwitchProjection extends Object {
    Switch swit;
    DataRenderer renderer;

    SwitchProjection(Switch sw, DataRenderer re) {
      swit = sw;
      renderer = re;
    }
  }
}
