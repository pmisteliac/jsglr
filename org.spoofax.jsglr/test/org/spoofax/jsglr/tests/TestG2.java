/*
 * Created on 05.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 *
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParserException;

public class TestG2 extends ParseTestCase {

    @Override
	public void gwtSetUp() throws FileNotFoundException, IOException,
            ParserException, InvalidParseTableException {
        super.gwtSetUp("G2", "txt");
    }


    public void testG2_1() throws FileNotFoundException, IOException {
        doParseTest("g2_1");
    }
}
