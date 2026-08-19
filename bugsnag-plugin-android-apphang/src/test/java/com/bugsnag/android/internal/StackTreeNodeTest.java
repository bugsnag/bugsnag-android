package com.bugsnag.android.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackTreeNodeTest {

    private static final String CLASS_NAME = "com.example.MyClass";
    private static final String METHOD_NAME = "myMethod";
    private static final String FILE_NAME = "MyClass.java";
    private static final int LINE_NUMBER = 42;

    @Test
    public void testConstructor() {
        StackTraceElement element = createElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTreeNode node = new StackTreeNode(element);

        assertNotNull(node);
        assertEquals(0, node.sampleCount);
        assertMatches(node, element);
    }

    @Test
    public void testConstructorWithNullFileName() {
        StackTraceElement element = createElement(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        StackTreeNode node = new StackTreeNode(element);

        assertNotNull(node);
        assertMatches(node, element);
    }

    @Test
    public void testMostSampledChildNodeForNodeWithNoChildren() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertNull(node.mostSampledChildNode());
    }

    @Test
    public void testMostSampledChildWithSingleChildNodeForNode() {
        StackTreeNode parentNode = createParentNode();
        StackTraceElement child = createChildNodeForElement();

        StackTreeNode childNode = parentNode.childNodeFor(child);
        assertSame(childNode, parentNode.mostSampledChildNode());
    }

    @Test
    public void testMostSampledChildNodeForNodeWithMultipleChildren() {
        StackTreeNode parentNode = createParentNode();

        StackTreeNode childNode1 = parentNode.childNodeFor(createElementWithIndex("Child", 1));
        childNode1.sampleCount = 5;

        StackTreeNode childNode2 = parentNode.childNodeFor(createElementWithIndex("Child", 2));
        childNode2.sampleCount = 10;

        StackTreeNode childNode3 = parentNode.childNodeFor(createElementWithIndex("Child", 3));
        childNode3.sampleCount = 3;

        StackTreeNode hottest = parentNode.mostSampledChildNode();
        assertNotNull(hottest);
        assertSame(childNode2, hottest);
        assertEquals(10, hottest.sampleCount);
    }

    @Test
    public void testChildCreatesFirstChildNodeFor() {
        StackTreeNode parentNode = createParentNode();
        StackTraceElement child = createChildNodeForElement();

        StackTreeNode childNode = parentNode.childNodeFor(child);

        assertNotNull(childNode);
        assertMatches(childNode, child);
        assertSame(childNode, parentNode.mostSampledChildNode());
    }

    @Test
    public void testChildNodeForReturnsSameNodeForMatchingElement() {
        StackTreeNode parentNode = createParentNode();
        StackTraceElement child = createChildNodeForElement();

        StackTreeNode childNode1 = parentNode.childNodeFor(child);
        StackTreeNode childNode2 = parentNode.childNodeFor(child);

        assertSame(childNode1, childNode2);
    }

    @Test
    public void testChildCreatesHashtableWhenSecondChildNodeForAdded() {
        StackTreeNode parentNode = createParentNode();

        StackTraceElement child1 = createElementWithIndex("Child", 1);
        StackTraceElement child2 = createElementWithIndex("Child", 2);

        StackTreeNode childNode1 = parentNode.childNodeFor(child1);
        StackTreeNode childNode2 = parentNode.childNodeFor(child2);

        assertNotNull(childNode1);
        assertNotNull(childNode2);
        assertSame(childNode1, parentNode.childNodeFor(child1));
        assertSame(childNode2, parentNode.childNodeFor(child2));
    }

    @Test
    public void testChildNodeForHandlesHashtableRehashing() {
        StackTreeNode parentNode = createParentNode();
        StackTreeNode[] capturedChildNodes = new StackTreeNode[20];

        // Add enough children to trigger rehashing (initial size is 8)
        for (int i = 0; i < capturedChildNodes.length; i++) {
            capturedChildNodes[i] = parentNode.childNodeFor(createElementWithIndex("Child", i));
        }

        // Verify all children are still accessible
        for (int i = 0; i < capturedChildNodes.length; i++) {
            StackTraceElement child = createElementWithIndex("Child", i);
            StackTreeNode childNode = parentNode.childNodeFor(child);
            assertNotNull("Child " + i + " should be found", childNode);
            assertSame(capturedChildNodes[i], childNode);
            assertMatches(childNode, child);
        }
    }

    @Test
    public void testMatchesWithIdenticalElements() {
        StackTraceElement element1 = createElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTraceElement element2 = createElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);

        StackTreeNode node = new StackTreeNode(element1);
        assertMatches(node, element2);
    }

    @Test
    public void testMatchesWithDifferentMethodName() {
        StackTreeNode node = createNode(CLASS_NAME, "method1", FILE_NAME, LINE_NUMBER);
        StackTraceElement element = createElement(CLASS_NAME, "method2", FILE_NAME, LINE_NUMBER);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testMatchesWithDifferentClassName() {
        StackTreeNode node =
                createNode("com.example.Class1", METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTraceElement element =
                createElement("com.example.Class2", METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testMatchesWithDifferentFileName() {
        StackTreeNode node =
                createNode(CLASS_NAME, METHOD_NAME, "File1.java", LINE_NUMBER);
        StackTraceElement element =
                createElement(CLASS_NAME, METHOD_NAME, "File2.java", LINE_NUMBER);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testMatchesWithDifferentLineNumber() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTraceElement element = createElement(CLASS_NAME, METHOD_NAME, FILE_NAME, 43);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testMatchesWithBothNullFileNames() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        StackTraceElement element =
                createElement(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        assertMatches(node, element);
    }

    @Test
    public void testMatchesWithOneNullFileName() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        StackTraceElement element = createElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testMatchesWithOtherNullFileName() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTraceElement element =
                createElement(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        assertDoesNotMatch(node, element);
    }

    @Test
    public void testEqualsWithIdenticalNodes() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertNodesEqual(node1, node2);
    }

    @Test
    public void testEqualsWithSameNode() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertTrue(node.equals(node));
    }

    @Test
    public void testEqualsWithNull() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertFalse(node.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        StackTreeNode node = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertFalse(node.equals("not a StackTreeNode"));
    }

    @Test
    public void testEqualsWithDifferentMethodName() {
        StackTreeNode node1 = createNode(CLASS_NAME, "method1", FILE_NAME, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, "method2", FILE_NAME, LINE_NUMBER);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testEqualsWithDifferentClassName() {
        StackTreeNode node1 =
                createNode("com.example.Class1", METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTreeNode node2 =
                createNode("com.example.Class2", METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testEqualsWithDifferentFileName() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, "File1.java", LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, "File2.java", LINE_NUMBER);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testEqualsWithDifferentLineNumber() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, 43);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testEqualsWithBothNullFileNames() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        assertNodesEqual(node1, node2);
    }

    @Test
    public void testEqualsWithOneNullFileName() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testEqualsWithOtherNullFileName() {
        StackTreeNode node1 = createNode(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER);
        StackTreeNode node2 = createNode(CLASS_NAME, METHOD_NAME, null, LINE_NUMBER);
        assertNodesNotEqual(node1, node2);
    }

    @Test
    public void testHashCollisionHandling() {
        StackTreeNode parentNode = createParentNode();

        // Add many elements to stress test the hashtable
        for (int i = 0; i < 20; i++) {
            parentNode.childNodeFor(createElementWithIndex("Child", i));
        }

        // Verify all children are still retrievable
        for (int i = 0; i < 20; i++) {
            StackTraceElement child = createElementWithIndex("Child", i);
            StackTreeNode childNode = parentNode.childNodeFor(child);
            assertNotNull("Child " + i + " should be found", childNode);
            assertMatches(childNode, child);
        }
    }

    @Test
    public void testMostSampledChildNodeForNodeWithNullInTable() {
        StackTreeNode parentNode = createParentNode();

        StackTreeNode childNode1 = parentNode.childNodeFor(createElementWithIndex("Child", 1));
        childNode1.sampleCount = 5;

        StackTreeNode childNode2 = parentNode.childNodeFor(createElementWithIndex("Child", 2));
        childNode2.sampleCount = 10;

        // The hashtable may have null entries; verify hottestChild handles them
        StackTreeNode hottest = parentNode.mostSampledChildNode();
        assertNotNull(hottest);
        assertTrue(hottest.sampleCount >= 5);
    }

    @Test
    public void testComplexTreeStructure() {
        StackTreeNode rootNode = createNode("com.example.Root", "rootMethod", "Root.java", 1);

        StackTreeNode level1aNode = rootNode.childNodeFor(
                createElement("com.example.L1A", "method1a", "L1A.java", 2));
        StackTreeNode level1bNode = rootNode.childNodeFor(
                createElement("com.example.L1B", "method1b", "L1B.java", 3));

        level1aNode.sampleCount = 15;
        level1bNode.sampleCount = 10;

        StackTreeNode level2aNode = level1aNode.childNodeFor(
                createElement("com.example.L2A", "method2a", "L2A.java", 4));
        level2aNode.sampleCount = 8;

        // Verify tree structure
        assertSame(level1aNode, rootNode.mostSampledChildNode());
        assertSame(level2aNode, level1aNode.mostSampledChildNode());
        assertNull(level1bNode.mostSampledChildNode());
    }

    @Test
    public void deepTreeTest() {
        final int treeDepth = 500;
        final int childNodesPerLevel = 500;

        StackTreeNode root = new StackTreeNode();
        StackTreeNode current = root;
        Map<StackTreeNode, List<StackTreeNode>> mirrorTree = new HashMap<>();

        for (int depth = 0; depth < treeDepth; depth++) {
            List<StackTreeNode> childNodes = new ArrayList<>();
            mirrorTree.put(current, childNodes);
            for (int childNodeCount = 0; childNodeCount < childNodesPerLevel; childNodeCount++) {
                StackTraceElement element = createElement(
                        CLASS_NAME,
                        "method" + depth,
                        "file",
                        childNodeCount
                );

                StackTreeNode newNode = current.childNodeFor(element);
                childNodes.add(newNode);
            }

            // Move down to the most sampled child for next depth level
            current = current.mostSampledChildNode();
            assertNotNull("Should have a child node at depth " + depth, current);
        }

        // Verify all nodes are retrievable
        current = root;
        for (int depth = 0; depth < treeDepth; depth++) {
            List<StackTreeNode> expectedChildren = mirrorTree.get(current);
            assertNotNull("Should have children at depth " + depth, expectedChildren);
            assertEquals("Should have 500 children at depth " + depth,
                    500, expectedChildren.size());

            // Verify each child is retrievable
            for (int childNodeCount = 0; childNodeCount < childNodesPerLevel; childNodeCount++) {
                StackTraceElement element = createElement(
                        CLASS_NAME,
                        "method" + depth,
                        "file",
                        childNodeCount
                );
                StackTreeNode retrievedNode = current.childNodeFor(element);
                assertSame("Should retrieve same node at depth " + depth
                                + ", child " + childNodeCount,
                        expectedChildren.get(childNodeCount), retrievedNode);
            }
            current = current.mostSampledChildNode();
            assertNotNull("node should have a child at depth " + depth, current);
        }
    }

    private StackTraceElement createElement(
            String className,
            String methodName,
            String fileName,
            int lineNumber
    ) {
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    private StackTreeNode createNode(
            String className,
            String methodName,
            String fileName,
            int lineNumber
    ) {
        return new StackTreeNode(createElement(className, methodName, fileName, lineNumber));
    }

    private StackTreeNode createParentNode() {
        return createNode("com.example.Parent", "parentMethod", "Parent.java", 10);
    }

    private StackTraceElement createChildNodeForElement() {
        return createElement("com.example.Child", "childMethod", "Child.java", 20);
    }

    private StackTraceElement createElementWithIndex(String baseName, int index) {
        return createElement(
                "com.example." + baseName + index,
                "method" + index,
                baseName + index + ".java",
                index
        );
    }

    private void assertMatches(StackTreeNode node, StackTraceElement element) {
        assertTrue("Node should match element", node.matches(element));
    }

    private void assertDoesNotMatch(StackTreeNode node, StackTraceElement element) {
        assertFalse("Node should not match element", node.matches(element));
    }

    private void assertNodesEqual(StackTreeNode node1, StackTreeNode node2) {
        assertTrue("Nodes should be equal", node1.equals(node2));
        assertTrue("Equality should be symmetric", node2.equals(node1));
    }

    private void assertNodesNotEqual(StackTreeNode node1, StackTreeNode node2) {
        assertFalse("Nodes should not be equal", node1.equals(node2));
    }
}
