package pi;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.json.*;

public class Proxy extends UnicastRemoteObject implements IProxy{
	

	private static final long serialVersionUID = 1L;
	private static String basePath = ".";
	private Map<String, String> serversListIP;
	
	public Proxy() throws RemoteException{
		super();
		serversListIP = new HashMap<String, String>();
	}
	
	@Override
	public boolean registerServer(String serverName, String serverIP) throws RemoteException
	{		
		if(!serversListIP.containsKey(serverName))
		{
			serversListIP.put(serverName, serverIP);
			return true;
		}
		else
			return false;
	}

	private boolean readJsonFromUrl(String url) throws IOException{

		URLConnection ur = new URL(url).openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(ur.getInputStream()));
		String jsonText;
		JSONObject json = null;
		JSONArray array = null;
		JSONStringer parser = null;
		while ((jsonText = in.readLine()) != null) {
			try {
				array = new JSONArray(jsonText);
				for(int i = 0; i < array.length(); i++){
					json = array.getJSONObject(i);
					downloadVideo(json);	
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		in.close();
		return false;
	}

	public boolean downloadVideo(JSONObject json) throws JSONException{
		
		String videoURL = null;
		String id = null;
		try {
			if(json.getString("video") != null){
				videoURL = json.getString("video");
				id = json.getString("id");
			}
				
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(videoURL);
		
		//chamar o python com este videoURL
		return false;	
	}
	
	private String executeCommand(String command) {
		 
		StringBuffer output = new StringBuffer();
 
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = 
                            new BufferedReader(new InputStreamReader(p.getInputStream()));
 
                        String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
 
		} catch (Exception e) {
			e.printStackTrace();
		}
 
		return output.toString();
 
	}
	
	public byte[] copyFile(String fromPath)
			throws IOException {
		try {
			File f = new File(basePath, fromPath);
			InputStream input = new FileInputStream(f);
			byte[] buffer = new byte[(int) f.length()];
			input.read(buffer);
			input.close();
			return buffer;
		} catch (FileNotFoundException e) {
			System.out.println("Erro ao copiar o ficheiro. Nao encontrado");
			return null;
		}
	}
	
	

	public static void main(String[] args) throws Exception {
		
		if( args.length != 0) {
			System.out.println("Use: java trab1.Proxy");
			return;
		}


//		System.getProperties().put( "java.security.policy", "pi/policy.all");
//		
//		if( System.getSecurityManager() == null) {
//			System.setSecurityManager( new RMISecurityManager());
//		}
//		
//		try { // start rmiregistry
//			LocateRegistry.createRegistry( 1099);
//		} catch( RemoteException e) { 
//			// if not start it
//			// do nothing - already started with rmiregistry
//		}
		
		Proxy proxy = new Proxy();
//		Naming.rebind("/trabalhoPI", proxy);
//		String ip = InetAddress.getLocalHost().getHostAddress().toString();
//		System.out.println( "Proxy running in " + ip + " ...");
		
		
		
		String command = "cd";
		String output1 = proxy.executeCommand(command);
		

		System.out.println(output1);
		
		
		
		//boolean tmp = p.readJsonFromUrl("http://localhost:3000/contents.json");


	}

	
}