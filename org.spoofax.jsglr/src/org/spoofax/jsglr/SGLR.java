/*
 * Created on 03.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk@ii.uib.no>
 * 
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import aterm.ATerm;
import aterm.pure.PureFactory;

public class SGLR {

    // FIXME: Should probably be put elsewhere
    private static final int EOF = 256;

    private PureFactory factory;

    private Frame acceptingStack;

    private List<Frame> activeStacks;

    private ParseTable parseTable;

    private Stack<ActionState> forShifter;

    private int currentToken;

    private int tokensSeen;

    private int lineNumber;

    private int columnNumber;

    private Stack<Frame> forActor;

    private Stack<Frame> forActorDelayed;

    private int maxBranches;

    private int maxToken;

    private int maxLine;

    private int maxColumn;

    private int maxTokenNumber;

    private static boolean debugging;

    private boolean logging;

    private AmbiguityManager ambMgr;

    private boolean detectCycles;

    private boolean filter;


    SGLR() {
        basicInit(null);
    }

    public SGLR(InputStream is) throws IOException, FatalException, InvalidParseTableException {
        basicInit(null);
        loadParseTable(is);
    }

    public SGLR(PureFactory pf, InputStream is) throws IOException, FatalException,
            InvalidParseTableException {
        basicInit(pf);
        loadParseTable(is);
    }

    private void basicInit(PureFactory pf) {
        debugging = false;
        detectCycles = true;
        logging = true;
        factory = pf;
        if (factory == null)
            factory = new PureFactory();
        activeStacks = new Vector<Frame>();
        ambMgr = new AmbiguityManager();
    }

    public static boolean isDebugging() {
        return debugging;
    }

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean enableLogging) {
        logging = enableLogging;
    }

    public void setDebug(boolean enableDebug) {
        debugging = enableDebug;
    }

    public void loadParseTable(InputStream r) throws IOException, InvalidParseTableException {
        if(isDebugging()) {
        	Tools.debug("loadParseTable()");
        }
        long start = System.currentTimeMillis();
        ATerm pt = factory.readFromFile(r);

        parseTable = new ParseTable(pt);
        long elapsed = System.currentTimeMillis() - start;
        
        if (isLogging()) {
            Tools.logger("Loading parse table took " + elapsed/1000.0f + "s");
            Tools.logger("No. of states: ", parseTable.getStateCount());
            Tools.logger("No. of productions: ", parseTable.getProductionCount());
            Tools.logger("No. of action entries: ", parseTable.getActionCount());
            Tools.logger("No. of gotos entries: ", parseTable.getGotoCount());

            Tools.logger((parseTable.hasRejects() ? "Includes" : "Excludes"), " rejects");
            Tools.logger((parseTable.hasPriorities() ? "Includes" : "Excludes"), " priorities");
            Tools.logger((parseTable.hasPrefers() ? "Includes" : "Excludes"), " prefer actions");
            Tools.logger((parseTable.hasAvoids() ? "Includes" : "Excludes"), " avoid actions");
        }
    }

    /**
     * Initializes the active stacks. At the start of parsing there is only one
     * active stack, and this stack contains the start symbol obtained from the
     * parse table.
     * 
     * @return the initial stack
     */
    private Frame initActiveStacks() {
        activeStacks.clear();
        Frame st0 = new Frame(parseTable.getInitialState());
        activeStacks.add(st0);
        return st0;
    }

    public ATerm parse(InputStream fis) throws IOException {

        if (isDebugging()) {
            Tools.debug("parse() - ", dumpActiveStacks());
        }

        long start = System.currentTimeMillis();
        
        tokensSeen = 0;
        columnNumber = 0;
        lineNumber = 1;

        acceptingStack = null;
        Frame st0 = initActiveStacks();

        do {
            if (isLogging()) {
                Tools.logger("Current token (#", tokensSeen, "): ", charify(currentToken));
            }

            currentToken = getNextToken(fis);
            parseCharacter();
            shifter();
        } while (currentToken != SGLR.EOF && activeStacks.size() > 0);

        if (isLogging()) {
            Tools.logger("Number of lines: ", lineNumber);
            Tools.logger("Maximum ", maxBranches, " parse branches reached at token ",
                         charify(maxToken), ", line ", maxLine, ", column ", maxColumn,
                         " (token #", maxTokenNumber, ")");
            
            long elapsed = System.currentTimeMillis() - start;
            Tools.logger("Parse time: " + elapsed/1000.0f + "s");
        }

        if(isDebugging()) {
            Tools.debug("Parsing complete: all tokens read");
        }
        
        if (acceptingStack == null)
            return null;

        if(isDebugging()) {
            Tools.debug("Accepting stack exists");
        }
        
        Link s = acceptingStack.findLink(st0);

        if (s != null) {
        	if(isDebugging()) {
        		Tools.debug("internal parse tree:\n", s.label);
        	}
            return parseResult(s.label, null);
        } else {
            Tools.debug("Accepting stack has no link");
            return null;
        }
    }

    private ATerm parseResult(IParseNode root, String sort) {
        
        IParseNode t = root;
        
        if(sort != null) {
             t = selectOnTopSort();
             if(t == null) {
                 return parseError("Desired top sort not found");
             }
        }
        
        if(detectCycles) {
            if(ambMgr.getMaxAmbiguityCount() > 0) {
                if(isCyclicTerm(t))
                    parseError("Term is cyclic");
            }
        }
        
        if(filter) {
            t = filterTree(t);
        }
        
        if(t != null) {
            ATerm r = yieldTree(t);
            int ambCount = ambMgr.getAmbiguityCount();
            return parseTable.getFactory().parse("parsetree(" + r + "," + ambCount + ")");
        }
        
        return null;
    }

    private ATerm yieldTree(IParseNode t) {
        return t.toParseTree(parseTable);
    }

    private IParseNode filterTree(IParseNode t) {
        return t;
    }

    private boolean isCyclicTerm(IParseNode t) {
        // TODO Auto-generated method stub
        return false;
    }

    private ATerm parseError(String msg) {
        System.err.println("Parse error: " + msg);
        return null;
    }

    private IParseNode selectOnTopSort() {
        // TODO Auto-generated method stub
        return null;
    }

    private void shifter() {
        if (isLogging()) {
            Tools.logger("#", tokensSeen, ": shifting ", forShifter.size(), " parser(s) -- token ",
                         charify(currentToken), ", line ", lineNumber, ", column ", columnNumber);
        }

        if (isDebugging()) {
            Tools.debug("shifter() - " + dumpActiveStacks());

            Tools.debug(" token   : " + currentToken);
            Tools.debug(" parsers : " + forShifter.size());
        }
        activeStacks.clear();

        IParseNode prod = parseTable.lookupProduction(currentToken);

        while (forShifter.size() > 0) {
            ActionState as = forShifter.pop();

            State s = as.s;
            Frame st0 = as.st;

            Frame st1 = findStack(activeStacks, s);
            if (st1 != null) {
                st1.addLink(st0, prod, 1);
            } else {
                st1 = new Frame(as.s);
                st1.addLink(st0, prod, 1);
                activeStacks.add(st1);
            }
        }

    }

    private String charify(int currentToken) {
        switch (currentToken) {
        case 32:
            return "\\32";
        case 256:
            return "EOF";
        case '\n':
            return "\\n";
        case 0:
            return "\\0";
        default:
            return "" + (char) currentToken;
        }
    }

    private void parseCharacter() {

        if (isDebugging()) {
            Tools.debug("parseCharacter() - " + dumpActiveStacks());
            Tools.debug(" # active stacks : " + activeStacks.size());
        }

        forActor = computeStackOfStacks(activeStacks);

        if (isDebugging()) {
            Tools.debug(" # for actor     : " + forActor.size());
        }

        forActorDelayed = new Stack<Frame>();
        forShifter = new Stack<ActionState>();

        while (forActor.size() > 0 || forActorDelayed.size() > 0) {
            if (forActor.size() == 0) {
                forActor.add(forActorDelayed.pop());
            }
            if (forActor.size() > 0) {
                Frame st = forActor.pop();
                if (!st.allLinksRejected()) {
                    actor(st);
                }

            }
        }
    }

    private void actor(Frame st) {

        if (isDebugging()) {
            Tools.debug("actor() - ", dumpActiveStacks());
        }

        State s = st.peek();

        if (isDebugging()) {
            Tools.debug(" state   : ", s.stateNumber);
            Tools.debug(" token   : ", currentToken);
        }

        List<ActionItem> actionItems = s.getActionItems(currentToken);

        if (isDebugging()) {
            Tools.debug(" actions : ", actionItems);
        }

        for (ActionItem ai : actionItems) {
            if (ai instanceof Shift) {
                Shift sh = (Shift) ai;
                forShifter.push(new ActionState(st, parseTable.getState(sh.nextState)));
                statsRecordParsers();
            } else if (ai instanceof Reduce) {
                Reduce red = (Reduce) ai;
                doReductions(st, red.production);
            } else if (ai instanceof Accept) {
                acceptingStack = st;
                if (isLogging()) {
                    Tools.logger("Reached the accepting state");
                }
            }
        }
    }

    private void statsRecordParsers() {
        if (forShifter.size() > maxBranches) {
            maxBranches = forShifter.size();
            maxToken = currentToken;
            maxColumn = columnNumber;
            maxLine = lineNumber;
            maxTokenNumber = tokensSeen;
        }
    }

    private void doReductions(Frame st, Production prod) {

        if (isDebugging()) {
            Tools.debug("doReductions() - " + dumpActiveStacks());

            Tools.debug(" state : " + st.peek().stateNumber);
            Tools.debug(" token : " + currentToken);
            Tools.debug(" label : " + prod.label);
            Tools.debug(" arity : " + prod.arity);
            Tools.debug(" stack : " + st.dumpStack());
        }

        List<Path> paths = st.computePathsToRoot(prod.arity);

        if (isDebugging()) {
            Tools.debug(" paths : " + paths.size());
        }

        for (Path path : paths) {

            List<IParseNode> kids = path.getATerms();

            if (isDebugging()) {
                Tools.debug(path);
                Tools.debug(kids);
            }

            Frame st0 = path.getEnd();

            if (isDebugging()) {
                Tools.debug(st0.state);
            }

            State next = parseTable.go(st0.peek(), prod.label);

            if (isLogging()) {
                Tools.logger("Goto(", st0.peek().stateNumber, ",", prod.label + ") == ",
                             next.stateNumber);
            }

            reducer(st0, next, prod, kids, path.getLength());
        }

        activeStacks.remove(st);

        if (isDebugging()) {
            Tools.debug("<doReductions() - " + dumpActiveStacks());
        }

    }

    private void reducer(Frame st0, State s, Production prod, List<IParseNode> kids, int length) {

        if (isLogging()) {
            Tools.logger("Reducing; state ", s.stateNumber, ", token: ", charify(currentToken),
                         ", production: ", prod.label);
        }

        if (isDebugging()) {
            Tools.debug("reducer() - ", dumpActiveStacks());

            Tools.debug(" state      : ", s.stateNumber);
            Tools.debug(" token      : ", charify(currentToken) + " (" + currentToken + ")");
            Tools.debug(" production : ", prod.label);
        }

        Node t = prod.apply(kids);

        Frame st1 = findStack(activeStacks, s);
        if (st1 == null) {
            /* Found no existing stack with for state s; make new stack */
            st1 = new Frame(s);
            Link nl = st1.addLink(st0, t, length);
            activeStacks.add(st1);
            if (st1.peek().rejectable()) {
                forActorDelayed.push(st1);
            } else {
                forActor.add(st1);
            }

            if (prod.status == Production.REJECT) {
                if (isLogging()) {
                    Tools.logger("Reject [new]");
                }
                nl.reject();
            }
        } else {
            /* A stack with state s exists; check for ambiguities */
            Link nl = st1.findLink(st0);

            if (nl != null) {
                if (isLogging()) {
                    Tools.logger("Ambiguity: direct link ", st0.state.stateNumber, " -> ",
                                 st1.state.stateNumber, " ", (prod.isReject() ? "{reject}" : ""));
                }
                
                ambMgr.createAmbiguityCluster(nl.label, t, tokensSeen - nl.getLength() - 1);

                if (prod.isReject()) {
                    nl.reject();
                }

            } else {
                nl = st1.addLink(st0, t, length);
                if (isDebugging()) {
                    Tools.debug(" added link ", nl, " from ", st1.state.stateNumber, " to ",
                                st0.state.stateNumber);
                }

                if (prod.isReject())
                    nl.reject();

                // FIXME activeStacks can be modified inside doLimitedReductions; what should we do?
                for (int i = 0; i< activeStacks.size(); i++) { // Frame st2 : activeStacks) {
                    Frame st2 = activeStacks.get(i);
                    if (st2.allLinksRejected())
                        continue;
                    if (forActor.contains(st2))
                        continue;
                    if (forActorDelayed.contains(st2))
                        continue;

                    for (ActionItem ai : st2.peek().getActionItems(currentToken)) {
                        if (ai instanceof Reduce) {
                            Reduce red = (Reduce) ai;
                            doLimitedReductions(st2, red.production, nl);
                        }
                    }

                }
            }
        }
    }

    private Frame findStack(List<Frame> stacks, State s) {
        // We need only check the top frames of the active stacks.
        if (isDebugging()) {
            Tools.debug("findStack() - ", dumpActiveStacks());
            Tools.debug(" looking for ", s.stateNumber);
        }
        for (Frame st : stacks)
            if (st.state.stateNumber == s.stateNumber) {
                return st;
            }
        return null;
    }

    private void doLimitedReductions(Frame st, Production prod, Link l) {
        if (isDebugging()) {
            Tools.debug("doLimitedReductions() - ", dumpActiveStacks());

            Tools.debug(" state : ", st.peek().stateNumber);
            Tools.debug(" token : ", currentToken);
            Tools.debug(" label : ", prod.label);
            Tools.debug(" arity : ", prod.arity);
            Tools.debug(" stack : ", st.dumpStack());
        }

        List<Path> paths = st.computePathsToRoot(prod.arity, l);

        if (isDebugging()) {
            Tools.debug(paths);
        }

        for (Path path : paths) {
            List<IParseNode> kids = path.getATerms();

            if (isDebugging()) {
                Tools.debug(path);
            }

            Frame st0 = path.getEnd();

            if (isDebugging()) {
                Tools.debug(st0.state);
            }
            State next = parseTable.go(st0.peek(), prod.label);

            if (isLogging()) {
                Tools.logger("Goto(", st0.peek().stateNumber, ",", prod.label, ") == ",
                             next.stateNumber);
            }

            reducer(st0, next, prod, kids, path.getLength());
        }
    }

    private Stack<Frame> computeStackOfStacks(List<Frame> st) {
        Stack<Frame> ret = new Stack<Frame>();
        for (Frame s : st)
            ret.push(s);
        return ret;
    }

    private int getNextToken(InputStream fis) throws IOException {
        int t = fis.read();

        tokensSeen++;
        columnNumber++;
        if (t == '\n') {
            lineNumber++;
            columnNumber = 0;
        }

        if (isDebugging()) {
            Tools.debug("getNextToken() - ", t);
        }

        return t == -1 ? SGLR.EOF : t;
    }

    private String dumpActiveStacks() {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        if (activeStacks == null) {
            sb.append(" GSS unitialized");
        } else {
            sb.append("{" + activeStacks.size() + "} ");
            for (Frame f : activeStacks) {
                if (!first)
                    sb.append(", ");
                sb.append(f.dumpStack());
                first = false;
            }
        }
        return sb.toString();
    }

    public void setCycleDetect(boolean detectCycles) {
        this.detectCycles = detectCycles;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }
}
