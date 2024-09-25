package com;

import java.io.*;
import java.net.*;

import controllers.nanonis.TypeWriter;

public class ABDReverseClient {
	public static int port = 6888;// was 6788

	public static TypeWriter out;

	public static void initClient() {
		try {

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void drawLine(int idx, double[] data) {
		if (!ABDServer.serverRunning)
			return;
		try {
			Socket clientSocket = new Socket("localhost", port);
			out = new TypeWriter(clientSocket.getOutputStream());
			out.writeString("drawLine");
			out.writeInt(idx);
			out.writeArrayFloat64_1D(data);
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void lithoStep(int idx) {
		if (!ABDServer.serverRunning)
			return;
		try {
			Socket clientSocket = new Socket("localhost", port);
			out = new TypeWriter(clientSocket.getOutputStream());
			out.writeString("lithoStep");
			out.writeInt(idx);
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
