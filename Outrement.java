import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;

public class Outrement {
    
    public static void main(String[] args) throws SecurityException, IOException {
        LoggerClass logInfo = new LoggerClass("OutrementServer" + ".txt");

        logInfo.logger.setLevel(Level.ALL);
        
        logInfo.logger.info("Outrement Started server");
        try {
            BookingServer bs2 = new BookingServer("OUT", logInfo);

            bs2.run();

            // while(true) {
            //     DatagramSocket ds = new DatagramSocket(7700);
            //     System.out.println("receive ds ");

            //     byte[]b1=new byte[1024];

            //     DatagramPacket dp=new DatagramPacket(b1,b1.length);

            //     ds.receive(dp);
            //     System.out.println("receive ");

            //     String str = new String(dp.getData());

            //     System.out.println("str " + str);

            //     ArrayList<String> result = new ArrayList<>();

            //     result = bs2.mapFunction(str, logInfo);

            //     System.out.println("Result in Outrement server" + result);

            //     ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //     DataOutputStream out = new DataOutputStream(baos);

            //     for (String element : result) {
            //         out.writeUTF(element);
            //     }

            //     byte[] bytes = baos.toByteArray();

            //     InetAddress ia=InetAddress.getLocalHost();

            //     DatagramPacket dp1=new DatagramPacket(bytes, bytes.length, ia, dp.getPort());

            //     ds.send(dp1);

            //     ds.close();
            // }

        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Error in verdun server " + e);

        }
    }
    
}
