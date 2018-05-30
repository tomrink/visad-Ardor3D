package visad.ardor3d;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Scene;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.GameTaskQueue;
import java.rmi.RemoteException;
import visad.DisplayEvent;
import visad.VisADException;


public final class SceneA3D implements Scene {
    private final Node root;
    private DisplayRendererA3D dspRenderer;

    public SceneA3D(DisplayRendererA3D dspRenderer) {
        this.dspRenderer = dspRenderer;
        root = new Node("root");
    }

    public Node getRoot() {
        return root;
    }

    @Override
    @MainThread
    public boolean renderUnto(final Renderer renderer) {

        renderer.draw(root);
            
        // executes all queued update tasks
        dspRenderer.getTaskQueueManager().getQueue(GameTaskQueue.UPDATE).execute(renderer);
            
        // executes all queued rendering tasks
        dspRenderer.getTaskQueueManager().getQueue(GameTaskQueue.RENDER).execute(renderer);
        
        // This might be better to do in a custom FrameHandler, but would require extending Ardor3D
        try {
           dspRenderer.getDisplay().notifyListeners(DisplayEvent.FRAME_DONE, 0, 0);
        }
        catch (VisADException | RemoteException e) {
        }
        return true;
    }

    @Override
    public PickResults doPick(final Ray3 pickRay) {
        // does nothing.
        return null;
    }
 }
