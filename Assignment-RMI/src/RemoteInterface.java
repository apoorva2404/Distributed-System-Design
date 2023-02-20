
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;

public interface RemoteInterface extends Remote {
    // Admin
    public String addMovieSlots (String movieID, String movieName, int bookingCapacity) throws RemoteException;
    public String removeMovieSlots (String movieID, String movieName) throws RemoteException, ParseException;
    public ArrayList listMovieShowsAvailability (String movieName, boolean isClientCall) throws RemoteException;

    // Customer
    public String bookMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClientCall) throws RemoteException;
    public ArrayList getBookingSchedule (String customerID, boolean isClientCall) throws RemoteException;
    public String cancelMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClient) throws IOException;
}

