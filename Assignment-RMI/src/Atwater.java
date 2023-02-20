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

public class Atwater {
    private static void addStaticData(RemoteImplementation exportedObj) {
        exportedObj.movies.put("AVENGERS",new ConcurrentHashMap<>(){
            {
                put("ATWA150223", new Booking(4));
            }
        });
        exportedObj.movies.get("AVENGERS").put("ATWA160223",new Booking(5));
        exportedObj.movies.get("AVENGERS").put("ATWA170223",new Booking(3));
        exportedObj.movies.put("TITANIC",new ConcurrentHashMap<>(){
            {
                put("ATWM170223", new Booking(4));
            }
        });
        exportedObj.movies.get("TITANIC").put("ATWM160223",new Booking(5));
        exportedObj.movies.get("TITANIC").put("ATWM170223",new Booking(3));
        exportedObj.movies.get("TITANIC").put("ATWE160223",new Booking(8));
        // rms
        exportedObj.movies.put("AVATAR",new ConcurrentHashMap<>(){
            {
                put("ATWA150223", new Booking(6));
            }
        });
        exportedObj.movies.get("AVATAR").put("ATWA160223",new Booking(8));

        // static data for getBookingSchedule
        exportedObj.movies.get("AVENGERS").put("ATWM160223", new Booking(8));

        ArrayList<String> customers = new ArrayList<>();
        customers.add("ATWC7777");
        customers.add("VERC7777");
        customers.add("OUTC7777");
        exportedObj.movies.get("AVENGERS").get("ATWM160223").cust_ids = customers;

        ArrayList<Customer> shows = new ArrayList<>();
        shows.add(new Customer("AVENGERS",3, "ATWM160223"));
        shows.add(new Customer("AVATAR",2, "ATWM160223"));
        exportedObj.customers.put("ATWC7777", shows);
        ArrayList<Customer> shows1 = new ArrayList<>();
        shows1.add(new Customer("AVENGERS", 2,"ATWM160223"));
        shows1.add(new Customer("AVATAR",2, "ATWM160223"));
        exportedObj.customers.put("VERC7777", shows1);
        ArrayList<Customer> shows2 = new ArrayList<>();
        shows2.add(new Customer("AVENGERS", 1,"ATWM160223"));
        shows2.add(new Customer("AVATAR",1, "ATWM160223"));
        exportedObj.customers.put("OUTC7777", shows2);
    }

    public static void main(String[] args) throws IOException {
        LoggerClass logInfo = new LoggerClass("AtwaterServer" + ".txt");

        logInfo.logger.setLevel(Level.ALL);

        logInfo.logger.info("Atwater Started server");
        try {
            RemoteImplementation exportedObj = new RemoteImplementation("ATW", logInfo);

            Naming.rebind("ATW", exportedObj);

            addStaticData(exportedObj);

            System.out.println("Atwater server registered. ");

            while(true) {
                DatagramSocket ds = new DatagramSocket(7600);

                byte[]b1=new byte[1024];

                DatagramPacket dp=new DatagramPacket(b1,b1.length);

                ds.receive(dp);

                String str = new String(dp.getData());

                ArrayList<String> result = new ArrayList<>();

                result = exportedObj.mapFunction(str, logInfo);

                System.out.println("Result in Atwater server" + result);

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
            System.out.println("Exception in Atwater.main: " + e);
        }
    }

//    private static void startRegistry(int RMIPortNum) throws RemoteException {
//        try {
//            Registry registry = LocateRegistry.getRegistry(RMIPortNum);
//            registry.list();
//        } catch (Exception e) {
//            System.out.println("RMI registry cannot be located at port " + RMIPortNum);
//            Registry registry = LocateRegistry.createRegistry(RMIPortNum);
//            System.out.println("RMI registry created at port " + RMIPortNum);
//        }
//    }
//
//    private static void listRegistry(String registryURL) throws RemoteException, MalformedURLException {
//        System.out.println("Registry " + registryURL + " contains: ");
//        String [] names = Naming.list(registryURL);
//        for(int i=0; i<names.length; i++) {
//            System.out.println(names[i]);
//        }
//
//    }

}