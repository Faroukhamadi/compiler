import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
  private final Deque<Object> stack = new ArrayDeque<>();

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
    stack.push(input.substring(pos + 1, pos + last + 1));
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
    if (num.length() > 0) {
      pos += num.length();
      stack.push(Integer.parseInt(num));
      return true;
    }
    return false;
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

  // VALUE ::= STRINGLIT / NUMBER / OBJECT / ARRAY
  private boolean parseValue() {
    return parseStringLit() || parseNumber() || parseObject() || parseArray();
  }

  // OBJECT ::= "{" (PAIR ("," PAIR)* )? "}"
  private boolean parseObject() {
    int pos0 = pos;
    int stack0 = stack.size();
    boolean success = parseChar('{') && parsePairs() && parseChar('}');
    if (!success) {
      pos = pos0;
      return false;
    }

    HashMap<String, Object> object = new HashMap<>();
    while (stack.size() > stack0) {
      Object value = stack.pop();
      String string = (String) stack.pop();
      object.put(string, value);
    }
    stack.push(object);
    return true;
  }

  // (PAIR ("," PAIR)* )?
  private boolean parsePairs() {
    if (parsePair()) {
      parsePairTails();
    }
    return true;
  }

  // PAIR ::= STRINGLIT ":" VALUE
  private boolean parsePair() {
    int pos0 = pos;
    boolean success = parseStringLit() && parseChar(':') && parseValue();
    if (!success) {
      pos = pos0;
    }
    return success;
  }

  // ("," PAIR)*
  private boolean parsePairTails() {
    while (true) {
      int pos0 = pos;
      boolean success = parseChar(',') && parsePair();
      if (!success) {
        pos = pos0;
        return true;
      }
    }
  }

  // ARRAY ::= "[" (VALUE ("," VALUE)* )? "]"
  private boolean parseArray() {
    int pos0 = pos;
    int stack0 = stack.size();
    boolean success = parseChar('[') && parseValues() && parseChar(']');
    if (!success) {
      pos = pos0;
      return false;
    }
    ArrayList<Object> array = new ArrayList<>();
    while (stack.size() > stack0) {
      array.add(stack.pop());
    }
    Collections.reverse(array);
    stack.push(array);
    return true;
  }

  // (VALUE ("," VALUE)* )?
  private boolean parseValues() {
    if (parseValue()) {
      parseValueTails();
    }
    return true;
  }

  // ("," VALUE)*
  private boolean parseValueTails() {
    while (true) {
      int pos0 = pos;
      boolean success = parseChar(',') && parseValue();
      if (!success) {
        pos = pos0;
        return true;
      }
    }
  }

}