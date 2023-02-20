import java.awt.print.Book;
import java.lang.reflect.Array;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

class Booking {
    public Booking(int capacity) {
        this.capacity = capacity;
    }
     public Booking(int capacity, String cust_ids) {
         this.capacity = capacity;
         this.cust_ids.add(cust_ids);
     }
     public int capacity;
     public ArrayList<String> cust_ids = new ArrayList<>();
 }

 class Customer {
    public Customer(String movieName, int bookedTickets, String movieId) {
        this.bookedTickets = bookedTickets;
        this.movieName = movieName;
        this.movieId = movieId;
    }
    public int bookedTickets;
    public String movieName;
    public String movieId;

 }

 class SortMovieDates {

 }

public class RemoteImplementation extends UnicastRemoteObject implements RemoteInterface {
    ConcurrentHashMap<String, ConcurrentHashMap<String, Booking>> movies = new ConcurrentHashMap<String, ConcurrentHashMap<String, Booking>>();
    ConcurrentHashMap<String, ArrayList<Customer>> customers = new ConcurrentHashMap<String, ArrayList<Customer>>();

    String serverId;

    LoggerClass logInfo;

    public RemoteImplementation(String serverId, LoggerClass logInfo) throws RemoteException {
        super();
        this.serverId = serverId;
        this.logInfo = logInfo;
    }

    private static String getSpecificPortFromMovieId(ArrayList<String> ports, String movieId) {
        if(movieId.substring(0, 3).equals("ATW")) {
            return "7600";
        } else if(movieId.substring(0, 3).equals("OUT")) {
            return "7700";
        } else if(movieId.substring(0, 3).equals("VER")) {
            return "7800";
        }
        return null;
    }

