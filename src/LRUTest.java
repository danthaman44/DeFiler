import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import common.DFileID;
import dblockcache.DBufferCache;
import dfs.DFS;


public class LRUTest extends Thread
{
    DFS myDfs;


    public static void main (String[] args)
        throws FileNotFoundException,
            IOException

    {

        DFS deFiler = new DFS();

        DBufferCache cache = new DBufferCache(10);
        deFiler.myCache = cache;

        VirtualDisk disk = new VirtualDisk("Sean's Disk", false);
        cache.myVD = disk;
        LRUTest myT = new LRUTest();
        myT.myDfs = deFiler;
        DiskThread diskThread = new DiskThread(disk);
        diskThread.start();
        myT.start();
    }


    public void run ()
    {
        try
        {   
        	System.out.println("We will demo our LRU policy by filling up our cache");
            byte[] first = ByteBuffer.allocate(4).putInt(25).array();
            byte[] second = ByteBuffer.allocate(4).putInt(35).array();
            byte[] empty = new byte[4];
            ArrayList<DFileID> dif = new ArrayList<DFileID>();
            for (int i = 0; i < 10; i++)
            {
                DFileID dFID = myDfs.createDFile();
                myDfs.write(dFID, first, 0, 4);
                dif.add(dFID);
                
                for(int j =0; j<myDfs.myCache.bufList.size();j++)
                {
                    if(j==0)System.out.println("This is the cache contents:");
                    System.out.print(myDfs.myCache.bufList.get(j).ID+" ");
                    
                }System.out.println();
                System.out.println("reading file 106, it should move to the front");
                myDfs.read(dif.get(0), empty, 0, 4);
                empty = new byte[4];
                
                for(int j =0; j<myDfs.myCache.bufList.size();j++)
                {
                    if(j==0)System.out.println("The cache now looks like:");
                    System.out.print(myDfs.myCache.bufList.get(j).ID+" ");
                    
                }
                System.out.println();
                System.out.println();

                
            }
            myDfs.sync();
            
            DFileID dFID = myDfs.createDFile();
            myDfs.write(dFID, second, 0, 4);
            System.out.println("Cache size exceeded. File 107 will be evicted because it was the least recently used");
            for(int j =0; j<myDfs.myCache.bufList.size();j++)
            {
                if(j==0)System.out.println("The cache now looks like:");
                System.out.print(myDfs.myCache.bufList.get(j).ID+" ");
                
            }
            System.out.println();
            System.out.println("107 is being fetched from disk. 107 will be brought back and 108 will be evicted");
            myDfs.read(dif.get(1), empty, 0, 4);
            for(int j =0; j<myDfs.myCache.bufList.size();j++)
            {
                if(j==0)System.out.println("The cache now looks like:");
                System.out.print(myDfs.myCache.bufList.get(j).ID+" ");
                
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
