import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Peer {

	private String SHARED_FILE_PATH;
	private int PEER_PORT = 6969;
	private Socket mySocket;
	private ServerSocket myServerSocket;
	private String IP_ADDRESS;
	private BufferedReader inFromPeer;
	private DataOutputStream outToPeer;
	private List<String> remotePeerList = new ArrayList<String>(); 
	private List<String> remotePeerFiles = new ArrayList<String>(); 
	private List<String> localPeerFiles = new ArrayList<String>(); 
	private Stack<String> missingFiles = new Stack<String>(); 

	Peer(String sharedFilePath, String IP, int port) {
		SHARED_FILE_PATH = sharedFilePath;
		IP_ADDRESS = IP;
		PEER_PORT = port;
	}
	
	void init() throws IOException{
		inFromPeer = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
		outToPeer = new DataOutputStream(mySocket.getOutputStream());
	}
	
	void setServerSocket(ServerSocket ss){
		myServerSocket = ss;
	}
	
	void download() throws Exception {
		mySocket = new Socket(IP_ADDRESS, PEER_PORT);
		init();
		System.out.println("Downloading");
		String peerFiles = requestRemoteFileList();
		mySocket.close();

		remotePeerFiles = convertToList(peerFiles);
		localPeerFiles = getMyFiles();
		missingFiles = calculateMissingFiles(localPeerFiles, remotePeerFiles);

		while (true) {
			if (!missingFiles.isEmpty()) {
				String fetchFile = missingFiles.pop();
				mySocket = new Socket(IP_ADDRESS, PEER_PORT);
				init();
				requestFile(fetchFile);
				mySocket.close();
			} else {
				mySocket = new Socket(IP_ADDRESS, PEER_PORT);
				init();
				terminateSync();
				mySocket.close();
				break;
			}
		}
	}
	
	String requestRemoteFileList() {
		String fileList = "";
		try {
			outToPeer.writeBytes("L\n"); // no problem?
			fileList = inFromPeer.readLine();
			outToPeer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileList;
	}
	

	void requestFile(String fileName) {
		OutputStream out = null;
		InputStream in = null;

		try {
			outToPeer.writeBytes("F" + fileName + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			out = new FileOutputStream(SHARED_FILE_PATH + fileName);
		} catch (FileNotFoundException ex) {
			System.out.println("File not found. ");
		}
		try {
			in = mySocket.getInputStream();
			byte[] bytes = new byte[100 * 1024];
			int count;
			while ((count = in.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
			outToPeer.flush();
		} catch (Exception e) {
			e.printStackTrace();
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

	List<String> getMyFiles() {
		List<String> allFilesList = new ArrayList<String>();
		File dir = new File(SHARED_FILE_PATH);
		if (dir.exists()) {
			for (final File fileEntry : dir.listFiles()) {
				allFilesList.add(fileEntry.getName());
			}
		}
		return allFilesList;
	}
	
	static Stack<String> calculateMissingFiles(List<String> myFilesList, List<String> peerFileList) {
		peerFileList.removeAll(myFilesList);
		Stack<String> missingFiles = new Stack<String>();
		for (int i = 0; i < peerFileList.size(); i++) {
			missingFiles.push(peerFileList.get(i));
		}
		return missingFiles;
	}
	
	void upload() throws IOException {
		System.out.println("Uploading...");
		while (true) {
			mySocket = myServerSocket.accept();
			init();
			System.out.println("Incoming Connection");

			String input = inFromPeer.readLine(); // getting stuck

			System.out.println(input);// here again

			if (input.charAt(0) == 'I') {
				remotePeerList.add(input.substring(1));
			} else if (input.equals("L")) {
				sendFileList();
				mySocket.close();
			} else if (input.charAt(0) == 'F') {
				String fileName = input.substring(1);
				sendFile(fileName);
				mySocket.close();
			} else if (input.equals("D")) {
				System.out.println("Terminated sync.");
				mySocket.close();
				break;
			}			
		}

	}


	void sendIP() {
		try {
			mySocket = new Socket(IP_ADDRESS, PEER_PORT);
			init();
			outToPeer.writeBytes("I" + InetAddress.getLocalHost().getHostAddress() + "\n");
			mySocket.close();
		} catch (Exception e) {
			System.out.println("Wrong IP or Peer not listening");
//			e.printStackTrace();
		}
	}

	void sendFileList() {

		File dir = new File(SHARED_FILE_PATH);
		StringBuffer allFileNames = new StringBuffer();
		if (dir.exists()) {
			for (final File fileEntry : dir.listFiles()) {
				allFileNames.append(fileEntry.getName() + ",");
			}
		}
		try {
			outToPeer.writeBytes(allFileNames.toString() + "\n");
			System.out.println("Sent file names " + allFileNames);
			outToPeer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void sendFile(String fileName) {
		InputStream in = null;
		OutputStream out = null;
		File file = new File(SHARED_FILE_PATH + fileName);
		// Get the size of the file
		byte[] bytes = new byte[16 * 1024];
		int count;
		try {
			in = new FileInputStream(file);
			out = mySocket.getOutputStream();
			while ((count = in.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
			outToPeer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	void terminateSync() {
		try {
			outToPeer.writeBytes("D\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
