package com.bugsnag.android.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * StackTreeNodes allow quite and compact lookup of stack trace elements as in a tree structure,
 * along with a count of the number of times they have been seen. This is used as the core
 * structure for stack sampling.
 * <p>
 * When sampling, the stack is stored upside-down with the thread entry point at the root of the
 * tree and each element seen in a stack trace as a series of branches.
 */
final class StackTreeNode {
    /**
     * The maximum number of elements that our linear-probing should consider before exiting.
     */
    private static final int MAX_PROBING_DISTANCE = 16;

    @Nullable
    final String fileName;

    @NonNull
    final String methodName;

    @NonNull
    final String className;

    final int lineNumber;

    final int hashCode;

    /**
     * The number of times this node has seen during sampling
     */
    int sampleCount = 1;

    /**
     * The child nodes that this StackTreeNode has. This is one of 3 data types:
     * - null: the node has no child nodes
     * - StackTreeNode: the node only has a single child
     * - StackTreeNode[]: the node has multiple child nodes (hashtable)
     * <p>
     * The array structure is used as a hybrid between a hashtable and a hashset. Roughly
     * equivalent to {@code HashMap<StackTreeNode, StackTreeNode>} where the key and values
     * are always the same object. The table is linear-probing to handle collisions and
     * accessed via {@link #hashtableLookup(StackTreeNode[], int, StackTreeNode, StackTraceElement)}
     * and {@link #rehash(StackTreeNode[])}.
     * <p>
     * Given that these objects are single-threaded, the cache-locality offered by linear probing
     * is better than the reduced clustering offering be quadratic probing while also being less
     * complex to maintain than linked-lists (which make load-factor more complex).
     */
    private Object childNodes = null;

    StackTreeNode(
            @Nullable String fileName,
            @NonNull String methodName,
            @NonNull String className,
            int lineNumber
    ) {
        this.fileName = fileName;
        this.methodName = methodName;
        this.className = className;
        this.lineNumber = lineNumber;
        this.hashCode = hashCodeFor(fileName, methodName, className, lineNumber);
    }

    /**
     * Convenience constructor to create a StackTreeNode from a StackTraceElement
     *
     * @param element The StackTraceElement to copy fields from
     */
    StackTreeNode(@NonNull StackTraceElement element) {
        this(element.getFileName(),
                element.getMethodName(),
                element.getClassName(),
                element.getLineNumber());
    }

    /**
     * Create an empty StackTreeNode typically used as a root node.
     */
    StackTreeNode() {
        this(null, "", "", -1);
    }

    void clear() {
        childNodes = null;
        sampleCount = 0;
    }

    /**
     * Return the child StackTreeNode with the highest sample count, or null if there are no
     * child nodes.
     *
     * @return the child with the highest sample count, or null if there are no child nodes
     */
    @Nullable
    StackTreeNode mostSampledChildNode() {
        if (childNodes == null) {
            return null;
        } else if (childNodes instanceof StackTreeNode) {
            return (StackTreeNode) childNodes;
        } else {
            StackTreeNode[] table = (StackTreeNode[]) childNodes;
            StackTreeNode hottest = table[0];
            for (int i = 1; i < table.length; i++) {
                StackTreeNode child = table[i];
                if (hottest == null || (child != null && child.sampleCount > hottest.sampleCount)) {
                    hottest = child;
                }
            }

            return hottest;
        }
    }

    StackTreeNode childNodeFor(@NonNull StackTraceElement element) {
        if (childNodes == null) {
            StackTreeNode newChild = new StackTreeNode(element);
            childNodes = newChild;
            return newChild;
        } else if (childNodes instanceof StackTreeNode
                && ((StackTreeNode) childNodes).matches(element)) {

            return (StackTreeNode) childNodes;
        } else if (childNodes instanceof StackTreeNode[]) {
            StackTreeNode[] table = (StackTreeNode[]) childNodes;
            int index = hashtableLookup(table, element);
            StackTreeNode child;
            if (index >= 0) {
                child = table[index];
                if (child == null) {
                    child = new StackTreeNode(element);
                    table[index] = child;
                }

                return child;
            } else {
                table = rehash(table);
                childNodes = table;

                index = hashtableLookup(table, element);
                child = new StackTreeNode(element);
                table[index] = child;
            }
            return child;
        } else {
            StackTreeNode child = (StackTreeNode) childNodes;
            StackTreeNode[] table = new StackTreeNode[8];
            table[hashtableLookup(table, child)] = child;

            StackTreeNode newChild = new StackTreeNode(element);
            table[hashtableLookup(table, element)] = newChild;

            childNodes = table;
            return newChild;
        }
    }

