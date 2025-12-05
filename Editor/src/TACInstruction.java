public class TACInstruction {
    public String op;
    public String arg1;
    public String arg2;
    public String result;

    public TACInstruction(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    @Override
    public String toString() {
        if (op.equals("LABEL")) {
            return result + ":";
        }
        if (op.equals("GOTO")) {
            return "goto " + result;
        }
        if (op.equals("IF_FALSE")) {
            return "ifFalse " + arg1 + " goto " + result;
        }
        if (arg2 != null) {
            return result + " = " + arg1 + " " + op + " " + arg2;
        }
        if (arg1 != null) {
            return result + " = " + op + " " + arg1;
        }
        return result + " = " + op;
    }
}
