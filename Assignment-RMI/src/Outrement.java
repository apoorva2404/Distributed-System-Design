import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Outrement {
    private static void addStaticData(RemoteImplementation exportedObj) {
        exportedObj.movies.put("TITANIC",new ConcurrentHashMap<>(){
            {
                put("OUTM150223", new Booking(4));
            }
        });
        exportedObj.movies.get("TITANIC").put("OUTM160223",new Booking(5));
        exportedObj.movies.get("TITANIC").put("OUTM150223",new Booking(3));
        exportedObj.movies.get("TITANIC").put("OUTE160223",new Booking(8));
        exportedObj.movies.put("AVATAR",new ConcurrentHashMap<>(){
            {
                put("OUTA180223", new Booking(6));
            }
        });
        exportedObj.movies.get("AVATAR").put("OUTA150223",new Booking(8));
        exportedObj.movies.put("AVENGERS",new ConcurrentHashMap<>(){
            {
                put("OUTA160223", new Booking(4));
            }
        });
        exportedObj.movies.get("AVENGERS").put("OUTE150223",new Booking(5));
        exportedObj.movies.get("AVENGERS").put("OUTA170223",new Booking(3));
    }

    public static void main(String[] args) throws IOException {
        LoggerClass logInfo = new LoggerClass("OutrementServer" + ".txt");
        logInfo.logger.setLevel(Level.ALL);
        logInfo.logger.info("Outrement Started server");
        try {
                RemoteImplementation exportedObj = new RemoteImplementation("OUT", logInfo);

                Naming.rebind("OUT", exportedObj);

                addStaticData(exportedObj);

                System.out.println("Outrement server registered. ");

                while(true) {
                    DatagramSocket ds = new DatagramSocket(7700);

                    byte[]b1=new byte[1024];

                    DatagramPacket dp=new DatagramPacket(b1,b1.length);

                    ds.receive(dp);

                    String str = new String(dp.getData());

                    ArrayList<String> result = new ArrayList<>();

                    result = exportedObj.mapFunction(str, logInfo);

                    System.out.println("Result in Outrement server" + result);

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
        } catch (Exception e) {
            e.printStackTrace();
            logInfo.logger.info(e.getMessage());
            System.out.println("Exception in Outrement.main: " + e);
        }
    }

//    private static void startRegistry(int RMIPortNum) throws RemoteException {
//        try {
//            Registry registry = LocateRegistry.getRegistry(RMIPortNum);
//            registry.list();
//        } catch (Exception e) {
//            System.out.println("RMI registry cannot be located at port " + RMIPortNum);
//            Registry registry = LocateRegistry.createRegistry(RMIPortNum);
//            System.out.println("RMI registry create at port " + RMIPortNum);
//        }
//    }
//
//    private static void listRegistry(String registryURL) throws RemoteException, MalformedURLException {
//        System.out.println("Registry " + registryURL + "contains: ");
//        String [] names = Naming.list(registryURL);
//        for(int i=0; i<names.length; i++) {
//            System.out.println(names[i]);
//        }
//
//    }
//
}