package io.github.yamatrireddy;

import io.github.yamatrireddy.exception.RSQLParseException;
import io.github.yamatrireddy.lexer.Lexer;
import io.github.yamatrireddy.lexer.Token;
import io.github.yamatrireddy.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LexerTest {

    private List<Token> tokenize(String input) {
        Lexer lexer = new Lexer(input);
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = lexer.next();
            tokens.add(t);
        } while (t.getType() != TokenType.EOF);
        return tokens;
    }

    @Test
    void singleComparison_producesValueOperatorValueEof() {
        List<Token> tokens = tokenize("name==Alice");
        assertThat(tokens).extracting(Token::getType)
                .containsExactly(TokenType.VALUE, TokenType.OPERATOR, TokenType.VALUE, TokenType.EOF);
        assertThat(tokens.get(0).getValue()).isEqualTo("name");
        assertThat(tokens.get(1).getValue()).isEqualTo("==");
        assertThat(tokens.get(2).getValue()).isEqualTo("Alice");
    }

    @Test
    void semicolonBecomesAndToken() {
        List<Token> tokens = tokenize("a==1;b==2");
        assertThat(tokens).extracting(Token::getType)
                .containsExactly(TokenType.VALUE, TokenType.OPERATOR, TokenType.VALUE,
                        TokenType.AND,
                        TokenType.VALUE, TokenType.OPERATOR, TokenType.VALUE, TokenType.EOF);
    }

    @Test
    void commaBecomesOrToken() {
        List<Token> tokens = tokenize("a==1,b==2");
        assertThat(tokens).extracting(Token::getType)
                .containsExactly(TokenType.VALUE, TokenType.OPERATOR, TokenType.VALUE,
                        TokenType.OR,
                        TokenType.VALUE, TokenType.OPERATOR, TokenType.VALUE, TokenType.EOF);
    }

    @Test
    void keywordAndIsRecognised() {
        List<Token> tokens = tokenize("a==1 and b==2");
        assertThat(tokens.get(3).getType()).isEqualTo(TokenType.AND);
    }

    @Test
    void keywordOrIsRecognised() {
        List<Token> tokens = tokenize("a==1 or b==2");
        assertThat(tokens.get(3).getType()).isEqualTo(TokenType.OR);
    }

    @Test
    void doubleQuotedStringIsUnquoted() {
        List<Token> tokens = tokenize("name==\"John Doe\"");
        assertThat(tokens.get(2).getValue()).isEqualTo("John Doe");
    }

    @Test
    void singleQuotedStringIsUnquoted() {
        List<Token> tokens = tokenize("name=='O\\'Brien'");
        assertThat(tokens.get(2).getValue()).isEqualTo("O'Brien");
    }

    @Test
    void namedOperatorGtMapsToSymbol() {
        List<Token> tokens = tokenize("age=gt=18");
        assertThat(tokens.get(1).getValue()).isEqualTo(">");
    }

    @Test
    void namedOperatorGeMapsToSymbol() {
        assertThat(tokenize("age=ge=18").get(1).getValue()).isEqualTo(">=");
    }

    @Test
    void namedOperatorLtMapsToSymbol() {
        assertThat(tokenize("age=lt=18").get(1).getValue()).isEqualTo("<");
    }

    @Test
    void namedOperatorLeMapsToSymbol() {
        assertThat(tokenize("age=le=18").get(1).getValue()).isEqualTo("<=");
    }

    @Test
    void inOperatorIsPreserved() {
        assertThat(tokenize("role=in=(a,b)").get(1).getValue()).isEqualTo("=in=");
    }

    @Test
    void outOperatorIsPreserved() {
        assertThat(tokenize("role=out=(a,b)").get(1).getValue()).isEqualTo("=out=");
    }

    @Test
    void parenthesesProduceCorrectTokens() {
        List<Token> tokens = tokenize("(a==1)");
        assertThat(tokens.get(0).getType()).isEqualTo(TokenType.L_PAREN);
        assertThat(tokens.get(4).getType()).isEqualTo(TokenType.R_PAREN);
    }

    @Test
    void positionIsRecordedForFirstToken() {
        List<Token> tokens = tokenize("name==Alice");
        assertThat(tokens.get(0).getPosition()).isEqualTo(0);
    }

    @Test
    void whitespaceIsSkipped() {
        List<Token> tokens = tokenize("  name  ==  Alice  ");
        assertThat(tokens.get(0).getValue()).isEqualTo("name");
        assertThat(tokens.get(1).getValue()).isEqualTo("==");
        assertThat(tokens.get(2).getValue()).isEqualTo("Alice");
    }

    @Test
    void unterminatedStringThrowsParseException() {
        assertThatThrownBy(() -> tokenize("name==\"unterminated"))
                .isInstanceOf(RSQLParseException.class)
                .hasMessageContaining("Unterminated");
    }

    @Test
    void eofIsReturnedRepeatedly() {
        Lexer lexer = new Lexer("");
        assertThat(lexer.next().getType()).isEqualTo(TokenType.EOF);
        assertThat(lexer.next().getType()).isEqualTo(TokenType.EOF);
    }

    @Test
    void nullInputThrowsNullPointerException() {
        assertThatThrownBy(() -> new Lexer(null))
                .isInstanceOf(NullPointerException.class);
    }
}
