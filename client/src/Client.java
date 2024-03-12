/*********************************************************************
 *
 * This file java file contains the class Client which represents
 * a client in the chat application. It allows users to connect to 
 * a server, authenticate themselves, send to server and receive 
 * messages form the server and gracefully disconnect from the server.
 *
 * file: Client.java
 * authors: Hamza Boukaftane, Mehdi El Harami and Omar Benzekri
 * date: 18 may 2023
 * modified: 21 may 2023
 *
 **********************************************************************/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client {
	
	private String username;
	
	private String password;
	
	private String serverAddress;
	
	private static int MAX_CHAR = 200;
	
	private boolean isActive = true;
	
	private int serverPort;
	
	private Socket socket;
	
	private Scanner scanner = new Scanner(System.in);
	
	private DataInputStream fromServerCanal;
	
	private DataOutputStream toServerCanal;
		
	private ClientMessageReceiver receiver;
	
	/**
	*
	* The main method to run the client application.
	* 
	* @param args the command-line arguments
	* @throws Exception if an error occurs during the client execution
	* 
	*/
	public static void main(String[] args) throws Exception {
		System.out.println("TP1 - Application Client");
		System.out.println("****************************************\n");
		Client client = new Client();
		client.joinServer();
		System.out.println("See you next time!");
		client.socket.close();
		System.exit(0);
	}
	
	/**
	*
	* This method allows clients to joins the chat server. It 
	* logs in user and allows him to receive and send
	* messages to all user in chat room in real time.
	* 
	*/
	private void joinServer() {
		getIPAddress();
		getSocket();
		authentificateClient();
		try {
			createConnexionWithServer();
			validateCredentials();
			receiver = new ClientMessageReceiver(fromServerCanal);
			Thread receiverThread = new Thread(receiver);
	        receiverThread.start();
			while (isActive) {
				sendMessage();
			}
		} catch (IOException e) {
			System.out.println("Chat room server is down.");
			System.out.println("Try again later.");
			System.exit(1);
		}
	}
	
	/**
	*
	* This method retrieves and validates the IP address of the chat server from the user.
	*
	*/
	private void getIPAddress() {
		System.out.println("Please enter desired server IP adress in this format (ex: 127.0.0.1)");
		String userInput = scanner.nextLine();
		if (!(InputValidator.isValidIPAddress(userInput))) {
			System.out.println("The IP address you have written is not valid");
			getIPAddress();
		} else {
			serverAddress = userInput;
		}
		
	}
	
	/**
	*
	* This method retrieves and validates the server port of the chat server from the user.
	*
	*/
	private void getSocket() {
		System.out.println("Please enter a port # between 5000 and 5050:");
		int userInput = scanner.nextInt();
		scanner.nextLine();
		if (!(InputValidator.isValidPortNumber(userInput))) {
			System.out.println("The port number you have written is not valid");
			getSocket();
		} else {
			serverPort = userInput;
		}
	}
	
	/**
	*
	* This method retrieves user's user name and password from user.
	*
	*/
	private void authentificateClient() {
		System.out.println("****************************************\nLogin information\n");
		System.out.println("Enter username :");
		username = scanner.nextLine();
		System.out.println("Enter password :");
		password = scanner.nextLine();
		if (username.isBlank() || password.isBlank()) {
			System.out.println("You have to enter at leats one character.");
			authentificateClient();
		}
	}
	
	/**
	*
	* This method establishes a connection with the chat server.
	* @throws IOException if an I/O error occurs while creating the connection.
	* 
	*/
	private void createConnexionWithServer() throws IOException {
		socket = new Socket(serverAddress, serverPort);
		toServerCanal = new DataOutputStream(socket.getOutputStream());
		fromServerCanal = new DataInputStream(socket.getInputStream());
	}
	
	/**
	*
	*	This method validates the client's credentials with the server.
	*
	*	@throws IOException if an I/O error occurs while validating the credentials
	*
	*/
	private void validateCredentials() throws IOException {
		toServerCanal.writeUTF(username);
		toServerCanal.writeUTF(password);
		System.out.println("Requesting communication with server ...");
		System.out.println("Server : please wait while we validate your credentials.\n");
		String validation = fromServerCanal.readUTF();
		System.out.println(validation);
		if (validation.equals("Invalid password : please try again.") || validation.equals("This user is already logged in.")) { 
			authentificateClient();
			validateCredentials();
		} else {
			System.out.println("Server started on " + serverAddress + " : " + serverPort);
			System.out.println("****************************************\n");
			String oldMessages = fromServerCanal.readUTF();
			System.out.println(oldMessages);
			System.out.println("****************************************\n");
		}
	}
	
	/**
	*
	*	This method sends user message to the server.
	*
	*	@throws IOException if an I/O error occurs while sending messages
	*
	*/
	private void sendMessage() throws IOException {
		System.out.println("Write your message or write 'quit' in order to close the client. "
				            + "Any message with more than 200 character will be cropped.");
		String inputMessage = scanner.nextLine();
		if (inputMessage.equals("quit")) {
			isActive = false;
			receiver.stop();
		} else if (!(inputMessage.isBlank())) {	
			inputMessage = inputMessage.substring(0, Math.min(inputMessage.length(), MAX_CHAR));
			LocalDateTime timestamp = LocalDateTime.now();
			DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss");
			String formattedTimestamp = timestamp.format(pattern);
			String header = "[ " + username + " - " + socket.getInetAddress().getHostAddress() 
							+ ":" + socket.getLocalPort() + " - " + formattedTimestamp + " ] : ";
			String message = header + inputMessage;
			toServerCanal.writeUTF(message);
		} else {
			System.out.println("You can not send empty message. Try again.");
			sendMessage();
		}
	}
	
	/**
	*
	*	This class contains the method for receiving messages from the server
	*	on another thread.
	*
	*/
	private class ClientMessageReceiver implements Runnable {
	    
		private boolean isActive;
		
		private DataInputStream fromServerCanal;

		/**
		*
		* Constructs a new ClientMessageReceiver object.
		* 
		* @param DataInputStream fromServerCanal communication canal from server.
		* 
		*/
		public ClientMessageReceiver( DataInputStream fromServerCanal ) {
	        this.isActive = true;
	        this.fromServerCanal = fromServerCanal;
	    }

		/**
		*
		* This method runs the message receiver thread.
		*
		*/
	    @Override
	    public void run() {
	        try {
	            while (isActive) {
	                String message = fromServerCanal.readUTF();
	                if (!(message.isBlank())) {
	                    System.out.println(message);
	                }
	            }
	            scanner.close();
	            //socket.close();
	        } catch (IOException e) {
				System.exit(1);
	        }
	    }
	    
	    /**
		*
		* This method stops the runnable message receiver thread
		* by changing class' isActive attribute to false.
		*
		*/
	    public void stop() {
	    	isActive = false;
	    }
	}
	
}
