public class Test {
    public static void main(final String[] args) {
        try {
            assert args.length > 0 : "my message";
        } catch (AssertionError e) {

            String message = e.getMessage();
            System.out.println(message);
        }
    }
}
