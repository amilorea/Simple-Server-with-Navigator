import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server implements Runnable {
	
	static boolean debug = true;
	
	private static int portMasterServer;
	private int portSubserver;
	private ServerSocket server;
	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private boolean serverStatus;
	private Integer numClient;
	
	public static void main(String[] args) {
		portMasterServer = Integer.parseInt(args[0]);
		println("Start Subserver");
		new Thread( new Server() ).start();
	}
	
	public static void println(String str) {
		if(debug) System.out.println(str);
	}
	
	public static void print(String str) {
		if(debug) System.out.print(str);
	}
	
	public static void error(String str) {
		System.err.println(str);
	}
	
	public Server() {
		try {
			server = new ServerSocket(0);
			println("Request to connect on port " + portMasterServer);
			
			socket = new Socket("localhost", portMasterServer);
			portSubserver = server.getLocalPort();
			println("Succesfully connect on port " + portMasterServer);
			
			dis = new DataInputStream( socket.getInputStream() );
			dos = new DataOutputStream( socket.getOutputStream() );
			
			serverStatus = true;
			numClient = 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error("Failed to init Subserver");
			e.printStackTrace();
		}
	}
	
	int getPort() {
		return portSubserver;
	}
	
	DataOutputStream getDos() {
		return dos;
	}
	
	void removeWorker() {
		synchronized(numClient) {
			numClient -= 1;
		}
		println("Number of Worker: " + numClient);
		if(numClient == 0 && serverStatus == false)
			System.exit(1);
	}
	
	void stopServer() {
		serverStatus = false;
		try {
			server.close();
			println("Sucessfully close Subserver on port " + portSubserver);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error("Error stopping Subserver on port " + portSubserver);
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Heartbeat h = new Heartbeat(this);
			h.start();
			
			dos.writeUTF("init@@@" + portSubserver);
			while(serverStatus) {
				if(serverStatus == false) {
					error("Failed");
					break;
				}
				Worker w = new Worker(this, server.accept());
				w.start();
				synchronized(numClient) {
					numClient += 1;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error("Failed to create Worker");
			e.printStackTrace();
		}
		println("Server closed");
	}
}

class Worker extends Thread {
	private Server server;
	private Socket socket;
	
	private DataInputStream dis;
	private DataOutputStream dos;
	
	private boolean workerStatus;
	private long serverTime;
	
	public Worker(Server server, Socket s) {
		this.server = server;
		this.socket = s;
		
		workerStatus = true;
		serverTime = System.currentTimeMillis();
		
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			server.println("Worker initialized at port " + server.getPort());
		}
		catch ( Exception e ) {
			server.error("Worker failed to init at port " + server.getPort());
			e.printStackTrace();
		}
	}
	
	public void println(String str) {
		server.println(str);
	}
	
	public void print(String str) {
		server.print(str);
	}
	
	public void error(String str) {
		server.error(str);
	}
	
	void stopWorker() {
		workerStatus = false;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while(workerStatus && !socket.isClosed()) {
				String message = dis.readUTF();
				int pos = message.indexOf("@@@");
				try {
					if(pos <= 0)
						throw new Exception();
					
					String cm = message.substring(pos + 3);
					String sm = message.substring(0, pos);
					System.out.println(sm + " | " + cm);
					if(sm.equals("gettime")) {
						dos.writeLong(System.currentTimeMillis() - serverTime);
					} else if(sm.equals("calculate")) {
						Map<Character, Integer> m = new HashMap<Character, Integer>(26);
						
						for(int i = 0; i < cm.length(); i++){
						    char c = cm.charAt(i);
	
						    if (!m.containsKey(c)){
						        m.put(c, 1);
						    } else {
						        m.put(c, m.get(c) + 1);
						    }
						}
	
						int max = 0;
						char ch = ' ';
						Iterator< Map.Entry<Character, Integer> > it = m.entrySet().iterator();
					    while (it.hasNext()) {
					        Map.Entry<Character, Integer> p = (Map.Entry<Character, Integer>)it.next();
					        if(max < (Integer)p.getValue()) {
					        	ch = (Character)p.getKey();
					        	max = (Integer)p.getValue();
					        }
					        it.remove(); // avoids a ConcurrentModificationException
					    }
	
					    dos.writeChar(ch);
					    dos.writeLong(max);
					} else if(sm.equals("stop")) {
						server.stopServer();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					server.error("Worker command exception at port " + server.getPort());
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			server.error("Worker IO exception at port " + server.getPort());
			server.removeWorker();
			e.printStackTrace();
		}
		server.println("Worker stopped at port " + server.getPort());
	}
	
}

class Heartbeat extends Thread {
	private static int HEARTBEAT_RATE = 2000;
	private static int HEARTBEAT_LAST = 3000;
	
	private DataOutputStream dos;
	private Server server;

	public Heartbeat(Server server){
		this.server = server;
		server.println("Heartbeat started");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			dos = server.getDos();
			int cycle = 0;
			while(true) {
				synchronized(dos) {
					dos.writeUTF("alive@@@" + cycle);
					cycle++;
				}
				Thread.currentThread().sleep(HEARTBEAT_RATE);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			server.error("Server stopped");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			server.error("Heartbeat interrupted");
			e.printStackTrace();
		}
		server.println("Heartbeat stopped");
		server.stopServer();
		
		try {
			Thread.currentThread().sleep(HEARTBEAT_LAST);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
