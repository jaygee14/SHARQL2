import AST.*;
public static void main(String[] args) throws Exception {

    var code = "create table patient\n"+
            "	wholeNumber patientID\n"+
            "	string name\n"+
            "	string phoneNum\n"+
            "	string patEmail\n"+
            "	string DOB\n"+
            "\n"+
            "create table prescriptions\n"+
            "	medicine meds\n"+
            "	patient p\n"+
            "	string datePerscribed\n"+
            "	string pharmacyAddress\n"+
            "	decimalNumber dosage\n"+
            "\n"+
            "create table medicine\n"+
            "	wholeNumber medicineID\n"+
            "	string medicineName\n"+
            "	string typeOf\n"+
            "\n"+
            "create table doctors\n"+
            "	wholeNumber staffID\n"+
            "	string name\n"+
            "	wholeNumber officeNum\n"+
            "	department d\n"+
            "\n"+
            "create table department\n"+
            "	wholeNumber departmentNum\n"+
            "	doctors depHead\n"+
            "	string depName\n"+
            "	list <doctors> staffList\n"+
            "\n"+
            "create table appointments\n"+
            "	patient p\n"+
            "	doctors doc\n"+
            "	rooms room\n"+
            "	string startTime\n"+
            "	string dateOf\n"+
            "	string reasonFor\n"+
            "\n"+
            "create table emergencyContact\n"+
            "	patient p\n"+
            "	string contactName\n"+
            "	string contactPhone\n"+
            "	enum (mother, father, sister, brother, partner, child, other) relationship\n"+
            "\n"+
            "create table bill\n"+
            "	patient p\n"+
            "	decimalNumber totalOwed\n"+
            "	decimalNumber amountPaid\n"+
            "	insuranceInfo insure\n"+
            "	enum (card, cash, electronic, check) paymentType\n"+
            "\n"+
            "create table insuranceInfo\n"+
            "	patient p\n"+
            "	wholeNumber insuranceID\n"+
            "	string insuranceProvider\n"+
            "	decimalNumber insuranceCoverage\n"+
            "\n"+
            "create table payroll\n"+
            "	doctor doc\n"+
            "	decimalNumber salary\n"+
            "	decimalNumber bonus\n"+
            "	decimalNumber overtime\n"+
            "\n"+
            "create table rooms\n"+
            "	wholeNumber roomNum\n"+
            "	string admitanceTime\n"+
            "	string dischargeTime\n"+
            "\n";

    //lex/parse
    Lexer lex = new Lexer(code);
    List<Token> tokenList = lex.Lex();
    SharQLParser parser = new SharQLParser(tokenList);
    //run interpreter
    Interpreter interpreter = new Interpreter();
    interpreter.run(parser.sharQL());
    interpreter.printDatabase();
    // search and print contents of database
    Table search = interpreter.database.get("patient"); // lookup patient table
    field f = search.getField("phoneNum"); // get schema from patient table
    field s = search.getField(4); // get schema from index

    System.out.println("First Search: " + f.name + "\n" + "Second Search: "+ s.name);


}