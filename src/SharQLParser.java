import AST.*;
import org.w3c.dom.css.CSSValueList;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;


public class SharQLParser {
    private List<Token> tokenList;
    private final TokenManager manager;


    private void handleError(String errorString) throws SyntaxErrorException {
        throw new SyntaxErrorException(errorString, manager.getCurrentLine(), manager.getCurrentColumnNumber());
    }

    public SharQLParser(List<Token> tokenList){
        this.manager = new TokenManager(tokenList);
    }

    public findOne parser1Test() throws SyntaxErrorException {
        return findOne().orElseThrow();
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
    Optional<value> value() throws SyntaxErrorException{

        Optional<valueList> newValueList = listOfValues();
        Optional<Token> passedValue = manager.checkPassers();

        if (passedValue.isEmpty() && newValueList.isEmpty()){
           handleError("Variable passed invalid input at: ");
       }
        var value = new value();

        if (newValueList.isPresent()){
            value.valueList = newValueList;
            return Optional.of(value);
        }
        var type = passedValue.get().Type;


        if (type == Token.TokenTypes.STRINGLITERAL){
            value.STRINGLITERAL = passedValue.get().Value;
            return Optional.of(value);
        }
        if (type == Token.TokenTypes.NUMBER){
            value.NUMBER = passedValue.get().Value;
            return Optional.of(value);
        }

        if (type == Token.TokenTypes.FINDONE){
            value.findOne = findOne();
            return Optional.of(value);
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

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
            var newValue = value();
            if (newValue.isEmpty()){
                handleError("Expected value at: ");
            }
            values.value.add(newValue.get());
        }
        return Optional.empty();
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

        insert newInsert = new insert(identifier.toString());

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
        if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            while (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                int whatever = 0;
            }
            if (manager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
                handleError("Expected dedent at: ");
            }
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

        if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
            handleError("Expected newline at: ");
        }

        if (manager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            handleError("Expected Indent at: ");
        }

        var values = values();
        var newFindOne = findOne();

        if (newFindOne.isEmpty() || values.isEmpty()){
            handleError("Expected value or find one at: ");
        }
        while (values.isPresent() || newFindOne.isPresent()){
            if (values.isPresent()){
                delete.values.add(values.get());
                values = values();
            }
            if (newFindOne.isPresent()){
                delete.findOne.add(newFindOne.get());
                newFindOne = findOne();
            }
            if (manager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
                handleError("Expected newline at: ");
            }
        }
        return Optional.empty();
    }
}