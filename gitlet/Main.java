package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Ed Scerbo
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            switch (args[0]) {
            case "init":
                doInit(args);
                break;
            case "add":
                doAdd(args);
                break;
            case "commit":
                doCommit(args);
                break;
            case "rm":
                doRemove(args);
                break;
            case "log":
                doLog(args);
                break;
            case "global-log":
                doGlobalLog(args);
                break;
            case "find":
                doFind(args);
                break;
            case "status":
                doStatus(args);
                break;
            case "checkout":
                doCheckout(args);
                break;
            case "branch":
                doBranch(args);
                break;
            case "rm-branch":
                doRemoveBranch(args);
                break;
            case "reset":
                doReset(args);
                break;
            case "merge":
                doMerge(args);
                break;
            default:
                System.out.println("No command with that name exists.");
            }
        }
    }

    /** Does the init command, where ARGS is input by user. */
    public static void doInit(String... args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
        } else {
            if (GITLET.exists()) {
                System.out.println("A Gitlet version-control "
                        + "system already exists in the current directory.");
            } else {
                GITLET.mkdir();
                BLOBS.mkdir();
                COMMITS.mkdir();
                BRANCHES.mkdir();
                STAGED.mkdir();
                REMOVED.mkdir();
                Commit initCommit = Commit.INITIAL_COMMIT;
                writeCommit(initCommit);
                String sha1 = initCommit.getSha1();
                File masterFile = new File(BRANCHES, "master");
                Utils.writeContents(masterFile, sha1);
                Utils.writeContents(HEAD, "master");
            }
        }
    }

    /** Does the add command, where ARGS is input by user. */
    public static void doAdd(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String fileName = args[1];
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("File does not exist.");
            } else {
                HashMap<String, String> trackedMap = headCommit().getTracked();
                String fileSHA1 = Utils.sha1(Utils.readContents(file));
                File stagedFile = new File(STAGED, fileName);
                if (trackedMap.keySet().contains(fileName)
                        && trackedMap.get(fileName).equals(fileSHA1)) {
                    File removedFile = new File(REMOVED, fileName);
                    stagedFile.delete();
                    removedFile.delete();
                } else {
                    String contents = Utils.readContentsAsString(file);
                    Utils.writeContents(stagedFile, contents);
                }
            }
        }
    }

    /** Does the commit command, where ARGS is input by user. */
    public static void doCommit(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            commitHelper(args[1], null);
        }
    }

    /** Helper function used by doCommit and doMerge. Implements
     *  the bulk of the commit command. Makes new commit with MESSAGE
     *  and MERGEPARENT.*/
    public static void commitHelper(String message, String mergeParent) {
        HashSet<String> stagedFiles, removedFiles;
        stagedFiles = new HashSet<>(Arrays.asList(STAGED.list()));
        removedFiles = new HashSet<>(Arrays.asList(REMOVED.list()));

        if (stagedFiles.isEmpty() && removedFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else if (message.equals("")) {
            System.out.println("Please enter a commit message.");
        } else {
            Commit headCommit = headCommit();
            SimpleDateFormat date;
            date = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            String timestamp = date.format(new Date());
            HashMap<String, String> blobMap = new HashMap<>();
            String parent = headCommit.getSha1();

            HashMap<String, String> trackedMap = headCommit.getTracked();
            for (String fileName : trackedMap.keySet()) {
                if (!stagedFiles.contains(fileName)
                        && !removedFiles.contains(fileName)) {
                    blobMap.put(fileName, trackedMap.get(fileName));
                }
            }
            for (String fileName : stagedFiles) {
                File file = new File(STAGED, fileName);
                String fileSHA1 = Utils.sha1(Utils.readContents(file));
                blobMap.put(fileName, fileSHA1);
                writeBlob(file);
            }

            Commit com;
            com = new Commit(timestamp, message, blobMap, parent, mergeParent);
            writeCommit(com);
            String branch = Utils.readContentsAsString(HEAD);
            File branchFile = new File(BRANCHES, branch);

            Utils.writeContents(branchFile, com.getSha1());

            for (File file : STAGED.listFiles()) {
                file.delete();
            }
            for (File file : REMOVED.listFiles()) {
                file.delete();
            }
        }
    }

    /** Does the remove command, where ARGS is input by user. */
    public static void doRemove(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String fileName = args[1];
            File workingDirFile = new File(fileName);
            File stagedFile = new File(STAGED, fileName);
            boolean isStaged = stagedFile.delete();
            Set<String> tracked = headCommit().getTracked().keySet();
            boolean isTracked = tracked.contains(fileName);
            if (isTracked) {
                File removedFile = new File(REMOVED, fileName);
                try {
                    removedFile.createNewFile();
                } catch (IOException ioEx) {
                    System.out.println(fileName + " not marked for removal.");
                }
                Utils.restrictedDelete(workingDirFile);
            }
            if (!isStaged && !isTracked) {
                System.out.println("No reason to remove the file.");
            }
        }
    }

    /** Does the log command, where ARGS is input by user. */
    public static void doLog(String... args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            commitHistory(headCommit());
        }
    }

    /** Does the global-log command, where ARGS is input by user. */
    public static void doGlobalLog(String... args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            for (File directory : COMMITS.listFiles()) {
                for (File commitFile : directory.listFiles()) {
                    Commit commit = Utils.readObject(commitFile, Commit.class);
                    commitLog(commit);
                }
            }
        }
    }

    /** Does the find command, where ARGS is input by user. */
    public static void doFind(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            boolean foundCommit = false;
            for (File directory : COMMITS.listFiles()) {
                for (File commitFile : directory.listFiles()) {
                    Commit commit = Utils.readObject(commitFile, Commit.class);
                    if (commit.getMessage().equals(args[1])) {
                        System.out.println(commit.getSha1());
                        foundCommit = true;
                    }
                }
            }
            if (!foundCommit) {
                System.out.println("Found no commit with that message.");
            }
        }
    }

    /** Does the status command, where ARGS is input by user. */
    public static void doStatus(String... args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String[] branchList = BRANCHES.list();
            String[] stagedList = STAGED.list();
            String[] removedList = REMOVED.list();
            String[] modifiedList = new String[modifiedButNotStaged().size()];
            modifiedButNotStaged().toArray(modifiedList);
            String[] untrackedList = new String[untrackedFiles().size()];
            untrackedFiles().toArray(untrackedList);
            Arrays.sort(branchList);
            Arrays.sort(stagedList);
            Arrays.sort(removedList);
            Arrays.sort(modifiedList);
            Arrays.sort(untrackedList);

            String currentBranch = Utils.readContentsAsString(HEAD);
            System.out.println("=== Branches ===");
            for (int i = 0; i < branchList.length; i += 1) {
                if (branchList[i].equals(currentBranch)) {
                    System.out.print("*");
                }
                System.out.println(branchList[i]);
            }
            System.out.println("");

            System.out.println("=== Staged Files ===");
            for (int i = 0; i < stagedList.length; i += 1) {
                System.out.println(stagedList[i]);
            }
            System.out.println("");

            System.out.println("=== Removed Files ===");
            for (int i = 0; i < removedList.length; i += 1) {
                System.out.println(removedList[i]);
            }
            System.out.println("");

            System.out.println("=== Modifications Not Staged For Commit ===");
            for (int i = 0; i < modifiedList.length; i += 1) {
                System.out.println(modifiedList[i]);
            }
            System.out.println("");

            System.out.println("=== Untracked Files ===");
            for (int i = 0; i < untrackedList.length; i += 1) {
                System.out.println(untrackedList[i]);
            }
            System.out.println("");
        }
    }

    /** Does the checkout command, where ARGS is input by user. */
    public static void doCheckout(String... args) {
        if (args.length == 3 && args[1].equals("--")) {
            if (!GITLET.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
            } else {
                doCheckout1(args[2]);
            }
        } else if (args.length == 4 && args[2].equals("--")) {
            if (!GITLET.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
            } else {
                doCheckout2(args[1], args[3]);
            }
        } else if (args.length == 2) {
            if (!GITLET.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
            } else {
                doCheckout3(args[1]);
            }
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Implements the version of checkout where the args
     *  are of the form checkout -- FILENAME. */
    public static void doCheckout1(String fileName) {
        checkout2Helper(headSHA1(), fileName);
    }

    /** Implements the version of checkout where the args
     *  are of the form checkout SHA1PREFIX -- FILENAME. */
    public static void doCheckout2(String sha1Prefix, String fileName) {
        if (sha1Prefix.length() == 8 * 5) {
            checkout2Helper(sha1Prefix, fileName);
        } else {
            String sha1 = sha1(sha1Prefix);
            if (sha1 != null) {
                checkout2Helper(sha1, fileName);
            }
        }
    }

    /** Helper function called by doCheckout2 after computing
     *  the full SHA1 from the sha1Prefix. Checks out
     *  FILENAME from the commit with given SHA1. */
    public static void checkout2Helper(String sha1, String fileName) {
        File prefixDir = new File(COMMITS, sha1.substring(0, 2));
        File serializedCommit = new File(prefixDir, sha1.substring(2));
        if (!serializedCommit.exists()) {
            System.out.println("No commit with that id exists.");
        } else {
            Commit commit = retrieveCommit(sha1);
            HashMap<String, String> trackedMap = commit.getTracked();
            if (!trackedMap.keySet().contains(fileName)) {
                System.out.println("File does not exist in that commit.");
            } else {
                String blobSHA1 = trackedMap.get(fileName);
                File file = new File(fileName);
                File blobPrefixDir = new File(BLOBS, blobSHA1.substring(0, 2));
                File blobFile = new File(blobPrefixDir, blobSHA1.substring(2));
                Utils.writeContents(file, Utils.readContentsAsString(blobFile));
            }
        }

    }

    /** Implements the version of checkout where the args
     *  are of the form checkout BRANCH. */
    public static void doCheckout3(String branch) {
        File branchFile = new File(BRANCHES, branch);
        String currentBranch = Utils.readContentsAsString(HEAD);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
        } else if (branch.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            String sha1 = Utils.readContentsAsString(branchFile);
            checkout3ResetHelper(sha1);
            Utils.writeContents(HEAD, branch);
        }
    }

    /** Does the branch command, where ARGS is input by user. */
    public static void doBranch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String newBranch = args[1];
            File newBranchFile = new File(BRANCHES, newBranch);
            if (newBranchFile.exists()) {
                System.out.println("A branch with that name already exists.");
            } else {
                Utils.writeContents(newBranchFile, headSHA1());
            }
        }
    }

    /** Does the rm-branch command, where ARGS is input by user. */
    public static void doRemoveBranch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String branch = args[1];
            File branchFile = new File(BRANCHES, branch);
            if (!branchFile.exists()) {
                System.out.println("A branch with that name does not exist.");
            } else if (branch.equals(Utils.readContentsAsString(HEAD))) {
                System.out.println("Cannot remove the current branch.");
            } else {
                branchFile.delete();
            }
        }
    }

    /** Does the reset command, where ARGS is input by user. */
    public static void doReset(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            String sha1Prefix = args[1];
            if (sha1Prefix.length() == 8 * 5) {
                resetHelper(sha1Prefix);
            } else {
                String sha1 = sha1(sha1Prefix);
                if (sha1 != null) {
                    resetHelper(sha1);
                }
            }
        }
    }

    /** Helper function called by doReset after computing
     *  the full SHA1 from the sha1Prefix. Resets the working
     *  directory to the commit with given SHA1. */
    public static void resetHelper(String sha1) {
        File prefixDir = new File(COMMITS, sha1.substring(0, 2));
        File serializedCommit = new File(prefixDir, sha1.substring(2));
        if (!serializedCommit.exists()) {
            System.out.println("No commit with that id exists.");
        } else {
            checkout3ResetHelper(sha1);
            String currentBranch = Utils.readContentsAsString(HEAD);
            File currentBranchFile = new File(BRANCHES, currentBranch);
            Utils.writeContents(currentBranchFile, sha1);
        }
    }

    /** Helper function called by both resetHelper and doCheckout3
     *  to do what they both have in common: Checks out
     *  FILENAME from the commit with given SHA1. */
    public static void checkout3ResetHelper(String sha1) {
        Commit currentCommit = headCommit();
        Set<String> currentTracked = currentCommit.getTracked().keySet();
        Commit otherCommit = retrieveCommit(sha1);
        Set<String> otherTracked = otherCommit.getTracked().keySet();
        for (String fileName : new File(".").list()) {
            if ((!currentTracked.contains(fileName)
                    && otherTracked.contains(fileName))
                    || (currentTracked.contains(fileName)
                    && !otherTracked.contains(fileName)
                    && new File(REMOVED, fileName).exists())) {
                String error = "There is an untracked file "
                        + "in the way; delete it or add it first.";
                System.out.println(error);
                return;
            }
        }

        for (String fileName : otherTracked) {
            checkout2Helper(sha1, fileName);
        }
        for (String fileName : currentTracked) {
            if ((!otherTracked.contains(fileName))) {
                Utils.restrictedDelete(fileName);
            }
        }

        for (File file : STAGED.listFiles()) {
            file.delete();
        }
        for (File file : REMOVED.listFiles()) {
            file.delete();
        }
    }

    /** Does the merge command, where ARGS is input by user. */
    public static void doMerge(String... args) {
        HashSet<String> stagedFiles;
        stagedFiles = new HashSet<>(Arrays.asList(STAGED.list()));
        HashSet<String> removedFiles;
        removedFiles = new HashSet<>(Arrays.asList(REMOVED.list()));
        String mergeBranch = args[1];
        File mergeBranchFile = new File(BRANCHES, mergeBranch);
        String currentBranch = Utils.readContentsAsString(HEAD);

        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (!stagedFiles.isEmpty() || !removedFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
        } else if (!mergeBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (mergeBranch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
        } else if (unsafeToMerge(mergeBranch)) {
            String error = "There is an untracked file "
                    + "in the way; delete it or add it first.";
            System.out.println(error);
        } else {
            String splitSha1 = splitPoint(mergeBranch);
            String mergeSha1 = Utils.readContentsAsString(mergeBranchFile);
            String currentSha1 = headSHA1();
            if (splitSha1.equals(mergeSha1)) {
                String error = "Given branch is an "
                        + "ancestor of the current branch.";
                System.out.println(error);
            } else if (splitSha1.equals(currentSha1)) {
                File file = new File(BRANCHES, currentBranch);
                Utils.writeContents(file, mergeSha1);
                System.out.println("Current branch fast-forwarded.");
            } else {
                HashMap<String, String> splitTracked
                        = retrieveCommit(splitSha1).getTracked();
                HashMap<String, String> mergeTracked
                        = retrieveCommit(mergeSha1).getTracked();
                HashMap<String, String> currentTracked
                        = retrieveCommit(currentSha1).getTracked();

                HashSet<String> fileNames = new HashSet<>();
                fileNames.addAll(splitTracked.keySet());
                fileNames.addAll(mergeTracked.keySet());
                fileNames.addAll(currentTracked.keySet());

                boolean inConflict = mergeHelper(splitTracked,
                        currentTracked, mergeTracked, mergeSha1);

                String message = "Merged " + mergeBranch
                        + " into " + currentBranch + ".";
                commitHelper(message, mergeSha1);
                if (inConflict) {
                    System.out.println("Encountered a merge conflict.");
                }
            }
        }
    }

    /** Nonsense helper function for doMerge solely to meet the
     *  completely arbitrary style restriction of 60 lines per
     *  method. The arguments to this method are SPLITTRACKED,
     *  CURRENTTRACKED, MERGETRACKED, AND MERGESHA1. Returns true
     *  if and only if ongoing merge will cause a conflict. Thank
     *  you and good night.*/
    public static boolean mergeHelper(HashMap<String, String> splitTracked,
                                   HashMap<String, String> currentTracked,
                                   HashMap<String, String> mergeTracked,
                                   String mergeSha1) {
        boolean inConflict = false;
        HashSet<String> fileNames = new HashSet<>();
        fileNames.addAll(splitTracked.keySet());
        fileNames.addAll(mergeTracked.keySet());
        fileNames.addAll(currentTracked.keySet());
        for (String fileName : fileNames) {
            String splitFileSha1 = null;
            String mergeFileSha1 = null;
            String currentFileSha1 = null;
            if (splitTracked.keySet().contains(fileName)) {
                splitFileSha1 = splitTracked.get(fileName);
            }
            if (mergeTracked.keySet().contains(fileName)) {
                mergeFileSha1 = mergeTracked.get(fileName);
            }
            if (currentTracked.keySet().contains(fileName)) {
                currentFileSha1 = currentTracked.get(fileName);
            }
            if (equals(splitFileSha1, currentFileSha1)
                    && !equals(splitFileSha1, mergeFileSha1)
                    && mergeFileSha1 != null) {
                checkout2Helper(mergeSha1, fileName);
                File file = new File(fileName);
                File stagedFile = new File(STAGED, fileName);
                String contents = Utils.readContentsAsString(file);
                Utils.writeContents(stagedFile, contents);
            } else if (equals(splitFileSha1, currentFileSha1)
                    && mergeFileSha1 == null) {
                doRemove("rm", fileName);
            } else if (!equals(splitFileSha1, mergeFileSha1)
                    && !equals(currentFileSha1, mergeFileSha1)) {
                String currentCont = "";
                String mergeContents = "";
                if (currentFileSha1 != null) {
                    String prefix = currentFileSha1.substring(0, 2);
                    File currentCommitPrefix = new File(BLOBS, prefix);
                    String tail = currentFileSha1.substring(2);
                    File currentComFile = new File(currentCommitPrefix, tail);
                    currentCont = Utils.readContentsAsString(currentComFile);
                }
                if (mergeFileSha1 != null) {
                    String prefix = mergeFileSha1.substring(0, 2);
                    File mergeComPrefix = new File(BLOBS, prefix);
                    String tail = mergeFileSha1.substring(2);
                    File mergeCommitFile = new File(mergeComPrefix, tail);
                    mergeContents = Utils.readContentsAsString(mergeCommitFile);
                }
                String newContents = "<<<<<<< HEAD\n" + currentCont
                        + "=======\n" + mergeContents + ">>>>>>>\n";
                File workingDirFile = new File(fileName);
                Utils.writeContents(workingDirFile, newContents);
                File stagedFile = new File(STAGED, fileName);
                Utils.writeContents(stagedFile, newContents);
                inConflict = true;
            }
        }
        return inConflict;
    }

    /** Returns true iff STR1 and STR2 are both null or
     *  are both equal as Strings. */
    public static boolean equals(String str1, String str2) {
        if (str1 != null) {
            return str1.equals(str2);
        } else {
            return str2 == null;
        }
    }

    /** Returns the SHA-1 code of the split point, that is,
     *  the most recent common ancestor of MERGEBRANCH and
     *  the current branch. */
    public static String splitPoint(String mergeBranch) {
        File mergeBranchFile = new File(BRANCHES, mergeBranch);
        String mergeBranchCommit = Utils.readContentsAsString(mergeBranchFile);
        String currentBranchCommit = headSHA1();
        String mergeBranchParent
                = retrieveCommit(mergeBranchCommit).getParent();
        String currentBranchParent
                = retrieveCommit(currentBranchCommit).getParent();
        HashSet<String> mergeBranchSeen = new HashSet<>();
        HashSet<String> currentBranchSeen = new HashSet<>();

        while (mergeBranchParent != null || currentBranchParent != null) {
            if (currentBranchSeen.contains(mergeBranchCommit)
                    || mergeBranchCommit.equals(currentBranchCommit)) {
                return mergeBranchCommit;
            } else if (mergeBranchSeen.contains(currentBranchCommit)) {
                return currentBranchCommit;
            } else {
                mergeBranchSeen.add(mergeBranchCommit);
                currentBranchSeen.add(currentBranchCommit);
                if (mergeBranchParent != null) {
                    mergeBranchCommit = mergeBranchParent;
                    mergeBranchParent
                            = retrieveCommit(mergeBranchParent).getParent();
                }
                if (currentBranchParent != null) {
                    currentBranchCommit = currentBranchParent;
                    currentBranchParent
                            = retrieveCommit(currentBranchParent).getParent();
                }
            }
        }
        return null;
    }

    /** Returns true if and only if merging MERGEBRANCH with
     *  the current branch would result in a merge conflict. */
    public static boolean unsafeToMerge(String mergeBranch) {
        File mergeBranchFile = new File(BRANCHES, mergeBranch);
        String splitSha1 = splitPoint(mergeBranch);
        String mergeSha1 = Utils.readContentsAsString(mergeBranchFile);
        HashMap<String, String> splitTracked
                = retrieveCommit(splitSha1).getTracked();
        HashMap<String, String> mergeTracked
                = retrieveCommit(mergeSha1).getTracked();
        HashMap<String, String> currentTracked = headCommit().getTracked();


        for (String fileName : untrackedFiles()) {
            String splitFileSha1 = null;
            String mergeFileSha1 = null;
            String currentFileSha1 = null;
            if (splitTracked.keySet().contains(fileName)) {
                splitFileSha1 = splitTracked.get(fileName);
            }
            if (mergeTracked.keySet().contains(fileName)) {
                mergeFileSha1 = mergeTracked.get(fileName);
            }
            if (currentTracked.keySet().contains(fileName)) {
                currentFileSha1 = currentTracked.get(fileName);
            }

            if (splitFileSha1 == null && mergeFileSha1 == null
                    && currentFileSha1 == null) {
                continue;
            } else if (equals(splitFileSha1, currentFileSha1)
                    && !equals(splitFileSha1, mergeFileSha1)) {
                return true;
            } else if (equals(splitFileSha1, mergeFileSha1)
                    || equals(currentFileSha1, mergeFileSha1)) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    /** Returns the set of all files that should currently
     *  be regarded as modified but not staged. */
    public static HashSet<String> modifiedButNotStaged() {
        HashSet<String> modifiedButNotStaged = new HashSet<>();

        for (String fileName : Utils.plainFilenamesIn(new File("."))) {
            boolean tracked
                    = headCommit().getTracked().keySet().contains(fileName);

            File file = new File(fileName);
            String workingDirSha1 = Utils.sha1(Utils.readContents(file));
            String commitSha1 = headCommit().getTracked().get(fileName);
            boolean changed = !workingDirSha1.equals(commitSha1);

            File stagedFile = new File(STAGED, fileName);
            boolean isStaged = stagedFile.exists();

            if (tracked && changed && !isStaged) {
                modifiedButNotStaged.add(fileName + " (modified)");
            }

            if (isStaged) {
                String stagedSha1 = Utils.sha1(Utils.readContents(stagedFile));
                if (!workingDirSha1.equals(stagedSha1)) {
                    modifiedButNotStaged.add(fileName + " (modified)");
                }
            }
        }

        for (String fileName : STAGED.list()) {
            File file = new File(fileName);
            if (!file.exists()) {
                modifiedButNotStaged.add(fileName + " (deleted)");
            }
        }

        for (String fileName : headCommit().getTracked().keySet()) {
            File file = new File(fileName);
            File removedFile = new File(REMOVED, fileName);
            if (!file.exists() && !removedFile.exists()) {
                modifiedButNotStaged.add(fileName + " (deleted)");
            }
        }

        return modifiedButNotStaged;
    }

    /** Returns the set of all currently untracked files in the
     *  working directory. */
    public static HashSet<String> untrackedFiles() {
        HashSet<String> untrackedFiles = new HashSet<>();
        for (String fileName : Utils.plainFilenamesIn(new File("."))) {
            boolean tracked
                    = headCommit().getTracked().keySet().contains(fileName);
            boolean isStaged = (new File(STAGED, fileName)).exists();
            boolean isRemoved = (new File(REMOVED, fileName)).exists();
            if ((!tracked && !isStaged) || isRemoved) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    /** Serializes the given COMMIT and sets its SHA-1 value. */
    public static void writeCommit(Commit commit) {
        byte[] serialized = Utils.serialize(commit);
        String sha1 = Utils.sha1(serialized);
        File prefixDir = new File(COMMITS, sha1.substring(0, 2));
        if (!prefixDir.exists()) {
            prefixDir.mkdir();
        }
        File commitFile = new File(prefixDir, sha1.substring(2));
        commit.setSha1(sha1);
        Utils.writeObject(commitFile, commit);
    }

    /** Serializes the given FILE. */
    public static void writeBlob(File file) {
        String sha1 = Utils.sha1(Utils.readContents(file));
        File prefixDir = new File(BLOBS, sha1.substring(0, 2));
        if (!prefixDir.exists()) {
            prefixDir.mkdir();
        }
        File blobFile = new File(prefixDir, sha1.substring(2));
        Utils.writeContents(blobFile, Utils.readContentsAsString(file));
    }

    /** Returns the commit with the given SHA1. */
    public static Commit retrieveCommit(String sha1) {
        if (sha1 == null) {
            return null;
        } else {
            File prefixDir = new File(COMMITS, sha1.substring(0, 2));
            File serializedCommit = new File(prefixDir, sha1.substring(2));
            return Utils.readObject(serializedCommit, Commit.class);
        }
    }

    /** Returns the SHA-1 hash code of the current commit. */
    public static String headSHA1() {
        String branch = Utils.readContentsAsString(HEAD);
        File branchFile = new File(BRANCHES, branch);
        return Utils.readContentsAsString(branchFile);
    }

    /** Returns the current commit. */
    public static Commit headCommit() {
        return retrieveCommit(headSHA1());
    }

    /** Prints the log of the given COMMIT. */
    public static void commitLog(Commit commit) {
        if (commit != null) {
            System.out.println("===");
            System.out.println("commit " + commit.getSha1());
            if (commit.getMergeParent() != null) {
                String parentPrefix = commit.getParent().substring(0, 7);
                String mergePrefix = commit.getMergeParent().substring(0, 7);
                System.out.println("Merge: "
                        + parentPrefix + " " + mergePrefix);
            }
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }

    /** Prints the log of every commit in the history of
     *  the given COMMIT. */
    public static void commitHistory(Commit commit) {
        if (commit != null) {
            commitLog(commit);
            Commit parent = retrieveCommit(commit.getParent());
            commitHistory(parent);
        }
    }

    /** If there is exactly one commit currently in the repo
     *  whose SHA-1 code begins with SHA1PREFIX, returns that
     *  SHA-1 code. Else, prints error message and returns
     *  null. */
    public static String sha1(String sha1Prefix) {
        String error = "Prefix does not identify a unique commit.";
        if (sha1Prefix.length() <= 2) {
            boolean found = false;
            String prefix = "";
            for (String directory : COMMITS.list()) {
                String dirPrefix
                        = directory.substring(0, sha1Prefix.length());
                if (dirPrefix.equals(sha1Prefix)) {
                    if (found) {
                        System.out.println(error);
                        return null;
                    } else {
                        prefix = directory;
                        found = true;
                    }
                }
            }
            if (found) {
                File prefixDir = new File(COMMITS, prefix);
                if (prefixDir.list().length == 1) {
                    return prefix + prefixDir.list()[0];
                } else {
                    System.out.println(error);
                    return null;
                }
            } else {
                System.out.println("No commit with that id exists.");
                return null;
            }
        } else {
            boolean found = false;
            String sha1 = "";
            String tail = sha1Prefix.substring(2);
            for (String directory : COMMITS.list()) {
                if (directory.equals(sha1Prefix.substring(0, 2))) {
                    File dirFile = new File(COMMITS, directory);
                    for (String fileName : dirFile.list()) {
                        String rem = fileName.substring(0, tail.length());
                        if (found) {
                            System.out.println(error);
                            return null;
                        } else if (rem.equals(tail)) {
                            sha1 = directory + fileName;
                            found = true;
                        }
                    }
                    break;
                }
            }
            if (found) {
                return sha1;
            } else {
                System.out.println("No commit with that id exists.");
                return null;
            }
        }
    }

    /** File representing the .gitlet directory. */
    static final File GITLET = new File(".gitlet");

    /** File representing the blobs directory. */
    static final File BLOBS = new File(GITLET, "blobs");

    /** File representing the commits directory. */
    static final File COMMITS = new File(GITLET, "commits");

    /** File representing the branches directory. */
    static final File BRANCHES = new File(GITLET, "branches");

    /** File representing the staged directory. */
    static final File STAGED = new File(GITLET, "staged");

    /** File representing the removed directory. */
    static final File REMOVED = new File(GITLET, "removed");

    /** File representing the head pointer. */
    static final File HEAD = new File(GITLET, "head");

}
