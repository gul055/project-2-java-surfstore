import java.util.*;
import java.io.*;
import org.apache.xmlrpc.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHandler {
    private XmlRpcClient client;
    private String baseDir;
    private int blockSize;
    private Hashtable<String, Vector> clientMap = new Hashtable<>();
	private Hashtable<String, Vector> serverMap = new Hashtable<>();

    
    public FileHandler(XmlRpcClient client, String baseDir, int blockSize) {
        this.client = client;
        this.baseDir = baseDir;
        this.blockSize = blockSize;

    }

    public void sync() {
        try {
            File dir = new File(this.baseDir);
            if (!dir.exists()) {
                System.err.println("no such directory!");
                System.exit(1);
            }

            File f = new File(dir + "/index.txt");

            loadClientMap(f);

            checkClientMap(dir);

            updateFileMap(dir);

            updateIndexFile(f);
            
        } catch (Exception exception) {
            System.err.println("Sync error: " + exception);
        }
    }

    public void loadClientMap(File f) {
        try {
            if (!f.exists()) {
                f.createNewFile();
            }

            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String d[] = s.nextLine().split(",|\\s+");
                String fname = d[0];
                int ver = Integer.parseInt(d[1]);
                Vector fileObj = new Vector<>();
                Vector hashlist = new Vector<>();
                for (int i = 2; i < d.length; i++) {
                    hashlist.add(d[i]);
                }

                fileObj.add(ver);
                fileObj.add(hashlist);

                clientMap.put(fname, fileObj);
            }
		    s.close();

        } catch (FileNotFoundException e1) {
            System.err.println("Error: file not found!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkClientMap(File f) {
        Set<String> localFileSet = new HashSet<>(Arrays.asList(f.list()));
        localFileSet.remove("index.txt");
        Set<String> fileTobeAddSet = new HashSet<>(localFileSet);
        fileTobeAddSet.removeAll(clientMap.keySet());
        for (String name : clientMap.keySet()) {
            if (!localFileSet.contains(name)) {
                Vector<String> hashlist = (Vector<String>) clientMap.get(name).get(1);
                if (!(hashlist.get(0).equals("0"))) {
                    //update map
                    Vector fileObj = new Vector<>();

                    fileObj.add((int)clientMap.get(name).get(0) + 1);
                    hashlist.clear();
                    hashlist.add("0");
                    fileObj.add(hashlist);
                    clientMap.put(name, fileObj);
                }
            }
            else if (!(getHashList(f.getAbsolutePath()+"/"+name).equals((Vector<String>)clientMap.get(name).get(1)))) {
                    Vector hashlist = getHashList(f.getAbsolutePath()+"/"+name);
                    Vector fileObj = new Vector<>();
                    fileObj.add((int)clientMap.get(name).get(0) + 1);
                    fileObj.add(hashlist);
                    clientMap.put(name, fileObj);
            }
        }

        for (String name : fileTobeAddSet) {
            Vector hashlist = getHashList(f.getAbsolutePath()+"/"+name);
            Vector fileObj = new Vector<>();
            fileObj.add(1);
            fileObj.add(hashlist);
            clientMap.put(name, fileObj);
        }
    }

    private Vector getHashList(String file){
        Vector hashlist = new Vector<>();

        try {
            File f = new File(file);
            byte[] bSize = new byte[this.blockSize];
            FileInputStream in = new FileInputStream(f);
            int readSize = 0;
            int totalSize = 0;
            
            while((readSize = in.read(bSize)) != -1) {
                totalSize += readSize;
                byte[] read = new byte[readSize];
                System.arraycopy(bSize, 0, read, 0, readSize);
                String hashval = getHashVal(read);
                hashlist.add(hashval);
            }

            if (totalSize == 0) {
                hashlist.add(getHashVal(new byte[0]));
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("no such file");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return hashlist;
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

    private void updateFileMap(File dir) {
        try {
            Vector params = new Vector();
            serverMap = (Hashtable<String, Vector>) client.execute("surfstore.getfileinfomap", params);
            Set<String> serverMapSet = serverMap.keySet();
            Set<String> clientMapSet = clientMap.keySet();
            for (String filename : clientMap.keySet()) {
                if (!serverMap.keySet().contains(filename) ||
                 (int) clientMap.get(filename).get(0) > (int) serverMap.get(filename).get(0)) {
                    Vector hashlist = (Vector) clientMap.get(filename).get(1);
                    if (!(((String) hashlist.get(0)).equals("0"))) {
                        System.err.println("Debug upload!");
                        uploadToSever(filename, dir);
                    }

                    params = new Vector<>();
                    params.add(filename);
                    params.add(clientMap.get(filename).get(0));
                    params.add(clientMap.get(filename).get(1));
                    System.err.println("Debug uploadfile!");
                    Vector result = (Vector) client.execute("surfstore.updatefile", params);
                    System.err.println("Debug uploadfile! out");
                    if (!(boolean)result.get(0)) {
                        System.err.println("Debug downloadfile!3");
                        params = new Vector<>();
                        serverMap = (Hashtable<String, Vector>) client.execute("surfstore.getfileinfomap", params);
                        serverMapSet = serverMap.keySet();
                        System.err.println("Debug downloadfile!2" + serverMapSet);
                        System.err.println("hashlist1: "+ serverMap.get(filename));
                        downloadFromServer(filename, dir);
                        clientMap.put(filename, serverMap.get(filename));
                        System.err.println("hashlist2: "+ serverMap.get(filename));
                    }
                }
                /*else if (Integer.parseInt((String)clientMap.get(filename).get(0)) == Integer.parseInt((String)serverMap.get(filename).get(0))) {
                    Vector<String> chashlist = (Vector<String>) clientMap.get(filename).get(1);
                    Vector<String> shashlist = (Vector<String>) serverMap.get(filename).get(1);
                    if (chashlist.equals(shashlist)) {
                        downloadFromServer(filename, dir);
                        clientMap.put(filename, serverMap.get(filename));
                    }
                        
                }*/
                else if ((int)clientMap.get(filename).get(0) <= (int)serverMap.get(filename).get(0)) {
                    System.err.println("Debug downloadfile!2");
                    downloadFromServer(filename, dir);
                    clientMap.put(filename, serverMap.get(filename));
                }
                
            }
            System.err.println("Debug downloadfile!4");
            Set<String> downloadNewFile = new HashSet<>(serverMapSet);
            downloadNewFile.removeAll(clientMapSet);
            System.err.println("Debug downloadfile!5");
            for (String filename : downloadNewFile) {
                System.err.println("Debug downloadfile!2" + filename);
                downloadFromServer(filename, dir);
                clientMap.put(filename, serverMap.get(filename));
                System.err.println("hashlist: "+ serverMap.get(filename));
            }
            System.err.println("Debug downloadfile!6");
        } catch (Exception e) {
            System.err.println("Error: Update error" + e);
        }
    }

    private void updateIndexFile(File f) {
        try {
            FileWriter myWriter = new FileWriter(f.getAbsolutePath());
            for (String filename : clientMap.keySet()) {
                StringBuilder sb = new StringBuilder(filename + "," + 
                clientMap.get(filename).get(0) + ","); 
                Vector<String> hashlist = (Vector<String>) clientMap.get(filename).get(1);
                for (String hashval : hashlist) {
                    sb.append(hashval + " ");
                }
                sb.setCharAt(sb.length() - 1, '\n');
                myWriter.write(new String(sb));
            }
            myWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFromServer(String filename, File dir) {
        try {
            File file = new File(dir.getAbsolutePath() + "/" + filename);
            System.err.println("Debug downloadfile!download fcn0" + file);
            Vector<String> hashlist = (Vector<String>) serverMap.get(filename).get(1);
            System.err.println("Debug downloadfile!download fcn1" + hashlist);

            if (((String) hashlist.get(0)).equals("0")) {
                if (file.exists())
                    file.delete();
                return;
            }
            System.err.println("Debug downloadfile!download fcn3delte");
            if (!file.exists())
                file.createNewFile();
            System.err.println("Debug downloadfile!download fcn2 create");
            Vector params = new Vector<>();
            FileWriter myWriter = new FileWriter(dir.getAbsolutePath() + "/" + filename);
            FileOutputStream out = new FileOutputStream(file);
            for (String hashval : hashlist) {
                params.clear();
                params.add(hashval);
                System.err.println("Debug downloadfile!download hashKey: " + hashval);
                byte[] writeIn = (byte[]) client.execute("surfstore.getblock", params);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                System.err.println("Debug downloadfile!download fcninside" + Base64.getEncoder().encodeToString(digest.digest(writeIn)));

                System.err.println(writeIn);
                String testing = Base64.getEncoder().encodeToString(digest.digest(writeIn));
                out.write(writeIn);
                //myWriter.write(testing);
            }
            myWriter.close();
            System.err.println("Debug downloadfile!download fcn4 hash");
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error: file not found");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            System.err.println("Error: Download failed");
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e);
        }
    }

    private void uploadToSever(String filename, File dir) {
        try {
            File f = new File(dir.getAbsolutePath() + "/" + filename);
            byte[] bSize = new byte[this.blockSize];
            FileInputStream in = new FileInputStream(f);
            int readSize = 0;
            int totalSize = 0;
            Vector params = new Vector<>();
            while((readSize = in.read(bSize)) != -1) {
                totalSize += readSize;
                byte[] read = new byte[readSize];
                System.arraycopy(bSize, 0, read, 0, readSize);
                
                params = new Vector<>();
                params.add(read);
                if (!(boolean) client.execute("surfstore.putblock", params)) {
                    System.err.println("Error: upload failed");
                    System.exit(1);
                }
            }
            in.close();

            if (totalSize == 0) {
                params = new Vector<>();
                params.add(new byte[0]);
                System.err.println("I did not upload anything");
                if (!(boolean) client.execute("surfstore.putblock", params)) {
                    System.err.println("Error: upload failed");
                    System.exit(1);
                }
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: file not found");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            System.err.println("Error: Upload failed");
        }

    }


}