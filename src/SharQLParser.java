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
    private Optional<findOne> findOne() throws SyntaxErrorException {
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
    private Optional<statement> statement() throws SyntaxErrorException {
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
        if (passedValue.get().Type == Token.TokenTypes.CREATE){
            statement.create = create();
        }
        if (passedValue.get().Type == Token.TokenTypes.FROM){
            statement.from = from();
        }
        return Optional.of(statement);
    }

    private Optional<compare> compare() throws SyntaxErrorException {

        var leftToken = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

        if (leftToken.isEmpty()) {
            return Optional.empty();
        }

        var operatorToken = manager.checkComparaters();

        if (operatorToken.isEmpty()) {
            handleError("Expected comparison operator at: ");
        }

        var newCompare = new compare();
        newCompare.left = leftToken.get().Value.orElseThrow();

        if (operatorToken.get().Type == Token.TokenTypes.EQUAL) {
            newCompare.thecompareOp = compareOp.equal;
        }

        if (manager.peek(0).isPresent()
                && manager.peek(0).get().Type == Token.TokenTypes.FINDONE) {

            newCompare.findOne = findOne();
            return Optional.of(newCompare);
        }

        var passedValue = manager.checkPassers();

        if (passedValue.isEmpty()) {
            handleError("Expected comparison value at: ");
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
    private Optional<value> value() throws SyntaxErrorException {

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
    private Optional<values> values() throws SyntaxErrorException {

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
    private Optional<valueList> listOfValues() throws SyntaxErrorException {

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
    private Optional<insert> insert() throws SyntaxErrorException {

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
    private Optional<delete> delete() throws SyntaxErrorException{
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
    private Optional<return_> return_() throws SyntaxErrorException{
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
    private Optional<returnName> returnName() throws SyntaxErrorException{
        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (identifier.isEmpty()){
            return Optional.empty();
        }
        var newReturnName = new returnName();
        newReturnName.name = identifier.get().Value.get();

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
    private Optional<where> where() throws SyntaxErrorException{
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
    private Optional<from> from() throws SyntaxErrorException {
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
        }

        manager.matchAndRemove(Token.TokenTypes.NEWLINE);

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
    private Optional<type> type() throws SyntaxErrorException {

        type newType = new type();

        if (manager.matchAndRemove(Token.TokenTypes.ENUM).isPresent()) {
            newType.enum_ = true;

            if (manager.matchAndRemove(Token.TokenTypes.OPENPAREN).isEmpty()){
                handleError("Expected open parenthesis at: ");
            }
            var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);

            if (identifier.isEmpty()){
                handleError("Expected identifier at: ");
            }
            newType.enumValue.add(identifier.get().Value.orElseThrow());

            while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                var nextIdentifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
                if (nextIdentifier.isEmpty()){
                    handleError("Expected identifier at: ");
                }

                newType.enumValue.add(nextIdentifier.get().Value.orElseThrow());
            }
            if (manager.matchAndRemove(Token.TokenTypes.CLOSEPAREN).isEmpty()){
                handleError("Expected closing parenthesis at: ");
            }
            return Optional.of(newType);
        }
        if (manager.matchAndRemove(Token.TokenTypes.STRING).isPresent()) {
            newType.thestring_ORwholeNumberORdecimalNumber =
                    Optional.of(string_ORwholeNumberORdecimalNumber.string_);

            return Optional.of(newType);
        }
        if (manager.matchAndRemove(Token.TokenTypes.WHOLENUMBER).isPresent()) {
            newType.thestring_ORwholeNumberORdecimalNumber =
                    Optional.of(string_ORwholeNumberORdecimalNumber.wholeNumber);

            return Optional.of(newType);
        }
        if (manager.matchAndRemove(Token.TokenTypes.DECIMALNUMBER).isPresent()) {
            newType.thestring_ORwholeNumberORdecimalNumber =
                    Optional.of(string_ORwholeNumberORdecimalNumber.decimalNumber);

            return Optional.of(newType);
        }
        if (manager.matchAndRemove(Token.TokenTypes.LIST).isPresent()) {

            newType.list = true;

            if (manager.matchAndRemove(Token.TokenTypes.LESSTHAN).isEmpty()){
                handleError("Expected < at: ");
            }
            var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
            if (identifier.isEmpty()){
                handleError("Expected identifier at: ");
            }
            newType.tableName.add(identifier.get().Value.orElseThrow());

            if (manager.matchAndRemove(Token.TokenTypes.GREATERTHAN).isEmpty()){
                handleError("Expected > at: ");
            }
            return Optional.of(newType);
        }

        var identifier = manager.matchAndRemove(Token.TokenTypes.IDENTIFIER);
        if (identifier.isPresent()) {
            newType.tableName.add(identifier.get().Value.orElseThrow());
            return Optional.of(newType);
        }
        return Optional.empty();
    }
    // field = type IDENTIFIER
    private Optional<field> field() throws SyntaxErrorException{
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

    // create = "create" "table" IDENTIFIER NEWLINE INDENT (field NEWLINE)+ NEWLINE* DEDENT
    private Optional<create> create() throws SyntaxErrorException{
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

        create.name = identifier.get().Value.get();
        handleNewLine();

        if(manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            handleError("Expected Indent at: ");
        }

        var newField = field();

        if (newField.isEmpty()){
            handleError("Expected field at: ");
        }

        while (newField.isPresent()){
            create.field.add(newField.get());
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