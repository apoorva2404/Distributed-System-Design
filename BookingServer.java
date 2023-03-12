import BookMyShowApp.BookingInterface;
import BookMyShowApp.BookingInterfaceHelper;
import BookMyShowApp.BookingInterfacePOA;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

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

class BookingServerImpl extends BookingInterfacePOA {
    ConcurrentHashMap<String, ConcurrentHashMap<String, Booking>> movies = new ConcurrentHashMap<String, ConcurrentHashMap<String, Booking>>();
    ConcurrentHashMap<String, ArrayList<Customer>> customers = new ConcurrentHashMap<String, ArrayList<Customer>>();

    String serverId;

    LoggerClass logInfo;

    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public BookingServerImpl(String serverId, LoggerClass logInfo) {
        super();
        this.serverId = serverId;
        this.logInfo = logInfo;
    }

    @Override
    public String addMovieSlots(String movieID, String movieName, int bookingCapacity) {
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

        //  for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
        //      String movie = entry.getKey();
        //      System.out.println("------------------------------");
        //      for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
        //          String movieId = custEntry.getKey();
        //          Booking booking = custEntry.getValue();
        //          System.out.println(movie + " - " + movieId + " - " + booking.capacity);
        //      }
        //  }
         // logger
         logInfo.logger.info("Added movie slots successfully " + LocalDateTime.now());
         return "Added movie slots successfully! ";

    }

    @Override
    public String removeMovieSlots(String movieID, String movieName) {
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

         ArrayList<String> availableShows =  new ArrayList<String>(Arrays.asList(listMovieShowsAvailability(movieName, false)));

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

        // -- check --
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<String, Integer>(movieID, targetMovieCapacity);
        System.out.println("entry " + entry);

        int targetSlotIndex = Arrays.asList(availableSlotsList).indexOf(entry);

        int movieSlotsLength = Arrays.asList(availableSlotsList).size();
        System.out.println("movieSlotsLength - " + movieSlotsLength);
        System.out.println("availableSlotsList - " + availableSlotsList);
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
    }

    private ArrayList<String> getPort(String serverId) {
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
    
    @Override
    public String[] listMovieShowsAvailability(String movieName, boolean isClientCall) {
        // String[] result = {"listMovieShowsAvailability!!"};
        // return result;

        logInfo.logger.info("Available Movie shows for" + movieName);
         ArrayList<String> availableShows = new ArrayList<>();
        try {
             ConcurrentHashMap<String, Booking> shows = movies.get(movieName);
             System.out.println("Available shows for movie - " + movieName);
             if(shows != null) {
                for (Map.Entry<String, Booking> show : shows.entrySet()) {
                    if(show.getValue().capacity > 0){
                        availableShows.add(show.getKey() + " " +show.getValue().capacity);
                    }
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
        String[] stockArr = new String[availableShows.size()];
        stockArr = availableShows.toArray(stockArr);
        return stockArr;
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


    @Override
    public String bookMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets, boolean isClientCall) {
        // todo same customer same server is working, but different customer on different server
        logInfo.logger.info("Booking "+ numberOfTickets + " tickets for movie" + movieName + "(" + movieID + ")" + "with customer id " + customerID);
        String result;
        // Already booked tickets
        ArrayList<String> allBookings = new ArrayList<String>(Arrays.asList(getBookingSchedule(customerID, true)));
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
                System.out.println("PRINT i " + i);

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

                    for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                        String movie = entry.getKey();

                        for (Map.Entry<String, Booking> custEntry : entry.getValue().entrySet()) {
                            String movieId = custEntry.getKey();
                            Booking booking = custEntry.getValue();
                            System.out.println(movie + " - " + movieId + " - " + booking.capacity);
                        }
                    }

                    result = "Movie booked successfully! ";
                    logInfo.logger.info(result + LocalDateTime.now());
                }
            } else {
                logInfo.logger.info("Sorry! Movie show does not exist");
                return "Sorry! Movie show does not exist";
            }
            System.out.println("result in book - " + result);
            return result;
        } catch(Exception e) {
            e.printStackTrace();
            logInfo.logger.info(e.getMessage());
            System.out.println("Error "+ e);

            return "Exception in bookMovieTickets";
        }
    }

