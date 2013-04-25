import java.io.IOException;

import common.DFileID;
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
			System.out.println("running Thread Load");
			try {
				myDfs.init();
			} catch (IllegalArgumentException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Inodecounter: " + myDfs.myCache.nextINodeCounter);
			myDfs.myCache.checkMap(0);
			myDfs.myCache.checkMap(1);
		
		
//		byte[] empty = new byte[100];
//		for(int i = 1; i < 4; i++){
//			try {
//				myDfs.read(new DFileID(i), empty, 0, 100);
//				System.out.println("#######################");
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			}
//		System.out.println("55th is: "+ empty[55]);

	}
}

