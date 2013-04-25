import java.io.FileNotFoundException;
import java.io.IOException;

import virtualdisk.DiskThread;
import virtualdisk.VirtualDisk;
import common.Constants;

import dblockcache.DBufferCache;
import dfs.DFS;


public class TestLoad

{

	public static void main(String[] args) throws FileNotFoundException, IOException

	{
		
		DFS deFiler = new DFS();
		DBufferCache cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS);
		deFiler.myCache = cache;
		VirtualDisk disk = new VirtualDisk("Seans disk", false);
		cache.myVD = disk;
		DiskThread diskThread = new DiskThread(disk);
		diskThread.start();

		ThreadLoad three = new ThreadLoad(deFiler);
		three.start();

	}

}