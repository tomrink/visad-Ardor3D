package visad.ardor3d;

import com.ardor3d.framework.FrameHandler;

public class RunnerA3D implements Runnable {
   private static final long FrameUpdateIntervalMillis = 20;
   
   private final FrameHandler frameWork;
   private Thread thread;
   private boolean pause = false;
   private boolean exit = false;
   
   public RunnerA3D(FrameHandler frameWork) {
      this.frameWork = frameWork;
   }
   
   public void run() {
        frameWork.init();
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