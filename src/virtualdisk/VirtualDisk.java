package virtualdisk;
/*
 * VirtualDisk.java
 *
 * A virtual asynchronous disk.
 *
 */

import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import common.Constants;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public class VirtualDisk implements IVirtualDisk {

	private String _volName;
	private RandomAccessFile _file;
	private int _maxVolSize;
	private Queue<DBuffer> readQ;
	private Queue<DBuffer> writeQ;

	/*
	 * VirtualDisk Constructors
	 */
	public VirtualDisk(String volName, boolean format) throws FileNotFoundException,
			IOException {

		_volName = volName;
		_maxVolSize = Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS;
		readQ = new LinkedList<DBuffer>();
		writeQ = new LinkedList<DBuffer>();

		/*
		 * mode: rws => Open for reading and writing, as with "rw", and also
		 * require that every update to the file's content or metadata be
		 * written synchronously to the underlying storage device.
		 */
		_file = new RandomAccessFile(_volName, "rws");

		/*
		 * Set the length of the file to be NUM_OF_BLOCKS with each block of
		 * size BLOCK_SIZE. setLength internally invokes ftruncate(2) syscall to
		 * set the length.
		 */
		_file.setLength(Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS);
		if(format) {
			formatStore();
		}
		/* Other methods as required */
	}
	
	public VirtualDisk(boolean format) throws FileNotFoundException,
	IOException {
		this(Constants.vdiskName, format);
	}
	
	public VirtualDisk() throws FileNotFoundException,
	IOException {
		this(Constants.vdiskName, false);
	}
	
	public void doReadRequest() throws IOException{
		if(readQ.peek() != null){
			DBuffer buffer = readQ.remove();
			readBlock(buffer);
		}
	}
	public void doWriteRequest() throws IOException{
		if(writeQ.peek() != null){
			DBuffer Wbuffer = writeQ.remove();
			writeBlock(Wbuffer);
		}
		
	}
	
	public void clear() { //resets the contents of the file
		formatStore();
	}

	/*
	 * Start an asynchronous request to the underlying device/disk/volume. 
	 * -- buf is an DBuffer object that needs to be read/write from/to the volume.	
	 * -- operation is either READ or WRITE  
	 */
	public synchronized void startRequest(DBuffer buf, DiskOperationType operation) throws IllegalArgumentException,
	IOException {
		if (operation == DiskOperationType.READ) {
			readQ.add(buf);
		}
		else {
			writeQ.add(buf);
		}
		notifyAll();
	}
	/*
	 * Clear the contents of the disk by writing 0s to it
	 */
	private void formatStore() {
		byte b[] = new byte[Constants.BLOCK_SIZE];
		setBuffer((byte) 0, b, Constants.BLOCK_SIZE);
		for (int i = 0; i < Constants.NUM_OF_BLOCKS; i++) {
			try {
				int seekLen = i * Constants.BLOCK_SIZE;
				_file.seek(seekLen);
				_file.write(b, 0, Constants.BLOCK_SIZE);
			} catch (Exception e) {
				System.out.println("Error in format: WRITE operation failed at the device block " + i);
			}
		}
	}

	/*
	 * helper function: setBuffer
	 */
	private static void setBuffer(byte value, byte b[], int bufSize) {
		for (int i = 0; i < bufSize; i++) {
			b[i] = value;
		}
	}

	/*
	 * Reads the buffer associated with DBuffer to the underlying
	 * device/disk/volume
	 */
	private int readBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		/* Boundary check */
		if (_maxVolSize < seekLen + Constants.BLOCK_SIZE) {
			return -1;
		}
		_file.seek(seekLen);
		int data= _file.read(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
		buf.ioComplete(DiskOperationType.READ);
		return data;
	}

	/*
	 * Writes the buffer associated with DBuffer to the underlying
	 * device/disk/volume
	 */
	private void writeBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		_file.seek(seekLen);
		_file.write(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
		buf.ioComplete(DiskOperationType.WRITE);
	}
}
