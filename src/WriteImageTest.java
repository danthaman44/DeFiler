import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.imageio.ImageIO;
import common.Constants;
import common.DFileID;
import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import dblockcache.DBufferCache;
import dfs.DFS;


public class WriteImageTest
{
    DFS myDfs;
    int numFinished;


    public synchronized void finished() throws IllegalArgumentException, IOException{
        numFinished+=1;
        if (numFinished==5){
            myDfs.save();
            System.out.println("Finished");
        }
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
        disk.clear();

        File fnew = new File("./A+reportcardsmall.jpg");
        BufferedImage originalImage = ImageIO.read(fnew);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "jpg",baos);
        byte[] imageInByte = baos.toByteArray();
        System.out.println("length of image: "+imageInByte.length);
        
        DiskThread diskThread = new DiskThread(disk);
        diskThread.start();
        deFiler.init();
        WriteImageTest me = new WriteImageTest();
        me.myDfs = deFiler;
        DFileID id1 = deFiler.createDFile();
        DFileID id2= deFiler.createDFile();
        DFileID id3 = deFiler.createDFile();
        DFileID id4 = deFiler.createDFile();
        DFileID id5 = deFiler.createDFile();
        WriteThread myT1 = new WriteThread(0,id1, me, imageInByte);
        WriteThread myT2 = new WriteThread(4145,id2, me, imageInByte);
        WriteThread myT3 = new WriteThread(8290,id3, me, imageInByte); //8290
        WriteThread myT4 = new WriteThread(12435,id4, me, imageInByte); //12435
        WriteThread myT5 = new WriteThread(16580,id5, me, imageInByte); //16580
        
        myT1.myDfs = deFiler;
        myT2.myDfs = deFiler;
        myT3.myDfs = deFiler;
        myT4.myDfs = deFiler;
        myT5.myDfs = deFiler;
        myT1.start();
        myT2.start();
        myT3.start();
        myT4.start();
        myT5.start();
        
    }
   


}
