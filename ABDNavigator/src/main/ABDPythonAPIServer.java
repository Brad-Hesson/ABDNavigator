package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

//import javax.json.Json;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import javafx.application.Platform;
import navigator.CircleSelectionLayer;
import navigator.NavigationLayer;
import navigator.Positioner;

import org.json.simple.parser.JSONParser;

import java.lang.*;


public class ABDPythonAPIServer
{
	public static int port = 12345;
	public static boolean serverRunning = false;
	public static Thread serverThread;
	public static ServerSocket server;
	
	public static void startServer()
	{
		if (serverRunning)
			return;
		
		try
		{
			//server = new ServerSocket(port);
			InetAddress host = InetAddress.getByName("localhost");
			InetSocketAddress endPoint = new InetSocketAddress(host, port);
			
			server = new ServerSocket();//port);
			server.bind(endPoint);
			serverRunning = true;
			
			serverThread = new Thread()
			{
				public void run()
				{
					System.out.println("Starting Python API Server...");
					while (serverRunning)
					{
						try
						{
							Socket connection = server.accept();
							BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );
							DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
								
							String out = handleRequest(in);
							//System.out.println(out);
							outStream.writeBytes(out);
							
							
								
							Thread.sleep(5);
							System.out.println(serverRunning);
						}
						catch (Exception ex2)
						{
							ex2.printStackTrace();
						}
					}
					
					try
					{
						//Thread.sleep(10);
						server.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			};
			
			serverThread.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static boolean editorUpdated = false;
	
	private static synchronized String handleRequest(BufferedReader in)
	{
		JSONObject outObj = new JSONObject();
		
		
		try
		{
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(in.readLine());
			System.out.println("JSON String" + obj);
			JSONObject jObj = (JSONObject)obj;
			Object s = jObj.get("op");
			if (s == null)
				return "\n";
			
			int op = ((Long)s).intValue();
			s = jObj.get("type");
			int type = ((Long)s).intValue();
			s = jObj.get("seq");
			int seq = ((Long)s).intValue();
			
			System.out.println("json object: " + jObj);
			
			//System.out.println("op: " + op);
			//System.out.println("type: " + type);
			//System.out.println("seq: " + seq);
			
			
			System.out.println("type: " + type);
			
			JSONObject data = (JSONObject)jObj.get("data");
			double x;
			double y;
			Positioner p;
			
			switch (type)
			{
				case 0: // query from python
					System.out.println("op: " + op);
					switch (op)
					{
						case 7: //get new scan image
							System.out.println("get new scan");
													
							SampleNavigator.scanner.scan.startSingleScan();
							
							while (SampleNavigator.scanner.scan.isScanning())
							{
								Thread.sleep(200);
								System.out.print(".");
							}
							System.out.println();
							
							//populate the JSON image array with a 1D (flattened from 2D) array of the image data
							float[][] scanImage = SampleNavigator.scanner.scan.getScanImage();
							JSONArray img = new JSONArray();
							//for (int j = scanImage[0].length-1; j >= 0; j --)//reverse order
							for (int j = 0; j < scanImage[0].length; j ++)
								for (int i = 0; i < scanImage.length; i ++)
									img.add( Float.valueOf(scanImage[i][j]) );
							//img.add( Double.valueOf(0.6) );
							outObj.put("img", img);
							
							ABDClient.waitForTip();
							
							break;
						
						case 11: //get resolution
							System.out.println("get resolution");
							
							if (SampleNavigator.scanner == null)
							{
								//hardcode resolution for testing
								outObj.put("points", Integer.valueOf(200));
								outObj.put("lines", Integer.valueOf(200));
							}
							else
							{
								outObj.put("points", Integer.valueOf(SampleNavigator.scanner.scan.xPixels));
								outObj.put("lines", Integer.valueOf(SampleNavigator.scanner.scan.yPixels));
							}
							
							break;
					}
					
					break;
					
				case 1: // perform an action
					switch (op)
					{
						case 1: //move tip
							System.out.println("move tip");
							
							
							s = data.get("tipx");
							x = ((Double)s).doubleValue();
							s = data.get("tipy");
							y = ((Double)s).doubleValue();
							System.out.println("x,y (nm,nm): " + x + "," + y);
							
							//convert from nm to -0.5 <-> 0.5
							double width = SampleNavigator.scanner.scan.getScanWidth();
							double height = SampleNavigator.scanner.scan.getScanHeight();
							
							double scaledX = x/width;
							double scaledY = y/height;
							System.out.println("scaled x,y: " + scaledX + "," + scaledY);
							
							//this adds the positioner and waits for the GUI thread to update
							p = SampleNavigator.scanner.scan.getPositioner("PyPositioner");
							if (p == null)
								p = SampleNavigator.addPositionerLater(scaledX, scaledY, "PyPositioner", SampleNavigator.scanner.scan);
							else
							{
								p.setTranslateX(scaledX);
					    		p.setTranslateY(scaledY);
							}
							
							System.out.println("start moving tip");
							p.moveTipNoThread();
							System.out.println("done moving tip");
							
							
							break;
							
						case 4: //start scan
							System.out.println("start scan");
							SampleNavigator.scanner.scan.startScan();
							break;
							
						case 8: //change window position (nm,nm)
							System.out.println("setting wondow position");
							
							s = data.get("x");
							x = ((Double)s).doubleValue();
							
							s = data.get("y");
							y = ((Double)s).doubleValue();
							
							System.out.println("x,y: " + x + "," + y);
							
							SampleNavigator.scanner.scan.setTranslateX(x);
							SampleNavigator.scanner.scan.setTranslateY(y);
							SampleNavigator.scanner.scan.fireTransforming();
							SampleNavigator.scanner.scan.moveScanRegion();
							
							System.out.println("refreshing attribute editor");
							/*
							editorUpdated = false;
							Platform.runLater(new Runnable() 
							{
						        public void run() 
						        {
						        	SampleNavigator.refreshAttributeEditor();
						        	editorUpdated = true;
						        }
						    });*/
							SampleNavigator.refreshAttributeEditorLater();
							System.out.println("done refreshing attribute editor");
							
							while (SampleNavigator.scanner.tipIsMoving)
							{
								Thread.sleep(5);
								System.out.print(".tipIsMoving.");
							}
							
							break;
							
						case 15: //z pulse
							System.out.println("z pulse");
							
							p = SampleNavigator.scanner.scan.getPositioner("PyPositioner");
							if (p != null)
							{
								p.zRampNoThread();
								
								//hardcoded 1s pause for z ramp to complete
								Thread.sleep(1000);
								System.out.println("z pulse complete... hopefully");
							}
							
							break;
					}
					
					break;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
		String out = new String( outObj.toString() + "\n");
		//System.out.println(out);
		//out = "{\"lines\":200,\"points\":200}\n";
		return out;
	}
	
	public static void stopServer()
	{
		if (!serverRunning)
			return;
		
		serverRunning = false;
		/*
		try
		{
			Thread.sleep(10);
			server.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}*/
		
		try
		{
			Socket clientSocket = new Socket("localhost", port);
			(new DataOutputStream(clientSocket.getOutputStream())).writeBytes( (new JSONObject()).clone().toString() );
			clientSocket.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
