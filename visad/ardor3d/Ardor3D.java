package visad.ardor3d;

import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.util.Timer;

/**
 * Only one of these per JVM (suggestion by J.Gouessej of Jogamp).
 * 
 */
public class Ardor3D {
   
   private static final Ardor3D instance = new Ardor3D();
   
   private static Timer timer;
   private static FrameHandler frameHandler;
   private static RunnerA3D runner;
   private static LogicalLayer logicalLayer;
   
   private Ardor3D() {
      timer = new Timer();
      frameHandler = new FrameHandler(timer);
      runner = new RunnerA3D(frameHandler);
      logicalLayer = new LogicalLayer();
   }
   
   public static Ardor3D getInstance() {
      return instance;
   }
   
   public static Timer getTimer() {
      return timer;
   }
   
   public static FrameHandler getFrameHander() {
      return frameHandler;
   }
   
   public static LogicalLayer getLogicalLayer() {
      return logicalLayer;
   }
   
   public static void start() {
      runner.start();
   }
   
   public static void toggleRunner() {
       runner.toggle();
   }
    
   public static void toggleRunner(boolean on) {
       runner.toggle(on);
   }
   
}