     @Override
    public String addMovieSlots(String movieID, String movieName, int bookingCapacity) throws RemoteException {
        // logger
         logInfo.logger.info("Adding " + bookingCapacity + " slots for " + movieID + " " + movieName);

         if(!movieID.substring(0, 3).equals(this.serverId)) {
             logInfo.logger.info("You can only add movies in your respective location");
             return "You can only add movies in your respective location";
         }
         if(movies.get(movieName) != null) {

             if(movies.get(movieName).get(movieID) != null) {

                 movies.get(movieName).get(movieID).capacity = movies.get(movieName).get(movieID).capacity + bookingCapacity;
             } else {
                 movies.get(movieName).put(movieID, new Booking((bookingCapacity)));
             }
         } else {
             movies.put(movieName, new ConcurrentHashMap(){{put(movieID, new Booking(bookingCapacity));}});
         }

         System.out.println("***************************");
         for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
             String movie = entry.getKey();
             System.out.println("------------------------------");
             for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
                 String movieId = custEntry.getKey();
                 Booking booking = custEntry.getValue();
                 System.out.println(movie + " - " + movieId + " - " + booking.capacity);
             }
         }
         // logger
         logInfo.logger.info("Added movie slots successfully " + LocalDateTime.now());
         return "Added movie slots successfully! ";
    }

    @Override
    public String removeMovieSlots (String movieID, String movieName) throws RemoteException, ParseException {
        logInfo.logger.info("Removing moving slots for " + movieID + " " + movieName);
        if(movies.get(movieName) == null) {
            logInfo.logger.info("Error occurred! Please enter a valid movie name");
            return "Please enter valid movie name";
        }
        if(movies.get(movieName).get(movieID) == null) {
            logInfo.logger.info("Error occurred! Please enter a valid movie id");
            return "Please enter valid movie id";
        }
        ArrayList<String> custIds = new ArrayList<>();
        custIds = movies.get(movieName).get(movieID).cust_ids;
        int targetMovieCapacity = movies.get(movieName).get(movieID).capacity;

        ConcurrentHashMap<String, Integer> customersToBeRescheduled = new ConcurrentHashMap<>();

        int ticketsToBeRemoved = 0;
         for (Map.Entry<String, ArrayList<Customer>> mapElement : customers.entrySet()) {
             String customerId = mapElement.getKey();
             ArrayList<Customer> listOfCustomerMovies = mapElement.getValue();

             for(Customer cData: listOfCustomerMovies) {
                 if(cData.movieName.equals(movieName) && cData.movieId.equals(movieID)){
                     ticketsToBeRemoved = ticketsToBeRemoved + cData.bookedTickets;
                     customersToBeRescheduled.put(customerId, cData.bookedTickets);
                     customers.get(customerId).remove(cData);
                     break;
                 }
             }
         }

         ArrayList<String> availableShows =  listMovieShowsAvailability(movieName, false);

        TreeMap<String, Integer> availableSlots = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) { // "ATWM090823"
                String dateString1 = o1.substring(4, 10);
                String dateString2 = o2.substring(4, 10);

                SimpleDateFormat date1 = new SimpleDateFormat("ddMMyy");
                try {
                    Integer res = date1.parse(dateString1).compareTo(date1.parse(dateString2));
                    if(res == 0) {
                        // M A E
                        Character[] timimgs = {'M', 'A', 'E'};

                        return Arrays.asList(timimgs).indexOf(o1.charAt(3)) > Arrays.asList(timimgs).indexOf(o1.charAt(3)) ? 1 : -1;
                    } else {
                        return res > 0 ? 1 : -1;
                    }

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // loop availableShows then take "ATWM080923 8" => split => availableSlots.put(item.split(" ")[0], Integer.prseInt(item.split(" ")[1]))
        for(String as: availableShows) {
            availableSlots.put(as.split(" ")[0], Integer.parseInt(as.split(" ")[1]));
        }

        Map.Entry<String, Integer>[] availableSlotsList = availableSlots.entrySet().toArray(new Map.Entry[availableSlots.size()]);

        int targetSlotIndex = Arrays.asList(availableSlotsList).indexOf(Map.entry(movieID, targetMovieCapacity));

        int movieSlotsLength = Arrays.asList(availableSlotsList).size();
        if(targetSlotIndex + 1 == movieSlotsLength) {
            // logger
            movies.get(movieName).remove(movieID);
            logInfo.logger.info("No other slots available. Please refund the amount to customers as the slot has been removed");
            return "No other slots available. Please refund the amount to customers";
        }

        boolean booked = false;
        // Book for all customers at once or dont book at all
        for(int i=targetSlotIndex + 1; i<movieSlotsLength; i++) {
            if(availableSlotsList[i].getValue() >= ticketsToBeRemoved) {
                for(Map.Entry<String, Integer> c: customersToBeRescheduled.entrySet()) {
                    bookMovieTickets(c.getKey(), availableSlotsList[i].getKey(), movieName, c.getValue(), false);
                }
                booked = true;
                break;
            }
        }

        if(!booked) {
            // logger
            movies.get(movieName).remove(movieID);
            logInfo.logger.info("No other slots available. Please refund the amount to customers as the slot has been removed");
            return "No other slots available. Please refund the amount to customers";
        }

        movies.get(movieName).remove(movieID);

        // logger
        logInfo.logger.info("Removed Movie Slot! " + LocalDateTime.now());
        return "Removed Movie Slot!";
    };

     @Override
     // we can call requestOtherServers from here
    public ArrayList listMovieShowsAvailability(String movieName, boolean isClientCall) {
         logInfo.logger.info("Available Movie shows for" + movieName);
         ArrayList<String> availableShows = new ArrayList<>();
         try {
             ConcurrentHashMap<String, Booking> shows = movies.get(movieName);
             System.out.println("Available shows for movie - " + movieName);
             for (Map.Entry<String, Booking> show : shows.entrySet()) {
                 if(show.getValue().capacity > 0){
                     availableShows.add(show.getKey() + " " +show.getValue().capacity);
                 }
             }
             if(!!isClientCall) {
                 DatagramSocket dsVerdun = new DatagramSocket();

                 DatagramSocket dsOutrement = new DatagramSocket();

                 String functionNamesWithParameter = "listMovieShowsAvailability" + "-" + movieName;

                 byte[]b = functionNamesWithParameter.getBytes();

                 InetAddress ia = InetAddress.getLocalHost();

                 ArrayList<String> ports = new ArrayList<>();

                 ports = getPort(this.serverId);

                 DatagramPacket dpVerdun = new DatagramPacket(b ,b.length, ia, Integer.parseInt(ports.get(0)));

                 DatagramPacket dpOutrement = new DatagramPacket(b ,b.length, ia, Integer.parseInt(ports.get(1)));

                 dsVerdun.send(dpVerdun);

                 dsOutrement.send(dpOutrement);

                 byte[] b1=new byte[1024];

                 byte[] b2=new byte[1024];

                 DatagramPacket dpVerdun1=new DatagramPacket(b1,b1.length);

                 dsVerdun.receive(dpVerdun1);

                 DatagramPacket dpOutrement1=new DatagramPacket(b2,b2.length);

                 dsOutrement.receive(dpOutrement1);

                 ByteArrayInputStream bais1 = new ByteArrayInputStream(dpVerdun1.getData());
                 ByteArrayInputStream bais2 = new ByteArrayInputStream(dpOutrement1.getData());

                 DataInputStream in1 = new DataInputStream(bais1);
                 DataInputStream in2 = new DataInputStream(bais2);

                 while (in1.available() > 0) {
                     String element = in1.readUTF();
                     if(element.equals("")){
                         break;
                     }
                     availableShows.add(element);
                 }

                 while (in2.available() > 0) {
                     String element = in2.readUTF();
                     if(element.equals("")){
                         break;
                     }
                     availableShows.add(element);
                 }
             }

         } catch (Exception e) {
             e.printStackTrace();
             System.out.println("Exception in listMovieShowsAvailability" + e);
         }
         logInfo.logger.info("Available Movie shows for" + movieName + " " + availableShows);
         return availableShows;
    };



     private static String bookCancelMovieOnOtherServer(String customerID, String movieID, String movieName, int numberOfTickets, boolean cancel) throws IOException {
         DatagramSocket dsServer1 = new DatagramSocket();
         String functionNamesWithParameter;
         if(cancel){
             functionNamesWithParameter = "cancelMovieTickets" + "-" + customerID + "-" + movieID + "-" + movieName + "-" + numberOfTickets;
         } else {
             functionNamesWithParameter = "bookMovieTickets"  + "-" + customerID + "-" + movieID + "-" + movieName + "-" + numberOfTickets;
         }

         byte[] b = functionNamesWithParameter.getBytes();

         InetAddress ia = InetAddress.getLocalHost();

         ArrayList<String> ports = new ArrayList<>();

         // PORT getSpecificPortFromMovieId
         String specificPort = getSpecificPortFromMovieId(ports, movieID);

         DatagramPacket dpServer1 = new DatagramPacket(b, b.length, ia, Integer.parseInt(specificPort));

         dsServer1.send(dpServer1);

         byte[] b1 = new byte[1024];

         DatagramPacket dpServer11 = new DatagramPacket(b1, b1.length);

         dsServer1.receive(dpServer11);

         return new String(dpServer11.getData()).trim();
     }

     // Customer
    // we can call requestOtherServers from here
    // - reduce capacity
    // - update customer data in both hashmaps
    // - movieID
    // - movieName check
    // - update numberofTickets booked
     @Override
    public String bookMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClientCall) throws RemoteException {
         // todo same customer same server is working, but different customer on different server
         logInfo.logger.info("Booking "+ numberOfTickets + " tickets for movie" + movieName + "(" + movieID + ")" + "with customer id " + customerID);
         String result;
         // Already booked tickets
         ArrayList<String> allBookings = getBookingSchedule(customerID, true);
         try {

             if (!movieID.substring(0, 3).equals(this.serverId)) {

                 int i = 0;
                 for (String booking : allBookings){
                     String[] parsedStr = booking.split(" ");
                     DateTimeFormatter f = DateTimeFormatter.ofPattern("ddMMyy");
                     LocalDate alreadyBookedDate = LocalDate.parse(parsedStr[1].substring(4), f);
                     LocalDate bookingDate = LocalDate.parse(movieID.substring(4), f);
                     LocalDate previousDate = bookingDate.minusDays(6);
                     if(alreadyBookedDate.isAfter(previousDate) && !parsedStr[1].contains(serverId) ){
                         i+=Integer.parseInt(parsedStr[2]);
                     }
                 }

                 if ((numberOfTickets+i)>3){
                     logInfo.logger.info("You cannot book more that 3 movie tickets");
                     return "You cannot book more that 3 movie tickets";
                 }

                 return bookCancelMovieOnOtherServer(customerID, movieID, movieName, numberOfTickets, false);
             }

             if (movies != null && movies.get(movieName) != null && movies.get(movieName).get(movieID) != null) {
                 if (numberOfTickets > movies.get(movieName).get(movieID).capacity) {
                     logInfo.logger.info("Sorry! we have only " + movies.get(movieName).get(movieID).capacity + " seats available");
                     return "Sorry! we have only " + movies.get(movieName).get(movieID).capacity + " seats available";
                 } else {
                     // todo Test below
                     for(String sched : allBookings) {

                         if(movieName.equals(sched.split(" ")[0]) && movieID.substring(3).equals(sched.split(" ")[1].substring(3))) {
                             logInfo.logger.info("You are already booked for the same slot! ");

                             return "You are already booked for the same slot! ";
                         }
                     }
                     movies.get(movieName).get(movieID).capacity = movies.get(movieName).get(movieID).capacity - numberOfTickets;
                     if(!movies.get(movieName).get(movieID).cust_ids.contains(customerID)) {
                         movies.get(movieName).get(movieID).cust_ids.add(customerID);
                     }


                     if (customers.containsKey(customerID)) {
                         boolean movieAlreadyExists = false;
                         for (Customer entry : customers.get(customerID)) {

                             if(entry.movieId.equals(movieID) && entry.movieName.equals(movieName)) {
                                 movieAlreadyExists = true;
                                 entry.bookedTickets = entry.bookedTickets + numberOfTickets;
                             }
                         }
                         if(!movieAlreadyExists) {
                             customers.get(customerID).add(new Customer(movieName, numberOfTickets, movieID));
                         }
//                         ------------
//                         if(entry.movieId.equals(movieID) && entry.movieName.equals(movieName)) {
//                             entry.bookedTickets = entry.bookedTickets + numberOfTickets;
//                         }

                     } else {
                         customers.put(customerID, new ArrayList<>());
                         customers.get(customerID).add(new Customer(movieName, numberOfTickets, movieID));
                     }
                     System.out.println("***************************");
                     for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                         String movie = entry.getKey();
                         System.out.println("------------------------------");
                         for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
                             String movieId = custEntry.getKey();
                             Booking booking = custEntry.getValue();
                             System.out.println(movie + " - " + movieId + " - " + booking.capacity);
                         }
                     }

                     result = "Movie booked successfully! " + LocalDateTime.now();
                     logInfo.logger.info(result);
                 }
             } else {
                 logInfo.logger.info("Sorry! Movie show does not exist");
                 return "Sorry! Movie show does not exist";
             }
             return result;
         } catch(Exception e) {
             e.printStackTrace();
             logInfo.logger.info(e.getMessage());
             System.out.println("Error "+ e);

             return "Exception in bookMovieTickets";
         }
     };

    // we can call requestOtherServers from here
     @Override
    public ArrayList getBookingSchedule (String customerID, boolean isClientCall) throws RemoteException {
         logInfo.logger.info("Booking Schedule for " + customerID);
         ArrayList bookingSchedule = new ArrayList();

         try {
             if(customers != null && !customerID.isEmpty()) {
                 customers.entrySet().forEach(entry -> {
                     if(entry.getKey().equals(customerID)) {
                         for(Customer data: entry.getValue()) {
                             bookingSchedule.add(data.movieName + " " + data.movieId + " " + data.bookedTickets);
                         }
                     }
                 });
             } else {
                 System.out.println("No customer in hashmap ");
             }


             if(!!isClientCall) {
                 DatagramSocket dsServer1 = new DatagramSocket();

                 DatagramSocket dsServer2 = new DatagramSocket();

                 String functionNamesWithParameter = "getBookingSchedule" + "-" + customerID;

                 byte[]b = functionNamesWithParameter.getBytes();

                 InetAddress ia = InetAddress.getLocalHost();

                 ArrayList<String> ports = getPort(this.serverId);

                 DatagramPacket dpServer1 = new DatagramPacket(b ,b.length, ia, Integer.parseInt(ports.get(0)));

                 DatagramPacket dpServer2 = new DatagramPacket(b ,b.length, ia, Integer.parseInt(ports.get(1)));

                 dsServer1.send(dpServer1);

                 dsServer2.send(dpServer2);

                 byte[] b1=new byte[1024];

                 byte[] b2=new byte[1024];

                 DatagramPacket dpServer11=new DatagramPacket(b1,b1.length);

                 dsServer1.receive(dpServer11);

                 DatagramPacket dpServer22=new DatagramPacket(b2,b2.length);

                 dsServer2.receive(dpServer22);

                 ByteArrayInputStream bais1 = new ByteArrayInputStream(dpServer11.getData());
                 ByteArrayInputStream bais2 = new ByteArrayInputStream(dpServer22.getData());

                 DataInputStream input1 = new DataInputStream(bais1);
                 DataInputStream input2 = new DataInputStream(bais2);

                 while (input1.available() > 0) {
                     String val = input1.readUTF();
                     if(val.equals("")){
                         break;
                     }
                     bookingSchedule.add(val);
                 }
                 // 2nd loop required ?
                 while (input2.available() > 0) {
                     String val = input2.readUTF();
                     if(val.equals("")){
                         break;
                     }
                     bookingSchedule.add(val);
                 }
             }
         } catch(Exception e) {
             System.out.println("Exception in getBookingSchedule " + e);
         }
         logInfo.logger.info("Booking Schedule for customer with ID " + customerID + " is: " + bookingSchedule);
         return bookingSchedule;
     };

    // we can call requestOtherServers from here
     @Override
    public String cancelMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClient) throws IOException {
         logInfo.logger.info("Cancel "+ numberOfTickets + " tickets for movie" + movieName + " with customer id " + customerID + " movieid " + movieID);
         if (!movieID.substring(0, 3).equals(this.serverId)) {
             return bookCancelMovieOnOtherServer(customerID, movieID, movieName, numberOfTickets, true);
         }
         if(movies.get(movieName) == null) {
             logInfo.logger.info("Movie does not exist! ");
             return "Movie does not exist! ";
         }
         if(movies.get(movieName).get(movieID) == null) {
             logInfo.logger.info("Movie Id does not exist! ");
            return "Movie Id does not exist! ";
         }
         if(customers.get(customerID) == null) {
             logInfo.logger.info("You dont have any booked ticket! ");
             return "You dont have any booked ticket! ";
         }
         for(Customer data: customers.get(customerID)) {
             if(!data.movieName.equals(movieName) && !data.movieName.equals(movieName)) {
                 logInfo.logger.info("You have not booked this movie - " + movieName + " " + movieID);
                 return "You have not booked this movie - " + movieName + " " + movieID;
             }
         }


         for(Customer c : customers.get(customerID)) {
             if(numberOfTickets > c.bookedTickets) {
                 logInfo.logger.info("Invalid input! You have booked only " + c.bookedTickets + "tickets");
                 return "Invalid input! You have booked only " + c.bookedTickets + "tickets";
             } else if(numberOfTickets == c.bookedTickets) {
                 movies.get(movieName).get(movieID).capacity = movies.get(movieName).get(movieID).capacity + numberOfTickets;
                 movies.get(movieName).get(movieID).cust_ids.remove(customerID);
                 customers.remove(c);
                 // JUST FOR LOGGING
                 System.out.println("***************************");
                 for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                     String movie = entry.getKey();
                     System.out.println("------------------------------");
                     for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
                         String movieId = custEntry.getKey();
                         Booking booking = custEntry.getValue();
                         System.out.println(movie + " - " + movieId + " - " + booking.capacity);
                     }
                 }
                 logInfo.logger.info("Cancelled movie tickets successfully ");

                 return "Cancelled movie tickets successfully ";
             } else if(numberOfTickets < c.bookedTickets) {
                 movies.get(movieName).get(movieID).capacity = movies.get(movieName).get(movieID).capacity + numberOfTickets;
                 c.bookedTickets = c.bookedTickets - numberOfTickets;
                 // JUST FOR LOGGING
                 System.out.println("***************************");
                 for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                     String movie = entry.getKey();
                     System.out.println("------------------------------");
                     for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
                         String movieId = custEntry.getKey();
                         Booking booking = custEntry.getValue();
                     }
                 }
                 logInfo.logger.info("Cancelled movie tickets successfully ");
                 return "Cancelled movie tickets successfully ";
             }
         }
         return "Cancelled movie tickets successfully " + LocalDateTime.now();
     };



     private ArrayList getPort(String serverId) {
         ArrayList<String> ports = new ArrayList<>();
        switch(serverId){
            case "ATW":
                ports.add("7800");
                ports.add("7700");
                return ports;

            case "OUT":
                ports.add("7600");
                ports.add("7800");
                return ports;

            case "VER":
                ports.add("7600");
                ports.add("7700");
                return ports;
        }
         return null;
     }

    public ArrayList mapFunction(String functionNameWithParameters, LoggerClass logInfo) {
        ArrayList result = new ArrayList();

        try {
            if(functionNameWithParameters.contains("listMovieShowsAvailability")) {
                String [] params = functionNameWithParameters.split("-");

                result = listMovieShowsAvailability(params[1].trim(), false);
            } else if(functionNameWithParameters.contains("getBookingSchedule")) {
                String [] params = functionNameWithParameters.split("-");

                result = getBookingSchedule(params[1].trim(), false);
            } else if(functionNameWithParameters.contains("bookMovieTickets")) {
                String [] params = functionNameWithParameters.split("-");

                String bookedTickets = bookMovieTickets(params[1].trim(), params[2].trim(), params[3].trim(), Integer.parseInt(params[4].trim()), false);
                result.add(bookedTickets);
            } else if(functionNameWithParameters.contains("cancelMovieTickets")){
                String [] params = functionNameWithParameters.split("-");

                String cancelTickets = cancelMovieTickets(params[1].trim(), params[2].trim(), params[3].trim(), Integer.parseInt(params[4].trim()), false);
                result.add(cancelTickets);
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Exception" + e);
        }
        return result;
    };

}

//        for(Map.Entry<String, HashMap<String, Booking>> movie : movies.entrySet()) {
//            System.out.println("movie name " + movie.getKey());
//
//            if(movie.getKey().equals(movieName)) {
//                for(Map.Entry<String, Booking> details: movie.getValue().entrySet()) {
//                    System.out.println("movieId " + details.getKey());
//                    if(details.getKey().equals(movieID)) {
//                        custIds = details.getValue().cust_ids;
//
//                    }
//
//                }
//            } else {
//                System.out.println("No movie with name " + movieName);
//            }
//            System.out.println("custIds " + custIds);
//
//        }

//        if()
//        if()
//        if(movies.get(movieName) != null) {
//            System.out.println("2. ");
//            if(movies.get(movieName).get(movieID) != null) {
//                System.out.println("3. ");
//                ArrayList<String> customerList = new ArrayList<>();
////                for (int i = 0; i < movies.get(movieName).get(movieID).cust_ids.size(); i++) {
////                    System.out.println("loop "+ movies.get(movieName).get(movieID).cust_ids.get(i));
////                }
//                movies.remove(movieName);
//            } else {
//                movies.remove(movieName);
//            }
//        } else {
//            return "There is no movie as " + movieName + " to delete";
//        }

