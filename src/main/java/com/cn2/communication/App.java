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

// For audio communication
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

// For encryption and decryption
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class App extends Frame implements WindowListener, ActionListener {
	// Definition of the app's fields
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;
	static JButton callButton;			
	static JTextField meesageTextField;
	public static Color darkGray;
	public static Color lightGray;			
	final static String newline="\n";		
	
	public static String destIp;
	public static String chatPort;
	public static String voicePort;
	public static String requestsPort;
	private static TargetDataLine microphone;
	private static SourceDataLine speaker;
	private static SecretKey secretKey;
	private static boolean callInProgress = false;
	private static DatagramSocket voiceSenderSocket;
	private static DatagramSocket voiceReceiverSocket;

	// Intialize the secret key for encryption and decryption
	static {
		try {
			byte[] keyBytes = "abcdefghigklmnop".getBytes();
			secretKey = new SecretKeySpec(keyBytes, "AES");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Construct the app's frame and initialize important parameters
	public App(String title) {
		// 1. Defining the components of the GUI
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
		inputTextField.setBackground(lightGray);
		inputTextField.setColumns(31);
		
		// Setting up the TextArea.
		textArea = new JTextArea(34,36);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(lightGray);
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

		// 4. Addint a KeyListener to the TextField to send the message when the Enter key is pressed
		inputTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendButton.doClick();
				}
			}
		});
	}
	
	// The main method of the application. It continuously listens for new messages.
	public static void main(String[] args){
		// 1. Create the app's window
		App app = new App("Voice and Chat App by Tzanetis Savvas and Zoidis Vasilis");																		  
		app.setSize(425,640);			  
		app.setVisible(true);

		// Ask the user for the destination IP address and port number
		destIp = JOptionPane.showInputDialog(null, "Enter the IP address to send messages to:", "Destination IP", JOptionPane.QUESTION_MESSAGE);
		chatPort = "12345"; 	// Replace with the target port number
		voicePort = "12346"; 	// Replace with the target port number
		requestsPort = "12347"; // Replace with the target port number

		// Shows the message destination IP address
		textArea.append("Sending to " + destIp + newline);
		inputTextField.setText("");

		// Create a thread to receive messages
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket textSocket = new DatagramSocket(Integer.parseInt(chatPort))) {
				while (true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					textSocket.receive(packet);
					if (packet.getAddress().getHostAddress().equals(destIp)) {
						String message = new String(packet.getData(), 0, packet.getLength());
						textArea.append("Received: " + decryptMessage(message) + newline);
						System.out.println("Message received");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

		// Create a thread to receive call messages
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket requestsSocket = new DatagramSocket(Integer.parseInt(requestsPort))) {
				while (true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					requestsSocket.receive(packet);
					if (packet.getAddress().getHostAddress().equals(destIp)) {
						String message = new String(packet.getData(), 0, packet.getLength());
						handleCallMessages(message);
					}
				}
			} catch (IOException e) {
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
					InetAddress address = InetAddress.getByName(destIp); // Replace with the target IP address
					int port = Integer.parseInt(chatPort); // Replace with the target port number
					byte[] buffer = encryptMessage(message).getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

					// Print the message locally
					textArea.append("Me: " + message + newline);
					inputTextField.setText("");

					//Send the message to the target
					textSocket.send(packet);
					textSocket.close();
					System.out.println("Message sent");
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else if (e.getSource() == callButton) {
			// The "Call" button was clicked
			if (callInProgress) {
				sendCallMessages("CALL_END");
				endCall();
			} else {
				sendCallMessages("CALL_REQUEST");
			}
		}
	}

	// Method to encrypt messages
	private static String encryptMessage(String message) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encrypted = cipher.doFinal(message.getBytes());
			System.out.println("Encrypted message: " + Base64.getEncoder().encodeToString(encrypted));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Method to decrypt messages
	private static String decryptMessage(String message) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(message));
			System.out.println("Decrypted message: " + new String(decrypted));
			return new String(decrypted);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error Decrypting");
			return null;
		}
	}

	// Method to handle call messages
	private static void handleCallMessages(String message) {
		switch(message) {
			case "CALL_REQUEST":
				int response = JOptionPane.showConfirmDialog(null, "Do you want to accept the call?", "Incoming call", JOptionPane.YES_NO_OPTION);
				if (response == JOptionPane.YES_OPTION) {
					sendCallMessages("CALL_ACCEPTED");
					startAudioCommunication();
					startAudioReception();
					callInProgress = true;
					callButton.setText("End");
				} else
					sendCallMessages("CALL_REJECTED");
				break;
			case "CALL_ACCEPTED":
				startAudioCommunication();
				startAudioReception();
				callInProgress = true;
				callButton.setText("End");
				break;
			case "CALL_REJECTED":
				textArea.append("Call rejected by the recipient" + newline);
				break;
			case "CALL_END":
				endCall();
				textArea.append("Call ended by the other user" + newline);
				break;
			default:
				break;
		}
	}

	// Method to send call messages
	private static void sendCallMessages(String message) {
		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(destIp);
			byte[] buffer = message.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Integer.parseInt(requestsPort));
			socket.send(packet);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Method to end the call
	private static void endCall() {
		callInProgress = false;
		if (voiceSenderSocket != null && !voiceSenderSocket.isClosed()) {
			voiceSenderSocket.close();
		}
		if (voiceReceiverSocket != null && !voiceReceiverSocket.isClosed()) {
			voiceReceiverSocket.close();
		}
		if (microphone != null && microphone.isOpen()) {
			microphone.close();
		}
		if (speaker != null && speaker.isOpen()) {
			speaker.close();
		}
		callButton.setText("Call");
	}

	// Method to start sending audio data
	public static void startAudioCommunication() {
		new Thread(() -> {
			try {
				// Start capturing and sending audio data
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				microphone = (TargetDataLine) AudioSystem.getLine(info);
				microphone.open(format);
				microphone.start();

				byte[] buffer = new byte[1024];
				voiceSenderSocket = new DatagramSocket();
				InetAddress address = InetAddress.getByName(destIp);

				while (callInProgress) {
					int bytesRead = microphone.read(buffer, 0, buffer.length);
					DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, Integer.parseInt(voicePort));
					voiceSenderSocket.send(packet);
				}
			} catch (IOException | LineUnavailableException ex) {
				ex.printStackTrace();
			} finally {
				if (voiceSenderSocket != null && !voiceSenderSocket.isClosed()) {
					voiceSenderSocket.close();
				}
				if (microphone != null && microphone.isOpen()) {
					microphone.close();
				}
			}
		}).start();
	}

	// Method to start receiving audio data and playing it
	public static void startAudioReception() {
		new Thread(() -> {
			try {
				// Start receiving and playing audio data
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
				speaker = (SourceDataLine) AudioSystem.getLine(info);
				speaker.open(format);
				speaker.start();

				voiceReceiverSocket = new DatagramSocket(Integer.parseInt(voicePort));
				byte[] buffer = new byte[1024];

				while (callInProgress) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					voiceReceiverSocket.receive(packet);
					speaker.write(packet.getData(), 0, packet.getLength());
				}
			} catch (IOException | LineUnavailableException ex) {
				ex.printStackTrace();
			} finally {
				if (voiceReceiverSocket != null && !voiceReceiverSocket.isClosed()) {
					voiceReceiverSocket.close();
				}
				if (speaker != null && speaker.isOpen()) {
					speaker.close();
				}
			}
		}).start();
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
