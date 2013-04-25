package virtualdisk;

import java.io.IOException;

import virtualdisk.VirtualDisk;


public class DiskThread extends Thread{
	private VirtualDisk myDisk;
	public DiskThread(VirtualDisk disk){
		myDisk = disk;
		
	}
	public void run(){
		//myDisk.clear();
		while(true){
			try {
				myDisk.doReadRequest();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				myDisk.doWriteRequest();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}



