package controllers.nanonis;

public class ResponseException extends Exception {
    String expected;
    String got;

    public ResponseException(String expected, String got) {
        super();

        this.expected = expected;
        this.got = got;
    }

    public static void checkHeader(String expected, String got) throws ResponseException {
        if (!expected.equals(got)) {
            throw new ResponseException(expected, got);
        }
    }

    @Override
    public String getMessage() {
        // TODO Auto-generated method stub
        return "Expected `" + expected + "` but got `" + got + "`";
    }

}
