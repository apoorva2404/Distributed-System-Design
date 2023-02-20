//import java.net.InetAddress;
//import java.net.DatagramSocket;
//import java.net.DatagramPacket;
//class Thread1 implements Runnable{
//    int portNumber;
//    RemoteImplementation exportedObj;
//    public Thread1(int portNumber, RemoteImplementation exportedObj) {
//        this.portNumber = portNumber;
//        this.exportedObj = exportedObj;
//    }
//    public void run() {
//        try {
//            while (true) {
//                System.out.println("PortNumber " + portNumber);
//
//                DatagramSocket datagramSocket = new DatagramSocket(portNumber);
//
//                byte[] b1 = new byte[1024];
//
//                DatagramPacket dp1 = new DatagramPacket(b1, b1.length);
//
//                datagramSocket.receive(dp1);
//
//                String str = new String(dp1.getData());
//
//                System.out.println(str);
//
//                byte[] b2 = exportedObj.mapFunction(str);
//
//                System.out.println(b2);
//                InetAddress ia = InetAddress.getLocalHost();
//                DatagramPacket dp2 = new DatagramPacket(b2, b2.length, ia, dp1.getPort());
//                datagramSocket.send(dp2);
//                datagramSocket.close();
//            }
//        } catch (Exception e) {
//            System.out.println("ServerThread"+e);
//        }
//    }
//}