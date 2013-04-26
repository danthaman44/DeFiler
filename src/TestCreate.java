import java.io.FileNotFoundException;
import java.io.IOException;

import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import common.Constants;

import dblockcache.DBufferCache;
import dfs.DFS;


public class TestCreate

{

	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		DFS deFiler = new DFS();
		System.out.println("running test create");
		System.out.println("Index of first data block: "+ Constants.DATA_BLOCK_FIRST);
		System.out.println("Number of map blocks: "+ Constants.NUM_OF_MAP_BLOCKS);
		System.out.println("Number of Inode blocks: "+ Constants.NUM_OF_INODE_BLOCKS); //not the number of inodes, the number of blocks used to store them
		DBufferCache cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS);
		deFiler.myCache = cache;
		
		VirtualDisk disk = new VirtualDisk("Seans disk", false);
		cache.myVD = disk;
		cache.myVD.clear();
		DiskThread diskThread = new DiskThread(disk);
		diskThread.start();

		ClientThread client1 = new ClientThread(deFiler);
		client1.start();
	}

}