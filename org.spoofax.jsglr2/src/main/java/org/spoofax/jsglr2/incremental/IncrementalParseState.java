package org.spoofax.jsglr2.incremental;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseStateFactory;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.ActiveStacksFactory;
import org.spoofax.jsglr2.stack.collections.ForActorStacksFactory;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public class IncrementalParseState<StackNode extends IStackNode>
    extends AbstractParseState<IIncrementalInputStack, StackNode> implements IIncrementalParseState {

    private boolean multipleStates = false;

    public IncrementalParseState(IIncrementalInputStack inputStack, IActiveStacks<StackNode> activeStacks,
        IForActorStacks<StackNode> forActorStacks) {
        super(inputStack, activeStacks, forActorStacks);
    }

    public static
//@formatter:off
   <StackNode_  extends IStackNode,
    InputStack_ extends IIncrementalInputStack,
    ParseState_ extends AbstractParseState<InputStack_, StackNode_> & IIncrementalParseState>
//@formatter:on
    ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, StackNode_, InputStack_, ParseState_>
        factory(ParserVariant variant) {
        return (inputStack, observing) -> {
            IActiveStacks<StackNode_> activeStacks =
                new ActiveStacksFactory(variant.activeStacksRepresentation).get(observing);
            IForActorStacks<StackNode_> forActorStacks =
                new ForActorStacksFactory(variant.forActorStacksRepresentation).get(observing);

            return (ParseState_) new IncrementalParseState<>(inputStack, activeStacks, forActorStacks);
        };
    }

    @Override public boolean newParseNodesAreReusable() {
        return !multipleStates;
    }

    @Override public void setMultipleStates(boolean multipleStates) {
        this.multipleStates = multipleStates;
    }
}
