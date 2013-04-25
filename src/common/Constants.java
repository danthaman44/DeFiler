package common;

/*
 * This class contains the global constants used in DFS
 */

public class Constants {

	/* The below constants indicate that we have approximately 268 MB of
	 * disk space with 67 MB of memory cache; a block can hold upto 32 inodes and
	 * the maximum file size is constrained to be 500 blocks. These are compile
	 * time constants and can be changed during evaluation.  Your implementation
	 * should be free of any hard-coded constants.  
	 */

	public static final int NUM_OF_BLOCKS = 1024; //262144; // 2^18
	public static final int BLOCK_SIZE = 1024; // 1kB

	public static final int NUM_OF_CACHE_BLOCKS = 65536; // 2^16
	public static final int MAX_FILE_SIZE = BLOCK_SIZE*50; // Constraint on the max file size
	public static final int INODE_SIZE = (4*MAX_FILE_SIZE/BLOCK_SIZE)+8; //32 Bytes
	public static final int MAX_DFILES = 512; // For recylcing DFileIDs
	public static final int NUM_OF_INODE_BLOCKS = NUM_OF_BLOCKS/10; //number of physical blocks that contain Inodes
	public static final int NUM_OF_DATA_BLOCKS = NUM_OF_BLOCKS - NUM_OF_INODE_BLOCKS; //number of physical blocks that contain data
	public static final int INODES_PER_BLOCK = BLOCK_SIZE/INODE_SIZE; //the # of inodes we can store in a physical block
	public static final int NUM_OF_MAP_BLOCKS = NUM_OF_DATA_BLOCKS/(BLOCK_SIZE/4);
	public static final int DATA_BLOCK_FIRST = NUM_OF_INODE_BLOCKS+NUM_OF_MAP_BLOCKS+1; //the index of the first data block
	
	/* DStore Operation types */
	public enum DiskOperationType {
		READ, WRITE
	};

	/* Virtual disk file/store name */
	public static final String vdiskName = "DSTORE.dat";
}
