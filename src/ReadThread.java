import common.DFileID;
import dfs.DFS;


public class ReadThread extends Thread
{
    int myOffset;
    int myFile;
    byte[] myImage;
    DFS myDfs;
    ReadImageTest myManager;
    
    
    public ReadThread(int start, int file, ReadImageTest manager)
    {
        myOffset= start;
        myFile = file;
        myImage = new byte [4145];
        myManager = manager;
    }
    public void run(){
        try{
        
        DFileID id = new DFileID(myFile);
        myDfs.read(id, myImage, 0, 4145);
        myManager.finished(myImage, myOffset);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
