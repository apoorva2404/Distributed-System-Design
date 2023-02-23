import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerClass {
    public Logger logger;

    FileHandler handler;
    public LoggerClass(String fileName) throws SecurityException, IOException {
        try{
            System.out.println(" file name " + System.getProperty("user.dir"));
            // Path
            String absoulteFilePath = System.getProperty("user.dir") + "/logs/" + fileName;
            File file = new File(absoulteFilePath);
            if(!file.exists()){
                file.createNewFile();
            }
            handler = new FileHandler(absoulteFilePath,true);
            logger = Logger.getLogger("abc");
            logger.addHandler(handler);
            SimpleFormatter f = new SimpleFormatter();
            handler.setFormatter(f);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}