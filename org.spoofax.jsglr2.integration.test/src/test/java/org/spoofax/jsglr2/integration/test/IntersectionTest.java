package org.spoofax.jsglr2.integration.test;

public class IntersectionTest extends BaseTestWithSpoofaxCoreSdf3 {

    public IntersectionTest() {
        super("intersection.sdf3");
    }

    /*
     * TODO: implement stack priorities during reducing to fix intersection problem (see P9707 Section 8.4) to make this
     * test pass
     * 
     * @Test public void testOneNotInIntersection() throws ParseError, ParseTableReadException, IOException {
     * testParseFailure("1"); }
     * 
     * @Test public void testTwoInIntersection() throws ParseError, ParseTableReadException, IOException {
     * testParseSuccessByAstString("2", "Two"); }
     * 
     * @Test public void testThreeNotInIntersecton() throws ParseError, ParseTableReadException, IOException {
     * testParseFailure("3"); }
     */

}