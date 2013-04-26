import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import common.Constants;
import common.DFileID;
import dblockcache.DBufferCache;
import dfs.DFS;


public class OverWriteTest extends Thread
{

    DFS myDfs;


    public static void main (String[] args)
        throws FileNotFoundException,
            IOException

    {

        DFS deFiler = new DFS();

        DBufferCache cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS);
        deFiler.myCache = cache;

        VirtualDisk disk = new VirtualDisk("Sean's Disk", false);
        cache.myVD = disk;
        cache.myVD.clear();
        OverWriteTest myT = new OverWriteTest();
        myT.myDfs = deFiler;
        DiskThread diskThread = new DiskThread(disk);
        diskThread.start();
        myT.start();
    }


    public void run ()
    {

        byte[] read = new byte[4];
        byte[] write = ByteBuffer.allocate(4).putInt(99).array();
        byte[] overwrite = ByteBuffer.allocate(4).putInt(101).array();
        

        DFileID dFID = myDfs.createDFile();
        try
        {
        	System.out.println("We will test writing a file, then writing over it");
            System.out.println("writing 4 byte representation of 99");
            myDfs.write(dFID, write, 0, 4);
            myDfs.read(dFID, read, 0, 4);
            for (int i = 0; i < 4; i++)
            {
               
                System.out.print(read[i] + " ");
            }
            System.out.println();
            System.out.println("writing 4 byte representation of 101 over that file");
            myDfs.write(dFID, overwrite, 0, 4);
            
            myDfs.read(dFID, read, 0, 4);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Reading the file it should be a 4 byte reprensentation of 101");
        for (int i = 0; i < 4; i++)
        {           
            System.out.print(read[i] + " ");
        }

    }
}
