package dfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import common.Constants;
import common.DFileID;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;


public class DFS
{

    private boolean _format;
    private String _volName;
    public HashMap<Integer, ArrayList<Integer>> fileMap; // FileID to list of
                                                          // blockID's
    public DBufferCache myCache;
    private int DFileIDCounter;


    /*
     * @volName: Explicitly overwrite volume name
     * @format: If format is true, the system should erase the underlying disk
     * contents and reinitialize the volume.
     */

    DFS (String volName, boolean format)
    {
        _volName = volName;
        _format = format;
        fileMap = new HashMap<Integer, ArrayList<Integer>>();
        DFileIDCounter = 0;
    }


    public DFS (boolean format)
    {
        this(Constants.vdiskName, format);
    }


    public DFS ()
    {
        this(Constants.vdiskName, false);
    }


    /*
     * Initialize all the necessary structures with sizes as specified in the
     * common/Constants.java
     */
    public void init () throws IllegalArgumentException, IOException
    {
        DFileIDCounter = myCache.getCounts();
        myCache.getAllMapBlocks();
        loadINodes();
    }


    public void save () throws IllegalArgumentException, IOException
    {
        //myCache.cacheBlockMap();
        saveINodes();
        myCache.storeAllMapBlocks();
        myCache.storeCounts(fileMap.size());
    }


    /*
     * creates a new DFile and returns the DFileID, which is useful to uniquely
     * identify the DFile
     */
    public synchronized DFileID createDFile ()
    {
        DFileIDCounter += 1;
        DFileID id = new DFileID(DFileIDCounter);
        fileMap.put(id.getDFileID(), new ArrayList<Integer>());
        return id;
    }


    /* destroys the file specified by the DFileID */
    public void destroyDFile (DFileID dFID)
    {
        for (Integer blockID : fileMap.get(dFID.getDFileID()))
        {
            myCache.destroy(blockID);
        }
        fileMap.remove(dFID.getDFileID());
    }


    /*
     * reads the file dfile named by DFileID into the buffer starting from the
     * buffer offset startOffset; at most count bytes are transferred
     */
    public int read (DFileID dFID, byte[] buffer, int startOffset, int count)
        throws IllegalArgumentException,
            IOException
    {
    	if(buffer.length<startOffset+count){
    		return -1;
    	}
        int bytesRead = 0;
        int bytesToBeRead = count+4;
        int file_size = -1;
        byte[] dataArray = new byte[startOffset + count];
        boolean firstBuf = true;
        for (int blockID : fileMap.get(dFID.getDFileID()))
        {
            DBuffer dbuffer = myCache.getBlock(blockID);
            if (bytesRead < bytesToBeRead)
            {
            	if(firstBuf){
            		// read file size
            		byte[] fileSize = new byte[4];
                    dbuffer.read(fileSize, 0, 4);
                    ByteBuffer wrapped = ByteBuffer.wrap(fileSize);
                    file_size = wrapped.getInt();
                    if(file_size<count){
                    	return -1;
                    }
                    // read data
                    int bytesToRead = Math.min(Constants.BLOCK_SIZE-4, Math.min(file_size, count));
                    // read this block
                    byte[] tograb = new byte[bytesToRead];

                    dbuffer.read(tograb, 4, bytesToRead);
                    myCache.releaseBlock(dbuffer);
                
                    for (int i = 0; i < bytesToRead; i++)
                    {
                    	dataArray[i+startOffset] = tograb[i];
                    }
                bytesRead += bytesToRead;
                firstBuf = false;
            	}
            	else{
            		// not first buffer
            		int bytesToRead = Math.min(Constants.BLOCK_SIZE, file_size-bytesRead);
                    // read this block
                    byte[] tograb = new byte[bytesToRead];
                    dbuffer.read(tograb, 0, bytesToRead);
                    myCache.releaseBlock(dbuffer);
                
                    for (int i = 0; i < bytesToRead; i++)
                    {
                    	dataArray[bytesRead + i+startOffset] = tograb[i];
                    }
                bytesRead += bytesToRead;
            	}
            }

            myCache.releaseBlock(dbuffer);
        }
        for (int i = startOffset; i < dataArray.length; i++)
        {
            buffer[i] = dataArray[i];
        }
        return Math.min(file_size,count);

    }


