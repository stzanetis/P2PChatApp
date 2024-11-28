package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class App extends Frame implements WindowListener, ActionListener {

	// Definition of the app's fields
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;
	static JButton callButton;		
	static JTextField meesageTextField;
	public static Color darkGray;			
	final static String newline="\n";		

	// TODO: Please define and initialize your variables here...
	public static Color lightGray;
	public static String destIp;
	public static String chatPort;
	public static String voicePort;
	
	// Construct the app's frame and initialize important parameters
	public App(String title) {
		// 1. Defining the components of the GUI
		// Setting up the characteristics of the frame
		super(title);
		darkGray = new Color(26, 26, 26);
		lightGray = new Color(211, 211, 211);
		setBackground(darkGray);
		setLayout(new FlowLayout());
		addWindowListener(this);

		setResizable(false);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth() - 425) / 2, (screenSize.height - getHeight() - 640) / 2);
		
		// Setting up the TextField
		inputTextField = new TextField();
		inputTextField.setColumns(31);
		inputTextField.setBackground(lightGray);
		
		// Setting up the TextArea.
		textArea = new JTextArea(34,36);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textArea.setBackground(lightGray);
		
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
		// 1. Create the app's window
		App app = new App("Voice and Chat App by Tzanetis Savvas and Zoidis Vasilis");																		  
		app.setSize(425,640);			  
		app.setVisible(true);

		// Ask the user for the destination IP address and port number
		destIp = JOptionPane.showInputDialog(null, "Enter the IP address to send messages to:", "Destination IP", JOptionPane.QUESTION_MESSAGE);
		chatPort = JOptionPane.showInputDialog(null, "Enter the port number to send messages to:", "Destination Port", JOptionPane.QUESTION_MESSAGE);
		voicePort = JOptionPane.showInputDialog(null, "Enter the port number to send voice data to:", "Voice Port", JOptionPane.QUESTION_MESSAGE);

		// Shows the message destination IP address
		textArea.append("Sending to " + destIp + newline);
		inputTextField.setText("");

		// Create a thread to receive text data
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket textSocket = new DatagramSocket(Integer.parseInt(chatPort))) {
				while(true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					textSocket.receive(packet);
					String senderIp = packet.getAddress().getHostAddress();
					if (senderIp.equals(destIp)) {
						String message = new String(packet.getData(), 0, packet.getLength());
						textArea.append("Received: " + message + "\n");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

		// Create a thread to receive audio data
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket audioSocket = new DatagramSocket(Integer.parseInt(voicePort));){
				// Create a speaker to play the audio
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
				SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
				speakers.open(format);
				speakers.start();
			
				while(true) {
					DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
					audioSocket.receive(requestPacket);
					String message0 = new String(requestPacket.getData(), 0, requestPacket.getLength());
					if (message0.equals("CALL_REQUEST")) {
						int response = JOptionPane.showConfirmDialog(null, "Do you want to accept the call?", "Incoming Call", JOptionPane.YES_NO_OPTION);
						if (response == JOptionPane.NO_OPTION) {
							return;
						} else {
							while(true) {
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
								audioSocket.receive(packet);
								String senderIp = packet.getAddress().getHostAddress();
								if (senderIp.equals(destIp)) {
									speakers.write(packet.getData(), 0, packet.getLength());
								}
							}
						}
					}
				}
			} catch (LineUnavailableException | IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	// The method that corresponds to the Action Listener. Whenever an action is performed
	// (i.e., one of the buttons is clicked) this method is executed. 
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check which button was clicked.
		if (e.getSource() == sendButton){	
			// The "Send" button was clicked
			String message = inputTextField.getText();
			if (!message.trim().isEmpty()) {
				try {
					DatagramSocket textSocket = new DatagramSocket();
					InetAddress address = InetAddress.getByName(destIp);
					int port = Integer.parseInt(chatPort);
					byte[] buffer = message.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

					// Print the message locally
					textArea.append("Me: " + message + newline);
					inputTextField.setText("");

					// Send the message to the target
					textSocket.send(packet);
					textSocket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else if (e.getSource() == callButton) {
			// The "Call" button was clicked
			// Send a call request to the target
			try {
				DatagramSocket callRequestSocket = new DatagramSocket();
				InetAddress address = InetAddress.getByName(destIp);
            	int callRequestPort = Integer.parseInt(voicePort);
            	String callRequestMessage = "CALL_REQUEST";
				byte[] buffer = callRequestMessage.getBytes();
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, callRequestPort);

				// Send the call request to the target
				callRequestSocket.send(packet);
				callRequestSocket.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				// Create a socket to send and receive audio data
				DatagramSocket audioSocket = new DatagramSocket();
				InetAddress address = InetAddress.getByName(destIp);
				int port = Integer.parseInt(voicePort);

				// Capture audio from the microphone
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
				microphone.open(format);
				microphone.start();

				// Create a thread to send audio data
				new Thread(() -> {
					byte[] buffer = new byte[1024];
					try {
						while(true) {
							int bytesRead = microphone.read(buffer, 0, buffer.length);
							DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, port);
							audioSocket.send(packet);
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					} finally {
						microphone.close();
						audioSocket.close();
					}
				}).start();
			} catch (LineUnavailableException | IOException ex) {
				ex.printStackTrace();
			}
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
