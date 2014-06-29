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
	private Map<String,String> replace;
	private JSONArray json;
	private String jsonText;

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
				System.out.println("Comunicacao nao completa");
				return true;
			}
		}
		else
			return false;
	}


	// Vai buscar o json ao servidor e faz download do youtube os videos a transmitir para os clientes
	private boolean readJsonFromUrl(String url) throws IOException{

		URL ur;
		BufferedReader in = null;
		try{
			ur = new URL(url);
			in = new BufferedReader(new InputStreamReader(ur.openStream(), "UTF-8"));
		} catch(Exception e){
			System.out.println("URL nao disponivel");
		}

		JSONObject jsobject = null;
		boolean done = false;

		try {
			jsonText = in.readLine();
			json = new JSONArray(jsonText);
			for(int i = 0; i < json.length(); i++){
				jsobject = json.getJSONObject(i);

				if(jsobject.get("video") != null || jsobject.get("video") != ""){
					String[] tmp = dir("videos");
					boolean found = false;
					for(int j = 0; j < tmp.length; j++ )
					{
						if(!tmp[j].equals(".DS_Store"))
						{
							String[] split = tmp[j].split("\\.(?=[^\\.]+$)");

							if(jsobject.get("id").equals(split[0]))
								found = true;	
						}
					}
					if(!found)
						downloadVideo(jsobject);
				}
			}
			verifyVideos();
			done = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	in.close();
	return done;

}

private void verifyVideos() throws RemoteException, InfoNotFoundException, JSONException{

	//		String[] tmp = dir("videos");
	//		for(int j = 0; j < tmp.length; j++ ){
	//			String[] split = tmp[j].split("\\.(?=[^\\.]+$)");
	//			boolean found = false;
	//			for(int i = 0; i< json.length(); i++){
	//				if(split[0].equals(json.getJSONObject(i).get("id")))
	//					found = true;
	//			}
	//			if(!found)
	//				cleanVideoFromDir(tmp[j]);
	//
	//		}
}

private void cleanVideoFromDir(String video) throws RemoteException, InfoNotFoundException{

	File file = new File("videos/" + video);
	file.delete();
}

// Faz download dos videos do youtube
private boolean downloadVideo(JSONObject json) throws JSONException{

	String videoURL = null;
	String id = null;
	try {
		//			if(json.get("video").contains("http"))
		//			{
		videoURL = String.valueOf(json.get("video"));
		id = String.valueOf(json.get("id"));
		String[] cmd = new String[4];
		cmd[0] = "python";
		cmd[1] = "/Users/joaopinto/Documents/videosPython/get_videos.py";
		cmd[2] = videoURL;
		cmd[3] = id;

		executeCommand(cmd);
		return true;
		//			}

	} catch (Exception e) {
		// TODO Auto-generated catch block
		System.out.println("Erro a fazer download");
		e.printStackTrace();
		return false;
	}

	//		return false;	
}

// Executa o comando de python na consola para fazer o download dos videos
private void executeCommand(String[] command) {

	//StringBuffer output = new StringBuffer();

	Process p;
	try {
		p = Runtime.getRuntime().exec(command);
		//p.waitFor();
		//			BufferedReader reader = 
		//					new BufferedReader(new InputStreamReader(p.getInputStream()));

		//			String line = "";			
		//			while ((line = reader.readLine())!= null) {
		//				output.append(line + "\n");
		//			}	

	} catch (Exception e) {
		e.printStackTrace();
	}

	//return output.toString();

}

private byte[] copyFile(String fromPath)
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
	else{
		f.mkdir();
		return f.list();
	}
}

private String stringConverter() throws JSONException, RemoteException, InfoNotFoundException{

	JSONArray js = json;
	String result = jsonText;
	for(int i = 0; i< js.length(); i++){
		JSONObject j = (JSONObject) js.get(i);
		if(j.get("video") != null || j.get("video") != ""){
			String[] dir = dir("videos");
			for(int x = 0; x< dir.length; x++){
				String[] split = dir[x].split("\\.(?=[^\\.]+$)");
				if(split[0].equals(String.valueOf(j.get("id")))){
					result = result.replace(String.valueOf(j.get("video")), dir[x]);
					
				}
			}
		}
	}

	//		Iterator<String> it = replace.keySet().iterator();
	//		
	//		while(it.hasNext()){
	//			String fileName = it.next();
	//			String url = replace.get(fileName);
	//			tmp.replaceAll(url, fileName);
	//		}
	return result;
}

private boolean clientCommunication() throws RemoteException{

	if(!serversListIP.isEmpty()){
		Iterator<String> it = serversListIP.keySet().iterator();
		while(it.hasNext()){
			String serverName = it.next();
			String ip = serversListIP.get(serverName);

			ITVClient client;
			try 
			{
				client = (ITVClient) Naming.lookup("//" + ip + "/" + serverName);
				client.receiveJson(stringConverter());
				String[] tmp = dir("videos");
				byte[] file;
				for(int i = 0; i<tmp.length; i++)
				{
					file = copyFile("videos/" + tmp[i]);
					client.pasteFile(file, "videos_client/" + tmp[i]);
				}
				return true;
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
				return false;
			} 
		}
	}
	return true;

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

	final Proxy proxy = new Proxy();
	Naming.rebind("/trabalhoPI", proxy);
	String ip = InetAddress.getLocalHost().getHostAddress().toString();
	System.out.println( "Proxy running in " + ip + " ...");	

	Timer timer = new Timer();
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run(){

			try {
				System.out.println("Actualizacao do sistema!");
				proxy.readJsonFromUrl("http://localhost:3000/contents.json");
				proxy.clientCommunication();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}, 0, 2000*60);

}


}