    /*
     * writes to the file specified by DFileID from the buffer starting from the
     * buffer offset startOffset; at most count bytes are transferred
     */
    public int write (DFileID dFID, byte[] buffer, int startOffset, int count)
        throws IllegalArgumentException,
            IOException
    {
    	if(buffer.length<startOffset+count){
    		return -1;
    	}
        int skipped = 0;
        int bytesWritten = 0;
        boolean writing = false;
        if (fileMap.get(dFID.getDFileID()).size() == 0)
        { // NEW FILE
        	
        	// bytes to be written for this file
        	int bytesNeeded = count+4; 
        	// bufs needed for this file
            int bufsNeeded =
                (int) Math.ceil((float) bytesNeeded / (float) Constants.BLOCK_SIZE); 
            // bytes to be written for this buf
            int bytesToBeWritten = Math.min(Constants.BLOCK_SIZE, (count + 4)); 
            
             // Write first Buffer
            	byte[] dataBuffer = new byte[bytesToBeWritten];
            	byte[] fileSize = ByteBuffer.allocate(4).putInt(count).array();
                    for (int m = 0; m < 4; m++)
                    {
                        dataBuffer[m] = fileSize[m];
                    }
                // dataBuffer now contains the file size. Now add some data
                for (int j = 4; j < bytesToBeWritten; j++)
                {
                    dataBuffer[j] = buffer[startOffset + j-4];
                }
                DBuffer dbuffer = myCache.getBufToWriteTo();
                dbuffer.write(dataBuffer, 0, bytesToBeWritten);
                fileMap.get(dFID.getDFileID()).add(dbuffer.getBlockID());
                bytesWritten += bytesToBeWritten;
                
                myCache.sync();
                myCache.releaseBlock(dbuffer);
            
            // Now write the rest of the data
            for (int i = 1; i < bufsNeeded; i++)
            {
                dbuffer = myCache.getBufToWriteTo();
                if (bytesWritten < bytesNeeded)
                {
                    // write
                	bytesToBeWritten = Math.min(Constants.BLOCK_SIZE, (bytesNeeded- bytesWritten)); 
                    dataBuffer = new byte[bytesToBeWritten];
                    for (int j = 0; j < bytesToBeWritten; j++)
                    {
                        dataBuffer[j] = buffer[startOffset + j + bytesWritten - 4];
                    }
                    dbuffer.write(dataBuffer, 0, bytesToBeWritten);
                    bytesWritten += bytesToBeWritten;
                    fileMap.get(dFID.getDFileID()).add(dbuffer.getBlockID());
                    myCache.sync();
                    myCache.releaseBlock(dbuffer);
                }
                else{
                    
                }
            }
        }

        else
        { // writing over a file
        	int bytesNeeded = count+4; 
        	// bufs needed for this file
        	int bufsNeeded =
                    (int) Math.ceil((float) bytesNeeded / (float) Constants.BLOCK_SIZE); 
        	boolean firstTime = true;
            for (int blockID : fileMap.get(dFID.getDFileID()))
            {
            	DBuffer dbuffer = myCache.getBlock(blockID);
            	if(firstTime){
            	
                  // bytes to be written for this buf
                 int bytesToBeWritten = Math.min(Constants.BLOCK_SIZE, (bytesNeeded-bytesWritten)); 
                 byte[] dataBuffer = new byte[bytesToBeWritten];
             	 byte[] fileSize = ByteBuffer.allocate(4).putInt(count).array();
                 for (int m = 0; m < 4; m++)
                 {
                      dataBuffer[m] = fileSize[m];
                 }
                 for (int j = 4; j < bytesToBeWritten; j++)
                 {
                     dataBuffer[j] = buffer[startOffset + j-4];
                 }
                    
            	dbuffer.write(dataBuffer, 0, bytesToBeWritten);
            	bytesWritten += bytesToBeWritten;
                
                myCache.sync();
                myCache.releaseBlock(dbuffer);
                firstTime=false;
            	
            	}
            	else
                {
            		if (bytesWritten < bytesNeeded){
            			// write
                    	int bytesToBeWritten = Math.min(Constants.BLOCK_SIZE, (bytesNeeded- bytesWritten)); 
                        byte[] dataBuffer = new byte[bytesToBeWritten];
                        for (int j = 0; j < bytesToBeWritten; j++)
                        {
                            dataBuffer[j] = buffer[startOffset + j + bytesWritten - 4];
                        }
                        dbuffer.write(dataBuffer, 0, bytesToBeWritten);
                        bytesWritten += bytesToBeWritten;
                        fileMap.get(dFID.getDFileID()).add(dbuffer.getBlockID());
                        myCache.sync();
                        myCache.releaseBlock(dbuffer);
            		}
            		
            		}
                    
            }
        }
        return 0;

    }


    /*
     * List all the existing DFileIDs in the volume
     */
    public List<DFileID> listAllDFiles ()
    {
        ArrayList<DFileID> files = new ArrayList<DFileID>();
        for (Integer id : fileMap.keySet())
        {
            files.add(new DFileID(id));
        }

        return files;
    }


