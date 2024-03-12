/*********************************************************************
 *
 * This file java file contains the class ClientHandler which 
 * represents a handler for individual client connections in
 * the chat application server. It manages the communication 
 * with a specific client, validates the client's credentials,
 * handles client messages reception, and broadcasts messages
 * to other connected clients.
 *
 * file: ClientHandler.java
 * authors: Hamza Boukaftane, Mehdi El Harami and Omar Benzekri
 * date: 18 may 2023
 * modified: 21 may 2023
 *
 **********************************************************************/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {

	private static int LIMIT = 15;
	
	private static int FIRST_MESSAGE = 0;
	
	private boolean isActive = true;
	
	private Socket socket; 
	
	private String username;
	
	private String password;
	
	private String userCredentialsDBName;
	
	private String messagesDBName;

	private ConcurrentHashMap<String, String> usersCredentials;
	
	private ConcurrentHashMap<String, ArrayList<String>> messagesSync;
	
	private ConcurrentHashMap<String, ClientHandler> connectedClients;
	
	private DataInputStream fromClientCanal;
	
	private DataOutputStream toClientCanal;
	
	/**
	*
	* Constructs a new ClientHandler object.
	* 
	* @param Socket socket the client socket
	* @param ConcurrentHashMap<String, String> usersCredentials the concurrent hash map containing user credentials
	* @param userCredentialsDBName the name of the user credentials database
	* @param ConcurrentHashMap<String, ArrayList<String>> messagesSync the concurrent hash map for synchronized messages
	* @param String messagesDBName the name of the messages database
	* @param ConcurrentHashMap<String, ClientHandler> connectedClients the concurrent hash map of connected clients
	* 
	*/
	public ClientHandler(
			Socket socket, 
			ConcurrentHashMap<String, String> usersCredentials, 
			String userCredentialsDBName,
			ConcurrentHashMap<String, ArrayList<String>> messagesSync,
			String messagesDBName,
			ConcurrentHashMap<String, ClientHandler> connectedClients) {
		this.socket = socket;
		this.usersCredentials = usersCredentials;
		this.userCredentialsDBName = userCredentialsDBName;
		this.messagesSync = messagesSync;
		this.messagesDBName = messagesDBName;
		this.connectedClients = connectedClients;
	}
	
	/**
	*
	* This method runs the client handler thread.
	* 
	*/
	public void run() {
		try {
			setUpCommunicationCanals();
			validateClientCredentials();
			connectedClients.put(username, this);
			String byeMessage = username + " has left the chat room.";
			while (isActive) {
				try {
					String message = fromClientCanal.readUTF();
					broadcastMessage(message, true);
				} catch (IOException e) {		
					isActive = false;
					connectedClients.remove(username);
					broadcastMessage(byeMessage, false);
				}
			}	
		} catch (IOException e) {
			System.out.println("User quit server without logging in.");
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("An error occured : \n");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @Getter
	 * This method gets the instance user name attribute
	 * 
	 * @return String user name client's user name
	 * 
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * 
	 * @Getter
	 * This method gets the instance communication canal attribute
	 * going from server to client instance
	 * 
	 * @return DataOutputStream toClientCanal communication canal to client
	 * 
	 */
	public DataOutputStream getToClientCanal() {
		return toClientCanal;
	}
	
	/**
	 * 
	 * This method sets up the communication canals for sending and receiving 
	 * data with the client instance.
	 *
	 * @throws IOException if an I/O error occurs while setting up the canals
	 * 
	 */
	private void setUpCommunicationCanals() throws IOException {
		toClientCanal = new DataOutputStream(socket.getOutputStream());
		fromClientCanal = new DataInputStream(socket.getInputStream());
	}
	
	/**
	 * 
	 * This method validates the client's credentials for authentication. If 
	 * the password is wrong, it sends message to client to try again. Else,
	 * if user name is not in the credentials database, it creates a new
	 * user, saves the new credentials, logs in the user and sends old message
	 * to client. Finally, if the user already exists and the password received 
	 * from client is valid, it logs in the user and sends old messages. It also
	 * verifies if users is already log in and send an error message to the 
	 * user trying to log in more than once at the same time.
	 *
	 * @throws IOException if an I/O error occurs during the validation process
	 * 
	 */
	private void validateClientCredentials() throws IOException {
		username = fromClientCanal.readUTF();
		password = fromClientCanal.readUTF();
		boolean isValidPassword = false;
		while (!isValidPassword) {
			boolean isAlreadyConnected = connectedClients.containsKey(username);
			boolean isExistantUser = usersCredentials.containsKey(username);
			if (isAlreadyConnected) {
				System.out.println(username + " attempted to log in more than once.\n"
						+ "The attempt was blocked.");
				toClientCanal.writeUTF("This user is already logged in.");
				username = fromClientCanal.readUTF();
				password = fromClientCanal.readUTF();
			} else if (isExistantUser && !(isAlreadyConnected)) {
				if(password.equals(usersCredentials.get(username))) {
					toClientCanal.writeUTF("Login Successful: Welcome to the chat room " + username);
					String ExistingUserMessage = username + " has joined the room";
					broadcastMessage(ExistingUserMessage, false);
					sendOldMessagesAfterLogin();
					isValidPassword = true;
				} else {
					toClientCanal.writeUTF("Invalid password : please try again.");
					System.out.println("A user tried to log in with the wrong credentials.");
					username = fromClientCanal.readUTF();
					password = fromClientCanal.readUTF();
				}
			} else {
				usersCredentials.put(username, password);
				addNewUserToCredentialsDB(username, password);
				toClientCanal.writeUTF("Account Created Successfully : Welcome to the chat room " + username);
				String NewUserMessage = "New user " + username + " has joined the room";
				broadcastMessage(NewUserMessage, false);
				sendOldMessagesAfterLogin();
				isValidPassword = true;
			}
		}
	}
	
	/**
	 * 
	* This method adds a new user to the credentials database.
	* 
	* @param String user name 
	* @param String password 
	* 
	*/
	private void addNewUserToCredentialsDB(String username,String password) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(userCredentialsDBName, true))) {
            writer.write(username + ":" + password);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("An error occurred while adding new credentials to the credentials database.");
            e.printStackTrace();
            System.out.format("Try again.");
			System.exit(1);
        }
	}
	
	/**
	*
	* This method sends up to 15 most recent messages to the client after login.
	* 
	* @throws IOException if an I/O error occurs while sending the messages
	* 
	*/
	private void sendOldMessagesAfterLogin() throws IOException {
		StringBuilder oldMessages = new StringBuilder();
		getOldMessages().forEach(message -> oldMessages.append(message).append("\n"));
		toClientCanal.writeUTF("You have " + getOldMessages().size() + " old messages\n" + oldMessages);
		
	}
	
	/**
	*
 	* This method adds new message to the message's database
 	* 
 	* @param String message
 	* 
 	*/
	private void addNewMessageToMessagesDB (String message) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(messagesDBName, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("An error occurred while adding new message to messages database.");
            e.printStackTrace();
            System.out.format("Try again.");
			System.exit(1);
        }
	}
	
	/**
	*
	* Broadcasts a message to all connected clients only if message from
	* user is not quit. Also, it adds the message to the message database
	* if the incoming message is from a client. If the message is not from
	* a client but from the server regarding new connections or 
	* disconnections, it is also broadcast to all connected user but it
	* is not added to the message's database.
	* 
	* @param String message the message to be broadcasted
	* @param boolean isClientMessage indicates if the message is from a client
	* 
	*/
	private void broadcastMessage(String message, boolean isClientMessage) {
		System.out.println(message);
		if (isClientMessage) {
			addNewMessageToMessagesDB(message);
			ArrayList<String> updatedList = getOldMessages();
			if (updatedList.size() == LIMIT) {
				updatedList.remove(FIRST_MESSAGE);
			}
			updatedList.add(message);
			messagesSync.replace("message", updatedList);
		}
		for (ClientHandler client: connectedClients.values()) {
			try {
				client.getToClientCanal().writeUTF(message);
				client.getToClientCanal().flush();
			} catch (IOException e) {
				System.out.println("An error occurred while sending to " + client.username 
									+ " new message form "+ username + ".");
				e.printStackTrace();
				System.out.format("Try again.");
				System.exit(1);
			}
		}
	}
	
	/**
	*
	* Retrieves the list of the 15 most recent messages from the 
	* synchronized messages database.
	* 
	* @return  ArrayList<String> the list of the 15 most recent messages
	* 
	*/
	private ArrayList<String> getOldMessages(){
		return messagesSync.get("message");
	}
}