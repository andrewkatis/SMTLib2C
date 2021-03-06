package skolem;

public enum UnaryOp {
    NEGATIVE ("-"),
    NOT ("not");

    private String str;

    private UnaryOp(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    public static UnaryOp fromString(String string) {
        for (UnaryOp op : UnaryOp.values()) {
            if (op.toString().equals(string)) {
                return op;
            }
        }
        return null;
    }
}
