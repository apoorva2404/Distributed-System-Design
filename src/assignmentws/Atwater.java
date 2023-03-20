package assignmentws;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.xml.ws.Endpoint;

public class Atwater {
	public static void main(String[] args) throws SecurityException, IOException {
        LoggerClass logInfo = new LoggerClass("Atwater" + ".txt");

        logInfo.logger.setLevel(Level.ALL);

        BookingImplementation atw = new BookingImplementation("ATW", logInfo);
		Endpoint endpoint = Endpoint.publish("http://localhost:8080/atwater", atw);

        Thread UDPCommunicator = new Thread(){
            public void run() {           
                try {
                while(true) {
                    // int port = serverName.substring(0, 3).equals("ATW") ? 7600 : serverName.substring(0, 3).equals("VER") ? 7800 :   serverName.substring(0, 3).equals("OUT") ? 7700 : 7600;

                    DatagramSocket ds = new DatagramSocket(7600);
    
                    byte[]b1=new byte[1024];
    
                    DatagramPacket dp=new DatagramPacket(b1,b1.length);
    
                    ds.receive(dp);
    
                    String str = new String(dp.getData());
    
                    ArrayList<String> result = new ArrayList<>();
    
                    result = atw.mapFunction(str, logInfo);
    
                    // System.out.println("Result in UDP server " + serverName + " " + result);
    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
                    DataOutputStream out = new DataOutputStream(baos);
    
                    for (String element : result) {
                        out.writeUTF(element);
                    }
    
                    byte[] bytes = baos.toByteArray();
    
                    InetAddress ia=InetAddress.getLocalHost();
    
                    DatagramPacket dp1=new DatagramPacket(bytes, bytes.length, ia, dp.getPort());
    
                    ds.send(dp1);
    
                    ds.close();
                }
            } catch(Exception err) {
                System.out.println("Error in Atwater thread " + err);
                err.printStackTrace();
            }
        }
    };

    UDPCommunicator.start();
    logInfo.logger.info("Atwater service is published " + endpoint.isPublished());
	// System.out.println("Atwater service is published: " + endpoint.isPublished());
	}

}