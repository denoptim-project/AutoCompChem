package autocompchem.datacollections;

public class DataFetchingException extends Exception {

    public DataFetchingException(String message)
    {
        super(message);
    }

    public DataFetchingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