    @Override
    public String[] getBookingSchedule(String customerID, boolean isClientCall) {
        // String[] result = {"getBookingSchedule!!"};
        // return result;

        logInfo.logger.info("Booking Schedule for " + customerID);
         ArrayList<String> bookingSchedule = new ArrayList<>();

         try {
             if(customers != null && !customers.isEmpty() && !customerID.isEmpty()) {
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
        String[] stockArr = new String[bookingSchedule.size()];
        stockArr = bookingSchedule.toArray(stockArr);
        return stockArr;
    }

    @Override
    public String cancelMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets, boolean isClient) {
        // return "cancelMovieTickets!!";
        logInfo.logger.info("Cancel "+ numberOfTickets + " tickets for movie" + movieName + " with customer id " + customerID + " movieid " + movieID);
         if (!movieID.substring(0, 3).equals(this.serverId)) {
             try {
                return bookCancelMovieOnOtherServer(customerID, movieID, movieName, numberOfTickets, true);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception in cancelling movie ticket " + e);
            }
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
            if(data.movieName.equals(movieName) && data.movieId.equals(movieID)) {
                break;
            } else {
                logInfo.logger.info("You have not booked this movie - " + movieName + " " + movieID);
                 return "You have not booked this movie - " + movieName + " " + movieID;
            }
            //  if(!data.movieName.equals(movieName) && !data.movieName.equals(movieName)) {
            //      logInfo.logger.info("You have not booked this movie - " + movieName + " " + movieID);
            //      return "You have not booked this movie - " + movieName + " " + movieID;
            //  }
         }


         for(Customer c : customers.get(customerID)) {
             if(numberOfTickets > c.bookedTickets) {
                 logInfo.logger.info("Invalid input! You have booked only " + c.bookedTickets + "tickets");
                 return "Invalid input! You have booked only " + c.bookedTickets + "tickets";
             } else if(numberOfTickets == c.bookedTickets) {
                 movies.get(movieName).get(movieID).capacity = movies.get(movieName).get(movieID).capacity + numberOfTickets;
                 movies.get(movieName).get(movieID).cust_ids.remove(customerID);
                 customers.get(customerID).remove(c);
                 // JUST FOR LOGGING
                 for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                     String movie = entry.getKey();
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
                 for (Map.Entry<String, ConcurrentHashMap<String, Booking>> entry : movies.entrySet()) {
                     String movie = entry.getKey();
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

    }


    @Override
    public String exchangeTickets(String customerID, String curr_movieID, String new_movieID, String new_movieName, int numberOfTickets) {
        try {
            ArrayList<String> allBookings = new ArrayList<String>(Arrays.asList(getBookingSchedule(customerID, true)));

            boolean flag = false;
            String booking = "";

            for(String sched : allBookings) {
                // movieName.equals(sched.split(" ")[0]) && 
                if(curr_movieID.equals(sched.split(" ")[1])) {
                    if(Integer.parseInt(sched.split(" ")[2]) >= numberOfTickets) {
                        flag = true;
                        booking = sched;
                        break; 
                    } else {
                        return "You have booked only " + Integer.parseInt(sched.split(" ")[2]) + " tickets. So, you cannot exchange " + numberOfTickets + " tickets";
                    }
                } 
            }

            if(!!flag) {
                String[] listAvailableMovies = listMovieShowsAvailability(new_movieName, true);
                System.out.println("listAvailableMovies - " + listAvailableMovies );
                for(String movieDetails: listAvailableMovies) {
                    System.out.println("movieDetails - " + movieDetails + " " + new_movieID);
                    System.out.println("movieDetails 1 - " + movieDetails.split(" ")[0].equals(new_movieID));
                    if(movieDetails.split(" ")[0].equals(new_movieID)) {
                        if(Integer.parseInt(movieDetails.split(" ")[1]) >= numberOfTickets) {
                           // book new 
                        //    if(bookMovieTickets(customerID, new_movieID, new_movieName, numberOfTickets, true) == "Movie booked successfully! ") {
                        //       System.out.println("returning string " + bookMovieTickets(customerID, new_movieID, new_movieName, numberOfTickets, true) == "Movie booked successfully! ");  
                        //    } else {
                        //     System.out.println("list " + bookMovieTickets(customerID, new_movieID, new_movieName, numberOfTickets, true));
                        //    }

                           String success = bookMovieTickets(customerID, new_movieID, new_movieName, numberOfTickets, true);
                           // not cancelling
                           System.out.println("success - " + success);

                           System.out.println("### " + success.trim().equals("Movie booked successfully!"));
                           if(success.trim().equals("Movie booked successfully!")) {
                                // cancel old tickets 
                                String result = cancelMovieTickets(customerID, curr_movieID, booking.split(" ")[0], numberOfTickets, true);

                                if(result.equals("Cancelled movie tickets successfully ")) {
                                    logInfo.logger.info("You have successfully exchanged the tickets");

                                    return "You have successfully exchanged the tickets";
                                }
                           } else {
                                logInfo.logger.info(success);
                                
                                return success;
                           }
                        } else {
                            // no capacity
                            logInfo.logger.info("No capacity left for " + new_movieID + " " + new_movieName);

                            return "No capacity left for " + new_movieID + " " + new_movieName;
                        }
                    } 
                    else {
                        logInfo.logger.info("No shows for " + new_movieName + " " + new_movieID);
                        // - not checking for second movie
                        // return "No shows for " + new_movieName + " " + new_movieID;
                    }
                }
            } else {
                logInfo.logger.info("You have not booked any tickets for this show ");

                return "You have not booked any tickets for this show ";
            }
        } catch (Exception e) {
            System.out.println("Exception in exchange tickets " + e);
            e.printStackTrace();
        }

        return "";
    }

    public ArrayList<String> mapFunction(String functionNameWithParameters, LoggerClass logInfo) {
        ArrayList<String> result = new ArrayList<>();

        try {
            if(functionNameWithParameters.contains("listMovieShowsAvailability")) {
                String [] params = functionNameWithParameters.split("-");

                result = new ArrayList<String>(Arrays.asList(listMovieShowsAvailability(params[1].trim(), false)));
            } else if(functionNameWithParameters.contains("getBookingSchedule")) {
                String [] params = functionNameWithParameters.split("-");

                result = new ArrayList<String>(Arrays.asList(getBookingSchedule(params[1].trim(), false)));
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

    public void shutdown() {
        orb.shutdown(false);
    }
}

public class BookingServer {
    private static String serverName;
    public static BookingServerImpl bookingImpl;
    public static LoggerClass logInfo;
    // private static String serverPort;

    BookingServer(String serverName, LoggerClass logInfo){
        BookingServer.serverName = serverName;
        BookingServer.logInfo = logInfo;
        // BookingServer.serverPort = serverPort;

    }

    

    public ArrayList<String> mapFunction(String functionNameWithParameters, LoggerClass logInfo) {
        return bookingImpl.mapFunction(functionNameWithParameters, logInfo);
    }

    public void run(){
        main(null);
    }
    public static void main(String[] args) {
        try {
            // Properties props = new Properties();
            // props.put("org.omg.CORBA.ORBInitialPort", serverPort);

            // Create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // Get ref of rootPoa
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

            // Activate the POA manager
            rootPoa.the_POAManager().activate();

            // Create servant
            BookingServerImpl helloImpl = new BookingServerImpl(serverName, logInfo);
            
            bookingImpl = helloImpl;

            

            Thread UDPCommunicator = new Thread(){

                public void run() {           
                    try {
                    while(true) {
                        int port = serverName.substring(0, 3).equals("ATW") ? 7600 : serverName.substring(0, 3).equals("VER") ? 7800 :   serverName.substring(0, 3).equals("OUT") ? 7700 : 7600;

                        DatagramSocket ds = new DatagramSocket(port);
        
                        byte[]b1=new byte[1024];
        
                        DatagramPacket dp=new DatagramPacket(b1,b1.length);
        
                        ds.receive(dp);
        
                        String str = new String(dp.getData());
        
                        ArrayList<String> result = new ArrayList<>();
        
                        result = helloImpl.mapFunction(str, logInfo);
        
                        System.out.println("Result in UDP server " + serverName + " " + result);
        
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
                    System.out.println("Exception in UDP thread " + e);
                    e.printStackTrace();
                }
                }
            };

            UDPCommunicator.start();


            // Register it with the ORB
            helloImpl.setORB(orb);

            // Object ref from the servant
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(helloImpl);

            BookingInterface href = BookingInterfaceHelper.narrow(ref);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");

            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind obj ref in naming
            String name = "Hello"; 

            NameComponent path[] = ncRef.to_name(serverName);

            ncRef.rebind(path, href);

            System.out.println(serverName + " server ready and waiting... ");

            orb.run();
        } catch(Exception e) {
            System.out.println("Exception in " + serverName + e);

            e.printStackTrace(System.out);
        }
        System.out.println("Server Exiting ...");
    }
}
