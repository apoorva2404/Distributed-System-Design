import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;

public class Verdun {

    public static void main(String[] args) throws SecurityException, IOException {
        LoggerClass logInfo = new LoggerClass("VerdunServer" + ".txt");

        logInfo.logger.setLevel(Level.ALL);

        logInfo.logger.info("Verdun Started server");
        try {
            BookingServer bs2 = new BookingServer("VER", logInfo);

            bs2.run();

            // while(true) {
            //     System.out.println("while me ");
            //     DatagramSocket ds = new DatagramSocket(7800);
            //     byte[]b1=new byte[1024];

            //     DatagramPacket dp=new DatagramPacket(b1,b1.length);
            //     System.out.println("dp ");

            //     ds.receive(dp);

            //     String str = new String(dp.getData());

            //     System.out.println("str " + str);

            //     ArrayList<String> result = new ArrayList<>();

            //     System.out.println("result " + result);

            //     result = bs2.mapFunction(str, logInfo);

            //     System.out.println("Result in Verdun server" + result);

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
