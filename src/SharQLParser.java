import AST.*;
import org.w3c.dom.css.CSSValueList;

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
        //Optional<valueList> newValueList = valueList();
        // if (manager.checkPassers().isEmpty()) || {
           // handleError("Variable passed invalid input at: ");
       // }
        return Optional.empty();
    }

    // values = value ("," value)*

    Optional<values> values(){
        return Optional.empty();
    }

    // valueList = "[" findOne? ("," findOne)* "]"

    Optional<valueList> listOfValues() throws SyntaxErrorException{

        if (manager.matchAndRemove(Token.TokenTypes.LEFTBRACE).isEmpty()){
            handleError("Value List needs opening bracket: ");
        }
        if (manager.matchAndRemove(Token.TokenTypes.FINDONE).isEmpty()){
            var valueList = new valueList();
            return Optional.of(valueList);
        }

        var newFindOne = new findOne();
        valueList listOfValues = new valueList();
        listOfValues.findOne.add(newFindOne);

        while (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            Optional<findOne> nextFindOne = findOne();
            if (nextFindOne.isEmpty()) {
                handleError("Expecting comparison at: ");
            }
            listOfValues.findOne.add(nextFindOne.get());
        }
        listOfValues.findOne.add(newFindOne);
        return Optional.of(listOfValues);
    }

    // insert = "insert" IDENTIFIER NEWLINE INDENT (values NEWLINE)+ NEWLINE* DEDENT
    Optional<insert> insert(){
        return Optional.empty();
    }
    // delete = "delete" IDENTIFIER NEWLINE INDENT ((values | findOne) NEWLINE)+ NEWLINE* DEDENT
}