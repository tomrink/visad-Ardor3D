//
// RendererA3D.java
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
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.GameTaskQueue;
import visad.ardor3d.SwitchNode;
import visad.*;
import visad.util.Delay;


import java.rmi.*;
import java.util.concurrent.Callable;


/**
   RendererJ3D is the VisAD abstract super-class for graphics rendering
   algorithms under Java3D.  These transform Data objects into 3-D
   (or 2-D) depictions in a Display window.<P>

   RendererJ3D is not Serializable and should not be copied
   between JVMs.<P>
*/
public abstract class RendererA3D extends DataRenderer {

  /** switch is parent of any BranchGroups created by this */
  SwitchNode sw = null;
  
  /** parent of sw for 'detach' */
  Node swParent = null;
  
  Node dataBranch = null;
  
  public RendererA3D() {
    super();
  }

  public void setLinks(DataDisplayLink[] links, DisplayImpl d)
       throws VisADException {
    if (getDisplay() != null || getLinks() != null) {
      throw new DisplayException("RendererJ3D.setLinks: already set\n" +
                                 "you are probably re-using a DataRenderer");
    }
    if (!(d instanceof DisplayImplA3D)) {
      throw new DisplayException("RendererJ3D.setLinks: must be DisplayImplJ3D");
    }
    setDisplay(d);
    setDisplayRenderer(d.getDisplayRenderer());
    setLinks(links);

    // set up switch logic for clean BranchGroup replacement
    SwitchNode swt = new SwitchNode();
    sw = swt;
    toggle(getEnabled());

    swParent = new Node();
    swParent.attachChild(swt);
    
    // add to DisplayRenderer
    addSwitch((DisplayRendererA3D) getDisplayRenderer(), swParent);

  }

  public void toggle(boolean on) {
    sw.setVisible(0, on);
    super.toggle(on);
  }

  public ShadowType makeShadowFunctionType(
         FunctionType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowFunctionTypeA3D(type, link, parent);
  }

  public ShadowType makeShadowRealTupleType(
         RealTupleType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowRealTupleTypeA3D(type, link, parent);
  }

  public ShadowType makeShadowRealType(
         RealType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowRealTypeA3D(type, link, parent);
  }

  public ShadowType makeShadowSetType(
         SetType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowSetTypeA3D(type, link, parent);
  }

  public ShadowType makeShadowTextType(
         TextType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowTextTypeA3D(type, link, parent);
  }

  public ShadowType makeShadowTupleType(
         TupleType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowTupleTypeA3D(type, link, parent);
  }

  abstract void addSwitch(DisplayRendererA3D displayRenderer, Node branch);

