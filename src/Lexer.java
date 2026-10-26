import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Lexer {

    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keyWords;
    public int lineNumber = 1;
    public int characterPosition = 1;
    List<Token> listOfTokens;
    int indentLevel = 0;
    private final Stack<Integer> indentLevels = new Stack<>();

    public Lexer(String word)  {
        this.textManager = new TextManager(word);
        keyWords = new HashMap<>();
        indentLevels.push(0);

        keyWords.put("identifier", Token.TokenTypes.IDENTIFIER);
        keyWords.put("number", Token.TokenTypes.NUMBER);
        keyWords.put("newLine", Token.TokenTypes.NEWLINE);
        keyWords.put("indent", Token.TokenTypes.INDENT);
        keyWords.put("dedent", Token.TokenTypes.DEDENT);
        keyWords.put("string", Token.TokenTypes.STRING);
        keyWords.put("from", Token.TokenTypes.FROM);
        keyWords.put("create", Token.TokenTypes.CREATE);
        keyWords.put("table", Token.TokenTypes.TABLE);
        keyWords.put("enum", Token.TokenTypes.ENUM);
        keyWords.put("decimalNumber", Token.TokenTypes.DECIMALNUMBER);
        keyWords.put("where", Token.TokenTypes.WHERE);
        keyWords.put("wholeNumber", Token.TokenTypes.WHOLENUMBER);
        keyWords.put("list", Token.TokenTypes.LIST);
        keyWords.put("delete", Token.TokenTypes.DELETE);
        keyWords.put("greaterThan", Token.TokenTypes.GREATERTHAN);
        keyWords.put("lessThan", Token.TokenTypes.LESSTHAN);
        keyWords.put("greaterThanEqual", Token.TokenTypes.GREATERTHANEQUAL);
        keyWords.put("lessThanEqual", Token.TokenTypes.LESSTHANEQUAL);
        keyWords.put("insert", Token.TokenTypes.INSERT);
        keyWords.put("return", Token.TokenTypes.RETURN);
        keyWords.put("findOne", Token.TokenTypes.FINDONE);
        keyWords.put("closeParen", Token.TokenTypes.CLOSEPAREN);
        keyWords.put("openParen", Token.TokenTypes.OPENPAREN);
        keyWords.put("equal", Token.TokenTypes.EQUAL);
        keyWords.put("notEqual", Token.TokenTypes.NOTEQUAL);
        keyWords.put("leftBrace", Token.TokenTypes.LEFTBRACE);
        keyWords.put("rightBrace", Token.TokenTypes.RIGHTBRACE);
        keyWords.put("comma", Token.TokenTypes.COMMA);
        keyWords.put("dot", Token.TokenTypes.DOT);

    }

    private void increaseLine(){
        lineNumber++;
    }

    private void increaseCharacterPosition(int n){

        characterPosition += n;
    }
    private void resetPosition(){

        characterPosition = 1;
    }
    private void resetLine(){
        lineNumber = 0;
    }

    private void updateIndentation(int spaces) throws SyntaxErrorException {

        if (spaces % 2 != 0) {
            throw new SyntaxErrorException(
                    "Invalid indentation", lineNumber, characterPosition);
        }

        if (spaces > indentLevels.peek()) {
            indentLevels.push(spaces);
            listOfTokens.add(new Token(Token.TokenTypes.INDENT,
                    lineNumber, characterPosition));
        }

        else if (spaces < indentLevels.peek()) {
            while (indentLevels.size() > 1 && spaces < indentLevels.peek()) {
                indentLevels.pop();
                listOfTokens.add(new Token(Token.TokenTypes.DEDENT,
                        lineNumber, characterPosition));
            }

            if (spaces != indentLevels.peek()) {
                throw new SyntaxErrorException(
                        "Invalid dedentation", lineNumber, characterPosition);
            }
        }
    }


    public List<Token> Lex() throws SyntaxErrorException {
        listOfTokens = new ArrayList<Token>();
        while (!textManager.isEnd()) {
            char c = textManager.peekCharacter();

            if (characterPosition == 1 &&
                    c != ' ' &&
                    c != '\n' &&
                    c != '\t') {
                updateIndentation(0);
            }


            if (Character.isLetter(c)) {
                String word = textManager.readWord();

                if (keyWords.containsKey(word)){
                    listOfTokens.add( new Token(keyWords.get(word), lineNumber, characterPosition, word));
                    increaseCharacterPosition(word.length());
                    //textManager.getCharacter();
                }
                else {
                    listOfTokens.add(new Token(Token.TokenTypes.IDENTIFIER, lineNumber, characterPosition, word));
                    increaseCharacterPosition(word.length());
                    //textManager.getCharacter();
                }
            }

            else if (Character.isDigit(c)) {
                String number = textManager.readNumber();

                listOfTokens.add(new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, number));
                increaseCharacterPosition(number.length());
            }
            else if (c == '\t'){
                updateIndentation(2);
                increaseCharacterPosition(2);
                textManager.getCharacter();

            }

            else if (c == '\n'){
                listOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                textManager.getCharacter();
                resetPosition();
                lineNumber++;
            }

            else if (c == '='){
                listOfTokens.add(new Token(Token.TokenTypes.EQUAL, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }

            else if (c == ' ') {
                boolean startOfLine = characterPosition == 1;
                int spaceCount = 0;

                while (!textManager.isEnd() && textManager.peekCharacter() == ' ') {
                    textManager.getCharacter();
                    spaceCount++;
                    characterPosition++;
                }

                if (startOfLine && !textManager.isEnd() && textManager.peekCharacter() != '\n') {
                    updateIndentation(spaceCount);
                }
            }
            else if (c == '/'){
                textManager.getCharacter();
                characterPosition++;
                if (!textManager.isEnd() && textManager.peekCharacter() == '*'){
                    textManager.getCharacter();
                    characterPosition++;
                    while(!textManager.isEnd()){
                        c = textManager.getCharacter();
                        characterPosition++;

                        if (c == '*' && !textManager.isEnd() && textManager.peekCharacter() == '/'){
                            textManager.getCharacter();
                            characterPosition++;
                            break;
                        }
                    }
                }
            }

            else if (c == '"'){
                String str = "";
                textManager.getCharacter();
                characterPosition++;

                while(!textManager.isEnd() && textManager.peekCharacter() != '"'){
                    str = str + textManager.getCharacter();
                    characterPosition++;
                }
                if (textManager.isEnd()){
                    throw new SyntaxErrorException("Unterminated String", lineNumber, characterPosition);
                }
                textManager.getCharacter();
                characterPosition++;
                listOfTokens.add(new Token(Token.TokenTypes.STRINGLITERAL, lineNumber, characterPosition, str));
            }
            else if (c == '('){
                listOfTokens.add(new Token (Token.TokenTypes.OPENPAREN, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == ')'){
                listOfTokens.add(new Token (Token.TokenTypes.CLOSEPAREN, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == ','){
                listOfTokens.add(new Token (Token.TokenTypes.COMMA, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == '<'){
                listOfTokens.add(new Token (Token.TokenTypes.LESSTHAN, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == '>'){
                listOfTokens.add(new Token (Token.TokenTypes.GREATERTHAN, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }

            else if (c == '['){
                listOfTokens.add(new Token (Token.TokenTypes.LEFTBRACE, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == ']'){
                listOfTokens.add(new Token (Token.TokenTypes.RIGHTBRACE, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else if (c == '.'){
                listOfTokens.add(new Token (Token.TokenTypes.DOT, lineNumber, characterPosition));
                textManager.getCharacter();
                characterPosition++;
            }
            else {
                textManager.getCharacter();
                characterPosition++;
            }
        }

//        for (int i = 0; i < listOfTokens.size(); i++){
//            System.out.println(i + ": " + listOfTokens.get(i).Type + " " + listOfTokens.get(i).Value);
//        }
        if (!listOfTokens.isEmpty() &&
                listOfTokens.getLast().Type != Token.TokenTypes.NEWLINE &&
                listOfTokens.getLast().Type != Token.TokenTypes.DEDENT) {

            listOfTokens.add(new Token(Token.TokenTypes.NEWLINE,
                    lineNumber, characterPosition));
        }

        while (indentLevels.size() > 1) {
            indentLevels.pop();
            listOfTokens.add(new Token(Token.TokenTypes.DEDENT,
                    lineNumber, characterPosition));
        }

        return listOfTokens;
    }
}