import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MasterServer implements Runnable {
	static boolean debug = true;
	
	private static ServerSocket mServer;
	private static String serverPort;
	
	private HashMap<Integer, Integer> servers;
	private int serverCount = 3;
	
	public static void main(String[] args) {
		serverPort = args[0];
		new Thread( new MasterServer(Integer.parseInt(serverPort)) ).start();
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

	public MasterServer(int port) {
		try {
			servers = new HashMap<Integer, Integer>();
			mServer = new ServerSocket(port);
			for(int i = 0; i < serverCount; i++) {
				activeSubserver();
			}
		} catch( Exception e ) {
			error("Error setup Subserver!");
			e.printStackTrace();
		}
	}

	public void activeSubserver() {
		ProcessBuilder pb = new ProcessBuilder("init.bat", serverPort);
		println("Now generate subserver");
		Process process;
		try {
			process = pb.start();
			int errCode = process.waitFor();
			println("Subserver started with error? " + (errCode == 0 ? "No" : "Yes"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error("Generate subserver failed");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			error("Subserver responding is interrupted");
			e.printStackTrace();
		}
	}
	
	public void addSubserver(int port) {
		servers.put(port, 1);
		println("Successfully added Subserver at port " + port);
	}
	
	public void removeSubserver(int port) {
		servers.remove(port);
		println("Successfully removed Subserver at port " + port);
	}
	
	public Set<Integer> getKeyset() {
		return servers.keySet();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		int cnt = 0;
		while(true) {
			try {
				// Status checker
				Navigator nv1 = new Navigator(this, mServer.accept(), cnt);
				nv1.start();
				cnt += 1;
			} catch (IOException e) {
				error("New navigator denied");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class Navigator extends Thread {
	private MasterServer mServer;
	private Socket socket;
	
	private DataInputStream dis;
	private DataOutputStream dos;
	
	private boolean heartbeatStatus;
	private boolean navigatorStatus;
	
	private int id;
	private int portSubserver;
	
	private static int TYPE_SUBSERVER = 1;
	private static int TYPE_CLIENT = 0;
	
	public Navigator(MasterServer server, Socket s, int id) {
		this.mServer = server;
		this.socket = s;
		this.id = id;
		
		heartbeatStatus = false;
		navigatorStatus = true;
		
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			mServer.println("Navigator initialized at id " + id);
		}
		catch ( Exception e ) {
			mServer.error("Navigator failed to init at id " + id);
			e.printStackTrace();
		}
	}
	
	public void println(String str) {
		mServer.println(str);
	}
	
	public void print(String str) {
		mServer.print(str);
	}
	
	public void error(String str) {
		mServer.error(str);
	}
	
	DataOutputStream getDos() {
		return dos;
	}
	
	boolean getHeartbeatStatus() {
		return heartbeatStatus;
	}
	
	void setHeartbeatStatus(boolean status) {
		this.heartbeatStatus = status;
	}
	
	void stopNavigator() {
		navigatorStatus = false;
	}
	
	public void run() {
		int type = 0;
		try {
			while(navigatorStatus && !socket.isClosed()) {
				String message = dis.readUTF();
				int pos = message.indexOf("@@@");
				try {
					if(pos <= 0)
						throw new Exception();
						
					String cm = message.substring(pos + 3);
					String sm = message.substring(0, pos);
					if(sm.equals("alive")) {
						if(heartbeatStatus == false) {
							heartbeatStatus = true;
						}
						
						type = TYPE_SUBSERVER;
					} else if(sm.equals("init")) {
						System.out.println(sm + " | " + cm);
						portSubserver = Integer.parseInt(cm);
						mServer.addSubserver(portSubserver);
						Nurse n = new Nurse(this, id);
						n.start();
						
						type = TYPE_SUBSERVER;
					} else if(sm.equals("stop")) {
						System.out.println(sm + " | " + cm);
						portSubserver = Integer.parseInt(cm);
						mServer.removeSubserver(portSubserver);
						
						type = TYPE_SUBSERVER;
					} else if(sm.equals("getport")) {
						System.out.println(sm + " | " + cm);
						
						Random       random    = new Random();
						List<Integer> keys     = new ArrayList<Integer>(mServer.getKeyset());
						Integer       randomKey = keys.get( random.nextInt(keys.size()) );
						dos.writeInt(randomKey);
						
						type = TYPE_CLIENT;
						break;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					mServer.error("Navigator command exception at id " + id);
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			mServer.error("Navigator IO exception at id " + id);
			e.printStackTrace();
		}
		mServer.println("Navigator stopped at id " + id);
		if(type == TYPE_SUBSERVER) {
			mServer.removeSubserver(portSubserver);
			mServer.activeSubserver();
		}
	}
}

class Nurse extends Thread {
	private static int NURSE_CHECKING_TIME = 4000;
	
	private Navigator navigator;
	private int id;

	public Nurse(Navigator n, int id){
		this.navigator = n;
		this.id = id;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			navigator.println("Nurse is taken care at id " + id);
			while(true) {
				Thread.currentThread().sleep(NURSE_CHECKING_TIME);
				if(navigator.getHeartbeatStatus() == false) {
					navigator.println("Subserver " + id + " is dead");
					navigator.stopNavigator();
					break;
				} else {
//					navigator.println("Subserver at " + id + " is dead");
					navigator.setHeartbeatStatus(false);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			navigator.error("Nurse error at id " + id);
			e.printStackTrace();
		}
		navigator.println("Nurse stopped at id " + id);
	}
}