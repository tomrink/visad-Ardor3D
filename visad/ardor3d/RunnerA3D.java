package visad.ardor3d;

import com.ardor3d.framework.FrameHandler;

public class RunnerA3D implements Runnable {
   FrameHandler frameWork;
   private Thread thread;
   private boolean pause = false;
   private boolean exit = false;
   
   public RunnerA3D(FrameHandler frameWork) {
      this.frameWork = frameWork;
      thread = new Thread(this);
      thread.start();
   }
   
   public void run() {
        frameWork.init();
        
        while (!exit) {
           if (!pause) {
              frameWork.updateFrame();
           }
           delay(20);
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