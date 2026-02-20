# Multithreading-Chat

**Company Name:** CodeTech It Solutions

**Name:** Rohit Waikar

**Project :** Multithreading chat project

**Intern Id:** CTIS4163

**Domain Name:** Java Programming

**Mentor Name:** Neela Santosh

#Description

This project demonstrates the development of a multithreaded client-server chat application using Java Sockets. The primary objective of this application is to enable real-time communication between multiple clients through a centralized server. The system follows the client-server architecture model and uses multithreading to handle multiple users simultaneously without blocking communication.

In modern networking applications, client-server communication is widely used to exchange information over a network. In this project, the server acts as the central communication hub, while multiple clients connect to it to send and receive messages. The application uses Java’s built-in networking package java.net and multithreading capabilities from java.lang to implement real-time messaging.

The server program creates a ServerSocket object that listens on a specific port number. When a client attempts to connect, the server accepts the connection using the accept() method, which returns a Socket object. Each connected client is handled in a separate thread to ensure that multiple users can communicate at the same time. Without multithreading, the server would handle only one client at a time, which would block other users from joining the chat.

To implement multithreading, a separate client handler class is created that implements the Runnable interface or extends the Thread class. Each time a new client connects, the server creates a new thread for that client. This thread continuously listens for incoming messages from the client and broadcasts them to all other connected clients. This mechanism enables real-time group communication.

On the client side, a Socket object is used to connect to the server using the server’s IP address and port number. The client program has two main tasks: sending messages to the server and receiving messages from it.
