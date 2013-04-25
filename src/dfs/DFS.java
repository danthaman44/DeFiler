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
        myCache.getCounts();
        DFileIDCounter = myCache.nextINodeCounter;
        myCache.getAllMapBlocks();
        loadINodes();
    }


    public void save () throws IllegalArgumentException, IOException
    {
        //myCache.cacheBlockMap();
        saveINodes();
        System.out.println("saved i nodes");
        myCache.storeAllMapBlocks();
        myCache.storeCounts(fileMap.size());
        System.out.println("finished saving");
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
        // if offset = 400 and each block holds 70, start in 5th buffer at
        // offset 50.
        int skipped = 0;
        int endPointer = 0;
        int bytesRead = 0;
        boolean reading = false;
        byte[] dataArray = new byte[count];
        for (int blockID : fileMap.get(dFID.getDFileID()))
        {
            DBuffer dbuffer = myCache.getBlock(blockID);
            if (bytesRead < count)
            {
                int seansabitch = Math.min(Constants.BLOCK_SIZE, (count - bytesRead));
                // read this block
                byte[] tograb = new byte[Constants.BLOCK_SIZE]; // 70 - 50

                dbuffer.read(tograb, 0, seansabitch);
                myCache.releaseBlock(dbuffer);
                
                for (int i = 0; i < seansabitch; i++)
                {
                    dataArray[bytesRead + i] = tograb[i];
                }
                bytesRead += seansabitch;
            }
            else
            {
                skipped += Constants.BLOCK_SIZE;
            }

            myCache.releaseBlock(dbuffer);
        }
        for (int i = 0; i < count; i++)
        {
            buffer[i] = dataArray[i];
        }
        return 0;

    }


    /*
     * writes to the file specified by DFileID from the buffer starting from the
     * buffer offset startOffset; at most count bytes are transferred
     */
    public int write (DFileID dFID, byte[] buffer, int startOffset, int count)
        throws IllegalArgumentException,
            IOException
    {
        int skipped = 0;
        int bytesWritten = 0;
        boolean writing = false;
        if (fileMap.get(dFID.getDFileID()).size() == 0)
        { // NEW FILE

            int bufsNeeded =
                (int) Math.ceil((float) count / (float) Constants.BLOCK_SIZE);
            for (int i = 0; i < bufsNeeded; i++)
            {
                DBuffer dbuffer = myCache.getBufToWriteTo();

                if (bytesWritten < count)
                {
                    // write
                    byte[] dataBuffer = new byte[Constants.BLOCK_SIZE];
                    for (int j = 0; j < Math.min(Constants.BLOCK_SIZE, count -
                                                                       bytesWritten); j++)
                    {
                        dataBuffer[j] = buffer[j + bytesWritten];
                    }
                    dbuffer.write(dataBuffer, 0, count - bytesWritten);
                    bytesWritten +=
                        Math.min(Constants.BLOCK_SIZE, count - bytesWritten);
                }
                fileMap.get(dFID.getDFileID()).add(dbuffer.getBlockID());
                myCache.sync();
                myCache.releaseBlock(dbuffer);
            }
        }

        else
        { // writing over a file
            for (int blockID : fileMap.get(dFID.getDFileID()))
            {
                if (writing)
                {
                    if (bytesWritten < count)
                    {
                        // read this block
                        DBuffer dbuffer = myCache.getBufToWriteTo();
                        dbuffer.write(buffer, bytesWritten, count -
                                                            bytesWritten);
                        bytesWritten +=
                            Math.min(Constants.BLOCK_SIZE, count - bytesWritten);
                    }
                }
                else if (startOffset < skipped + Constants.BLOCK_SIZE)
                {
                    // this is the first block we write to
                    DBuffer dbuffer = myCache.getBufToWriteTo();
                    int offset = startOffset - skipped;
                    dbuffer.write(buffer, 0, count);
                    bytesWritten += Math.min(Constants.BLOCK_SIZE, count);
                    writing = true;
                    myCache.sync();
                    myCache.releaseBlock(dbuffer);
                }
                else
                {
                    skipped += Constants.BLOCK_SIZE;
                }
            }
        }
        return 0;

    }


    /* returns the size in bytes of the file indicated by DFileID. */
    public int sizeDFile (DFileID dFID)
    {
        return 0;
    }


    /*
     * List all the existing DFileIDs in the volume
     */
    public List<Integer> listAllDFiles ()
    {
        ArrayList<Integer> files = new ArrayList<Integer>();
        for (Integer id : fileMap.keySet())
        {
            files.add(id);
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
        for (Integer file : fileMap.keySet())
        {
            if (iNodesWritten % iNodesInBlock == 0)
            { // new block
                dbuffer = myCache.getInodeBufToWriteTo();
                offset = 0;
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
            System.out.println("This file has" + fileMap.get(file).size() +
                               "blocks");
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
            myCache.sync();
            myCache.releaseBlock(dbuffer);

        }
    }


    public void loadINodes () throws IllegalArgumentException, IOException
    {
        fileMap = new HashMap<Integer, ArrayList<Integer>>();
        int numberOfFiles = myCache.nextINodeCounter;
        System.out.println("number of files, from super block " + numberOfFiles);

        // Get All INodes
        ArrayList<DBuffer> iNodeBuffers = new ArrayList<DBuffer>();
        float iNodesInBlock = Constants.BLOCK_SIZE / Constants.INODE_SIZE;

        for (int i = 0; i < Math.ceil((float) numberOfFiles / iNodesInBlock); i++)
        {
            iNodeBuffers.add(myCache.getBlock(i+1));
        }
        System.out.println("got this many blocks" + iNodeBuffers.size());
        int iNodesRead = 0;

        for (int j = 1; j < iNodeBuffers.size() + 1; j++)
        {
            DBuffer iNodeBlock = iNodeBuffers.get(j - 1);
            int lastBytePointer = 0;
            while (iNodesRead < numberOfFiles && iNodesRead < j * 4)
            {
                // Get File ID
                byte[] fileID = new byte[4];
                iNodeBlock.read(fileID, lastBytePointer, 4);
                lastBytePointer += 4;
                ByteBuffer wrapped = ByteBuffer.wrap(fileID);
                int file_ID = wrapped.getInt();

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