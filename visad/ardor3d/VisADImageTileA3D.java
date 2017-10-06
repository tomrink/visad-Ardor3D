package visad.ardor3d;
import visad.data.CachedBufferedByteImage;

import java.awt.image.BufferedImage;


public class VisADImageTileA3D {

   BufferedImage[] images;
   int numImages;
   public int current_index = 0;
   private boolean doingPrefetch = false;
   
   public int height;
   public int width;
   public int yStart;
   public int xStart;


   public VisADImageTileA3D(int numImages, int height, int yStart, int width, int xStart) {
     this.numImages = numImages;
     this.height = height;
     this.yStart = yStart;
     this.width = width;
     this.xStart = xStart;
     images = new BufferedImage[numImages];
   }

   public void setImages(BufferedImage[] images) {
     this.images = images;
     this.numImages = images.length;
   }

   public BufferedImage[] getImages() {
     return this.images;
   }

   public BufferedImage getImage(int index) {
     return images[index];
   }

   public void setImage(int index, BufferedImage image) {
     images[index] = image;
   }


   private int lookAheadIndexBaseIndex = 0;

   public void setCurrent(int idx) {
     current_index = idx;

     //Have a local array here in case the images array changes in another thread
     BufferedImage[] theImages = images;

     if (theImages != null && idx>=0 && idx< theImages.length) {
      //-imageComp.updateData(this, 0, 0, 0, 0); // See note above

       BufferedImage image = theImages[idx];
       if(image == null) {
           //      System.err.println ("Animate image is null for index:" + idx);
       } else {
           //Do the lookahead
           if(image instanceof CachedBufferedByteImage) {
               //Find the next image
               CachedBufferedByteImage nextImage = null;
               //If we are at the end of the loop then go to the beginning
               int nextIdx = idx+1;
               if(nextIdx>=theImages.length)
                   nextIdx = 0;
               nextImage = (CachedBufferedByteImage)theImages[nextIdx];
               if(!doingPrefetch && nextImage!=null && !nextImage.inMemory()) {
                   final CachedBufferedByteImage imageToLoad = nextImage;
                   Runnable r = new Runnable() {
                           public  void run() {
                               doingPrefetch = true;
                               try {
                                   imageToLoad.getBytesFromCache();
                               } finally {
                                   doingPrefetch = false;
                               }

                           }
                       };
                   Thread t = new Thread(r);
                   t.start();
               }
           }
       }
     }
   }
}
