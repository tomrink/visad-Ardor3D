//
// ShadowAnimationFunctionTypeJ3D.java
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

import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.extension.SwitchNode;
import visad.*;

import java.util.Vector;
import java.rmi.*;

/**
   The ShadowAnimationFunctionTypeJ3D class shadows the FunctionType class for
   AnimationRendererJ3D, within a DataDisplayLink, under Java3D.<P>
*/
public class ShadowAnimationFunctionTypeA3D extends ShadowFunctionTypeA3D {

  private static final int MISSING1 = Byte.MIN_VALUE;      // least byte

  public ShadowAnimationFunctionTypeA3D(MathType t, DataDisplayLink link,
                                ShadowType parent)
         throws VisADException, RemoteException {
    super(t, link, parent);
  }

  public boolean doTransform(Object group, Data data, float[] value_array,
                             float[] default_values, DataRenderer renderer)
         throws VisADException, RemoteException {

    DataDisplayLink link = renderer.getLink();

    // return if data is missing or no ScalarMaps
    if (data.isMissing()) {
      ((AnimationRendererA3D) renderer).markMissingVisADBranch();
      return false;
    }
    if (getLevelOfDifficulty() == NOTHING_MAPPED) return false;

    ShadowFunctionOrSetType adaptedShadowType =
      (ShadowFunctionOrSetType) getAdaptedShadowType();
    DisplayImpl display = getDisplay();

    // analyze data's domain (its a Field)
    Set domain_set = ((Field) data).getDomainSet();

    // ShadowRealTypes of Domain
    ShadowRealType[] DomainComponents = adaptedShadowType.getDomainComponents();


    //if (((AnimationRendererJ3D)renderer).animation1D &&
    //     domain_set.getDimension() == 1) {
    if (((AnimationRendererA3D)renderer).animation1D) {

      Vector domain_maps = DomainComponents[0].getSelectedMapVector();
      ScalarMap amap = null;
      if (domain_set.getDimension() == 1 && domain_maps.size() == 1) {
        ScalarMap map = (ScalarMap) domain_maps.elementAt(0);
        if (Display.Animation.equals(map.getDisplayScalar())) {
          amap = map;
        }
      }
      if (amap == null) {
        throw new BadMappingException("time must be mapped to Animation");
      }
      AnimationControlA3D control = (AnimationControlA3D) amap.getControl();
      ((AnimationRendererA3D)renderer).animation1D = false;

      // get any usable frames from the old scene graph
      SwitchNode old_swit = null;
      Node[] old_nodes = null;
      double[] old_times = null;
      boolean[] old_mark = null;
      int old_len = 0;
      boolean reuse = ((AnimationRendererA3D) renderer).getReUseFrames();
      if (group instanceof Node &&
          ((Node) group).getNumberOfChildren() > 0) {
        Node g = (Node) ((Node) group).getChild(0);

        // WLH 06 Feb 06 - support switch in a branch group.
        if (g instanceof Node &&
            ((Node) g).getNumberOfChildren() > 0) {
            g = (Node) ((Node) g).getChild(0);
        }

        if (g instanceof SwitchNode) {
          old_swit = (SwitchNode) g;

          old_len = old_swit.getNumberOfChildren();
          if (old_len > 0) {
            old_nodes = new Node[old_len];
            for (int i=0; i<old_len; i++) {
              old_nodes[i] = (Node) old_swit.getChild(i);
            }
            // remove old_nodes from old_swit
            for (int i=0; i<old_len; i++) {
              old_nodes[i].getParent().detachChild(old_nodes[i]);
            }
            old_times = new double[old_len];
            old_mark = new boolean[old_len];
            for (int i=0; i<old_len; i++) {
              old_mark[i] = false;
              if (old_nodes[i] instanceof VisADNodeA3D && reuse) {
                old_times[i] = ((VisADNodeA3D) old_nodes[i]).getTime();
              }
              else {
                old_times[i] = Double.NaN;
              }
            }
          }
        }
      } // end if (((Node) group).numChildren() > 0)

      // create frames for new scene graph
      double[][] values = domain_set.getDoubles();
      double[] times = values[0];
      int len = times.length;
      double delta = Math.abs((times[len-1] - times[0]) / (1000.0 * len));

      // create new Switch and make live
      // control.clearSwitches(this); // already done in DataRenderer.doAction
      SwitchNode swit = null;
      if (old_swit != null) {
        swit = old_swit;
        ((AVControlA3D) control).addPair((SwitchNode) swit, domain_set, renderer);
        ((AVControlA3D) control).init();
      }
      else {
        swit = (SwitchNode) makeSwitch();

        addSwitch(group, swit, control, domain_set, renderer);
      }

      // insert old frames into new scene graph, and make
      // new (blank) VisADNodes for rendering new frames
      VisADNodeA3D[] nodes = new VisADNodeA3D[len];
      boolean[] mark = new boolean[len];
      for (int i=0; i<len; i++) {
        for (int j=0; j<old_len; j++) {
          if (!old_mark[j] && Math.abs(times[i] - old_times[j]) < delta) {
            old_mark[j] = true;
            nodes[i] = (VisADNodeA3D) old_nodes[j];
            break;
          }
        }
        if (nodes[i] != null) {
          mark[i] = true;
        }
        else {
          mark[i] = false;
          nodes[i] = new VisADNodeA3D(times[i]);
        }
        addToSwitch(swit, nodes[i]);
      }
      for (int j=0; j<old_len; j++) {
        if (!old_mark[j]) {
          ((RendererA3D) renderer).flush(old_nodes[j]);
          old_nodes[j] = null;
        }
      }
      // make sure group is live
      if (group instanceof Node) {
        ((AnimationRendererA3D) renderer).setBranchEarly((Node) group);
      }
      // change animation sampling, but don't trigger re-transform
      if (((AnimationRendererA3D) renderer).getReUseFrames() &&
          ((AnimationRendererA3D) renderer).getSetSetOnReUseFrames()) {
        control.setSet(domain_set, true);
      }
      else {
        control.setCurrent(0);
      }
      old_nodes = null;
      old_times = null;
      old_mark = null;

      // render new frames
      for (int i=0; i<len; i++) {
        if (!mark[i]) {
          // not necessary, but perhaps if this is modified
          // int[] lat_lon_indices = renderer.getLatLonIndices();
          Node branch = (Node) makeBranch();
          ((AnimationRendererA3D) renderer).setVisADBranch(nodes[i]);
          recurseRange(branch, ((Field) data).getSample(i),
                       value_array, default_values, renderer);
          ((AnimationRendererA3D) renderer).setVisADBranch(null);
          nodes[i].attachChild(branch);
          // not necessary, but perhaps if this is modified
          // renderer.setLatLonIndices(lat_lon_indices);
        }
      }
    }
    else {
      super.doTransform(group, data, value_array, default_values, renderer);
    }

    return false;
  }
}
