/*
 * Copyright (c) 2020-2022 Brendan Grieve (bundabrg) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.grieve.bcf.parsers;

import au.com.grieve.bcf.*;
import au.com.grieve.bcf.exceptions.ParserInvalidResultException;
import au.com.grieve.bcf.exceptions.ParserRequiredArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Supports a single argument parser
 */
@Getter
public abstract class SingleParser extends Parser {
  private String input;

  private List<Candidate> completions;

  public SingleParser(CommandManager<?, ?> manager, ArgNode argNode, CommandContext context) {
    super(manager, argNode, context);
  }

  @Override
  public void parse(List<String> input, boolean defaults) throws ParserRequiredArgumentException {
    parsed = true;
    if (input == null || input.size() == 0) {
      // Check if a default is provided or if its not required
      if (!defaults
          || (getParameter("default") == null && getParameter("required", "true").equals("true"))) {
        throw new ParserRequiredArgumentException(this);
      }

      this.input = getParameter("default");
      return;
    }

    this.input = input.remove(0);
  }

  @Override
  public List<Candidate> getCompletions() {
    if (input == null) {
      return new ArrayList<>();
    }

    if (completions == null) {
      completions =
          complete().stream()
              .map(
                  s -> new Candidate(s, s, getParameter("description"), String.valueOf(hashCode())))
              .collect(Collectors.toList());
    }

    return completions;
  }

  @Override
  public Object getResult() throws ParserInvalidResultException {
    if (input == null || input.isEmpty()) {
      throw new ParserInvalidResultException(this, "Invalid command");
    }

    return super.getResult();
  }
}
