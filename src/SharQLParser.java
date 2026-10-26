import AST.*;
import java.util.List;
import java.util.Optional;


public class SharQLParser {
    private List<Token> tokenList;
    private final TokenManager manager;


    private void handleError(String errorString) throws SyntaxErrorException {
        throw new SyntaxErrorException(errorString, manager.getCurrentLine(), manager.getCurrentColumnNumber());
    }

    private void handleNewLine() throws SyntaxErrorException {
       if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
           handleError("Expected Newline at: ");
       }
}

    public SharQLParser(List<Token> tokenList){
        this.manager = new TokenManager(tokenList);
    }

    public findOne parser1Test() throws SyntaxErrorException {
        return findOne().orElseThrow();
    }
    public sharQL sharQL() throws SyntaxErrorException {

        sharQL top = new sharQL();
        var newStatement = statement();

        while (newStatement.isPresent()){
            top.statement.add(newStatement.get());
            newStatement = statement();
        }
        return top;
    }

    //findOne = "findOne" "(" compare ( "," compare) * ")"
    Optional<findOne> findOne() throws SyntaxErrorException {
        if (manager.matchAndRemove(Token.TokenTypes.FINDONE).isEmpty()){
            return Optional.empty(); //is not a findOne call
        }
        if (manager.matchAndRemove(Token.TokenTypes.OPENPAREN).isEmpty()){
            handleError("FindOne calls must have open parenthesis at: ");
        }
        findOne newFindOne = new findOne();
        var compareOne = compare();

        if (compareOne.isEmpty()){
            handleError("findOne must have compare statement at: ");
        }
        newFindOne.compare.add(compareOne.get());

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {

            Optional<compare> nextCompare = compare();

            if (nextCompare.isEmpty()) {
                handleError("Expecting comparison at: ");
            }

            newFindOne.compare.add(nextCompare.get());
        }
        if(manager.matchAndRemove(Token.TokenTypes.CLOSEPAREN).isEmpty()){
            handleError("Must close findOne call: ");
        }
        return Optional.of(newFindOne);
    }
    // statement = create | insert | delete | from
    Optional<statement> statement() throws SyntaxErrorException {
        Optional<Token> passedValue = manager.checkStatement();
        if (passedValue.isEmpty()){
            return Optional.empty();
        }
        var statement = new statement();
        if (passedValue.get().Type == Token.TokenTypes.INSERT){
            statement.insert = insert();
        }
        if (passedValue.get().Type == Token.TokenTypes.DELETE){
            statement.delete = delete();
        }
        return Optional.of(statement);
    }

    // compare = IDENTIFIER ( "=" | "<" | ">" | "<>" | "<=" | ">=") (NUMBER | STRINGLITERAL | IDENTIFIER | findOne)
    Optional<compare> compare() throws SyntaxErrorException {
        var leftToken = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

        if (leftToken.isEmpty()) {
            return Optional.empty();
        }
        var operatorToken = manager.checkComparaters();
        if (operatorToken.isEmpty()){
            handleError("Compare statement needs compare operation at: ");
        }
        var passedValue = manager.checkPassers();

        if (passedValue.isEmpty()){
            handleError("Compare statement received incompatible data type at: ");
        }

        compare newCompare = new compare();

        newCompare.left = leftToken.get().Value.orElseThrow();
        if (operatorToken.get().Type == Token.TokenTypes.EQUAL) {
            newCompare.thecompareOp = compareOp.equal;
        }

        if (passedValue.get().Type == Token.TokenTypes.NUMBER) {
            newCompare.NUMBER = passedValue.get().Value;
        }
        else if (passedValue.get().Type == Token.TokenTypes.STRINGLITERAL) {
            newCompare.STRINGLITERAL = passedValue.get().Value;
        }
        else if (passedValue.get().Type == Token.TokenTypes.IDENTIFIER) {
            newCompare.IDENTIFIER = passedValue.get().Value;
        }

        return Optional.of(newCompare);
    }

    // value = NUMBER | STRINGLITERAL | IDENTIFIER | findOne | valueList
    Optional<value> value() throws SyntaxErrorException {

        Optional<Token> passedValue = manager.peek(0);

        if (passedValue.isEmpty()){
            return Optional.empty();
        }
        var newValue = new value();
        var type = passedValue.get().Type;

        if (type == Token.TokenTypes.STRINGLITERAL){
            manager.matchAndRemove(Token.TokenTypes.STRINGLITERAL);
            newValue.STRINGLITERAL = passedValue.get().Value;
            return Optional.of(newValue);
        }
        if (type == Token.TokenTypes.NUMBER){
            manager.matchAndRemove(Token.TokenTypes.NUMBER);
            newValue.NUMBER = passedValue.get().Value;
            return Optional.of(newValue);
        }
        if (type == Token.TokenTypes.IDENTIFIER){
            manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            newValue.name = passedValue.get().Value;
            return Optional.of(newValue);
        }
        if (type == Token.TokenTypes.FINDONE){
            newValue.findOne = findOne();
            return Optional.of(newValue);
        }
        if (type == Token.TokenTypes.LEFTBRACE){
            newValue.valueList = listOfValues();
            return Optional.of(newValue);
        }
        return Optional.empty();
    }


    // values = value ("," value)*
    Optional<values> values() throws SyntaxErrorException {

        Optional<value> value = value();
        if (value.isEmpty()){
            return Optional.empty();
        }
        var values = new values();
        values.value.add(value.get());

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
            var newValue = value();

            if (newValue.isEmpty()){
                handleError("Expected value at: ");
            }
            values.value.add(newValue.get());
        }
        return Optional.of(values);
    }

    // valueList = "[" findOne? ("," findOne)* "]"
    Optional<valueList> listOfValues() throws SyntaxErrorException {

        if (manager.matchAndRemove(Token.TokenTypes.LEFTBRACE).isEmpty()) {
            return Optional.empty();
        }

        valueList listOfValues = new valueList();

        Optional<findOne> newFindOne = findOne();

        newFindOne.ifPresent(findOne -> listOfValues.findOne.add(findOne));

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {

            Optional<findOne> nextFindOne = findOne();

            if (nextFindOne.isEmpty()) {
                handleError("Expected findOne after comma: ");
            }

            listOfValues.findOne.add(nextFindOne.get());
        }

        if (manager.matchAndRemove(Token.TokenTypes.RIGHTBRACE).isEmpty()) {
            handleError("Value List needs closing bracket: ");
        }

        return Optional.of(listOfValues);
    }

    // insert = "insert" IDENTIFIER NEWLINE INDENT (values NEWLINE)+ NEWLINE* DEDENT
    Optional<insert> insert() throws SyntaxErrorException {

        if (manager.matchAndRemove(Token.TokenTypes.INSERT).isEmpty()){
            return Optional.empty();
        }

        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (identifier.isEmpty()){
            handleError("Insert statement requires identifier at: ");
        }

        insert newInsert = new insert(identifier.get().Value.orElseThrow());

        if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
            handleError("Expected newline at: ");
        }
        if(manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            handleError("Expected indent at: ");
        }
        var value = values();
        if (value.isEmpty()){
            handleError("Expected value at: ");
        }
        while (value.isPresent()){
            newInsert.values.add(value.get());
            if(manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
                handleError("Expected newline at: ");
            }
            value = values();
        }
        while (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            int whatever = 0;
        }
        if (manager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            handleError("Expected dedent at: ");
        }

        return Optional.of(newInsert);
    }
    // delete = "delete" IDENTIFIER NEWLINE INDENT ((values | findOne) NEWLINE)+ NEWLINE* DEDENT
    Optional<delete> delete() throws SyntaxErrorException{
        if (manager.matchAndRemove(Token.TokenTypes.DELETE).isEmpty()){
            return Optional.empty();
        }
        var delete = new delete();
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

        if (identifier.isEmpty()){
            handleError("Expected identifier at: ");
        }
        delete.name = identifier.get().Value.orElseThrow();

        if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
            handleError("Expected newline at: ");
        }

        if (manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            handleError("Expected Indent at: ");
        }

        //var values = values();
        var newFindOne = findOne();

        if (newFindOne.isEmpty()){
            handleError("Expected value or find one at: ");
        }
        while (newFindOne.isPresent()){

            delete.findOne.add(newFindOne.get());
            newFindOne = findOne();
            if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
                handleError("Expected newline at: ");
            }
        }
        return Optional.of(delete);
    }
    // return = "return" returnName ( "," returnName)*
    Optional<return_> return_() throws SyntaxErrorException{
        if (manager.matchAndRemove(Token.TokenTypes.RETURN).isEmpty()){ // check for return declaration
            return Optional.empty();
        }
        var newReturnName = returnName(); // create return name
        if (newReturnName.isEmpty()){
            handleError("expected return name at: "); //handle case with no return name
        }
        var newReturn = new return_(); // create new return
        newReturn.returnName.add(newReturnName.get()); // add return name to return object

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){ // while commas are present
            var anotherReturnName = returnName(); // create returnName object
            if (anotherReturnName.isEmpty()){
                handleError("expected return name at: "); // handle case of no return object
            }
            newReturn.returnName.add(anotherReturnName.get());
        }
        return Optional.of(newReturn);
    }


    // returnName = IDENTIFIER ( "." IDENTIFIER)*
    Optional<returnName> returnName() throws SyntaxErrorException{
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (identifier.isEmpty()){
            return Optional.empty();
        }
        var newReturnName = new returnName();
        newReturnName.field.add(identifier.get().Value.get());

        while(manager.matchAndRemove(Token.TokenTypes.DOT).isPresent()){
            var anotherIdentifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if(anotherIdentifier.isEmpty()){
                handleError("Expected Identifier value at: ");
            }
            newReturnName.field.add(anotherIdentifier.get().Value.get());
        }

        return Optional.of(newReturnName);
    }

    //where = "where" compare ("," returnName)*
    Optional<where> where() throws SyntaxErrorException{
        if (manager.matchAndRemove(Token.TokenTypes.WHERE).isEmpty()){ // check for where token
            return Optional.empty();
        }
        var newWhere = new where(); // create new where ast
        var newCompare = compare(); // create compare

        if (newCompare.isEmpty()){
            handleError("Expected compare statement at: "); // handle case with no compare
        }

        newWhere.compare.add(newCompare.get()); // add compare to where ast

        while(manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
            var newReturnName = returnName();
            if (newReturnName.isEmpty()) {
                handleError("Expected returnName at: "); // handle case of no return name;
            }
        }
        return Optional.of(newWhere);
    }
     // from = "from" IDENTIFIER NEWLINE INDENT where? NEWLINE? return? NEWLINE* DEDENT
    Optional<from> from() throws SyntaxErrorException {
        if (manager.matchAndRemove(Token.TokenTypes.FROM).isEmpty()) {
            return Optional.empty(); // check if from statement
        }
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER); // hold identifier value
        if (identifier.isEmpty()) {
            handleError("Expected Identifier at: "); // handle no identifier
        }
        var from = new from();
        from.name = identifier.get().Value.get(); // assign identifier to from.name
        handleNewLine();

        if (manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            handleError("Expected indent at: ");
        }

        var where = where();
        if (where.isPresent()){
            from.where = where;
            if (manager.matchAndRemove(Token.TokenTypes.WHERE).isPresent()){
                handleError("Only one where statement accepted: ");
            }
        }

        if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){ // check for newline
            if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                handleError("Only one newline accepted at: "); // check for 2nd newline
            }
        }

        var return_ = return_();

        if (return_.isPresent()){
            from.return_ = return_;
        }

        var token = manager.matchAndRemove(Token.TokenTypes.NEWLINE);
        while(token.isPresent()){
            token = manager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }

        if (manager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            handleError("Expected dedent at: ");
        }

        return Optional.of(from);
    }

    // type = "enum" "(" IDENTIFIER ("," IDENTIFIER)* ")" | "string" | "wholeNumber" | "decimalNumber" | "list" "<" IDENTIFIER ">" | IDENTIFIER
    Optional<type> type() throws SyntaxErrorException{
        if (manager.matchAndRemove(Token.TokenTypes.ENUM).isEmpty()){
            return Optional.empty();
        }

        if (manager.matchAndRemove(Token.TokenTypes.OPENPAREN).isEmpty()){
            handleError("Expected open parenthesis at: ");
        }

        var type = new type();

        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (identifier.isEmpty()){
            handleError("Expected identifier at: ");
        }

        while(manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
            var newIdentifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (newIdentifier.isEmpty()){
                handleError("Expected identifier at: ");
            }
            type.tableName.add(newIdentifier.get().Value.get());
        }

        var variable = manager.checkType();

        if (variable.isEmpty()){
            handleError("Invalid syntax for type at: ");
        }
        if (variable.get().Type == Token.TokenTypes.STRING){

        }

        if (variable.get().Type == Token.TokenTypes.WHOLENUMBER){

        }

        if (variable.get().Type == Token.TokenTypes.DECIMALNUMBER){

        }

        if (variable.get().Type == Token.TokenTypes.LIST){
            if (manager.matchAndRemove(Token.TokenTypes.LESSTHAN).isEmpty()){
                handleError("Expected greater than symbol at: ");
            }

            var newIdentifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

            if (newIdentifier.isEmpty()){
                handleError("Expected Identifier at: ");
            }

            type.tableName.add(newIdentifier.get().Value.get());

            type.list = true;
        }


//        if (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
//            var newIdentifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
//            if (newIdentifier.isEmpty()){
//                handleError("Expected Identifier at: ");
//            }
//
//        }

        return Optional.of(type);
    }

    // field = type IDENTIFIER
    Optional<field> field() throws SyntaxErrorException{
        var newType = type();
        if (newType.isEmpty()){
            return Optional.empty();
        }
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if(identifier.isEmpty()){
            handleError("Expected Identifier at: ");
        }

        field newField = new field();

        newField.name = identifier.get().Value.get();
        newField.type = newType.get();

        return Optional.of(newField);
    }

    // create = "create" "table" IDENTIFIER NEWLINE INDENT (field NEWLINE) + NEWLINE* DEDENT
    Optional<create> create() throws SyntaxErrorException{
        if (manager.matchAndRemove(Token.TokenTypes.CREATE).isEmpty()){
            return Optional.empty();
        }
        if (manager.matchAndRemove(Token.TokenTypes.TABLE).isEmpty()){
            handleError("Expected table declaration at: ");
        }
        var create = new create();
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

        if (identifier.isEmpty()){
            handleError("Expected Identifier at: ");
        }
        handleNewLine();

        if(manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            handleError("Expected Newline at: ");
        }

        var newField = field();
        if (newField.isEmpty()){
            handleError("Expected field at: ");
        }

        while (newField.isPresent()){
            handleNewLine();
            newField = field();
        }

        while (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            int whatever = 0;
        }

        if (manager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            handleError("Expected Dedent at: ");
        }
        return Optional.of(create);
    }
    }