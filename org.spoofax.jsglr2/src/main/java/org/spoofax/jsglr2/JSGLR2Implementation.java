package org.spoofax.jsglr2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.messages.IMessage;
import org.spoofax.jsglr2.imploder.IImplodeResult;
import org.spoofax.jsglr2.imploder.IImploder;
import org.spoofax.jsglr2.imploder.ITokenizer;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.tokens.Tokens;

public class JSGLR2Implementation<ParseForest extends IParseForest, IntermediateResult, AbstractSyntaxTree, ImplodeResult extends IImplodeResult<IntermediateResult, AbstractSyntaxTree>>
    implements JSGLR2<AbstractSyntaxTree> {

    public final IParser<ParseForest> parser;
    public final IImploder<ParseForest, IntermediateResult, AbstractSyntaxTree, ImplodeResult> imploder;
    public final ITokenizer<ImplodeResult> tokenizer;

    JSGLR2Implementation(IParser<ParseForest> parser,
        IImploder<ParseForest, IntermediateResult, AbstractSyntaxTree, ImplodeResult> imploder,
        ITokenizer<ImplodeResult> tokenizer) {
        this.parser = parser;
        this.imploder = imploder;
        this.tokenizer = tokenizer;
    }

    @Override public JSGLR2Result<AbstractSyntaxTree> parseResult(String input, @Nullable FileObject resource,
        String startSymbol) {
        ParseResult<ParseForest> parseResult = parser.parse(input, resource, startSymbol);

        if(parseResult.isSuccess()) {
            ParseSuccess<ParseForest> success = (ParseSuccess<ParseForest>) parseResult;

            ImplodeResult implodeResult = imploder.implode(input, resource, success.parseResult);

            Tokens tokens = tokenizer.tokenize(input, resource, implodeResult);

            List<IMessage> messages = new ArrayList<>();

            messages.addAll(parseResult.messages);
            messages.addAll(implodeResult.messages());

            return new JSGLR2Success<>(implodeResult.ast(), tokens, messages);
        } else {
            ParseFailure<ParseForest> failure = (ParseFailure<ParseForest>) parseResult;

            return new JSGLR2Failure<>(failure, parseResult.messages);
        }
    }

}