    boolean matches(StackTraceElement element) {
        // this ordering is tuned for early-exiting on the most likely cases
        if (!element.getMethodName().equals(methodName)) {
            return false;
        }

        if (!element.getClassName().equals(className)) {
            return false;
        }

        if (element.getLineNumber() != lineNumber) {
            return false;
        }

        String fileName = element.getFileName();
        if (fileName == null) {
            return this.fileName == null;
        } else {
            return fileName.equals(this.fileName);
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof StackTreeNode)) {
            return false;
        }

        StackTreeNode otherNode = (StackTreeNode) other;
        // hashCode offers us a fast-path
        if (otherNode.hashCode != hashCode) {
            return false;
        }

        if (!otherNode.methodName.equals(methodName)) {
            return false;
        }

        if (!otherNode.className.equals(className)) {
            return false;
        }

        if (otherNode.fileName == null) {
            if (fileName != null) {
                return false;
            }
        } else if (!otherNode.fileName.equals(fileName)) {
            return false;
        }

        return otherNode.lineNumber == lineNumber;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "StackTreeNode{"
                + "fileName='" + fileName + '\''
                + ", methodName='" + methodName + '\''
                + ", className='" + className + '\''
                + ", lineNumber=" + lineNumber
                + '}';
    }

    private static int hashCodeFor(@NonNull StackTraceElement element) {
        return hashCodeFor(
                element.getFileName(),
                element.getMethodName(),
                element.getClassName(),
                element.getLineNumber()
        );
    }

    private static int hashCodeFor(
            @Nullable String fileName,
            @NonNull String methodName,
            @NonNull String className,
            int lineNumber
    ) {
        int result = 17;

        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + methodName.hashCode();
        result = 31 * result + className.hashCode();
        result = 31 * result + lineNumber;

        return result;
    }

    private StackTreeNode[] rehash(StackTreeNode[] table) {
        StackTreeNode[] newTable = new StackTreeNode[table.length * 2];
        for (StackTreeNode node : table) {
            if (node != null) {
                int index = hashtableLookup(newTable, node);
                newTable[index] = node;
            }
        }
        return newTable;
    }

    private static int hashtableLookup(StackTreeNode[] table, StackTreeNode node) {
        return hashtableLookup(table, node.hashCode, node, null);
    }

    private static int hashtableLookup(StackTreeNode[] table, StackTraceElement element) {
        return hashtableLookup(table, hashCodeFor(element), null, element);
    }

    /**
     * Lookup where a given StackTreeNode *or* StackTraceElement should be in the given
     * {@code table}. Returns one of:
     * - the index of the StackTreeNode
     * - the index where the StackTreeNode should be inserted
     * - {@code -1} if the table is full and does not contain the StackTreeNode
     *
     * @param table    the table to perform the lookup in
     * @param hashCode the pre-calculated hashCode for the node
     * @param node     the new node to find a space for within the table
     * @param element  the element to find a matching node for within the table
     * @return the index of the node within the table, or -1 if the table is full
     */
    private static int hashtableLookup(
            StackTreeNode[] table,
            int hashCode,
            @Nullable StackTreeNode node,
            @Nullable StackTraceElement element
    ) {
        int mask = table.length - 1;
        int index = hashCode & mask;
        int maxProbingDistance = Math.min(mask, MAX_PROBING_DISTANCE);

        for (int i = 0; i < maxProbingDistance; i++) {
            StackTreeNode tableElement = table[index];
            if (tableElement == null) {
                return index;
            }

            if (hashCode == tableElement.hashCode) {
                //noinspection DataFlowIssue
                boolean matches = (node != null)
                        ? tableElement.equals(node)
                        : tableElement.matches(element);

                if (matches) {
                    return index;
                }
            }

            index = (index + 1) & mask;
        }

        return -1;
    }
}