  /** re-transform if needed;
      return false if not done */
  public boolean doAction() throws VisADException, RemoteException {
    Node branch;
    boolean all_feasible = get_all_feasible();
    boolean any_changed = get_any_changed();
    boolean any_transform_control = get_any_transform_control();
/*
System.out.println("doAction " + getDisplay().getName() + " " +
                   getLinks()[0].getThingReference().getName() +
                   " any_changed = " + any_changed +
                   " all_feasible = " + all_feasible +
                   " any_transform_control = " + any_transform_control);
*/
    if (all_feasible && (any_changed || any_transform_control)) {
/*
System.out.println("doAction " + getDisplay().getName() + " " +
                   getLinks()[0].getThingReference().getName() +
                   " any_changed = " + any_changed +
                   " all_feasible = " + all_feasible +
                   " any_transform_control = " + any_transform_control);
*/
      // exceptionVector.removeAllElements();
      clearAVControls();
      try {
        // doTransform creates a BranchGroup from a Data object
        branch = doTransform();
      }
      catch (BadMappingException e) {
        addException(e);
        branch = null;
      }
      catch (UnimplementedException e) {
        addException(e);
        branch = null;
      }
      catch (RemoteException e) {
        addException(e);
        branch = null;
      }
      catch (DisplayInterruptException e) {
        branch = null;
      }

      if (branch != null) {
        Spatial prevNode = null;
        if (sw.getNumberOfChildren() > 0) {
          prevNode = sw.getChild(0);
        }
        if (prevNode != null && prevNode == branch) {
           // branch already attached. See setBranchEarly()
        }
        else {
          final Spatial node = prevNode;
          final Node fbranch = branch;
          Callable updateCallable = new Callable() {
            public Object call() {
              sw.detachChild(node);
              sw.attachChildAt(fbranch, 0);               
              return null;
            }
          };
          GameTaskQueue uQueue = DisplayManagerA3D.queueManager.getQueue(GameTaskQueue.UPDATE);
          uQueue.enqueue(updateCallable);
          //sw.detachChild(prevNode);
          //sw.attachChildAt(branch, 0);
        }
        ((DisplayRendererA3D) getDisplayRenderer()).markNeedDraw();
        dataBranch = branch;
         
//        synchronized (this) {
//          if (!branchNonEmpty[currentIndex] ||
//              branches[currentIndex].numChildren() == 0) {
//            /* WLH 18 Nov 98 */
//            branches[currentIndex].addChild(branch);
//            branchNonEmpty[currentIndex] = true;
//          }
//          else { // if (branchNonEmpty[currentIndex])
//            if (!(branches[currentIndex].getChild(0) == branch)) {// TDR, Nov 02
//              flush(branches[currentIndex]);
//              branches[currentIndex].setChild(branch, 0);
//            }
//          } // end if (branchNonEmpty[currentIndex])
//        } // end synchronized (this)
      }
      else { // if (branch == null)
        clearBranch();
        all_feasible = false;
        set_all_feasible(all_feasible);
      }
    }
    else { // !(all_feasible && (any_changed || any_transform_control))
      DataDisplayLink[] links = getLinks();
      for (int i=0; i<links.length; i++) {
        links[i].clearData();
      }
    }
    return (all_feasible && (any_changed || any_transform_control));
  }

  public Node getBranch() {
    return dataBranch;
  }
  
  public void setBranchEarly(Node branch) {
    sw.attachChildAt(branch, 0);
  }

  public void clearBranch() {
     sw.detachChild(dataBranch);
     ((DisplayRendererA3D)getDisplayRenderer()).markNeedDraw();
     dataBranch = null;
  }

  public void flush(Node branch) {
    /*
    if (branches == null) return;
    Enumeration ch = branch.getAllChildren();
    while(ch.hasMoreElements()) {
      Node n = (Node) ch.nextElement();
      if (n instanceof Group) {
        flush((Group) n);
      }
      else if (n instanceof Shape3D &&
               ((Shape3D) n).getCapability(Shape3D.ALLOW_APPEARANCE_READ)) {
        Appearance appearance = ((Shape3D) n).getAppearance();
        if (appearance != null &&
            appearance.getCapability(Appearance.ALLOW_TEXTURE_READ)) {
          Texture texture = appearance.getTexture();
          if (texture != null &&
              texture.getCapability(Texture.ALLOW_IMAGE_READ)) {
            ImageComponent ic = texture.getImage(0);
            if (ic != null && ic.getCapability(ImageComponent.ALLOW_IMAGE_READ)) {
              if (ic instanceof ImageComponent2D) {
                Image image = ((ImageComponent2D) ic).getImage();
                if (image != null) image.flush();
// System.out.println("flush");
              }
              else if (ic instanceof ImageComponent3D) {
                Image[] images = ((ImageComponent3D) ic).getImage();
                if (images != null) {
                  for (int j=0; j<images.length; j++) {
                    if (images[j] != null) images[j].flush();
// System.out.println("flush");
                  }
                }
              }
            }
          }
        }
      }
    }
    */
  }

  public void clearScene() {
    flush(swParent);
    ((DisplayRendererA3D) getDisplayRenderer()).clearScene(this, swParent);
    dataBranch = null;
    sw = null;
    swParent = null;
    super.clearScene();
  }

  /** create a BranchGroup scene graph for Data in links;
      this can put Behavior objects in the scene graph for
      DataRenderer classes that implement direct manipulation widgets;
      may reduce work by only changing scene graph for Data and
      Controls that have changed:
      1. use boolean[] changed to determine which Data objects have changed
      2. if Data has not changed, then use Control.checkTicks loop like in
         prepareAction to determine which Control-s have changed */
  public abstract Node doTransform()
         throws VisADException, RemoteException;

}

