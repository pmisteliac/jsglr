package org.spoofax.jsglr2.inputstack.incremental;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;

public class EagerIncrementalInputStackTest extends AbstractIncrementalInputStackTest {

    @Override protected IIncrementalInputStack getStack(IncrementalParseNode root, String inputString) {
        return new EagerIncrementalInputStack(root, inputString);
    }

}
