import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Supplier;

public final class Combinators {

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
    Combinators parser = new Combinators(EXAMPLE_INPUT);
    if (parser.value.parse()) {
      System.out.println("GREAT SUCCESS");
    } else {
      System.out.println("MISERABLE FAILURE");
    }
  }

  private final String input;
  private int pos = 0;
  private final Deque<Object> stack = new ArrayDeque<>();

  public Combinators(String input) {
    this.input = input;
  }

  // LEXICAL
  private void skipWhitespace() {
    while (pos < input.length() && (input.charAt(pos) == ' ' || input.charAt(pos) == '\n')) {
      pos++;
    }
    System.out.println("this is pos in skipwhitespace: " + pos);
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
    System.out.println("this is pos in parseStringLit: " + pos);
    skipWhitespace();
    System.out.println("this is pos in parseStringLit: " + pos);
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
      System.out.println("this is pos in parseNumber: " + pos);
      return true;
    }
    System.out.println("this is pos in parseNumber: " + pos);
    return false;
  }

  private boolean parseChar(char c) {
    if (pos >= input.length()) {
      System.out.println("this is pos in parseChar1: " + pos);
      return false;
    }
    boolean success = input.charAt(pos) == c;
    if (!success) {
      System.out.println("This is the character in the param: " + c);
      System.out.println("This is the character at pos: " + input.charAt(pos));
      System.out.println("this is pos in parseChar2: " + pos);
      return false;
    }
    pos++;
    skipWhitespace();
    System.out.println("this is pos in parseChar3: " + pos);
    return true;
  }

  // ------combinators-------
  private interface Parser {
    boolean parse();
  }

  // lexical

  private final class StringLitParser implements Parser {
    @Override
    public boolean parse() {
      return parseStringLit();
    }
  }

  private final class NumberParser implements Parser {
    @Override
    public boolean parse() {
      return parseNumber();
    }
  }

  private final class CharParser implements Parser {
    public final char c;

    private CharParser(char c) {
      this.c = c;
    }

    @Override
    public boolean parse() {
      return parseChar(c);
    }
  }

  // combinators
  private final class Sequence implements Parser {
    public final Parser[] children;

    private Sequence(Parser... children) {
      this.children = children;
    }

    @Override
    public boolean parse() {
      int pos0 = pos;
      for (Parser child : children) {
        if (!child.parse()) {
          pos = pos0;
          return false;
        }
      }
      return true;
    }
  }

  public static final class ForwardReference implements Parser {
    private final Supplier<Parser> supplier;

    public ForwardReference(Supplier<Parser> supplier) {
      this.supplier = supplier;
    }

    @Override
    public boolean parse() {
      return supplier.get().parse();
    }

  }

  public static final class Repetition implements Parser {
    public final Parser child;

    public Repetition(Parser child) {
      this.child = child;
    }

    @Override
    public boolean parse() {
      while (child.parse())
        ;
      return true;
    }
  }

  public final class ComposeObject implements Parser {
    public final Parser child;

    public ComposeObject(Parser child) {
      this.child = child;
    }

    @Override
    public boolean parse() {
      int stack0 = stack.size();
      boolean success = child.parse();
      if (!success) {
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
  }

  public final class ComposeArray implements Parser {
    public final Parser child;

    public ComposeArray(Parser child) {
      this.child = child;
    }

    @Override
    public boolean parse() {
      int stack0 = stack.size();
      boolean success = child.parse();
      if (!success) {
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
  }

  private static final class Choice implements Parser {
    public final Parser[] children;

    private Choice(Parser... children) {
      this.children = children;
    }

    @Override
    public boolean parse() {
      for (Parser child : children) {
        if (child.parse())
          return true;
      }
      return false;
    }
  }

  // parser

  private final Parser pair = new Sequence(
      new StringLitParser(),
      new CharParser(':'),
      new ForwardReference(() -> this.value));

  private final Parser pairTails = new Repetition(
      new Sequence(
          new CharParser(','),
          pair));

  public static final class Optional implements Parser {
    public final Parser child;

    public Optional(Parser child) {
      this.child = child;
    }

    @Override
    public boolean parse() {
      child.parse();
      return true;
    }
  }

  private final Parser pairs = new Optional(
      new Sequence(
          pair,
          pairTails));

  private final Parser object = new ComposeObject(new Sequence(
      new CharParser('{'),
      pairs,
      new CharParser('}')));

  private final Parser valueTails = new Repetition(
      new Sequence(
          new CharParser(','),
          new ForwardReference(() -> this.value)));

  private final Parser values = new Optional(
      new Sequence(
          new ForwardReference(() -> this.value),
          valueTails));

  private final Parser array = new ComposeArray(new Sequence(
      new CharParser('['),
      values,
      new CharParser(']')));

  private final Parser value = new Choice(
      new StringLitParser(),
      new NumberParser(),
      object,
      array);

}