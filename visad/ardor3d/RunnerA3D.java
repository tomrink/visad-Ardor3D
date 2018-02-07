package visad.ardor3d;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.RenderContext;

public class RunnerA3D implements Runnable {
   private static final long FrameUpdateIntervalMillis = 20;
   
   private final FrameHandler frameWork;
   private final CanvasRenderer canvasRenderer;
   private final DisplayRendererA3D dspRenderer;
   private final Updater updater;
   private Thread thread;
   private boolean pause = false;
   private boolean exit = false;
   
   public RunnerA3D(FrameHandler frameWork, CanvasRenderer canvasRenderer, DisplayRendererA3D dspRenderer, Updater updater) {
      this.frameWork = frameWork;
      this.canvasRenderer = canvasRenderer;
      this.dspRenderer = dspRenderer;
      this.updater = updater;
      thread = new Thread(this);
      thread.start();
   }
   
   public void run() {
        frameWork.init();
        /* This must be done after init above */
        RenderContext renderContext = canvasRenderer.getRenderContext();
        ContextCapabilities contextCapabilities = renderContext.getCapabilities();
        dspRenderer.setCapabilities(contextCapabilities);
        
        while (!exit) {
           if (!pause) {
              /* experiment
              frameWork.getTimer().update();
              updater.update(frameWork.getTimer());
              
              if (dspRenderer.getNeedDraw()) {
                 frameWork.updateFrame();
              }
              */
              frameWork.updateFrame();
           }
           delay(FrameUpdateIntervalMillis);
        }
   }
   
   public void start() {
      if (thread == null) {
         thread = new Thread(this);
         thread.start();
      }
   }
   
   public void toggle() {
      pause = !pause;
   }
   
   public void toggle(boolean on) {
      pause = !on;
   }
   
   public void exit() {
      exit = true;
   }
   
   private void delay(long millis) {
      try {
         java.lang.Thread.sleep(millis);
      }
      catch (Exception e) {
         e.printStackTrace();
      }      
   }
}