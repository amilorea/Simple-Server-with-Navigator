import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	
	public static boolean debug = true;
	
	private static int mPort;
	private static String ip = "localhost";
	private int port;

	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;
	
	public static void main(String arg[]) {
		mPort = Integer.parseInt(arg[0]);
		Client c = new Client();
		c.init();
	}
	
	public Client() {

		try {
			println("Attempt to connect to Master Server at port " + mPort);
			socket = new Socket(ip, mPort);
			dis = new DataInputStream( socket.getInputStream() );
			dos = new DataOutputStream( socket.getOutputStream() );
			println("Connected to Master Server");
		}
		catch ( Exception e) {
			error("Could not connect to Master Server");
			e.printStackTrace();
		}
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
	
	private void init() {
		try {
			if(!socket.isClosed()) {
				dos.writeUTF("getport@@@");
				port = dis.readInt();
				println("Subserver port is set on " + port);
				socket.close();
				
				socket = new Socket(ip, port);
				dis = new DataInputStream( socket.getInputStream() );
				dos = new DataOutputStream( socket.getOutputStream() );
				start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error("Could not get Subserver's port");
			e.printStackTrace();
		}
	}
	
	private void start() {
		Scanner sc = new Scanner(System.in);
		while(!socket.isClosed()) {
			String str = sc.nextLine();
			try {
				dos.writeUTF(str);
				int pos = str.indexOf("@@@");
				if(pos <= 0)
					throw new Exception();
				
				String cm = str.substring(pos + 3);
				String sm = str.substring(0, pos);
				println(sm + " | " + cm);
				if(sm.equals("getport")) {
					port = dis.readInt();
				} else if(sm.equals("gettime")) {
					long time = dis.readLong();
					println("Time received " + time);
				} else if(sm.equals("calculate")){
					char ch = dis.readChar();
					print("Char recevied " + ch);
					long f = dis.readLong();
					println(" with frequent " + f);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				error("Worker IO exception at port " + port);
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				error("Worker command exception at port " + port);
				e.printStackTrace();
			}
		}
		sc.close();
		return;
	}
	
}
