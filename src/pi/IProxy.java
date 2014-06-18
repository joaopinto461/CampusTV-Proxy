package pi;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IProxy extends Remote{
	
	
	public boolean registerServer(String serverName, String serverIP) throws RemoteException;
	


}
