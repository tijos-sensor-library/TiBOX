package tibox;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NB200UDPSample {

	public static void main(String[] args) {

		try {
			
			NB200.networkStartup();
			
		   	System.out.println("UdpDemo start...");
			DatagramSocket udpSocket  = null;
	    	try
	    	{
		    		
				udpSocket = new DatagramSocket();
		        String host = "192.168.1.86";
		        int port = 8080;
		        
		
		        byte [] msg = ("Hello Server").getBytes();
		       
		        DatagramPacket dp = new DatagramPacket(msg, msg.length, InetAddress.getByName(host), port);
		        udpSocket.send(dp);
		        
		        byte [] buffer = new byte[1024];
	        	dp.setData(buffer);
	        	dp.setAddress(null);
		        	
	            udpSocket.receive(dp);
		            
	            String info = new String(dp.getData(), 0, dp.getLength());
	            System.out.println("Received: " + info);
	            System.out.println("Remote :" + dp.getAddress().getHostAddress());
	            
	    	}
	    	catch(Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	finally
	    	{
	    		udpSocket.close();
	    	}
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
