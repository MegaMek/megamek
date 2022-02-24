/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.commandline;

import megamek.client.ui.Messages;

/**
 * Very simple skeleton for the command line parser. Provides basic scanner
 * primitives and token types. Descendants should implement at least
 * <code>start</code> function
 */
public abstract class AbstractCommandLineParser {

    private static final String INCORRECT_ARGUMENTS_MESSAGE = Messages.getString("MegaMek.Help.IncorrectArguments");

    /**
     * Exception thrown in case of error
     */
    public static class ParseException extends Exception {
        private static final long serialVersionUID = -3077985683676777509L;

        public ParseException(String message) {
            super(message);
        }
    }

    /**
     * Prefix of the option. Subclasses may overwrite.
     */
    protected String OPTION_PREFIX = "-";

    /**
     * End of input token
     */
    protected static final int TOK_EOF = -1;

    /**
     * Option token
     */
    protected static final int TOK_OPTION = 0;

    /**
     * Literal (any string that does not start with defice (sp?) actually)
     */
    protected static final int TOK_LITERAL = 3;

    /**
     * Parser input
     */
    private String[] args;

    /**
     * The length of the input array
     */
    private int argsLen;

    /**
     * Index of the of the next token to process
     */
    private int position;

    /**
     * Current token
     */
    private int token;

    /**
     * Current arg value
     */
    private String argValue;

    /**
     * Current token value
     */
    private String tokenValue;

    /**
     * Constructs new parser
     * 
     * @param args <code>array</code> of arguments to parse
     */
    public AbstractCommandLineParser(String[] args) {
        assert (args != null) : "args must be non null";
        this.args = args;
        argsLen = args.length;
    }

    /**
     * Main entry point of the parser
     * 
     * @throws ParseException if it fails to parse
     */
    public void parse() throws ParseException {
        nextToken();
        start();
    }

    /**
     * 
     * @return current argument
     */
    protected String getArgValue() {
        return argValue;
    }

    /**
     *
     * @return current token
     */
    protected int getTokenType() {
        return token;
    }

    /**
     * 
     * @param token to set the current token to
     */
    protected void setToken(int token) {
        this.token = token;
    }

    /**
     * 
     * @return <code>String</code> value of the current token
     */
    protected String getTokenValue() {
        return tokenValue;
    }

    /**
     * 
     * @param tokenValue to set the current token to
     */
    protected void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    /**
     * 
     * @return <code>String</code> value of the current token
     */
    protected int getPosition() {
        return position;
    }

    /**
     * Real entry point of parser
     * 
     * @throws ParseException if the parser fails
     */
    protected abstract void start() throws ParseException;

    public abstract String help();

    protected boolean hasNext() {
        return position < argsLen;
    }

    /**
     *
     * @return <code>String</code> error message for user
     */
    public String formatErrorMessage(Exception e) {
        return (INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
                + help());
    }

    /**
     * Reads the next available token.
     */
    protected void nextToken() {
        nextArg();
        if (argValue != null) {
            if (argValue.startsWith(OPTION_PREFIX)) {
                token = TOK_OPTION;
                tokenValue = argValue.substring(OPTION_PREFIX.length());
            } else {
                token = TOK_LITERAL;
                tokenValue = argValue;
            }
        } else {
            tokenValue = null;
            token = TOK_EOF;
        }
    }

    protected void nextArg() {
        if (position < argsLen) {
            argValue = args[position++];
        } else {
            argValue = null;
        }
    }
}
