import java.io.*;
import java.net.*;
import java.math.*;
import java.security.*;
import java.security.spec.*;
import java.util.Scanner;

import javax.crypto.*;
import javax.crypto.spec.*;

public class ChatClient{
	private static ObjectInputStream cipherIn;					//secure channels
	private static ObjectOutputStream cipherOut;
	private static ObjectInputStream in;;						//insecure channels
	private static ObjectOutputStream out;
	private static String KEY_DIR="../data/";						//location of the secret key
	
	private static Boolean DEBUG=false;
	
	/*
	 * 
	 * Diffie-Hellman Parameters for 1024 bits Modulus (1024-bit prime modulus P, base G)
	 * 
	 */
	private static final byte SKIP_1024_MODULUS_BYTES[] = {
	    (byte)0xF4, (byte)0x88, (byte)0xFD, (byte)0x58,
	    (byte)0x4E, (byte)0x49, (byte)0xDB, (byte)0xCD,
	    (byte)0x20, (byte)0xB4, (byte)0x9D, (byte)0xE4,
	    (byte)0x91, (byte)0x07, (byte)0x36, (byte)0x6B,
	    (byte)0x33, (byte)0x6C, (byte)0x38, (byte)0x0D,
	    (byte)0x45, (byte)0x1D, (byte)0x0F, (byte)0x7C,
	    (byte)0x88, (byte)0xB3, (byte)0x1C, (byte)0x7C,
	    (byte)0x5B, (byte)0x2D, (byte)0x8E, (byte)0xF6,
	    (byte)0xF3, (byte)0xC9, (byte)0x23, (byte)0xC0,
	    (byte)0x43, (byte)0xF0, (byte)0xA5, (byte)0x5B,
	    (byte)0x18, (byte)0x8D, (byte)0x8E, (byte)0xBB,
	    (byte)0x55, (byte)0x8C, (byte)0xB8, (byte)0x5D,
	    (byte)0x38, (byte)0xD3, (byte)0x34, (byte)0xFD,
	    (byte)0x7C, (byte)0x17, (byte)0x57, (byte)0x43,
	    (byte)0xA3, (byte)0x1D, (byte)0x18, (byte)0x6C,
	    (byte)0xDE, (byte)0x33, (byte)0x21, (byte)0x2C,
	    (byte)0xB5, (byte)0x2A, (byte)0xFF, (byte)0x3C,
	    (byte)0xE1, (byte)0xB1, (byte)0x29, (byte)0x40,
	    (byte)0x18, (byte)0x11, (byte)0x8D, (byte)0x7C,
	    (byte)0x84, (byte)0xA7, (byte)0x0A, (byte)0x72,
	    (byte)0xD6, (byte)0x86, (byte)0xC4, (byte)0x03,
	    (byte)0x19, (byte)0xC8, (byte)0x07, (byte)0x29,
	    (byte)0x7A, (byte)0xCA, (byte)0x95, (byte)0x0C,
	    (byte)0xD9, (byte)0x96, (byte)0x9F, (byte)0xAB,
	    (byte)0xD0, (byte)0x0A, (byte)0x50, (byte)0x9B,
	    (byte)0x02, (byte)0x46, (byte)0xD3, (byte)0x08,
	    (byte)0x3D, (byte)0x66, (byte)0xA4, (byte)0x5D,
	    (byte)0x41, (byte)0x9F, (byte)0x9C, (byte)0x7C,
	    (byte)0xBD, (byte)0x89, (byte)0x4B, (byte)0x22,
	    (byte)0x19, (byte)0x26, (byte)0xBA, (byte)0xAB,
	    (byte)0xA2, (byte)0x5E, (byte)0xC3, (byte)0x55,
	    (byte)0xE9, (byte)0x2F, (byte)0x78, (byte)0xC7
	  };
	private static final BigInteger P_MODULUS = new BigInteger (1,SKIP_1024_MODULUS_BYTES);
	private static final BigInteger G_BASE = BigInteger.valueOf(2);
	private static final DHParameterSpec PARAMETER_SPEC = new DHParameterSpec(P_MODULUS,G_BASE);
	
