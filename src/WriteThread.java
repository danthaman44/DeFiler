import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import common.DFileID;
import dfs.DFS;


public class WriteThread extends Thread
{
 
    DFS myDfs;
    int offset;
    DFileID id;
    WriteImageTest myManager;
    byte[] myImage;
    
    public WriteThread(int start, DFileID id1, WriteImageTest manager, byte[] imageData){
        offset=start;
        id =id1;
        myManager = manager;
        myImage = imageData;
    }
    public void run ()
    {
        try
        {
            
            myDfs.write(id, myImage, offset, 4145);
            myManager.finished();
            
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
