package visad.ardor3d;
import com.ardor3d.scenegraph.Node;
import visad.ardor3d.SwitchNode;
import java.util.ArrayList;
import java.util.Iterator;


public class VisADImageNodeA3D {

   VisADImageTileA3D[] images;
   public ArrayList<VisADImageTileA3D> imageTiles = new ArrayList<VisADImageTileA3D>(); 
   public int numChildren = 0;
   public Node branch;
   SwitchNode swit;
   public int current_index = 0;

   public int numImages;
   public int data_width;
   public int data_height;

   public VisADImageNodeA3D() {
   }

   public VisADImageNodeA3D(Node branch, SwitchNode swit) {
     this.branch = branch;
     this.swit = swit;
   }

   public void addTile(VisADImageTileA3D tile) {
    imageTiles.add(tile);
    numChildren++;
   }

   public VisADImageTileA3D getTile(int index) {
     return imageTiles.get(index);
   }

   public Iterator getTileIterator() {
     return imageTiles.iterator();
   }

   public int getNumTiles() {
     return numChildren;
   }


   public void setCurrent(int idx) {
     current_index = idx;

     for (int i=0; i<numChildren; i++) {
       imageTiles.get(i).setCurrent(idx);
     }
   }


   public void initialize() {
   }

   public void setBranch(Node branch) {
     this.branch = branch;
   }

   public void setSwitch(SwitchNode swit) {
     this.swit = swit;
   }

   public SwitchNode getSwitch() {
     return swit;
   }

   public Node getBranch() {
     return branch;
   }
}