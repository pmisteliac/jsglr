package org.spoofax.jsglr2.incremental;

import org.metaborg.parsetable.actions.IGoto;
import org.metaborg.parsetable.query.ActionsForCharacterSeparated;
import org.metaborg.parsetable.query.ActionsPerCharacterClass;
import org.metaborg.parsetable.query.ProductionToGotoForLoop;
import org.metaborg.parsetable.states.State;
import org.spoofax.jsglr2.incremental.lookaheadstack.EagerLookaheadStack;
import org.spoofax.jsglr2.incremental.lookaheadstack.ILookaheadStack;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseStateFactory;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public class IncrementalParseState
//@formatter:off
   <StackNode  extends IStackNode>
//@formatter:on
    extends AbstractParseState<IncrementalParseForest, StackNode> implements IIncrementalParseState {

    private boolean multipleStates = false;
    ILookaheadStack lookahead;

    public static final State NO_STATE = new State(-1,
        new ActionsForCharacterSeparated(new ActionsPerCharacterClass[0]), new ProductionToGotoForLoop(new IGoto[0]));

    public IncrementalParseState(String inputString, String filename, IActiveStacks<StackNode> activeStacks,
        IForActorStacks<StackNode> forActorStacks) {
        super(inputString, filename, activeStacks, forActorStacks);
    }

    @Override public void initParse(IncrementalParseForest updatedTree, String inputString) {
        this.lookahead = new EagerLookaheadStack(updatedTree, inputString); // TODO switch types between Lazy and Eager
        this.currentChar = lookahead.actionQueryCharacter();
    }

    public static
//@formatter:off
   <StackNode_  extends IStackNode,
    ParseState_ extends AbstractParseState<IncrementalParseForest, StackNode_> & IIncrementalParseState>
//@formatter:on
    ParseStateFactory<IncrementalParseForest, StackNode_, ParseState_> factory() {
        return (inputString, filename, activeStacks, forActorStacks) -> (ParseState_) new IncrementalParseState<>(
            inputString, filename, activeStacks, forActorStacks);
    }

    @Override public String actionQueryLookahead(int length) {
        return lookahead.actionQueryLookahead(length);
    }

    @Override public boolean hasNext() {
        return lookahead.get() != null; // null is the lookahead of the EOF node
    }

    @Override public void next() {
        currentOffset += lookahead.get().width();
        lookahead.popLookahead();
        currentChar = lookahead.actionQueryCharacter();
    }

    @Override public ILookaheadStack lookahead() {
        return lookahead;
    }

    @Override public boolean isMultipleStates() {
        return multipleStates;
    }

    @Override public void setMultipleStates(boolean multipleStates) {
        this.multipleStates = multipleStates;
    }
}
