package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** A commit object.
 *  @author Ed Scerbo
 */
public class Commit implements Serializable {

    /** A single initial commit object to be used by all repos. */
    public static final Commit INITIAL_COMMIT = new InitialCommit();

    /** Default constructor so InitialCommit subclass doesn't get angry. */
    Commit() { }

    /** A brand new Commit with TIMESTAMP, MESSAGE, TRACKED map, PARENT,
     *  and MERGEPARENT. */
    Commit(String timestamp, String message, HashMap<String,
            String> tracked, String parent, String mergeParent) {
        _timestamp = timestamp;
        _message = message;
        _tracked = tracked;
        _parent = parent;
        _mergeParent = mergeParent;
    }

    /** Returns the commit's timestamp. */
    String getTimestamp() {
        return _timestamp;
    }

    /** Sets the commit's TIMESTAMP. */
    void setTimestamp(String timestamp) {
        _timestamp = timestamp;
    }

    /** Returns the commit's message. */
    String getMessage() {
        return _message;
    }

    /** Sets the commit's MESSAGE. */
    void setMessage(String message) {
        _message = message;
    }

    /** Returns the commit's HashMap pairing file name
     *  keys to SHA-1 hash code values. */
    HashMap<String, String> getTracked() {
        return _tracked;
    }

    /** Sets the commit's TRACKED map. */
    void setTracked(HashMap<String, String> tracked) {
        _tracked = tracked;
    }

    /** Returns the SHA-1 hash code of the commit's parent. */
    String getParent() {
        return _parent;
    }

    /** Returns the SHA-1 hash code of the commit's merge
     *  parent if it has one, null otherwise. */
    String getMergeParent() {
        return _mergeParent;
    }

    /** Returns the SHA-1 hash code of the commit. */
    String getSha1() {
        return _sha1;
    }

    /** Sets the SHA1 hash code of the commit. */
    void setSha1(String sha1) {
        _sha1 = sha1;
    }

    /** An initial commit.*/
    public static class InitialCommit extends Commit implements Serializable {
        /** An initial commit. */
        InitialCommit() {
            SimpleDateFormat date;
            date = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            setTimestamp(date.format(new Date(0)));
            setMessage("initial commit");
            setTracked(new HashMap<>());
        }
    }

    /** The commit's timestamp. */
    private String _timestamp;

    /** The commit's message. */
    private String _message;

    /** A HashMap containing all the blobs tracked by the commit.
     *  It pairs file name keys to approrpiate SHA-1 hash code values. */
    private HashMap<String, String> _tracked;

    /** The SHA-1 hash code of the commit's parent. */
    private String _parent;

    /** The SHA-1 hash code of the commit's merge parent, if it
     *  has one, null otherwise. */
    private String _mergeParent;

    /** The SHA-1 hash code of the commit. Upon construction it is
     *  set to null. The SHA-1 code only gets computed and set
     *  after Main.writeCommit runs. This way, we can store the
     *  SHA-1 internally without using it in the SHA-1 calculation. */
    private String _sha1;

}
