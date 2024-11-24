package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	// Definition of the app's fields
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;
	public static Color dark_gray;
	public static Color light_gray;			
	final static String newline="\n";		
	static JButton callButton;
	
	public static String destIp;
	public static String destPort;
	
	// Construct the app's frame and initialize important parameters
	public App(String title) {
		// 1. Defining the components of the GUI

		// Setting up the characteristics of the frame
		super(title);
		dark_gray = new Color(26, 26, 26);
		light_gray = new Color(211, 211, 211);
		setBackground(dark_gray);
		setLayout(new FlowLayout());
		addWindowListener(this);
		setResizable(false);
		
		// Setting up the TextField
		inputTextField = new TextField();
		inputTextField.setColumns(31);
		
		// Setting up the TextArea.
		textArea = new JTextArea(34,40);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(light_gray);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		// Setting up the buttons
		sendButton = new JButton("Send");
		callButton = new JButton("Call");
						
		// 2. Adding the components to the GUI
		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);
		
		// 3. Linking the buttons to the ActionListener
		sendButton.addActionListener(this);
		callButton.addActionListener(this);
	}
	
	// The main method of the application. It continuously listens for new messages.
	public static void main(String[] args){
		destIp = JOptionPane.showInputDialog(null, "Enter the IP address to send messages to:", "Destination IP", JOptionPane.QUESTION_MESSAGE);
		destPort = JOptionPane.showInputDialog(null, "Enter the port number to send messages to:", "Destination Port", JOptionPane.QUESTION_MESSAGE);

		// 1. Create the app's window
		App app = new App("Voice and Chat App by Tzanetis Savvas and Zoidis Vasilis");																		  
		app.setSize(425,640);				  
		app.setVisible(true);

		// Shows the message destination IP address
		textArea.append("Sending to " + destIp + "on port " + destPort + newline);
		inputTextField.setText("");

		// 2. Listen for new messages
		do{		
			new Thread(() -> {
				while (true) {
					try {
						DatagramSocket socket = new DatagramSocket(8080); // Replace with the port number you want to use
						byte[] buffer = new byte[1024];
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						socket.receive(packet);
						String message = new String(buffer, 0, packet.getLength());
						textArea.append("Stranger: " + message + newline);
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}while(true);
	}
	
	// The method that corresponds to the Action Listener. Whenever an action is performed
	// (i.e., one of the buttons is clicked) this method is executed. 
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check which button was clicked.
		if (e.getSource() == sendButton){	
			String message = inputTextField.getText();
			if (!message.trim().isEmpty()) {
				try {
					DatagramSocket socket = new DatagramSocket();
					InetAddress address = InetAddress.getByName(destIp); // Replace with the target IP address
					int port = 8080; // Replace with the target port number
					byte[] buffer = message.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

					// Print the message locally
					textArea.append("Me: " + message + newline);
					inputTextField.setText("");

					//Send the message to the target
					socket.send(packet);
					socket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}else if(e.getSource() == callButton){
			// The "Call" button was clicked	
			// TODO: Your code goes here...
		}
	}

	// These methods have to do with the GUI. You can use them if you wish to define what the program should do in specific scenarios
	// (e.g., when closing the window).
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
