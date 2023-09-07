package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import java.io.InputStream
import com.bugsnag.android.Thread as BugsnagThread

internal class TraceParser(
    private val logger: Logger,
    private val projectPackages: Collection<String>,
) {

    /**
     * This is a stateful parser where each line is parsed based on the current state of the parser
     * as opposed to tracking the state on the stack (recursive descent). This provides a certain
     * level of flexibility and fault-tolerance in the parser.
     */
    private var state = STATE_LOOKING_FOR_THREAD

    /**
     * The Thread we are currently parsing content (stack-frames) into
     */
    private var currentThread: BugsnagThread? = null

    /**
     * Each thread is preceded by a number of attribute lines, these attributes are parsed into
     * this Map. The `Map` is cleared between threads.
     */
    private val threadAttrs = HashMap<String, String>()

    fun parse(inputStream: InputStream, threadConsumer: (BugsnagThread) -> Unit) {
        inputStream.bufferedReader().forEachLine { line ->
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty()) {
                currentThread?.also(threadConsumer)
                currentThread = null
                threadAttrs.clear()

                state = STATE_LOOKING_FOR_THREAD
            } else {
                parseTrimmedLine(trimmedLine)
            }
        }
    }

    private fun parseTrimmedLine(line: String) {
        when (state) {
            STATE_LOOKING_FOR_THREAD ->
                if (line[0] == '"') {
                    currentThread = parseThreadDeclaration(line)
                    state = STATE_READING_THREAD_ATTRIBUTES
                }

            STATE_READING_THREAD_ATTRIBUTES ->
                if (line[0] != '|') {
                    state = STATE_READING_STACKTRACE

                    currentThread?.id = threadAttrs["tid"] ?: threadAttrs["sysTid"] ?: ""
                    currentThread?.state = getCurrentThreadState()

                    parseStackframe(line)?.let { currentThread?.stacktrace?.add(it) }
                } else {
                    parseThreadAttributes(line)
                }

            STATE_READING_STACKTRACE -> {
                parseStackframe(line)?.let { currentThread?.stacktrace?.add(it) }
            }
        }
    }

    @VisibleForTesting
    internal fun parseStackframe(line: String): Stackframe? {
        return when (line.first()) {
            'a' -> parseJavaFrame(line)

            // "#" and "native" are native / C frames
            '#', 'n' -> parseNativeFrame(line)

            else -> null
        }
    }

    @VisibleForTesting
    internal fun parseJavaFrame(line: String): Stackframe? {
        if (!line.startsWith("at ")) {
            return null
        }

        val filenameStart = line.lastIndexOf('(')
        val filenameEnd = line.lastIndexOf(')')

        if (filenameStart == -1 || filenameEnd == -1 || filenameEnd <= filenameStart) {
            return null
        }

        val method = line.substring(3, filenameStart)
        val filename = line.substring(filenameStart + 1, filenameEnd)
        val file = filename.substringBefore(':')
        val lineNumber = filename
            .substringAfter(':', missingDelimiterValue = "")
            .toIntOrNull()

        return Stackframe(
            method,
            file,
            lineNumber,
            projectPackages.any { methodToClassName(method).startsWith(it) }.takeIf { it },
        ).apply {
            type = ErrorType.ANDROID
        }
    }

    @VisibleForTesting
    internal fun parseNativeFrame(line: String): Stackframe? {
        // first we walk forwards through the frame to find PC & Filename
        val pcIndex = line.indexOf("pc ")
        if (pcIndex == -1) {
            return null
        }

        val pcEnd = line.indexOf(' ', pcIndex + 3)
        if (pcEnd == -1) {
            return null
        }

        val slash = line.indexOf('/', pcEnd + 1)
        if (slash == -1) {
            return null
        }

        val symbolLBrace = line.indexOf('(', slash + 1)
        if (symbolLBrace == -1) {
            return null
        }

        // then we walk backwards for the BuildID & Function
        val buildIDRBrace = line.lastIndexOf(')')
        if (buildIDRBrace == -1) {
            return null
        }

        val buildIDLBrace = line.lastIndexOf('(', buildIDRBrace - 1)
        if (buildIDLBrace == -1 || buildIDRBrace < buildIDLBrace) {
            return null
        }

        val symbolRBrace = line.lastIndexOf(')', buildIDLBrace - 1)
        if (symbolRBrace == -1 || symbolRBrace < symbolLBrace) {
            return null
        }

        val plus = line.lastIndexOf('+', symbolRBrace - 1)

        val buildId = line.substring(buildIDLBrace + 1, buildIDRBrace).removePrefix("BuildId: ")
        val symbol = if (plus in symbolLBrace..symbolRBrace) {
            line.substring(symbolLBrace + 1, plus)
        } else {
            line.substring(symbolLBrace + 1, symbolRBrace)
        }

        return Stackframe(
            symbol,
            line.substring(slash, symbolLBrace - 1).trim(),
            line.substring(pcIndex + 3, pcEnd).toLongOrNull(16),
            null,
            null,
            null
        ).apply {
            type = ErrorType.C
            codeIdentifier = buildId
        }
    }

    @VisibleForTesting
    internal fun parseThreadDeclaration(line: String): BugsnagThread {
        val threadNameEnd = line.lastIndexOf('"')
        val attrsEnd = line.indexOfOrDefault('(', threadNameEnd + 1, line.length)

        // parse the attributes on the thread declaration line
        parseThreadAttributes(line.substring(threadNameEnd + 1, attrsEnd))

        return BugsnagThread(
            null, // we retrieve this after the attributes have been read
            line.substring(1, threadNameEnd),
            ErrorType.ANDROID,
            false,
            getCurrentThreadState(),
            logger,
        )
    }

    private fun parseThreadAttributes(line: String) {
        var index = line.indexOfFirst { it != '|' && !it.isWhitespace() }

        while (index >= 0 && index < line.lastIndex) {
            val eq = line.indexOf('=', index)
            val space = line.indexOf(' ', index)

            if (space != -1 && space < eq) {
                // an attribute with no value, captures "daemon" and "Native" in:
                // "daemon prio=6 tid=30 Native"
                threadAttrs[line.substring(index, space)] = ""
                index = space + 1
            } else if (eq != -1) {
                val attrName: String = line.substring(index, eq)

                // check that the '=' is not the end of the line
                if (eq < line.lastIndex) {
                    when (line[eq + 1]) {
                        '"' -> {
                            val close = line.indexOfOrDefault('"', eq + 2, line.length)
                            threadAttrs[attrName] = line.substring(eq + 2, close)
                            index = close + 1
                        }

                        '(' -> {
                            val close = line.indexOfOrDefault(')', eq + 2, line.lastIndex)
                            threadAttrs[attrName] = line.substring(eq + 1, close + 1)
                            index = close + 2
                        }

                        else -> {
                            val end = if (space == -1) line.length else space
                            threadAttrs[attrName] = line.substring(eq + 1, end)
                            index = end + 1
                        }
                    }
                } else {
                    threadAttrs[line.substring(index, eq)] = ""
                    index = eq + 1
                }
            } else if (index < line.lastIndex) {
                // we treat the rest of the line as an attribute name
                threadAttrs[line.substring(index)] = ""
                index = line.length
            }
        }
    }

    private fun getCurrentThreadState(): BugsnagThread.State {
        val state = threadAttrs["state"]
        if (state != null) {
            when (state) {
                "R" -> return BugsnagThread.State.RUNNABLE
                "S" -> return BugsnagThread.State.WAITING
                "Z" -> return BugsnagThread.State.TERMINATED
            }
        }

        return when {
            threadAttrs.contains("Runnable") ||
                threadAttrs.contains("Native") -> BugsnagThread.State.RUNNABLE

            threadAttrs.contains("Waiting") ||
                threadAttrs.contains("WaitingForTaskProcessor") ||
                threadAttrs.contains("Sleeping") -> BugsnagThread.State.WAITING

            else -> BugsnagThread.State.UNKNOWN
        }
    }

    private fun methodToClassName(method: String): String {
        return method.substringBeforeLast('.')
    }

    private fun String.indexOfOrDefault(
        ch: Char,
        startIndex: Int = 0,
        default: Int = -1,
    ): Int {
        val index = indexOf(ch, startIndex)
        if (index == -1) {
            return default
        }

        return index
    }

    companion object {
        private const val STATE_LOOKING_FOR_THREAD = 1
        private const val STATE_READING_THREAD_ATTRIBUTES = 2
        private const val STATE_READING_STACKTRACE = 3
    }
}
