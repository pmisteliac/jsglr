package org.spoofax.jsglr2.parseforest;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.productions.ProductionType;
import org.spoofax.jsglr2.parser.AbstractParseState;

import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;

import java.util.ArrayList;
import java.util.List;

public abstract class ParseForestManager
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<ParseForest, StackNode>>
//@formatter:on
{

    protected final ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing;

    protected ParseForestManager(ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        this.observing = observing;
    }

    /**
     * @param stack
     *            The parse node will be added to the link _to_ this stack node.
     */
    abstract public ParseNode createParseNode(ParseState parseState, IStackNode stack, IProduction production,
        Derivation firstDerivation);

    /**
     * @param stack
     *            The derivation will be added to the parse node on the link _to_ this stack node.
     */
    abstract public Derivation createDerivation(ParseState parseState, IStackNode stack, IProduction production,
        ProductionType productionType, ParseForest[] parseForests);

    abstract public void addDerivation(ParseState parseState, ParseNode parseNode, Derivation derivation);

    abstract public ParseForest createCharacterNode(ParseState parseState);

    abstract public ParseForest[] parseForestsArray(int length);

    public ParseForest filterStartSymbol(ParseForest parseForest, String startSymbol, ParseState parseState) {
        ParseNode topNode = (ParseNode) parseForest;
        List<Derivation> derivationsWithStartSymbol = new ArrayList<>();

        for(Derivation derivation : topNode.getDerivations()) {
            String derivationStartSymbol = derivation.production().startSymbolSort();

            if(derivationStartSymbol != null && derivationStartSymbol.equals(startSymbol))
                derivationsWithStartSymbol.add(derivation);
        }

        if(derivationsWithStartSymbol.isEmpty())
            return null;
        else
            return (ParseForest) filteredTopParseNode(topNode, derivationsWithStartSymbol);
    }

    abstract protected ParseNode filteredTopParseNode(ParseNode parseNode, List<Derivation> derivations);

}
