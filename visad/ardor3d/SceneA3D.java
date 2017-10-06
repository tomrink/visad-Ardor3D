package visad.ardor3d;

import com.ardor3d.framework.Scene;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;

public class SceneA3D implements Scene {
   
   private final Node root;
   private final Node transform;
   
   private boolean needDraw = false;
    
   public SceneA3D() {
      root = new Node();
      transform = new Node();
      
      /**
       * Create a ZBuffer to display pixels closest to the camera above
       * farther ones.
       */
      final ZBufferState buf = new ZBufferState();
      buf.setEnabled(true);
      buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
      root.setRenderState(buf);

      // ---- LIGHTS
      /**
       * Set up a basic, default light.
       */
      PointLight light = new PointLight();
      PointLight light2 = new PointLight();
      light2.setLocation(new Vector3(-100,-100,-100));
      light2.setEnabled(true);

      light.setDiffuse(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
      light.setAmbient(new ColorRGBA(0.05f, 0.05f, 0.05f, 1.0f));
      light.setLocation(new Vector3(100, 100, 100));
      light.setEnabled(true);

      /**
       * Attach the light to a lightState and the lightState to rootNode.
       */
      LightState lightState = new LightState();
      lightState.setEnabled(true);
      lightState.setTwoSidedLighting(false);
      lightState.attach(light);
      lightState.attach(light2);
      root.setRenderState(lightState);

      root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);

      root.attachChild(transform);      
   }

   @Override
   public boolean renderUnto(Renderer renderer) {
       if (transform.isDirty(DirtyType.Transform)) {
          root.updateGeometricState(20, true);
          root.draw(renderer);
          return true;
       }
       else if (needDraw) {
          synchronized(this) {
             root.updateGeometricState(20, true);
             root.draw(renderer);
             needDraw = false;
          }
          return true;
       }
       return false;      
   }

   @Override
   public PickResults doPick(Ray3 pickRay) {
      return null;
   }
   
   public synchronized void markNeedDraw() {
      needDraw = true;
   }
   
   public Node getRootNode() {
      return root;
   }
   
   public Node getTransformNode() {
      return transform;
   }
}