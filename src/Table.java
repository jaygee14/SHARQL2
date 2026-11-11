import AST.*;
import java.util.LinkedList;

public class Table {
    LinkedList<field> fields;
    LinkedList<LinkedList<Object>> records;

    public Table(LinkedList<field> fields) {
        this.fields = fields;
        this.records = new LinkedList<>();
    }
}