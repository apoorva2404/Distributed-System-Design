package assignmentws;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style=Style.RPC)
public interface BookingInterface {


	public String addMovieSlots (String movieID, String movieName, int bookingCapacity) throws RemoteException;
    public String removeMovieSlots (String movieID, String movieName) throws RemoteException, ParseException;
    public String[] listMovieShowsAvailability (String movieName, boolean isClientCall) throws RemoteException;

    // Customer
    public String bookMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClientCall) throws RemoteException;
    public String[] getBookingSchedule (String customerID, boolean isClientCall) throws RemoteException;
    public String cancelMovieTickets (String customerID, String movieID, String movieName, int numberOfTickets, boolean isClient) throws IOException;
	public String exchangeTickets(String customerID, String curr_movieID, String new_movieID, String new_movieName, int numberOfTickets);



}

