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
	private Map <String, ArrayList<String>> clientsFiles;
	private JSONArray json;
	private String jsonText;
	private static String serverURL;

	public Proxy() throws RemoteException{
		super();
		serversListIP = new HashMap<String, String>();
		clientsFiles = new HashMap<String,ArrayList<String>>();
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
			for(int i = 0; i < json.length(); i++)
			{
				jsobject = json.getJSONObject(i);

				if(jsobject.get("video") != null || jsobject.get("video") != "")
				{
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

		String[] tmp = dir("videos");
		for(int j = 0; j < tmp.length; j++ ){
			String[] split = tmp[j].split("\\.(?=[^\\.]+$)");
			boolean found = false;
			for(int i = 0; i< json.length(); i++){
				if(split[0].equals(String.valueOf(json.getJSONObject(i).get("id")))){
					if(String.valueOf(json.getJSONObject(i).get("video")).contains("youtube")){
						found = true;
					}
				}
			}
			if(!found)
				cleanVideoFromDir(tmp[j]);

		}
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

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);

		} catch (Exception e) {
			e.printStackTrace();
		}

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

	private byte[] downloadUrl(String toDownload) throws MalformedURLException {
		
		URL u = new URL(toDownload);
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	    try {
	        byte[] chunk = new byte[4096];
	        int bytesRead;
	        InputStream stream = u.openStream();

	        while ((bytesRead = stream.read(chunk)) > 0) {
	            outputStream.write(chunk, 0, bytesRead);
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }

	    return outputStream.toByteArray();
	}
	
	public boolean pasteFile(String toPath)
			throws RemoteException, IOException {
		try{
			byte[] f = downloadUrl("");
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

		private String stringConverter() throws JSONException, RemoteException, InfoNotFoundException{

			JSONArray js = json;
			String result = jsonText;
			for(int i = 0; i< js.length(); i++){
				JSONObject j = (JSONObject) js.get(i);
				if(j.get("video") != null || j.get("video") != ""){
					String[] dir = dir("videos");
					for(int x = 0; x< dir.length; x++){
						String[] split = dir[x].split("\\.(?=[^\\.]+$)");
						if(split[0].equals(String.valueOf(j.get("id"))))
							result = result.replace(String.valueOf(j.get("video")), dir[x]);
					}
				}
			}
			return result;
		}

		private boolean clientCommunication() throws RemoteException{
			if(!serversListIP.isEmpty())
			{

				Iterator<String> it = serversListIP.keySet().iterator();
				while(it.hasNext())
				{
					String serverName = it.next();
					String ip = serversListIP.get(serverName);

					ITVClient client;
					try 
					{
						client = (ITVClient) Naming.lookup("//" + ip + "/" + serverName);
						client.receiveJson(stringConverter(), serverURL);
						String[] tmp = dir("videos");
						byte[] file;

						if(clientsFiles.containsKey(serverName))
						{
							for(int i = 0; i<tmp.length; i++)
							{
								if(!clientsFiles.get(serverName).contains(tmp[i]))
								{
									file = copyFile("videos/" + tmp[i]);
									client.pasteFile(file, "videos_client/" + tmp[i]);
									clientsFiles.get(serverName).add(tmp[i]);
								}				
							}

							Iterator<String> i = clientsFiles.get(serverName).iterator();
							ArrayList<String> filesToDel = new ArrayList<String>();
							boolean found = false;
							while(i.hasNext())
							{
								String arrayFile = i.next();
								for( int j = 0; j< tmp.length; j++)
								{
									if(arrayFile.equals(tmp[j]))
									{
										found = true;
									}
								}

								if(!found)
								{
									client.cleanVideoFromDir(arrayFile);	
									filesToDel.add(arrayFile);
								}
							}

							Iterator<String> fToDel = filesToDel.iterator();
							while(fToDel.hasNext())
							{
								clientsFiles.get(serverName).remove(fToDel.next());
							}	
						}
						else{
							ArrayList<String> arraylist = new ArrayList<String>();
							for(int i = 0; i<tmp.length; i++)
							{
								file = copyFile("videos/" + tmp[i]);
								client.pasteFile(file, "videos_client/" + tmp[i]);
								arraylist.add(tmp[i]);
							}
							clientsFiles.put(serverName, arraylist);
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

			if( args.length != 1) {
				System.out.println("Use: java trab1.Proxy server");
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
			serverURL = args[0];
			final Proxy proxy = new Proxy();
			Naming.rebind("/trabalhoPI", proxy);
			String ip = InetAddress.getLocalHost().getHostAddress().toString();
			System.out.println("Proxy running in " + ip + " ...");	

			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask(){
				public void run(){

					try {
						System.out.println("-------------------------------------------------------------------");
						System.out.println("Actualizacao do sistema!");
						proxy.readJsonFromUrl("http://" + serverURL + ":3000/playlist_items.json");
						new Timer().schedule(new TimerTask(){
							public void run()
							{
								try {
									proxy.clientCommunication();
								} catch (RemoteException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}, 3000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, 0, 1000*60);

		}


	}