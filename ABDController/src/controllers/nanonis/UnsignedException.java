package controllers.nanonis;

public class UnsignedException extends Exception {
    public String message;
    public UnsignedException(String message) {
        super();

        this.message = message;
    }    
}
