import org.apache.xmlrpc.*;
import java.util.*;

public class Server { 

	// A simple ping, simply returns True
	public boolean ping() {
		System.out.println("Ping()");
		return true;
	}

	// Given a hash value, return the associated block
	public byte[] getblock(String hashvalue) {
		System.out.println("GetBlock(" + hashvalue + ")");

		byte[] blockData = new byte[16];
		for (int i = 0; i < blockData.length; i++) {
			blockData[i] = (byte) i;
		}

		return blockData;
	}

	// Store the provided block
	public boolean putblock(byte[] blockData) {
		System.out.println("PutBlock()");

		return true;
	}

	// Determine which of the provided blocks are on this server
	public Vector hasblocks(Vector hashlist) {
		System.out.println("HasBlocks()");

		return hashlist;
	}

	// Returns the server's FileInfoMap
	public Hashtable getfileinfomap() {
		System.out.println("GetFileInfoMap()");

		// file1.dat
		Integer ver1 = new Integer(3); // file1.dat's version

		Vector<String> hashlist = new Vector<String>(); // file1.dat's hashlist
		hashlist.add("h0");
		hashlist.add("h1");
		hashlist.add("h2");

		Vector fileinfo1 = new Vector();
		fileinfo1.add(ver1);
		fileinfo1.add(hashlist);

		// file2.dat
		Integer ver2 = new Integer(5); // file2.dat's version

		Vector fileinfo2 = new Vector();
		fileinfo2.add(ver2);
		fileinfo2.add(hashlist); // use the same hashlist

		Hashtable<String, Object> result = new Hashtable<String, Object>();
		result.put("file1.dat", fileinfo1);
		result.put("file2.dat", fileinfo2);

		return result;
	}

	// Update's the given entry in the fileinfomap
	public Vector updatefile(String filename, int version, Vector hashlist) {
		System.out.println("UpdateFile(" + filename + ")");

		Vector result = new Vector();
		result.add(true); // was the update successful?
		result.add(5); // the cloud's version here

		return result;
	}

	public static void main (String [] args) {

		try {

			System.out.println("Attempting to start XML-RPC Server...");

			WebServer server = new WebServer(8080);
			server.addHandler("surfstore", new Server());
			server.start();

			System.out.println("Started successfully.");
			System.out.println("Accepting requests. (Halt program to stop.)");

		} catch (Exception exception){
			System.err.println("Server: " + exception);
		}
	}
}
