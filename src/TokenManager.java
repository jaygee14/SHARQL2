import java.lang.Object;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;

public class TokenManager {

    private final List<Token> tokenList;

    public TokenManager(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    Optional<Token> peek(int num){
        return Optional.of(tokenList.get(num));
    }

    public int getCurrentLine(){
        return tokenList.getFirst().LineNumber;
    }

    public int getCurrentColumnNumber(){
        return tokenList.getFirst().ColumnNumber;
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        return first == second;
    }

    private final Token.TokenTypes[] comparisonTokens = {Token.TokenTypes.LESSTHAN, Token.TokenTypes.LESSTHANEQUAL,
            Token.TokenTypes.GREATERTHANEQUAL, Token.TokenTypes.GREATERTHAN, Token.TokenTypes.NOTEQUAL};

    private final Token.TokenTypes[] passerTokens = {Token.TokenTypes.NUMBER, Token.TokenTypes.STRINGLITERAL,
            Token.TokenTypes.FINDONE, Token.TokenTypes.IDENTIFIER};


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

}
