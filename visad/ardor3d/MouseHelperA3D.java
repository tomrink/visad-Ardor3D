/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package visad.ardor3d;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import visad.DisplayRenderer;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.MouseBehavior;
import visad.MouseHelper;
import visad.Real;
import visad.RealType;
import visad.VisADRay;

/**
 *
 * @author rink
 */
public class MouseHelperA3D extends MouseHelper {
   
   private FlatField zWorldToNormDepth = null;
   
   public MouseHelperA3D(DisplayRenderer r, MouseBehavior b) {
      super(r, b);
   }
   
   public void setTranslationFactor(int screen_x, int screen_y) {
     Camera camera = ((DisplayRendererA3D)getDisplayRenderer()).getCanvasRenderer().getCamera();
     Real worldZ0;
     
     if (zWorldToNormDepth == null) {
        int nPts = 101;
        float del = 0.01f;
        float nDpth = 0;
        float[] normDepth = new float[nPts];
        float[] zWorld = new float[nPts];
        final Vector2 pos = Vector2.fetchTempInstance().set(camera.getWidth()/2, camera.getHeight()/2);
        for (int k=0; k<nPts; k++) {
           Vector3 wc = camera.getWorldCoordinates(pos, nDpth);
           normDepth[k] = nDpth;
           zWorld[k] = wc.getZf();
           nDpth += del;
        }
        Vector2.releaseTempInstance(pos);
        
        try {
          Gridded1DSet domSet = new Gridded1DSet(RealType.Generic, new float[][] {zWorld}, nPts);
          zWorldToNormDepth = new FlatField(new FunctionType(RealType.Generic, RealType.Generic), domSet);
          zWorldToNormDepth.setSamples(new float[][] {normDepth});
        }
        catch (Exception e) {
           e.printStackTrace();
        }
     }
     
     double normDepthAtWorldOrigin = 0;
     
     try {
        normDepthAtWorldOrigin = ((Real)zWorldToNormDepth.evaluate(new Real(RealType.Generic, 0.0))).getValue();
     } catch (Exception e) {
        e.printStackTrace();
     }
      
     final Vector2 pos = Vector2.fetchTempInstance().set(screen_x, screen_y);
     final Vector2 posx = Vector2.fetchTempInstance().set(screen_x+1, screen_y);
     final Vector2 posy = Vector2.fetchTempInstance().set(screen_x, screen_y+1);
     
     final Vector3 wc = camera.getWorldCoordinates(pos, normDepthAtWorldOrigin);
     final Vector3 wcx = camera.getWorldCoordinates(posx, normDepthAtWorldOrigin);
     final Vector3 wcy = camera.getWorldCoordinates(posy, normDepthAtWorldOrigin);
     
     Vector2.releaseTempInstance(pos);
     Vector2.releaseTempInstance(posx);
     Vector2.releaseTempInstance(posy);
     
     xmul = wcx.getXf() - wc.getXf();
     ymul = -(wcy.getYf() - wc.getYf());
   }
   
}
