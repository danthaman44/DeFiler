import java.io.IOException;
import java.util.ArrayList;

import common.Constants;
import common.DFileID;

import dblockcache.DBuffer;
import dfs.DFS;

public class ClientThread extends Thread

{

	DFS myDfs;

	public ClientThread(DFS dfs)

	{
		myDfs = dfs;

	}

	public void run()
	{
		System.out.println("Client thread starting");

		if (myDfs.myCache.bufList.isEmpty()) 
			System.out.println("Cache empty!"); //this should go off
		try {
			myDfs.init();
			System.out.println("finished loading");
		} catch (IllegalArgumentException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (DBuffer buf: myDfs.myCache.bufList) {
			System.out.println("BufferID: "+buf.ID);
		}
		System.out.println("Checking cache before file creation");
		System.out.println("Inode block counter: " + myDfs.myCache.nextINodeCounter);
		myDfs.myCache.checkMap(0);
		myDfs.myCache.checkMap(1);
		myDfs.myCache.checkMap(2);
		myDfs.myCache.checkMap(3);

		
		
		byte[] write = new byte[100];
		DFileID fileID1 = myDfs.createDFile();
		DFileID fileID2 = myDfs.createDFile();
		DFileID fileID3 = myDfs.createDFile();
		DFileID fileID4 = myDfs.createDFile();
		try {
			myDfs.write(fileID1, write, 0, 100);
			System.out.println("File 1 ID: " + fileID1.getDFileID());
			System.out.println("file1 written");
			myDfs.write(fileID2, write, 0, 100);
			System.out.println("File 2 ID: " + fileID2.getDFileID());
			System.out.println("file2 written");
			myDfs.write(fileID3, write, 0, 100);
			System.out.println("File 3 ID: " + fileID2.getDFileID());
			System.out.println("file3 written");
			myDfs.write(fileID4, write, 0, 100);
			System.out.println("File 4 ID: " + fileID2.getDFileID());
			System.out.println("file4 written");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (DBuffer buf: myDfs.myCache.bufList) {
			System.out.println("BufferID: "+buf.ID);
		}
		System.out.println("Checking cache after file creation");
		System.out.println("Inode block counter: " + myDfs.myCache.nextINodeCounter);
		myDfs.myCache.checkMap(0);
		myDfs.myCache.checkMap(1);
		myDfs.myCache.checkMap(2);
		myDfs.myCache.checkMap(3);


		System.out.println("File Map size: "+ myDfs.fileMap.keySet().size());
		for (int k: myDfs.fileMap.keySet()) {
			System.out.println("file: "+ k);
		}
		
		
		System.out.println("Deleting a file");
		DFileID fileID = new DFileID(1);
		myDfs.destroyDFile(fileID);
		System.out.println("Checking cache after file deletion");
		System.out.println("Inode block counter: " + myDfs.myCache.nextINodeCounter);
		myDfs.myCache.checkMap(0);
		myDfs.myCache.checkMap(1);
		myDfs.myCache.checkMap(2);
		myDfs.myCache.checkMap(3);
		for (DBuffer buf: myDfs.myCache.bufList) {
			System.out.println("BufferID: "+buf.ID);
		}
		
		try {
			System.out.println("DFS saving contents");
		    myDfs.save();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

  }

}