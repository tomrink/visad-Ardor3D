//
// DisplayPanelJ3D.java
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

// GUI handling
import java.awt.*;
import javax.swing.*;

import javax.media.j3d.*;

public class DisplayPanelA3D extends JPanel {

  private DisplayImplA3D display;
  private DisplayRendererA3D renderer;
  //private UniverseBuilderJ3D universe;
  private VisADCanvasA3D canvas;

  public DisplayPanelA3D(DisplayImplA3D d) {
    this(d, null, null);
  }

  public DisplayPanelA3D(DisplayImplA3D d, GraphicsConfiguration config,
                         VisADCanvasA3D c) {
    display = d;
    renderer = (DisplayRendererA3D) display.getDisplayRenderer();
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    canvas = (c != null) ? c : new VisADCanvasA3D(renderer, config);
    canvas.setComponent(this);
    //add(canvas);

    //universe = new UniverseBuilderJ3D(canvas);
    //BranchGroup scene =
    //  renderer.createSceneGraph(universe.view, universe.vpTrans, canvas);
    //universe.addBranchGraph(scene);

    setPreferredSize(new java.awt.Dimension(256, 256));
    setMinimumSize(new java.awt.Dimension(0, 0));
  }

  public void setVisible(boolean v){
    super.setVisible(v);
    //canvas.setVisible(v);
  }

  public void destroy() {
    canvas = null;
    display = null;
    renderer = null;
    //if (universe != null) {
    //  universe.destroy();
    //  universe = null;
    //}
  }

}

