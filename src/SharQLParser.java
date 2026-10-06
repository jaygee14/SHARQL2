import AST.findOne;
import AST.compare;
import java.util.List;
import java.util.Optional;


public class SharQLParser {
    private List<Token> tokenList;
    private TokenManager manager;
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

        var compareOne = compare();

        if (compareOne.isEmpty()){
            handleError("findOne must have compare statement at: ");
        }
        if (manager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
            Optional<compare> compareTwo = compare();
            if (compareTwo.isEmpty()){
                handleError("Expecting comparison at: ");
            }
        }
        if(manager.matchAndRemove(Token.TokenTypes.CLOSEPAREN).isEmpty()){
            handleError("Must close findOne call: ");
        }
        return Optional.of(new findOne());
    }

    // compare = IDENTIFIER ( "=" | "<" | ">" | "<>" | "<=" | ">=") (NUMBER | STRINGLITERAL | IDENTIFIER | findOne)
    Optional<compare> compare() throws SyntaxErrorException {
        if (manager.matchAndRemove(Token.TokenTypes.IDENTIFIER).isEmpty()){
            return Optional.empty(); // not a comparison
        }
        if (manager.checkComparaters().isEmpty()){
            handleError("Compare statement needs compare operation at: ");
        }
        if (manager.checkPassers().isEmpty()){
            handleError("Compare statement received incompatible data type at: ");
        }
        return Optional.of(new compare());
    }
}