import AST.*;
import java.util.*;

public class Table {
    ArrayList<field> fields;
    HashMap<String, field> fieldMap;
    LinkedList<LinkedList<InterpreterDataType>> records;



    public Table(LinkedList<field> schema) {
        fields = new ArrayList<>();
        fieldMap = new HashMap<>(); // convert fields from linkedlist to hashmap
        records = new LinkedList<>();

        for (field f : schema) { // fill hashmap
            fields.add(f);
            fieldMap.put(f.name, f);
        }
    }

    public field getField(String name) {
        return fieldMap.get(name); // get field by name
    }

    public field getField(int index) {
        return fields.get(index); // get field by positions
    }



    public void printTable() {

        System.out.println("Fields:");

        for (field f : fields) {
            System.out.println("    " + f.type + " " + f.name);
        }

        System.out.println("Records:");

        for (LinkedList<InterpreterDataType> record : records) {
            System.out.println(record);
        }
    }
}