	public static void main(String args[]){
		
		Socket socket = null;
		int serverPort;	
		String host;
		
		if (args.length != 2){
            System.out.println("Usage: java ChatClient host port");
            return;
		}
		host=args[0];
		serverPort = Integer.parseInt(args[1]);
		System.out.println("Trying to connect to "+host+", port "+serverPort+".");
		
		try{
			socket = new Socket(host,serverPort);
			
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socket.getInputStream());
			
			System.out.println("Insecure Connection Established.");
			
			//receive the username from the user
			Scanner scan = new Scanner(System.in);
			String username,message;
			System.out.println("\nUsername:");
			username=scan.next();
			username+=scan.nextLine();
			
			/*******HANDSHAKE PROTOCOL***********/
			Message syn=new Message(username,"");		//send the username
			out.writeObject(syn);
			out.reset();
			
			syn=(Message)in.readObject();				//receive the server reply
			message=syn.getMessage();					
			
			if(message.compareToIgnoreCase("NEW")==0){
				try{
					System.out.println("We have detected you are a new client.\n"
							+ "A password is required in order to encrypt your session key.\n"
							+ "Remember, only you will know about this password. We will not store it anywhere.\n"
							+ "This is your master and private key.\n"
							+ "Please enter a passphrase:");
							
					String pass1=null,pass2=null;
					char[] passwd;
					Console cons;
					
					do{
						System.out.println("\n[Both passwords must match and be at least 8 characters]");
						if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
							pass1=new String(passwd);
						    java.util.Arrays.fill(passwd,' ');
						}
						
						System.out.print("Please retype it:\n");
						if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
							pass2=new String(passwd);
						    java.util.Arrays.fill(passwd,' ');
						}
						
					}while(pass1.compareTo(pass2)!=0 || pass1.length()<8);
					
					System.out.println("Initiating the key agreement protocol ...");
					System.out.println("Generating a Diffie-Hellman KeyPair...");
					KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
					kpg.initialize(PARAMETER_SPEC);
				    KeyPair keyPair = kpg.genKeyPair();
				    
				    System.out.println("Receiving the server's public key ...");
				    syn=(Message)in.readObject();
				    byte[] keyBytes = syn.getEncondedPublicKey();
				    KeyFactory kf = KeyFactory.getInstance("DH");
				    X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(keyBytes);
				    PublicKey serverPublicKey = kf.generatePublic(x509Spec);
				    
				    System.out.println("Sending my public key ...");
				    keyBytes = keyPair.getPublic().getEncoded();
				    syn.setEncondedPublicKey(keyBytes);
				    out.writeObject(syn);
				    out.reset();
				    
				    System.out.println("Performing the KeyAgreement...");
				    KeyAgreement ka = KeyAgreement.getInstance("DH");
				    ka.init(keyPair.getPrivate());
				    ka.doPhase(serverPublicKey,true);
				    
				    System.out.println("Receiving the initialization vector ...");
				    byte[] initializationVector = new byte[8];
				    syn=(Message)in.readObject();
				    initializationVector=syn.getInitializationVector();
				    
				    System.out.println("Creating a session key ...");
				    byte[] sessionKeyBytes = ka.generateSecret();
				    SecretKeyFactory skf = SecretKeyFactory.getInstance("TripleDES");
				    DESedeKeySpec tripleDesSpec = new DESedeKeySpec(sessionKeyBytes);
				    SecretKey sessionKey = skf.generateSecret(tripleDesSpec);
				    
				    System.out.println("Creating the CipherStreams to be used with server...");
				    
				    Cipher decrypter = Cipher.getInstance("TripleDES/CFB8/NoPadding");
				    Cipher encrypter = Cipher.getInstance("TripleDES/CFB8/NoPadding");
				    
				    IvParameterSpec spec = new IvParameterSpec(initializationVector);
				    
				    encrypter.init(Cipher.ENCRYPT_MODE, sessionKey, spec);
				    decrypter.init(Cipher.DECRYPT_MODE, sessionKey, spec);
				    
				    cipherOut = new ObjectOutputStream(new CipherOutputStream(out, encrypter));
				    cipherOut.flush();
				    cipherIn = new ObjectInputStream(new CipherInputStream(in, decrypter));
				    
				    //save the session key in an encrypted file for future reference
				    System.out.println("Saving and encrypting your session key ...");
				    String keyFilename =KEY_DIR+username+"_key";
				    String keyFilenameEncrypted =KEY_DIR+username+"_key_DES";
				    
				    serializeSessionKey(keyFilename, new SessionKey(initializationVector, sessionKey));
				    encryptSerializedKey(pass1, keyFilename, keyFilenameEncrypted);
				    
				    syn=(Message)cipherIn.readObject();
				    System.out.println(syn.getMessage());
				}
			    catch (GeneralSecurityException ex){
			       System.out.println("An error has ocurred ...\nDetails: "+ex.getMessage());
			       ex.printStackTrace();
			    }
			}
			else{
				System.out.println("We have detected you are a returning client.\n"
						+ "Please enter your passphrase in order to initiate session:");
						
				String pass1=null,pass2=null;
				char[] passwd;
				Console cons;
				
				do{
					System.out.println("\n[Both passwords must match and be at least 8 characters]");
					if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
						pass1=new String(passwd);
					    java.util.Arrays.fill(passwd,' ');
					}
					
					System.out.print("Please retype it:\n");
					if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
						pass2=new String(passwd);
					    java.util.Arrays.fill(passwd,' ');
					}
					
				}while(pass1.compareTo(pass2)!=0 || pass1.length()<8);
				
				System.out.println("Retrieving your session key ...");
				String keyFilename =KEY_DIR+username+"_key";
			    String keyFilenameEncrypted =KEY_DIR+username+"_key_DES";
				decryptSerializedKey(pass1, keyFilename, keyFilenameEncrypted);
				SessionKey sessionKey=unserializeSessionKey(keyFilename);
				if(sessionKey==null){
					System.out.println("An error has ocurred while retrieving your session key. Possible causes:\n"
							+ "\t->You entered a wrong password :/.\n"
							+ "\t->You are an intruder >:).");
					File f = new File(keyFilename);
					f.delete();
//					syn.setMessage("FAIL");
//					out.writeObject(syn);
//					out.reset();
//					out.flush();
					return;
				}
