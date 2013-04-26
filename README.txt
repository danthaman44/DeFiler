                                                                     
                                                                     
                                                                     
                                             
/**********************************************
 * Please DO NOT MODIFY the format of this file
 **********************************************/

/*************************
 * Team Info & Time spent
 *************************/

	Name1: Sean Miller 	
	NetId1: spm23	 	
	Time spent: 40 hours 	

	Name2: Alex Bruce 	
	NetId2: ajb60	 	
	Time spent: 40 hours  

	Name3: Dan Deng 	
	NetId3: wdd3	 	
	Time spent: 40 hours 	

/******************
 * Files to submit
 ******************/
	ClientThread.java
	TestMain.java
	Constants.java
	DFileID.java
	DFS.java
	DiskThread.java
	VirtualDisk.java
	DBuffer.java
	DBufferCache.java


/************************
 * Implementation details
 *************************/
 
Testing

We have included five tests with our implementation of DeFiler.
We have submitted a separate jar file for each test. All the jars can
be run from the command line. Also, all test threads continue
indefinitely until control-c is pressed. Press control-c to stop a test

***OverWriteTest***
To run, type "java -jar overwrite.jar" into the command line
Writes a number to the disk, then writes over the same file with another number. 
We read the file on disk and see that is completely and only the second number written

***LRUTest***
To run, type "java -jar LRUTest.jar" into the command line
Demonstrates our LRU policy. Writes ten files to a cache of size ten. Prints the cache after each file is created to
show that new files are at front of cache, older files at back. After each write, an older file is read,
and brought to the front of the cache. An 11th file is written, evicting the last file (107) in the cache.
A read is then called on this file, so it is brought back to cache, while another block (108) is evicted

***TestCreate/TestLoad***
Two part test prints the cache, the blockMap of free blocks, and fileMap across runs:

First run TestCreate with "java -jar testcreate.jar"
At first everything is empty. Then four new files are written. The cache, blockMap, and fileMap
are printed again to reflect these changes. Everything is saved to disk

Then run TestLoad with "java -jar testload.jar"
The disk from the TestCreate is loaded, demonstrating persistance. Two files are deleted,
then cache, blockMap, and fileMap are printed again to reflect these changes

These tests should be run sequentially.

***WriteImageTest/ReadImageTest***
Our big compilation test. Here we demonstrate that an image can be written in pieces by different user threads each to separate files, written to disk, and then recreated from those files using multiple threads after a system restart.
In order to test this:
The WriteImageTest.jar and "A+reportcardsmall.jpg" need to be in the same folder
Run WriteImageTest with "java -jar WriteImageTest.jar"
This will write to disk 5 parts of the image

Then run ReadImageTest with "java -jar ReadImageTest.jar"
This will recreate the original image and save it as "ThreadTestImage.jpg"
from command line open "ThreadedTestImage.jpg"
Compare this with the original image
with the created "ThreadTestImage.jpg" to confirm this worked.

There are some print lines in this test. They may be ignored.

 
DFS*****************************************************************************************

The DFS stores a hashmap, fileMap, which maps file id's to the physical blocks. This map is initialized
from the disk's inodes upon startup of the entire system. The DFS also keeps a counter of the number of files
created so far, DFileIDCounter, read from the superblock. The DFS is where the client can request all 
Create, Read, Update, Delete (CRUD) operations. Create increments the DFileIDCounter, and adds the new file to the fileMap. 
Destroy takes a fileId, and removes that entry from fileMap. 
Read reads 'count' bytes from the given file to the given buffer, starting at index startOffset in that buffer. If count is less than the size of the file, it only reads the file size. It returns how many bytes it read, or -1 if there is an error. 
Write writes 'count' bytes from the given buffer to the given file reading starting at index startOffset in the buffer.
The first 4 bytes of each file store the size of that file in bytes.

For these operations the DFS uses fileMap to look up which block IDs it needs, then requests them from the cache.
The DFS explicitly calls sync and and releaseBlock to ensure all written content is
backed up to disk, and then DBuffers are freed. Save should be called by the client before shutdown. It saves the fileMap
to disk as Inodes, it saves the cache level blockMap, and it saves the file counter to the super block.

Inodes. Each inode has three parts: file_id, file_size, and pointers to blocks. Inodes can vary in size, as files vary in number of blocks. We fit many inodes into each block in the inode region. The Inode region is 1 tenth of all block space. All of these constants are determined in the Constants file.
 
DBuffer***********************************************************************************

