package visad.ardor3d;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture2D;
import visad.data.CachedBufferedByteImage;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.TextureManager;

public class VisADImageTileA3D {

   Renderer renderer;
   Texture2D texture;
   Image[] images;
   int numImages;
   public int current_index = 0;
   private boolean doingPrefetch = false;
   
   public int height;
   public int width;
   public int yStart;
   public int xStart;
   private TextureState ts;


   public VisADImageTileA3D(Renderer renderer, int numImages, int height, int yStart, int width, int xStart) {
     this.renderer = renderer;
     this.numImages = numImages;
     this.height = height;
     this.yStart = yStart;
     this.width = width;
     this.xStart = xStart;
     images = new Image[numImages];
   }

   public void setImages(Image[] images) {
     this.images = images;
     this.numImages = images.length;
   }

   public Image[] getImages() {
     return this.images;
   }

   public Image getImage(int index) {
     return images[index];
   }

   public void setImage(int index, Image image) {
     images[index] = image;
   }
   
   public void setTexture(Texture2D texture) {
      this.texture = texture;
   }
   
   public void setTexture(TextureState ts) {
      this.ts = ts;
   }

   private int lookAheadIndexBaseIndex = 0;

   public void setCurrent(int idx) {
     current_index = idx;

     //Have a local array here in case the images array changes in another thread
     Image[] theImages = images;

     if (theImages != null && idx>=0 && idx< theImages.length) {

       Image image = theImages[idx];
       if(image == null) {
           //      System.err.println ("Animate image is null for index:" + idx);
       } else {
          
          if (renderer != null) {
             try {
                renderer.updateTexture2DSubImage(texture, 0, 0, width, height, image.getData(0), 0, 0, width);
                
                
//    com.ardor3d.image.Texture.MinificationFilter minFilter;
//    com.ardor3d.image.Texture.ApplyMode applyMode = com.ardor3d.image.Texture.ApplyMode.Replace;
//    if (false) {
//       minFilter = com.ardor3d.image.Texture.MinificationFilter.BilinearNoMipMaps;
//    }
//    else {
//       minFilter = com.ardor3d.image.Texture.MinificationFilter.NearestNeighborNoMipMaps;
//    }
//        
//    com.ardor3d.image.Texture2D texture = (com.ardor3d.image.Texture2D) TextureManager.loadFromImage(image, minFilter);
//    texture.setMagnificationFilter(com.ardor3d.image.Texture.MagnificationFilter.NearestNeighbor);
//    texture.setApply(applyMode);
//    ts.setTexture(texture);
                
             }
             catch (Exception e) {
                e.printStackTrace();
             }
          }
          
           //Do the lookahead
//           if(image instanceof CachedBufferedByteImage) {
//               //Find the next image
//               CachedBufferedByteImage nextImage = null;
//               //If we are at the end of the loop then go to the beginning
//               int nextIdx = idx+1;
//               if(nextIdx>=theImages.length)
//                   nextIdx = 0;
//               nextImage = (CachedBufferedByteImage)theImages[nextIdx];
//               if(!doingPrefetch && nextImage!=null && !nextImage.inMemory()) {
//                   final CachedBufferedByteImage imageToLoad = nextImage;
//                   Runnable r = new Runnable() {
//                           public  void run() {
//                               doingPrefetch = true;
//                               try {
//                                   imageToLoad.getBytesFromCache();
//                               } finally {
//                                   doingPrefetch = false;
//                               }
//
//                           }
//                       };
//                   Thread t = new Thread(r);
//                   t.start();
//               }
//           }
       }
     }
   }
}