    /* Write back all dirty blocks to the volume, and wait for completion. */
    public void sync () throws IllegalArgumentException, IOException
    {
        myCache.sync();
    }


    public void saveINodes () throws IllegalArgumentException, IOException
    {
        float iNodesWritten = 0;
        float iNodesInBlock = Constants.BLOCK_SIZE / Constants.INODE_SIZE;
        int offset = 0;
        DBuffer dbuffer = null;
        ArrayList<DBuffer> bufsUsed = new ArrayList<DBuffer>();
        for (Integer file : fileMap.keySet())
        {
            if (iNodesWritten % iNodesInBlock == 0)
            { // new block
                dbuffer = myCache.getInodeBufToWriteTo();
                offset = 0;
                bufsUsed.add(dbuffer);
            }

            byte[] toBeWritten = new byte[Constants.INODE_SIZE];

            // write FileID
            byte[] fileID = ByteBuffer.allocate(4).putInt(file).array();
            for (int k = 0; k < 4; k++)
            {
                toBeWritten[k] = fileID[k];
            }
            // write FileSize
            int blocksInFile = fileMap.get(file).size();
            byte[] fileSize =
                ByteBuffer.allocate(4).putInt(blocksInFile).array();
            for (int m = 0; m < 4; m++)
            {
                toBeWritten[m + 4] = fileSize[m];
            }

            // write BlockID's
            int i = 8;
            for (Integer blockID : fileMap.get(file))
            {
                byte[] bytes = ByteBuffer.allocate(4).putInt(blockID).array();
                for (int j = 0; j < 4; j++)
                {
                    toBeWritten[i + j] = bytes[j];
                }
                i += 4; // we have written one blockID
            }
            int size = 8 + blocksInFile * 4;
            dbuffer.write(toBeWritten, offset, size);
            offset += size;
            DFileIDCounter += 1;
            iNodesWritten++;

        }
        myCache.sync();
        for(DBuffer buf: bufsUsed){
        	myCache.releaseBlock(buf);
        }
    }


    public void loadINodes () throws IllegalArgumentException, IOException
    {
        fileMap = new HashMap<Integer, ArrayList<Integer>>();
        int numberOfFiles = DFileIDCounter;

        // Get All INodes
        ArrayList<DBuffer> iNodeBuffers = new ArrayList<DBuffer>();
        for (int i = 0; i < Math.ceil((float) numberOfFiles / Constants.INODES_PER_BLOCK); i++)
        {
            iNodeBuffers.add(myCache.getBlock(i+1));
        }
        int iNodesRead = 0;

        for (int j = 1; j < iNodeBuffers.size() + 1; j++)
        {
            DBuffer iNodeBlock = iNodeBuffers.get(j-1);
            int lastBytePointer = 0;
            while (iNodesRead < numberOfFiles && iNodesRead < j * Constants.INODES_PER_BLOCK)
            {
                // Get File ID
                byte[] fileID = new byte[4];
                iNodeBlock.read(fileID, lastBytePointer, 4);
                ByteBuffer wrapped = ByteBuffer.wrap(fileID);
                int file_ID = wrapped.getInt();
                lastBytePointer += 4;

                ArrayList<Integer> blockIDs = new ArrayList<Integer>();
                fileMap.put(file_ID, blockIDs);

                // Get File Size
                byte[] file_size = new byte[4];
                iNodeBlock.read(file_size, lastBytePointer, 4);
                lastBytePointer += 4;
                ByteBuffer wrapped2 = ByteBuffer.wrap(file_size);
                int fileSize = wrapped2.getInt(); // file size in # of Blocks

                // Get Blocks in File
                ArrayList<Integer> blocks =
                    getBlocksForINode(iNodeBlock, lastBytePointer, fileSize);
                fileMap.get(file_ID).addAll(blocks);
                lastBytePointer += (fileSize * 4);

                iNodesRead++;
            }
            myCache.releaseBlock(iNodeBlock);
        }
    }


    private ArrayList<Integer> getBlocksForINode (DBuffer iNodeBlock,
                                                  int lastBytePointer,
                                                  int blocksInFile)
    {
        ArrayList<Integer> blocks = new ArrayList<Integer>();

        for (int i = 0; i < blocksInFile; i++)
        {
            byte[] data = new byte[4];
            iNodeBlock.read(data, lastBytePointer, 4);
            lastBytePointer += 4;

            ByteBuffer wrapped2 = ByteBuffer.wrap(data);
            int blockID = wrapped2.getInt();
            blocks.add(blockID);

        }
        return blocks;

    }
}