//				out.writeObject(syn);
//				out.reset();
//				out.flush();
				System.out.println("Success!\nCreating the CipherStreams to be used with server...");
				
				try{
					
					Cipher decrypter = Cipher.getInstance("TripleDES/CFB8/NoPadding");
				    Cipher encrypter = Cipher.getInstance("TripleDES/CFB8/NoPadding");
				    
				    IvParameterSpec spec = new IvParameterSpec(sessionKey.getSpecification());
				    
				    encrypter.init(Cipher.ENCRYPT_MODE, sessionKey.getSessionkey(), spec);
				    decrypter.init(Cipher.DECRYPT_MODE, sessionKey.getSessionkey(), spec);
				    
				    cipherOut = new ObjectOutputStream(new CipherOutputStream(out, encrypter));
				    cipherOut.flush();
				    cipherIn = new ObjectInputStream(new CipherInputStream(in, decrypter));
				    
				    syn=(Message)cipherIn.readObject();
				    System.out.println(syn.getMessage());
				}
				 catch (GeneralSecurityException ex){
			       System.out.println("An error has ocurred ...\nDetails: "+ex.getMessage());
			       ex.printStackTrace();
				 }
			}
			
			Input inputBuffer=new Input();
			inputBuffer.start();
			
			String msg = "";
		    InputStreamReader inputStream = new InputStreamReader(System.in);
		    BufferedReader reader = new BufferedReader(inputStream);
		    Message m;
			while(true){
			    msg=reader.readLine();
			    m=new Message(username,msg);
			    cipherOut.writeObject(m);
			    cipherOut.reset();
			    cipherOut.flush();
			    System.out.println();
			}
			
		}
		catch (IOException e) {
			System.out.println("An error has ocurred: "+e.getMessage());
		} catch (ClassNotFoundException e) {
			if(DEBUG)e.printStackTrace();
		}
	}
	
	public static boolean serializeSessionKey(String keyFilename,SessionKey sessionKey){
		
		try{
			File f = new File(keyFilename);
			f.createNewFile();
			
			OutputStream file = new FileOutputStream(keyFilename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try{
				output.writeObject(sessionKey);
			}
			finally{
				output.close();
			}
		}
		catch(IOException e){
			System.out.println("An error has ocurred serializing key to disk: "+e.getMessage());
			if(DEBUG)e.printStackTrace();
			return false;
		}
		return true;
	}
	public static SessionKey unserializeSessionKey(String keyFilename){
		
		SessionKey sessionKey=null;
		
		try{
			File f = new File(keyFilename);
			f.createNewFile();
			
			InputStream file = new FileInputStream(keyFilename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			try{
				sessionKey=(SessionKey)input.readObject();
			}
			finally{
				input.close();
				f.delete();
			}
		}
		catch(Throwable e){
			if(DEBUG){
				System.out.println("An error has ocurred unserializing key to memory: "+e.getMessage());
				e.printStackTrace();
			}
		}
		return sessionKey;
	}
	public static boolean encryptSerializedKey(String password,String keyFilename,String keyFilenameEncrypted){
		
		try {
			File f = new File(keyFilename);
			File fe = new File(keyFilenameEncrypted);
			f.createNewFile();
			fe.createNewFile();
			
			FileInputStream fis = new FileInputStream(keyFilename);
			FileOutputStream fos = new FileOutputStream(keyFilenameEncrypted);
			encrypt(password, fis, fos);
			f.delete();

		} catch (Throwable e) {
			if(DEBUG)e.printStackTrace();
		}
		return true;
	}
	public static boolean decryptSerializedKey(String password,String keyFilename,String keyFilenameEncrypted){
		
		try {
			File f = new File(keyFilename);
			File fe = new File(keyFilenameEncrypted);
			f.createNewFile();
			fe.createNewFile();
			
			FileInputStream fis = new FileInputStream(keyFilenameEncrypted);
			FileOutputStream fos = new FileOutputStream(keyFilename);
			decrypt(password, fis, fos);
		} catch (Throwable e) {
			if(DEBUG)e.printStackTrace();
		}
		return true;
	}
	public static void encrypt(String key, InputStream is, OutputStream os) throws Throwable {
		encryptOrDecrypt(key, Cipher.ENCRYPT_MODE, is, os);
	}

	public static void decrypt(String key, InputStream is, OutputStream os) throws Throwable {
		encryptOrDecrypt(key, Cipher.DECRYPT_MODE, is, os);
	}
	public static void encryptOrDecrypt(String key, int mode, InputStream is, OutputStream os) throws Throwable {

		DESKeySpec dks = new DESKeySpec(key.getBytes());
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		SecretKey desKey = skf.generateSecret(dks);
		Cipher cipher = Cipher.getInstance("DES"); // DES/ECB/PKCS5Padding for SunJCE

		if (mode == Cipher.ENCRYPT_MODE) {
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
			CipherInputStream cis = new CipherInputStream(is, cipher);
			doCopy(cis, os);
		} else if (mode == Cipher.DECRYPT_MODE) {
			cipher.init(Cipher.DECRYPT_MODE, desKey);
			CipherOutputStream cos = new CipherOutputStream(os, cipher);
			doCopy(is, cos);
		}
	}

	public static void doCopy(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[64];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			os.write(bytes, 0, numBytes);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	static class Input extends Thread {
		Message message;
		Input(){
			message=new Message("","");
		}
		
		public void run(){
			try{
				while(true){
					message=(Message)cipherIn.readObject();
					System.out.println("\nNew message from "+message.getUsername()+": "+message.getMessage()+"\n");
				}
			}
			catch (IOException e) {
				System.out.println("Disconnected from server ...");
				System.exit(0);
			}
			catch (ClassNotFoundException e) {
			}
		}
	}
}