package controllers.nanonis;

import java.util.Optional;

public class NanonisException extends Exception {
    private String message;

    public NanonisException(String message) {
        super();
        this.message = message;
    }

    public static void checkError(Optional<String> error) throws NanonisException {
        if (error.isPresent()) {
            throw new NanonisException(error.get());
        }
    }

    @Override
    public String getMessage() {
        // TODO Auto-generated method stub
        return message;
    }

}
