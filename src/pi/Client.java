package pi;

import java.io.*;

import org.json.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;

public class Client extends UnicastRemoteObject implements ITVClient{


	private static final long serialVersionUID = 1L;
	private static String basePath = ".";
	private String serverName;
	private String proxyURL;
	private String ip;
	private JSONArray array;

	public Client(String serverName, String proxyURL, String ip) throws RemoteException{
		this.serverName = serverName;
		this.proxyURL = proxyURL;
		this.ip = ip;
		array = null;
	}



	public static void register (String serverName, String proxyURL, String ip)
	{
		IProxy server;

		try
		{
			server = (IProxy) Naming.lookup("//" + proxyURL + "/trabalhoPI");
			boolean success = server.registerServer(serverName, ip);
			if(!success)
				System.exit(0);
		} 
		catch (Exception e) 
		{
			System.out.println("Proxy nao encontrado no endereco fornecido");
			System.exit(0);
		}		
	}

	public void receiveJson(String jarray, String serverURL) throws IOException, RemoteException, InfoNotFoundException{

		String fName = "data.js";
		File file = new File(fName);

		if(!file.exists())
			file.createNewFile();
		Writer out = new BufferedWriter(new OutputStreamWriter(
			    new FileOutputStream(fName), "UTF-8"));
			try {
				String content = "var data = " + jarray + ";" + "\n" + "var server = " + "\"" + "http://" + serverURL +":3000" + "\"" + ";";
			    out.write(content);
			} finally {
			    out.close();
			}

	}
	
	public void cleanVideoFromDir(String video) throws RemoteException, InfoNotFoundException{

		File file = new File("videos_client/" + video);
		file.delete();
	}

	// Lista a directoria
	private String[] dir(String dir) throws RemoteException, InfoNotFoundException {
		File f = new File(new File(basePath), dir);
		
		if(f.exists())
			return f.list();
		else{
			f.mkdir();
			return f.list();
		}
	}

	public boolean pasteFile(byte[] f, String toPath)
			throws RemoteException, IOException {
		try{
			dir("videos_client");
			File file = new File(basePath, toPath);
			OutputStream out = new FileOutputStream(file);
			out.write(f);
			out.close();
			return true;
		} catch(Exception e){
			System.out.println("Erro na gravacao do ficheiro");
			return false;
		}
	}


	public static void main( String[] args) throws Exception
	{
		if( args.length != 2){
			System.out.println("Use: java pi.Client serverName contactServerURL");
			return;
		}

		try { // start rmiregistry
			LocateRegistry.createRegistry( 1099);
		} catch( RemoteException e) { 
			// if not start it
			// do nothing - already started with rmiregistry
		}

		String serverName = args[0];	
		String ip = InetAddress.getLocalHost().getHostAddress().toString();
		String proxyURL = args[1];

		try{

			ITVClient client = new Client(serverName, proxyURL, ip);
			Naming.rebind("/" + serverName, client);
		} 
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Erro ao criar Client");
			System.exit(0);
		}

		register(serverName, proxyURL, ip);
		System.out.println("Client RMI running in " + ip + " ...");


	}
}

