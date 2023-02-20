import java.io.IOException;
import java.rmi.*;
import java.net.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Verdun {
    private static void addStaticData(RemoteImplementation exportedObj) {
        exportedObj.movies.put("TITANIC",new ConcurrentHashMap<>(){
            {
                put("VERA150223", new Booking(6));
            }
        });
        exportedObj.movies.get("TITANIC").put("VERA160223",new Booking(8));
        exportedObj.movies.put("AVENGERS",new ConcurrentHashMap<>(){
            {
                put("VERM170223", new Booking(4));
            }
        });
        exportedObj.movies.get("AVENGERS").put("VERM150223",new Booking(5));
        exportedObj.movies.get("AVENGERS").put("VERM180223",new Booking(3));
        exportedObj.movies.get("AVENGERS").put("VERE180223",new Booking(8));
        exportedObj.movies.put("AVATAR",new ConcurrentHashMap<>(){
            {
                put("VERA150223", new Booking(4));
            }
        });
        exportedObj.movies.get("AVATAR").put("VERA170223",new Booking(5));
        exportedObj.movies.get("AVATAR").put("VERM190223",new Booking(3));
    }

    public static void main(String[] args) throws IOException {
        LoggerClass logInfo = new LoggerClass("VerdunServer" + ".txt");
        logInfo.logger.setLevel(Level.ALL);
        logInfo.logger.info("Verdun Started server");

        try {
            RemoteImplementation exportedObj = new RemoteImplementation("VER", logInfo);

            Naming.rebind("VER", exportedObj);

            System.out.println("Verdun server registered. ");

            addStaticData(exportedObj);

            while(true) {
                DatagramSocket ds = new DatagramSocket(7800);
                byte[]b1=new byte[1024];

                DatagramPacket dp=new DatagramPacket(b1,b1.length);

                ds.receive(dp);

                String str = new String(dp.getData());

                ArrayList<String> result = new ArrayList<>();

                result = exportedObj.mapFunction(str, logInfo);

                System.out.println("Result in Verdun server" + result);

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
            logInfo.logger.info(e.getMessage());
            System.out.println("Exception in Verdun.main: " + e);
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

}