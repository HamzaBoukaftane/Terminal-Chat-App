## Localhost Chat Room with Multiple Clients (server.jar & client.jar)

This repository provides a simple chat room application implemented using Java and sockets. It allows multiple clients to connect to a central server running on your local machine (localhost) and communicate in real-time.

**Key Features:**

* **Multi-Client Communication:** Supports multiple clients connecting to the server and sending messages to each other.
* **Localhost Connection:** Utilizes local IP address (127.0.0.1) for communication, making it ideal for testing and experimentation on your own machine.
* **Separate Server and Client JARs:** The application is divided into two JAR files: `server.jar` and `client.jar`.

**Tech Stack:**

* Java

**Running the Chat Room:**

1. **Prerequisites:** Ensure you have Java installed on your system. You can download and install it from [https://www.java.com/download/](https://www.java.com/download/).

2. **Clone the Repository:**

   ```bash
   git clone https://github.com/<your-username>/terminal-chat-app.git
   ```

3. **Navigate to the Directory:**

   ```bash
   cd terminal-chat-app/exec
   ```

4. **Run the Server:**

   Open a terminal window and execute the following command to start the server:

   ```bash
   java -jar server.jar
   ```

   Follow the instruction on the terminal panel and enter local IP address `127.0.0.1`.

5. **Run Multiple Clients:**

   Open additional terminal windows for each client you want to connect. In each window, execute the following command to start a client:

   ```bash
   java -jar client.jar
   ```

    Follow the instruction on the terminal panel and enter local IP address `127.0.0.1`.

**Explanation:**

* The `server.jar` establishes a server socket on the specified port (`<server_port>`) and listens for incoming client connections.
* The `client.jar` connects to the server using the provided server IP (`127.0.0.1`) and port (`<server_port>`) and opens a client socket on its own unique port (`<client_port>`).
* Clients can then send and receive messages through the established sockets, facilitating real-time chat communication.

**Additional Notes:**

* This is a basic implementation and might require further development for features like user authentication, private messaging, or message history.
* The specific code for handling messages and displaying chat functionality might reside within the JAR files themselves. Refer to the code within `server.jar` and `client.jar` (if provided) for more details.

**Contribution**

Feel free to contribute to this repository by enhancing the chat functionalities, adding new features, or improving the overall user experience.

**License**

This repository is licensed under the MIT License (see LICENSE file for details).

Happy chatting!
