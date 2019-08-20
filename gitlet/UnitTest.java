package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    @Test
    public void basicAddTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        File stagedF = new File(Main.STAGED, "f.txt");
        File stagedG = new File(Main.STAGED, "g.txt");
        assertTrue(stagedF.exists() && stagedG.exists());
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void basicRmTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        File stagedF = new File(Main.STAGED, "f.txt");
        File stagedG = new File(Main.STAGED, "g.txt");
        assertTrue(stagedF.exists() && stagedG.exists());
        Main.doCommit("commit", "Two files");
        assertTrue(!stagedF.exists() && !stagedG.exists());
        Main.doRemove("rm", "f.txt");
        assertFalse(f.exists());
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void basicFindTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doFind("find", "sucka");
        Main.doFind("find", "Two files");
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void basicBranchTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doBranch("branch", "other");
        File other = new File(Main.BRANCHES, "other");
        assertTrue(other.exists());
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void basicRemoveBranchTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doBranch("branch", "other");
        File other = new File(Main.BRANCHES, "other");
        assertTrue(other.exists());
        Main.doRemoveBranch("rm-branch", "other");
        assertFalse(other.exists());
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void alreadyAdded() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doAdd("add", "f.txt");
        File stagedF = new File(Main.STAGED, "f.txt");
        assertFalse(stagedF.exists());
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || Main.GITLET.exists());
    }

    @Test
    public void checkoutAndMergeTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doBranch("branch", "other");
        File other = new File(Main.BRANCHES, "other");
        assertTrue(other.exists());
        File h = new File("h.txt");
        Utils.writeContents(h, "This is a wug too!");
        Main.doAdd("add", "h.txt");
        Main.doRemove("rm", "g.txt");
        Utils.writeContents(f, "This is a wug too!");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Another one");
        Main.doCheckout("checkout", "other");
        Utils.writeContents(f, "This is not a wug.");
        Main.doAdd("add", "f.txt");
        File k = new File("k.txt");
        Utils.writeContents(k, "This is a wug three!");
        Main.doAdd("add", "k.txt");
        Main.doCommit("commit", "A third one");
        Main.doCheckout("checkout", "master");
        String masterHead = Main.headSHA1();
        Main.doMerge("merge", "other");
        assertFalse(g.exists());
        String hContents = Utils.readContentsAsString(h);
        assertTrue(hContents.equals("This is a wug too!"));
        String kContents = Utils.readContentsAsString(k);
        assertTrue(kContents.equals("This is a wug three!"));
        String fContents = Utils.readContentsAsString(f);
        String fCheck = "<<<<<<< HEAD\n"
                + "This is a wug too!=======\n" + "This is not a wug.>>>>>>>\n";
        assertTrue(fContents.equals(fCheck));
        String headParent = Main.headCommit().getParent();
        assertTrue(masterHead.equals(headParent));
        File masterBranch = new File(Main.BRANCHES, "master");
        File otherBranch = new File(Main.BRANCHES, "other");
        assertTrue(masterBranch.exists() && otherBranch.exists());
        String currentHead = Utils.readContentsAsString(Main.HEAD);
        assertTrue(currentHead.equals("master"));
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.restrictedDelete(h);
        Utils.restrictedDelete(k);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || h.exists()
                || k.exists() || Main.GITLET.exists());
    }

    @Test
    public void secondCheckoutAndMergeTest() {
        Main.doInit("init");
        File f = new File("f.txt");
        Utils.writeContents(f, "This is a wug.");
        File g = new File("g.txt");
        Utils.writeContents(g, "This is not a wug.");
        Main.doAdd("add", "g.txt");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Two files");
        Main.doBranch("branch", "other");
        File other = new File(Main.BRANCHES, "other");
        assertTrue(other.exists());
        File h = new File("h.txt");
        Utils.writeContents(h, "This is a wug too!");
        Main.doAdd("add", "h.txt");
        Main.doRemove("rm", "g.txt");
        Utils.writeContents(f, "This is a wug too!");
        Main.doAdd("add", "f.txt");
        Main.doCommit("commit", "Another one");
        Main.doCheckout("checkout", "other");
        Main.doRemove("rm", "f.txt");
        File k = new File("k.txt");
        Utils.writeContents(k, "This is a wug three!");
        Main.doAdd("add", "k.txt");
        Main.doCommit("commit", "Another one");
        Main.doCheckout("checkout", "master");
        String masterHead = Main.headSHA1();
        Main.doMerge("merge", "other");
        assertFalse(g.exists());
        String hContents = Utils.readContentsAsString(h);
        assertTrue(hContents.equals("This is a wug too!"));
        String kContents = Utils.readContentsAsString(k);
        assertTrue(kContents.equals("This is a wug three!"));
        String fContents = Utils.readContentsAsString(f);
        String fCheck = "<<<<<<< HEAD\n"
                + "This is a wug too!=======\n" + ">>>>>>>\n";
        assertTrue(fContents.equals(fCheck));
        String headParent = Main.headCommit().getParent();
        assertTrue(masterHead.equals(headParent));
        Utils.restrictedDelete(f);
        Utils.restrictedDelete(g);
        Utils.restrictedDelete(h);
        Utils.restrictedDelete(k);
        Utils.recursiveDelete(Main.GITLET);
        assertFalse(f.exists() || g.exists() || h.exists()
                || k.exists() || Main.GITLET.exists());
    }
}


