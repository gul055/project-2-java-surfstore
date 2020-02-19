import org.apache.xmlrpc.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Server { 

	private Hashtable<String, byte[]> blockMap = new Hashtable<>();
	private Hashtable<String, Vector> fileInfoMap = new Hashtable<>();

	// A simple ping, simply returns True
	public boolean ping() {
		System.out.println("Ping()");
		return true;
	}

	// Given a hash value, return the associated block
	public byte[] getblock(String hashvalue) {
		System.out.println("GetBlock(" + hashvalue + ")");

		/*byte[] blockData = new byte[16];
		for (int i = 0; i < blockData.length; i++) {
			blockData[i] = (byte) i;
		}*/

		if (blockMap.containsKey(hashvalue)) {
			return blockMap.get(hashvalue);
		}

		return new byte[0];
	}

	// Store the provided block
	public boolean putblock(byte[] blockData) {
		System.out.println("PutBlock()");

		if (blockData == null) {
			return false;
		}
		String hashVal = getHashVal(blockData);
		blockMap.put(hashVal, blockData);
		//System.err.println("hashcode: " + hashVal + "block data is: " + blockData);

		return true;
	}

	private String getHashVal(byte[] blockData) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			/*StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			hashVal = new String(hexString);*/
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] hash = digest.digest(blockData);

		return Base64.getEncoder().encodeToString(hash);
	}

	// Determine which of the provided blocks are on this server
	// I DO NOT USE IT...
	public Vector hasblocks(Vector hashlist) {
		System.out.println("HasBlocks()");
		Vector hashOut = hashlist;
		hashOut.retainAll(blockMap.keySet());
		return hashOut;
	}

	// Returns the server's FileInfoMap
	public Hashtable getfileinfomap() {
		System.out.println("GetFileInfoMap()");
		/*
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
		*/

		return fileInfoMap;
	}

	// Update's the given entry in the fileinfomap
	public Vector updatefile(String filename, int version, Vector hashlist) {
		System.out.println("UpdateFile(" + filename + ")");

		Vector result = new Vector();

		if (fileInfoMap.containsKey(filename)) {
			Vector fileObj = fileInfoMap.get(filename);
			int currVer = (int) fileObj.get(0);
			if (currVer >= version) {
				result.add(false);
				result.add(currVer);
				return result;
			}
		}

		Vector newfileObj = new Vector<>();
		newfileObj.add(version);
		newfileObj.add(hashlist);
		fileInfoMap.put(filename, newfileObj);
		result.add(true);
		result.add(version);

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
