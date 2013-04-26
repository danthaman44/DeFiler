import java.io.IOException;

import common.DFileID;
import dblockcache.DBuffer;
import dfs.DFS;


	public class ThreadLoad extends Thread

	{

		DFS myDfs;

		public ThreadLoad(DFS dfs)

		{
			myDfs = dfs;

		}

		public void run()
		{
			System.out.println("running test load");
			try {
				myDfs.init();
			} catch (IllegalArgumentException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Files in our system. We should have four files from test create");
			for (int k: myDfs.fileMap.keySet()) {
				System.out.println("File id = " + k);
			}
			
			System.out.println("Printing cache, should be empty except for super block and map blocks");
			for (DBuffer buf: myDfs.myCache.bufList) {
				System.out.println("BufferID: "+buf.ID);
			}
			System.out.println("Checking block map in cache, should be all 1s");
			myDfs.myCache.checkMap(0);
			myDfs.myCache.checkMap(1);
			myDfs.myCache.checkMap(2);
			myDfs.myCache.checkMap(3);
					
			System.out.println("Deleting middle two files, files 2 and 3");
			DFileID fileID = new DFileID(2);
			myDfs.destroyDFile(fileID);
			
			DFileID fileID2 = new DFileID(3);
			myDfs.destroyDFile(fileID2);
			
			System.out.println("Checking block map in cache after deletion, should be 1 0 0 1");
			myDfs.myCache.checkMap(0);
			myDfs.myCache.checkMap(1);
			myDfs.myCache.checkMap(2);
			myDfs.myCache.checkMap(3);
			
			System.out.println("Checking cache after file deletion");
			System.out.println("Checking block map in cache, indexes 1 and 2 should be 0");
			System.out.println("Files in our system after delete. Files 2 and 3 should be gone");
			for (int k: myDfs.fileMap.keySet()) {
				System.out.println("File id = " + k);
			}
		


	}
}

