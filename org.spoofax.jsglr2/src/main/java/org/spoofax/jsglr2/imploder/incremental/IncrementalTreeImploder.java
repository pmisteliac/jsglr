package org.spoofax.jsglr2.imploder.incremental;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.spoofax.jsglr2.imploder.ImplodeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.imploder.input.IImplodeInputFactory;
import org.spoofax.jsglr2.imploder.treefactory.ITreeFactory;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;

public abstract class IncrementalTreeImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>,
    Tree,
    Input       extends IncrementalImplodeInput<ParseNode, Tree>>
//@formatter:on
    extends TreeImploder<ParseForest, ParseNode, Derivation, Tree, Input> {

    private final IIncrementalImplodeInputFactory<ParseNode, Tree, Input> incrementalInputFactory;
    private final TreeImploder<ParseForest, ParseNode, Derivation, Tree, Input> regularImplode;
    private final Map<String, WeakHashMap<ParseNode, SubTree<Tree>>> cache = new HashMap<>();

    public IncrementalTreeImploder(IImplodeInputFactory<Input> inputFactory,
        IIncrementalImplodeInputFactory<ParseNode, Tree, Input> incrementalInputFactory,
        ITreeFactory<Tree> treeFactory) {
        super(inputFactory, treeFactory);
        this.regularImplode = new TreeImploder<>(inputFactory, treeFactory);
        this.incrementalInputFactory = incrementalInputFactory;
    }

    @Override public ImplodeResult<SubTree<Tree>, Tree> implode(String inputString, @Nullable FileObject resource,
        ParseForest parseForest) {
        String filename = resource != null ? resource.getName().getURI() : "";

        // TODO: maybe do caching on resource, not filename?

        if(filename.equals("")) {
            return regularImplode.implode(inputString, resource, parseForest);
        }

        @SuppressWarnings("unchecked") ParseNode topParseNode = (ParseNode) parseForest;

        if(!cache.containsKey(filename)) {
            cache.put(filename, new WeakHashMap<>());
        }

        SubTree<Tree> result =
            implodeParseNode(incrementalInputFactory.get(inputString, cache.get(filename)), topParseNode, 0);

        return new ImplodeResult<>(resource, result, result.tree, Collections.emptyList());
    }

    public void clearCache() {
        cache.clear();
    }

    // TODO it is very ugly to have these methods here. It's only used in benchmarking, but it should not exist in the
    // regular implementation
    public WeakHashMap<ParseNode, SubTree<Tree>> getFromCache(String filename) {
        return cache.get(filename);
    }

    public void addToCache(String filename, WeakHashMap<ParseNode, SubTree<Tree>> map) {
        cache.put(filename, map);
    }

    @Override protected SubTree<Tree> implodeParseNode(Input input, ParseNode parseNode, int startOffset) {
        if(input.resultCache.containsKey(parseNode))
            return input.resultCache.get(parseNode);

        SubTree<Tree> result = super.implodeParseNode(input, parseNode, startOffset);
        input.resultCache.put(parseNode, result);
        return result;
    }

}
