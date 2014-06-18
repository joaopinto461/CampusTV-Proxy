package pi;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.Naming;
import java.rmi.NotBoundException;
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
	private JSONArray json;

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
			if(clientCommunication())
				return true;
			else{
				System.out.println("Comunicação nao completa");
				return true;
			}
		}
		else
			return false;
	}
	

	// Vai buscar o json ao servidor e faz download do youtube os videos a transmitir para os clientes
	private boolean readJsonFromUrl(String url) throws IOException{

		URLConnection ur = new URL(url).openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(ur.getInputStream()));
		String jsonText;
		JSONObject jsobject = null;
		boolean done = false;
		while ((jsonText = in.readLine()) != null) {
			try {
				json = new JSONArray(jsonText);
				for(int i = 0; i < json.length(); i++){
					jsobject = json.getJSONObject(i);
					downloadVideo(jsobject);	
				}
				done = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		in.close();
		return done;

	}

	// Faz download dos videos do youtube
	public boolean downloadVideo(JSONObject json) throws JSONException{

		String videoURL = null;
		String id = null;
		try {
			if(json.getString("video").contains("http"))
			{
				videoURL = json.getString("video");
				id = String.valueOf(json.get("id"));
				String command = "python";
				String pathToScript = "/Users/joaopinto/Documents/videosPython/get_videos.py";
				System.out.println(command + " " + pathToScript + " " + videoURL + " " + id);
				String output1 = executeCommand(command + " " + pathToScript + " " + videoURL + " " + id);
				System.out.println("OUTPUT: " + output1);
			}
			else{
				System.out.println("Conteudo sem video");
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Erro a fazer download");
			e.printStackTrace();
		}

		return false;	
	}

	// Executa o comando de python na consola para fazer o download dos videos
	private String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			//p.waitFor();
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

	// Lista a directoria
	private String[] dir(String dir) throws RemoteException, InfoNotFoundException {
		File f = new File(new File(basePath), dir);
		if(f.exists())
			return f.list();
		else
			throw new InfoNotFoundException("Directoria nao encontrada: " + dir);
	}

	public boolean clientCommunication() throws RemoteException{

		Iterator<String> it = serversListIP.keySet().iterator();
		while(it.hasNext()){
			String serverName = it.next();
			String ip = serversListIP.get(serverName);

			try 
			{
				ITVClient client = (ITVClient) Naming.lookup("//" + ip + "/" + serverName);
				client.receiveJson(json);
				String[] tmp = dir("videos");
				byte[] file;
				for(int i = 0; i<tmp.length; i++)
				{
					file = copyFile("videos/" + tmp[i]);
					client.pasteFile(file, "videos_client/" + tmp[i]);
				}
				return true;
			} catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} 
		}
		return true;

	}
	
	// Metodo executado de x em x tempo
	private TimerTask resetContents() throws IOException{
		
		readJsonFromUrl("http://localhost:3000/contents.json");
		clientCommunication();
		return null;	
	}



	public static void main(String[] args) throws Exception {

		if( args.length != 0) {
			System.out.println("Use: java trab1.Proxy");
			return;
		}


		System.getProperties().put( "java.security.policy", "pi/policy.all");

		if( System.getSecurityManager() == null) {
			System.setSecurityManager( new RMISecurityManager());
		}

		try { // start rmiregistry
			LocateRegistry.createRegistry( 1099);
		} catch( RemoteException e) { 
			// if not start it
			// do nothing - already started with rmiregistry
		}

		Proxy proxy = new Proxy();
		Naming.rebind("/trabalhoPI", proxy);
		String ip = InetAddress.getLocalHost().getHostAddress().toString();
		System.out.println( "Proxy running in " + ip + " ...");	
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(proxy.resetContents(), 0, 1000*60*60);

	}


}