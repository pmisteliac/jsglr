package org.spoofax.jsglr2.parseforest.basic;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.productions.ProductionType;
import org.spoofax.jsglr2.parseforest.ParseForestManagerFactory;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;

public class BasicParseForestManager
//@formatter:off
   <StackNode  extends IStackNode,
    ParseState extends AbstractParseState<IBasicParseForest, StackNode>>
//@formatter:on
    extends
    AbstractBasicParseForestManager<IBasicParseForest, IBasicDerivation<IBasicParseForest>, IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>, StackNode, ParseState> {

    public BasicParseForestManager(
        ParserObserving<IBasicParseForest, IBasicDerivation<IBasicParseForest>, IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>, StackNode, ParseState> observing) {
        super(observing);
    }

    public static
//@formatter:off
   <StackNode_  extends IStackNode,
    ParseState_ extends AbstractParseState<IBasicParseForest, StackNode_>>
//@formatter:on
    ParseForestManagerFactory<IBasicParseForest, IBasicDerivation<IBasicParseForest>, IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>, StackNode_, ParseState_>
        factory() {
        return BasicParseForestManager::new;
    }

    @Override protected IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>
        constructParseNode(IProduction production) {
        return new BasicParseNode<>(production);
    }

    @Override protected IBasicDerivation<IBasicParseForest> constructDerivation(IProduction production,
        ProductionType productionType, IBasicParseForest[] parseForests) {
        return new BasicDerivation<>(production, productionType, parseForests);
    }

    @Override protected IBasicParseForest constructCharacterNode(int character) {
        return new BasicCharacterNode(character);
    }

    @Override public IBasicParseForest[] parseForestsArray(int length) {
        return new IBasicParseForest[length];
    }

}
