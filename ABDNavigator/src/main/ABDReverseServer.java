package main;

import java.io.*;
import java.net.*;

import navigator.LithoRasterLayer;

public class ABDReverseServer {
	public static int port = 6888;// was 6788
	public static boolean serverRunning = false;
	public static Thread serverThread;
	public static ServerSocket server;

	public static void startServer() {
		if (serverRunning)
			return;

		try {
			InetAddress host = InetAddress.getByName("localhost");
			InetSocketAddress endPoint = new InetSocketAddress(host, port);

			server = new ServerSocket();// port);
			server.bind(endPoint);
			serverRunning = true;

			serverThread = new Thread() {
				public void run() {
					// try
					// {
					System.out.println("Starting Reverse ABDServer...");
					while (serverRunning) {
						try {
							Socket connection = server.accept();
							TypeReader in = new TypeReader(new DataInputStream(connection.getInputStream()));
							String req = in.readString();
							switch (req) {
								case "lithoStep":
									int idx = in.readInt();
									LithoRasterLayer.highlightInstanceSegment(idx);
									break;
								case "drawLine":
									int lineNumber = in.readInt();
									double[] data = in.readArrayFloat64_1D();
									SampleNavigator.scanner.scan.setLine(lineNumber, data);
									break;
								default:
									break;
							}
							connection.close();
						} catch (Exception ex2) {
							ex2.printStackTrace();
						}
					}
					// }
					// catch (Exception ex2)
					// {
					// ex2.printStackTrace();
					// stopServer();
					// }
				}
			};

			serverThread.start();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void stopServer() {
		if (!serverRunning)
			return;

		serverRunning = false;

		try {
			Thread.sleep(10);
			server.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
