import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class P2Peer2 {
	final static int APPLICATION_PORT = 6969;
	final static int CONNECTION_TIMEOUT = 15000;
	final static int KB_TIMEOUT = 15000;
	final static String SHARED_FILE_PATH = P2Peer2.class.getProtectionDomain().getCodeSource().getLocation().getPath()
			+ "../src" + File.separator + "P2Peer2_Shared_Files" + File.separator;

	private static List<String> peerList = new ArrayList<String>();
	private static Peer newPeer;
	private static ServerSocket serverSocket;

	public static void main(String argv[]) throws Exception {

		String peerIP = "";
		BufferedReader KBinput = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			if (peerList.isEmpty()) {
				System.out.println("Waiting for Incoming connections for 10 seconds");
				try {
					serverSocket = new ServerSocket(APPLICATION_PORT);
					serverSocket.setSoTimeout(CONNECTION_TIMEOUT);
					newPeer = new Peer(SHARED_FILE_PATH, "", APPLICATION_PORT);
					newPeer.setServerSocket(serverSocket);
					newPeer.upload();
					serverSocket.close();
				} catch (java.io.InterruptedIOException e) {
					serverSocket.close();
					System.out.println(
							"No Peer found, enter the IP address of another peer in 10 sec: ");
					long startTime = System.currentTimeMillis();
					while ((System.currentTimeMillis() - startTime) < KB_TIMEOUT && !KBinput.ready()) {
					}

					if (KBinput.ready()) {
						peerIP = KBinput.readLine();
					    System.out.println("Connecting to: " + peerIP);
						peerList.add(peerIP);
					} else {
					    System.out.println("Keyboard timed out");
					}
				} catch (Exception e) {
					System.out.println("Port not available, will try again");
				}
			} else {
				peerIP = peerList.remove(0);
				try {
					newPeer = new Peer(SHARED_FILE_PATH, peerIP, APPLICATION_PORT);
					newPeer.sendIP();
					newPeer.download();
				} catch (Exception e) {
					System.out.println("Could not connect to peer at " + peerIP + ", will try again ");
					peerList.add(peerIP);
				}
			}
		}
	}
}
