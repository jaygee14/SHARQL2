import java.util.HashMap;

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
}