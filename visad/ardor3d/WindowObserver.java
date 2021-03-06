package visad.ardor3d;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import visad.VisADException;

/**
 *
 * 
 */
public class WindowObserver extends WindowAdapter {
   
   private static final HashMap<Window, WindowObserver> windowAdapters = new HashMap();

   private final ArrayList<DisplayImplA3D> displays = new ArrayList();
   
   private final ArrayList<UpdaterA3D> updaters = new ArrayList();
   
   private final Window window;
   
   public WindowObserver(Window window) {
      this.window = window;
   }
   
   public static void attach(Window window, DisplayImplA3D display, UpdaterA3D updater) {
      WindowObserver adapter = getWindow(window);
      adapter.addDisplay(display);
      adapter.addUpdater(updater);
   }
   
   public static WindowObserver getWindow(Window window) {
      WindowObserver adapter = windowAdapters.get(window);
      if (adapter == null) {
        adapter = new WindowObserver(window);
        window.addWindowListener(adapter);
        windowAdapters.put(window, adapter);
      }
      return adapter;
   }
   
   public void addDisplay(DisplayImplA3D display) {
      displays.add(display);
   }
   
   public void addUpdater(UpdaterA3D updater) {
      updaters.add(updater);
   }
   
   @Override
   public void windowClosing(WindowEvent e) {
      try {
         Iterator<DisplayImplA3D> iterD = displays.iterator();
         while (iterD.hasNext()) {
            iterD.next().destroy();
         }
         
         Iterator<UpdaterA3D> iterU = updaters.iterator();
         while (iterU.hasNext()) {
            iterU.next().destroy();
         }
      }
      catch (VisADException | RemoteException exc) {
         exc.printStackTrace();
      }
      finally {
         updaters.clear();
         displays.clear();
         windowAdapters.remove(window);
      }
   }
   
}