Our DBuffer follows the prescribed API. The DBuffer has multiple class variables. isClean is
true if the contents of the buffer have been backed to disk by sync, otherwise isClean is false. 
isValid is true if a buffer contains content that has been fetched from disk, otherwise it is false. 
A buffer is pinned if it has been requested by the DFS. Each buffer has a byte[] called contents. This
is where any and all data is stored. The buffer interacts with the disk through the startPush (writing data
to disk) and startFetch (reading data from disk) methods. Both methods pin the buffer until iocomplete is
called from the disk. Only one client thread can access a buffer at one time. Each buffer interacts with the DFS through the read and write methods. 
The both methods uses a loop to copy the contents of the DBuffer to/from a byte[] provided by the DFS. ioComplete
is a method called from the disk level. The buffer becomes valid, clean, and can now be accessed by another
thread. All threads operating on a buffer will wait until the buffer is valid or clean, depending on the operation type.
 
DBufferCache ********************************************************************************

Our cache uses an arrayList bufList to store buffer objects. When the DFS wished to perform a CRUD (Create, Read, Update, Delete)
operation on a file, getBlock or an equivalent method will be called. The getBlock method is called when the DFS
is reading or writing to a file that already exist. The DFS will look up the block ids associated with a file
in fileMap call getBlock for each block. getBlock scans the arrayList of DBuffers. If the buffer for the desired block
does not exist, a new block is created with the appropriate ID. A disk IO operation is then initiated to
fetch content from the VirtualDisk through startFetch. The Dbuffer becomes held, and cannot be accessed by other threads
until iocomplete is signaled from disk, and releaseBlock is called. The getBuffToWriteTo method is extremely similar to getBlock,
except that it used when new files are created. getBuffToWriteTo checks blockMap, an int[] held by the cache. blockMap is an
array of ones an zeros representing the availability of data blocks in the disk. A 0 at index 5 in blockMap means
that the fifth data block is available to be given to a new file. getInodeBufToWriteTo is another similar method, that 
returns a Dbuffer to store Inode data. The id assigned to these inode buffers is calculated from
the number of files. 

When the system is saved before shut down, getInodeBuf is called
to store the Inodes, which are interpreted from the DFS's filemap. All three methods make a block held, which means no
other client threads can access it. A block is held until releaseBlock is called. All three methods are sync

All three methods follow the same LRU policy. A requested DBuffer is moved to the front of bufList. 
If the cache is full (aka, bufList has reached maxSize), the last block in the buflist (the LRU buf)
is evicted. The method sync is called after all CRUD operations by the client, it backs up the contents of every DBuffer
in the cache into the disk. The cache does not persist across runs. 

The DBufferCache class also supports methods (GetCounts, SetCounts) for reading and writing the superblock to and from disk. 
setCounts takes in the number of files, which is being updated at the DFS level. These methods seamlessly
convert integers to bytes and back, allowing for smooth storage at the disk level. When destroy is called
by the DFS, the appropriate indexes in the blockMap are changed to 0, indicating those physical blocks
are free again.

The method destroy is triggered from the DFS level, when the client . At the cache level, destroy removes



Virtual Disk ******************************************************************************** 
Our disk structure is as follows:

Block 0 is the superblock. It stores one integer, the number of files currently written to disk. This number is sent to the cache upon initialization

Blocks 1 - NUM_OF_INODE_BLOCKS are the Inode blocks. Each stores INODES_PER_BLOCK inodes, with a one-to-one
relation between Inodes and files. An Inode contains the file id, the number of physical blocks storing that file,
and pointers to those blocks.

Blocks NUM_OF_INODES+1 - (NUM_OF_INODES+NUM_OF_MAP_BLOCKS) are the map blocks. They each store a piece of the cache's blockMap.
When the cache is initialized, blockMap is constructed from these blocks

Blocks DATA_BLOCK_FIRST-NUM_OF_BLOCKS are the data blocks. They store file contents.
The size of each file is stored in its first indexed data block (not its Inode block)

The disk keeps a read queue and a write queue. Requests are added to the appropriate queue based on
their classification. The disk class itself calls doReadRequest and doWriteRequest, which in turn 
call read and write. The DiskThread is set up to alternate processing requests from each queue, unless 
one of the queues is empty. The VirtualDisk also contains a clear method, which calls format store. 
This method can be used to reset the disk at any point. 
 
Client Thread*******************************************************************************
 
These threads simulate users who wish to perform operations on files. 
 