import java.util.*;
import org.apache.xmlrpc.*;

public class Client {
	public static void main (String [] args) {

	  if (args.length != 3) {
		System.err.println("Usage: Client host:port /basedir blockSize");
		System.exit(1);
	  }

	  if (!args[1].startsWith("/") || args[1].indexOf(" ") >= 0) {
		System.err.println("Usage: basedir is invalid");
		System.exit(1);
	  }

	  try {
		int blockSize = Integer.parseInt(args[2]);
		if (blockSize < 0) {
			System.err.println("Usage: Blocksize is invalid.");
			System.exit(1);
		}

        XmlRpcClient client = new XmlRpcClient("http://" + args[0] + "/RPC2");

        Vector params = new Vector();

		 // Test ping
		 params = new Vector();
		 if ((boolean) client.execute("surfstore.ping", params))
		 	System.out.println("Ping() successful");

		/*
		 // Test PutBlock
		 params = new Vector();
		 byte[] blockData = new byte[10];
		 params.addElement(blockData);
         boolean putresult = (boolean) client.execute("surfstore.putblock", params);
		 System.out.println("PutBlock() successful");

		 // Test GetBlock
		 params = new Vector();
		 params.addElement("h0");
         byte[] blockData2 = (byte[]) client.execute("surfstore.getblock", params);
		 System.out.println("GetBlock() successfully read in " + blockData2.length + " bytes");
			*/
		FileHandler f = new FileHandler(client, args[1], blockSize);
		f.sync();

	} catch (NumberFormatException e) {
		System.err.println("Usage: Blocksize is invalid.");
		System.exit(1);
	} catch (Exception exception) {
        System.err.println("Client: " + exception);
	}
   }
}
