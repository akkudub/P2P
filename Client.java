import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Client {
	static boolean HAS_IP = false;
	static String IP_ADDRESS;
	final static int port = 6789;
	final static String SHARED_FILE_PATH = Client.class.getProtectionDomain().getCodeSource().getLocation().getPath()
			+ "../src" + File.separator + "Client_Shared_Files" + File.separator;

	private static List<String> peerList = new ArrayList<String>();
	private static Peer hulk;
	private static String peerFiles = null;
	private static Stack<String> missingFilesList = new Stack<String>();
	private static List<String> myFilesList = new ArrayList<String>();
	private static List<String> peerFileList = new ArrayList<String>();

	private static Socket receiverSocket;
	private static Socket senderSocket;
	private static ServerSocket serverSocket;

	public static void main(String argv[]) throws Exception {

		String peerName = "";

		// IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String client_IP = "";
		peerList.add(InetAddress.getLocalHost().getHostAddress());

		while (true) {
			if (peerList.isEmpty()) {
				System.out.println("I'm a server");
				try {
					serverSocket = new ServerSocket(6789);
					serverSocket.setSoTimeout(10000);
					send();
					serverSocket.close();
				} catch (java.io.InterruptedIOException e) {
					System.out.println(
							"Time Out 10 Sec. No Peer found, please enter the IP address of the peer you want to connect to. ");
					peerName = input.readLine();
					peerList.add(peerName);
				} catch (Exception e){
					System.out.println("Port not available, will try again");
				}
			} else {
				String peerIP = peerList.remove(0);
				try{
				receiverSocket = new Socket(peerIP, 6789);
				hulk = new Peer(SHARED_FILE_PATH, receiverSocket);
				hulk.sendIP();
				receiverSocket.close();
				receive();
				}catch(Exception e){
					System.out.println("Could not connect to peer at " + peerIP + ", will try again ");
					peerList.add(peerIP);
				}
			}

		}

	}

	static void receive() throws UnknownHostException, IOException {
		receiverSocket = new Socket(IP_ADDRESS, 6789);
		hulk = new Peer(SHARED_FILE_PATH, receiverSocket);

		System.out.println("Request - CLient");
		peerFiles = hulk.requestFileList();

		receiverSocket.close();

		peerFileList = convertToList(peerFiles);
		myFilesList = getMyFiles();
		missingFilesList = getMissingFiles(myFilesList, peerFileList);

		while (true) {
			if (!missingFilesList.isEmpty()) {
				String fetchFile = missingFilesList.pop();

				receiverSocket = new Socket(IP_ADDRESS, 6789);
				hulk = new Peer(SHARED_FILE_PATH, receiverSocket);

				hulk.requestFile(fetchFile);

				receiverSocket.close();

			} else {
				receiverSocket = new Socket(IP_ADDRESS, 6789);
				hulk = new Peer(SHARED_FILE_PATH, receiverSocket);
				hulk.terminateSync();
				receiverSocket.close();
				break;

			}
		}

	}

	static void send() throws IOException {
		System.out.println("Send - Server");
		while (true) {

			senderSocket = serverSocket.accept();
			hulk = new Peer(SHARED_FILE_PATH, senderSocket);
			System.out.println("Established connection");

			String input = hulk.getInputStream().readLine(); // getting stuck

			System.out.println(input);// here again

			if (input.charAt(0) == 'I') {
				peerList.add(input.substring(1));
			} else if (input.equals("L")) {
				hulk.sendFileList();
				senderSocket.close();
			} else if (input.charAt(0) == 'F') {
				String fileName = input.substring(1);
				hulk.sendFile(fileName);
				senderSocket.close();
			} else if (input.equals("D")) {
				System.out.println("Terminated sync.");
				senderSocket.close();
				break;
			}
		}

	}

	static List<String> convertToList(String filenames) {
		List<String> list = new ArrayList<String>();
		String fileNames[] = filenames.split(",");
		for (int i = 0; i < fileNames.length; i++) {
			list.add(fileNames[i]);
		}
		return list;
	}

	static List<String> getMyFiles() {
		List<String> allFilesList = new ArrayList<String>();
		File dir = new File(SHARED_FILE_PATH);
		if (dir.exists()) {
			for (final File fileEntry : dir.listFiles()) {
				allFilesList.add(fileEntry.getName());
			}
		}
		return allFilesList;
	}

	static Stack<String> getMissingFiles(List<String> myFilesList, List<String> peerFileList) {
		peerFileList.removeAll(myFilesList);
		Stack<String> missingFiles = new Stack<String>();
		for (int i = 0; i < peerFileList.size(); i++) {
			System.out.println(peerFileList.get(i));
			missingFiles.push(peerFileList.get(i));
		}
		return missingFiles;
	}

}
