import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

import AST.*;

public class Interpreter {

    HashMap<String, Table> database = new HashMap<>();

    public void run(sharQL program) throws Exception {
        for (statement s : program.statement){
            processStatement(s);
        }
    }

    public void printDatabase(){
        database.forEach((key, value) -> {
            System.out.println("Table: " + key);
            value.printTable();
            System.out.println();
        });
    }

    private void processStatement(statement s) throws Exception {
        if (s.create.isPresent()){ // check for creat instruction
            processCreate(s.create.get()); // get create ast
        }
        else if (s.insert.isPresent()){
            processInsert(s.insert.get());
        }
        else throw new Exception("Unknown Statement!");
    }

    private void processCreate(create c) throws Exception {

        if (c == null){
            throw new Exception("Invalid create statement!");
        }

        if (c.name == null || c.name.isEmpty() || c.field == null || c.field.isEmpty()){ // check for incomplete create
            throw new Exception("Invalid create statement!");
        }

        if (database.containsKey(c.name)){
            throw new Exception("Table already exists!"); // handle table collisions
        }

        database.put(c.name, new Table(c.field)); // put create contents into database
    }



    private InterpreterDataType checkFields(field f, value v)
            throws Exception {

        if (f.type.thestring_ORwholeNumberORdecimalNumber.isPresent()) {

            String type = f.type.thestring_ORwholeNumberORdecimalNumber.get().toString();

            switch (type) {

                case "_string":
                    if (v.STRINGLITERAL.isEmpty()) {
                        throw new Exception("Expected String");
                    }

                    StringInterpreterDataType stringData = new StringInterpreterDataType();

                    stringData.Value = v.STRINGLITERAL.get();

                    return stringData;

                case "decimalNumber":
                    if (v.NUMBER.isEmpty()) {
                        throw new Exception("Expected Decimal Number");
                    }

                    FloatInterpreterDataType floatData = new FloatInterpreterDataType();

                    try {
                        floatData.Value = Float.parseFloat(v.NUMBER.get());
                    } catch (NumberFormatException e) {
                        throw new Exception("Invalid Decimal Number");
                    }

                    return floatData;

                case "wholeNumber":
                    if (v.NUMBER.isEmpty()) {
                        throw new Exception("Expected Whole Number");
                    }

                    WholeNumberInterpreterDataType intData = new WholeNumberInterpreterDataType();

                    try {
                        intData.Value = Integer.parseInt(v.NUMBER.get());
                    } catch (NumberFormatException e) {
                        throw new Exception("Invalid Whole Number");
                    }

                    return intData;

                default:
                    throw new Exception("Unknown field type: " + type);
            }
        }

        throw new Exception("Unsupported field type");
    }

    private LinkedList<InterpreterDataType> findOne(findOne f, String tableName) throws Exception {
        if (f.compare.isEmpty()) {
            throw new Exception("Invalid findOne call!");
        }

        Table search = database.get(tableName);

        if (search == null) {
            throw new Exception("Table could not be found");
        }

        for (compare c : f.compare){
            search.getField(c.left);
        }

        for (LinkedList<InterpreterDataType> record : search.records) {

            boolean matches = true;

            for (compare c : f.compare) {

                // 1. Get the field name from c
                field currentField = search.getField(c.left);

                if (currentField == null) {
                    throw new Exception("Field does not exist: " + c.left);
                }

                String compareValue;

                if (c.STRINGLITERAL.isPresent()) {
                    compareValue = c.STRINGLITERAL.get();
                }
                else if (c.NUMBER.isPresent()) {
                    compareValue = c.NUMBER.get();
                }
                else if (c.IDENTIFIER.isPresent()) {
                    compareValue = c.IDENTIFIER.get();
                }
                else {
                    throw new Exception("Invalid comparison value");
                }

                // 2. Find the field's index in the table
                int i = 0;
                for (field row : search.fields){
                    if(Objects.equals(c.left, row.name)){
                        break;
                    }
                    else i++;
                }

                // 3. Get the value from the record at that index
                field result = search.getField(i);
                // 4. Compare the record's value with c's value

                // 5. If the comparison fails:
                //    matches = false;
                //    break;
            }

            if (matches) {
                return record;
            }
        }

        throw new Exception("No matching record found");
    }



    private void processInsert(insert i) throws Exception {

        if (i == null || i.name == null || i.name.isEmpty() || i.values == null || i.values.isEmpty()) {
            throw new Exception("Invalid insert statement");
        }

        Table search = database.get(i.name);

        if (search == null) {
            throw new Exception("Unable to find table");
        }

        for (values v : i.values) {

            if (v == null || v.value == null || v.value.size() != search.fields.size()) {
                throw new Exception("Invalid number of values");
            }

            LinkedList<InterpreterDataType> record = new LinkedList<>();

            for (int j = 0; j < v.value.size(); j++) {

                field f = search.fields.get(j);
                value val = v.value.get(j);

                record.add(checkFields(f, val));
            }

            search.records.add(record);
        }
    }

}