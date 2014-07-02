package pi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.json.*;

public interface ITVClient extends Remote {

	
	public void receiveJson(String array, String serverURL) throws RemoteException, IOException, InfoNotFoundException;
	
	public boolean pasteFile(byte[] f, String toPath)
			throws RemoteException, IOException;
	
	public void cleanVideoFromDir(String video) throws RemoteException, InfoNotFoundException;
	
	
}
