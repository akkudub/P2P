import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Peer {
	//private variables, self explanatory
	
	private String SHARED_FOLDER;
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
		SHARED_FOLDER = sharedFilePath;
		IP_ADDRESS = IP;
		PEER_PORT = port;
	}
	
	//init to re initialise the output and input streams every time a new socket is created
	void init() throws IOException{
		inFromPeer = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
		outToPeer = new DataOutputStream(mySocket.getOutputStream());
	}
	
	//set a server socket after an incoming connection has been accepted
	void setServerSocket(ServerSocket ss){
		myServerSocket = ss;
	}
	
	//main download function to be used when downloading files from a remote peer
	void download() throws Exception {
		mySocket = new Socket(IP_ADDRESS, PEER_PORT);
		init();
		System.out.println("Downloading...");
		//get the list of files
		String peerFiles = requestRemoteFileList();
		mySocket.close();
		//calculate the missing files
		remotePeerFiles = convertToList(peerFiles);
		localPeerFiles = getLocalFiles();
		missingFiles = calculateMissingFiles(localPeerFiles, remotePeerFiles);

		while (true) {
			if (!missingFiles.isEmpty()) {
				String fetchFile = missingFiles.pop();
				mySocket = new Socket(IP_ADDRESS, PEER_PORT);
				init();
				requestRemoteFile(fetchFile);
				mySocket.close();
			} else {
				mySocket = new Socket(IP_ADDRESS, PEER_PORT);
				init();
				endSync();
				mySocket.close();
				break;
			}
		}
	}
	
	//helper function to handle the sending of the application layer protocol of requesting for file list
	private String requestRemoteFileList() {
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
	
	//helper function to handle the request for one remote file
	private void requestRemoteFile(String fileName) {
		OutputStream output = null;
		InputStream input = null;

		try {
			outToPeer.writeBytes("F" + fileName + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			output = new FileOutputStream(SHARED_FOLDER + fileName);
		} catch (FileNotFoundException ex) {
			System.out.println("File not found. ");
		}
		try {
			input = mySocket.getInputStream();
			byte[] bytes = new byte[100 * 1024];
			int count;
			while ((count = input.read(bytes)) > 0) {
				output.write(bytes, 0, count);
			}
			outToPeer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//convert the string of filenames to an array list so it can be compared
	private static List<String> convertToList(String filenames) {
		List<String> list = new ArrayList<String>();
		String fileNames[] = filenames.split(",");
		for (int i = 0; i < fileNames.length; i++) {
			list.add(fileNames[i]);
		}
		return list;
	}
	
	//get the filenames in the local shared folder
	private List<String> getLocalFiles() {
		List<String> allFilesList = new ArrayList<String>();
		File dir = new File(SHARED_FOLDER);
		if (dir.exists()) {
			for (final File fileEntry : dir.listFiles()) {
				allFilesList.add(fileEntry.getName());
			}
		}
		return allFilesList;
	}
	
	//calculate the missing files for the local peer
	private static Stack<String> calculateMissingFiles(List<String> myFilesList, List<String> peerFileList) {
		peerFileList.removeAll(myFilesList);
		Stack<String> missingFiles = new Stack<String>();
		for (int i = 0; i < peerFileList.size(); i++) {
			missingFiles.push(peerFileList.get(i));
		}
		return missingFiles;
	}
	
	// main upload handler for a peer
	void upload() throws IOException {
		System.out.println("Uploading...");
		while (true) {
			mySocket = myServerSocket.accept();
			init();
			System.out.println("Incoming Connection");

			String input = inFromPeer.readLine(); // getting stuck

			System.out.println(input);// here again
			
			//handle all the protocol messages
			if (input.charAt(0) == 'I') {
				remotePeerList.add(input.substring(1));
			} else if (input.equals("L")) {
				sendLocalFileList();
				mySocket.close();
			} else if (input.charAt(0) == 'F') {
				String fileName = input.substring(1);
				sendLocalFile(fileName);
				mySocket.close();
			} else if (input.equals("D")) {
				System.out.println("Ending sync.");
				mySocket.close();
				break;
			}			
		}

	}

	// send your own IP address
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
	
	//helper function to handle the sending of the application layer protocol of sending local file list
	private void sendLocalFileList() {

		File dir = new File(SHARED_FOLDER);
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
	//helper function to handle the sending of a local file
	private void sendLocalFile(String fileName) {
		InputStream input = null;
		OutputStream output = null;
		File file = new File(SHARED_FOLDER + fileName);
		// Get the size of the file
		byte[] bytes = new byte[16 * 1024];
		int count;
		try {
			input = new FileInputStream(file);
			output = mySocket.getOutputStream();
			while ((count = input.read(bytes)) > 0) {
				output.write(bytes, 0, count);
			}
			outToPeer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//handle the application layer protocol of sending the message to end the sync
	private void endSync() {
		try {
			outToPeer.writeBytes("D\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
