import java.lang.Object;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;

public class TokenManager {

    private final List<Token> tokenList;

    public TokenManager(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    public Optional<Token> peek(int index) {

        if (index < 0 || index >= tokenList.size()) {
            return Optional.empty();
        }
        return Optional.of(tokenList.get(index));
    }

    public int getCurrentLine(){
        if (tokenList.isEmpty()){
            return -1;
        }
        return tokenList.getFirst().LineNumber;
    }

    public int getCurrentColumnNumber(){
        if (tokenList.isEmpty()){
            return -1;
        }
        return tokenList.getFirst().ColumnNumber;
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        return first == second;
    }

    private final Token.TokenTypes[] comparisonTokens = {Token.TokenTypes.LESSTHAN, Token.TokenTypes.LESSTHANEQUAL,
            Token.TokenTypes.GREATERTHANEQUAL, Token.TokenTypes.GREATERTHAN, Token.TokenTypes.NOTEQUAL, Token.TokenTypes.EQUAL};

    private final Token.TokenTypes[] passerTokens = {Token.TokenTypes.NUMBER, Token.TokenTypes.STRINGLITERAL,
            Token.TokenTypes.FINDONE, Token.TokenTypes.IDENTIFIER};

    private final Token.TokenTypes[] statementTokens = {Token.TokenTypes.CREATE, Token.TokenTypes.INSERT, Token.TokenTypes.DELETE, Token.TokenTypes.FROM};

    private final Token.TokenTypes[] typeTokens = {Token.TokenTypes.STRING, Token.TokenTypes.WHOLENUMBER, Token.TokenTypes.DECIMALNUMBER, Token.TokenTypes.LIST, Token.TokenTypes.IDENTIFIER};

    Optional<Token> matchAndRemove(Token.TokenTypes token) {

        if(tokenList.isEmpty()){
            return Optional.empty();
        }
        if (tokenList.getFirst().Type == token){
            Optional<Token> passedToken = Optional.of(tokenList.getFirst());
            tokenList.removeFirst();
            return passedToken;
        }
        return Optional.empty();
    }

    Optional<Token> checkComparaters(){
        Optional<Token> token = Optional.empty();
        for (Token.TokenTypes comparisonToken : comparisonTokens) {
            token = matchAndRemove(comparisonToken);
            if (token.isPresent()) {
                break;
            }
        }
        return token;
    }

    Optional<Token> checkPassers(){
        Optional<Token> token = Optional.empty();
        for (Token.TokenTypes passerToken : passerTokens) {
            token = matchAndRemove(passerToken);
            if (token.isPresent()) {
                break;
            }
        }
        return token;
    }

    Optional<Token> checkType(){
        Optional<Token> token = Optional.empty();
        for (Token.TokenTypes type : typeTokens) {
            token = matchAndRemove(type);
            if (token.isPresent()) {
                break;
            }
        }
        return token;
    }

    Optional<Token> checkStatement() {

        Optional<Token> token = peek(0);

        if (token.isEmpty()){
            return Optional.empty();
        }

        for (Token.TokenTypes statementToken : statementTokens) {
            if (token.get().Type == statementToken) {
                return token;
            }
        }

        return Optional.empty();
    }

}
