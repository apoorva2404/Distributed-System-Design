import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;

public class Atwater {

    public static void main(String[] args) throws SecurityException, IOException {
        LoggerClass logInfo = new LoggerClass("AtwaterServer" + ".txt");

        logInfo.logger.setLevel(Level.ALL);

        logInfo.logger.info("Atwater Started server");
        try {
            BookingServer bs1 = new BookingServer("ATW", logInfo);
            
            bs1.run();
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Error in atwater server " + e);

        }
    }
}
