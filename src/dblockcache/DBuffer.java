package dblockcache;
import java.io.IOException;
import java.nio.ByteBuffer;

import virtualdisk.VirtualDisk;
import common.Constants;
import common.Constants.DiskOperationType;


public class DBuffer {
	public int ID; //if ID is less than NUM_OF_INODE_BLOCKS, this is a buffer for an Inode block
	public boolean isValid; //true if Dbuffer contains content from disk
	public boolean isClean; //true is the DBuffer contents have been backed up on disk
	public byte[] contents; //the actual data contents in the Dbuffer
	public boolean held; //true if I/O operation in progress
	public boolean pinned; //true is DFS has called getBlock and not released yet
	
	public DBuffer(int _ID, boolean _isValid, boolean _isClean) {
		ID = _ID;
		isValid = _isValid;
		isClean = _isClean;
		held = false;
		pinned = false;
		contents = new byte[Constants.BLOCK_SIZE];
	}
	
	/* Start an asynchronous fetch of associated block from the volume */
	public void startFetch(VirtualDisk VD) throws IllegalArgumentException, IOException {
		pinned = true;
		VD.startRequest(this, DiskOperationType.READ);
	}
	
	
	/* Start an asynchronous write of buffer contents to block on volume */
	public void startPush(VirtualDisk VD) throws IllegalArgumentException, IOException {
		pinned = true;
		VD.startRequest(this, DiskOperationType.WRITE);
	}
	
	/* Check whether the buffer has valid data */ 
	public boolean checkValid() {
		if (isValid) {return true;}
		else {return false;}
	}
	
	/* Wait until the buffer has valid data, i.e., wait for fetch to complete */
	public synchronized boolean waitValid(){
		while(!isValid) {
			try { 
				wait(); 
			} catch(InterruptedException e) { 
				System.out.println("InterruptedException caught"); 
			} 
		}
		return true;
	}
	
	/* Check whether the buffer is dirty, i.e., has modified data written back to disk? */
	public boolean checkClean() {
		return isClean;
	}
	
	/* Wait until the buffer is clean, i.e., wait until a push operation completes */
	public synchronized boolean waitClean() {
		while(!isClean) {
			try { 
				wait(); 
			} catch(InterruptedException e) { 
				System.out.println("InterruptedException caught"); 
			} 
		}
		return true;
	}
	
	/* Check if buffer is evictable: not evictable if I/O in progress, or buffer is held */
	public boolean isBusy() {
		if (held) return false;
		if (pinned) return false;
		return true;
	}


	public int read(byte[] buffer, int startOffset, int count) {
		//if (!isValid) return -1;
		int len = contents.length;
		if (count > len) {count = len-startOffset;}
		int index = startOffset+count;
		int j=0;
		for (int i = startOffset; i<index; i++) {
			buffer[j] = contents[i];
			j++;
		}
		return count;
	}


	public int write(byte[] buffer, int startOffset, int count) {
		int len = contents.length;
		if (count > len) {count = len-startOffset;}
		int index = startOffset+count;
		int j = 0;
		for (int i = startOffset; i<index; i++) {
			contents[i] = buffer[j];
			j++;
		}
		isClean = false;
		return count;	
	}
	
	/* An upcall from VirtualDisk layer to inform the completion of an IO operation */
	public synchronized void ioComplete(DiskOperationType operation) {
		if (operation == DiskOperationType.READ) isValid = true;
		else isClean = true;
		pinned = false;
		notifyAll();
	}
	
	/* An upcall from VirtualDisk layer to fetch the blockID associated with a startRequest operation */
	public int getBlockID() {
		return ID;
	}
	
	/* An upcall from VirtualDisk layer to fetch the buffer associated with DBuffer object*/
	public byte[] getBuffer() {
		return contents;
	}
	
	public String print(){
		String cont = "";
		for (int i = 0; i<contents.length; i+=4) {
			byte[] integer = new byte[4];
			integer[0] = contents[i];
			integer[1] = contents[i+1];
			integer[2] = contents[i+2];
			integer[3] = contents[i+3];
			ByteBuffer wrapped = ByteBuffer.wrap(integer);
	        int file_ID = wrapped.getInt();
	        cont = cont+ " " + file_ID;
		}
		
        return cont;
	}
	
}
