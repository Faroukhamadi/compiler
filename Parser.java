import java.util.Scanner;

public final class Parser {
  // SETUP

  private static final String EXAMPLE_INPUT = "{\n" +
      "    \"version\": 17,\n" +
      "    \"bundles\": [\n" +
      "        { \"name\" : \"org.graalvm.component.installer.Bundle\" },\n" +
      "        { \"name\" : \"org.graalvm.component.installer.commands.Bundle\" },\n" +
      "        { \"name\" : \"org.graalvm.component.installer.remote.Bundle\" },\n" +
      "        { \"name\" : \"org.graalvm.component.installer.os.Bundle\" }\n" +
      "    ]\n" +
      "}";

  public static void main(String[] args) {
    Parser parser = new Parser(EXAMPLE_INPUT);
    if (parser.parseValue()) {
      System.out.println("GREAT SUCCESS");
    } else {
      System.out.println("MISERABLE FAILURE");
    }
  }

  private final String input;
  private int pos = 0;

  public Parser(String input) {
    this.input = input;
  }

  // LEXICAL
  private void skipWhitespace() {
    while (pos < input.length() && (input.charAt(pos) == ' ' || input.charAt(pos) == '\n')) {
      pos++;
    }
  }

  private boolean parseStringLit() {
    if (pos >= input.length()) {
      return false;
    }
    if (input.charAt(pos) != '"') {
      return false;
    }
    int last = input.substring(pos + 1).indexOf('"');
    if (last < 0) {
      return false;
    }
    pos += last + 2;
    skipWhitespace();
    return true;
  }

  private boolean parseNumber() {
    if (pos >= input.length()) {
      return false;
    }
    Scanner scanner = new Scanner(input.substring(pos));
    String num = scanner.useDelimiter("[^0-9]").next();
    pos += num.length();
    return num.length() > 0;
  }

  private boolean parseChar(char c) {
    if (pos >= input.length()) {
      return false;
    }
    boolean success = input.charAt(pos) == c;
    if (!success) {
      return false;
    }
    pos++;
    skipWhitespace();
    return true;
  }

  // PARSER

  /*
   * VALUE ::= STRINGLIT / NUMBER / OBJECT / ARRAY
   * OBJECT ::= "{" (PAIR ("," PAIR)* )? "}"
   * PAIR ::= STRINGLIT ":" VALUE
   * ARRAY ::= "[" (VALUE ("," VALUE)* )? "]"
   */

}