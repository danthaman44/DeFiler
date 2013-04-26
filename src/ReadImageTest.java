import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;
import common.DFileID;
import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import dblockcache.DBufferCache;
import dfs.DFS;


public class ReadImageTest extends Thread
{
    byte[] image;
    int numFinished = 0;
    
    public synchronized void finished(byte[] data, int offset) throws IOException{
     numFinished+=1;
     for(int i=offset; i<offset+4145; i++){
         image[i]=data[i-offset];
     }
     if(numFinished==5){
         BufferedImage blah = ImageIO.read(new ByteArrayInputStream(image));
         ImageIO.write(blah, "jpg", new File("ThreadedTestImage.jpg"));
         System.out.println("finished!");
     }
    }
    public ReadImageTest(){
        
    }


    public static void main (String[] args)
        throws FileNotFoundException,
            IOException

    {
        DFS deFiler = new DFS();

        DBufferCache cache = new DBufferCache(10);
        deFiler.myCache = cache;

        VirtualDisk disk = new VirtualDisk("Sean's Disk", false);
        cache.myVD = disk;
        ReadImageTest me = new ReadImageTest();
        me.numFinished = 0;
        me.image = new byte[20725];
        
        ReadThread myT1 = new ReadThread(0,1, me);
        ReadThread myT2 = new ReadThread(4145,2, me);
        ReadThread myT3 = new ReadThread(8290,3, me);
        ReadThread myT4 = new ReadThread(12435,4, me);
        ReadThread myT5 = new ReadThread(16580,5, me);
        myT1.myDfs = deFiler;
        myT2.myDfs = deFiler;
        myT3.myDfs = deFiler;
        myT4.myDfs = deFiler;
        myT5.myDfs = deFiler;
        
        DiskThread diskThread = new DiskThread(disk);
        diskThread.start();
        deFiler.init();
        myT1.start();
        myT2.start();
        myT3.start();
        myT4.start();
        myT5.start();

    }
    
    


}
