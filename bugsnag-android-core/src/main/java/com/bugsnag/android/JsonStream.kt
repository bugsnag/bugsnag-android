/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bugsnag.android

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.Flushable
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer

/*
 * Originally based on JsonWriter by Jesse Wilson Et al.
 *
 * JsonStream includes much of the JsonWriter implementation converted to Kotlin.
 */
class JsonStream private constructor(
    private val out: Writer,
    private val serializeNulls: Boolean,
    private val objectJsonStreamer: ObjectJsonStreamer
) : Closeable, Flushable {
    private var stack = IntArray(32)
    private var stackSize = 0

    init {
        push(JsonScope.EMPTY_DOCUMENT)
    }

    /**
     * A string containing a full set of spaces for a single level of
     * indentation, or null for no pretty printing.
     */
    private var indent: String? = null

    /**
     * The name/value separator; either ":" or ": ".
     */
    private var separator = ":"

    /**
     * Returns true if this writer has relaxed syntax rules.
     */
    /**
     * Configure this writer to relax its syntax rules. By default, this writer
     * only emits well-formed JSON as specified by [RFC 7159](http://www.ietf.org/rfc/rfc7159.txt).
     * Setting the writer to lenient permits the following:
     *
     * Top-level values of any type. With strict writing, the top-level value must be an object
     * or an array. Numbers may be [NaNs][Double.isNaN] or [Infinite][Double.isInfinite].
     *
     */
    var isLenient: Boolean = false

    /**
     * Returns true if this writer writes JSON that's safe for inclusion in HTML
     * and XML documents.
     */
    /**
     * Configure this writer to emit JSON that's safe for direct inclusion in HTML
     * and XML documents. This escapes the HTML characters `<`, `>`,
     * `&` and `=` before writing them to the stream. Without this
     * setting, your XML/HTML encoder should replace these characters with the
     * corresponding escape sequences.
     */
    var isHtmlSafe: Boolean = false

    private var deferredName: String? = null

    constructor(out: Writer) : this(out, false, ObjectJsonStreamer())

    internal constructor(stream: JsonStream, streamer: ObjectJsonStreamer) : this(
        stream.out,
        stream.serializeNulls,
        streamer
    )

    /**
     * Sets the indentation string to be repeated for each level of indentation
     * in the encoded document. If `indent.isEmpty()` the encoded document
     * will be compact. Otherwise the encoded document will be more
     * human-readable.
     *
     * @param indent a string containing only whitespace.
     */
    fun setIndent(indent: String) {
        if (indent.isEmpty()) {
            this.indent = null
            this.separator = ":"
        } else {
            this.indent = indent
            this.separator = ": "
        }
    }

    /**
     * Begins encoding a new array. Each call to this method must be paired with
     * a call to [.endArray].
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun beginArray(): JsonStream {
        writeDeferredName()
        return open(JsonScope.EMPTY_ARRAY, "[")
    }

    /**
     * Ends encoding the current array.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun endArray(): JsonStream {
        return close(JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY, "]")
    }

    /**
     * Begins encoding a new object. Each call to this method must be paired
     * with a call to [.endObject].
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun beginObject(): JsonStream {
        writeDeferredName()
        return open(JsonScope.EMPTY_OBJECT, "{")
    }

    /**
     * Ends encoding the current object.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun endObject(): JsonStream {
        return close(JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT, "}")
    }

    /**
     * Enters a new scope by appending any necessary whitespace and the given
     * bracket.
     */
    @Throws(IOException::class)
    private fun open(empty: Int, openBracket: String?): JsonStream {
        beforeValue()
        push(empty)
        out.write(openBracket)
        return this
    }

    /**
     * Closes the current scope by appending any necessary whitespace and the
     * given bracket.
     */
    @Throws(IOException::class)
    private fun close(empty: Int, nonempty: Int, closeBracket: String?): JsonStream {
        val context = peek()
        check(!(context != nonempty && context != empty)) { "Nesting problem." }
        check(deferredName == null) { "Dangling name: $deferredName" }

        stackSize--
        if (context == nonempty) {
            newline()
        }
        out.write(closeBracket)
        return this
    }

    private fun push(newTop: Int) {
        if (stackSize == stack.size) {
            val newStack = IntArray(stackSize * 2)
            System.arraycopy(stack, 0, newStack, 0, stackSize)
            stack = newStack
        }
        stack[stackSize++] = newTop
    }

    /**
     * Returns the value on the top of the stack.
     */
    private fun peek(): Int {
        check(stackSize != 0) { "JsonStream is closed." }
        return stack[stackSize - 1]
    }

    /**
     * Replace the value on the top of the stack with the given value.
     */
    private fun replaceTop(topOfStack: Int) {
        stack[stackSize - 1] = topOfStack
    }

    /**
     * Encodes the property name.
     *
     * @param name the name of the forthcoming value. May not be null.
     * @return this writer.
     */
    @Throws(IOException::class)
    fun name(name: String?): JsonStream {
        if (name == null) {
            throw NullPointerException("name == null")
        }
        check(deferredName == null)
        check(stackSize != 0) { "JsonStream is closed." }
        deferredName = name
        return this
    }

    @Throws(IOException::class)
    private fun writeDeferredName() {
        if (deferredName != null) {
            beforeName()
            string(deferredName!!)
            deferredName = null
        }
    }

    /**
     * Encodes `value`.
     *
     * @param value the literal string value, or null to encode a null literal.
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: String?): JsonStream {
        if (value == null) {
            return nullValue()
        }
        writeDeferredName()
        beforeValue()
        string(value)
        return this
    }

    /**
     * Writes `value` directly to the writer without quoting or
     * escaping.
     *
     * @param value the literal string value, or null to encode a null literal.
     * @return this writer.
     */
    @Throws(IOException::class)
    fun jsonValue(value: String?): JsonStream {
        if (value == null) {
            return nullValue()
        }
        writeDeferredName()
        beforeValue()
        out.write(value)
        return this
    }

    /**
     * Encodes `null`.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun nullValue(): JsonStream {
        if (deferredName != null) {
            if (serializeNulls) {
                writeDeferredName()
            } else {
                deferredName = null
                return this // skip the name and the value
            }
        }
        beforeValue()
        out.write("null")
        return this
    }

    /**
     * Encodes `value`.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: Boolean): JsonStream {
        writeDeferredName()
        beforeValue()
        out.write(if (value) "true" else "false")
        return this
    }

    /**
     * Encodes `value`.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: Boolean?): JsonStream {
        if (value == null) {
            return nullValue()
        }
        writeDeferredName()
        beforeValue()
        out.write(if (value) "true" else "false")
        return this
    }

    /**
     * Encodes `value`.
     *
     * @param value a finite value.
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: Double): JsonStream {
        if (!this.isLenient && (value.isNaN() || value.isInfinite())) {
            // omit these values instead of attempting to write them
            deferredName = null
        } else {
            writeDeferredName()
            beforeValue()
            out.write(value.toString())
        }
        return this
    }

    /**
     * Encodes `value`.
     *
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: Long): JsonStream {
        writeDeferredName()
        beforeValue()
        out.write(value.toString())
        return this
    }

    /**
     * Encodes `value`.
     *
     * @param value a finite value. May not be [NaNs][Double.isNaN] or
     * [infinities][Double.isInfinite].
     * @return this writer.
     */
    @Throws(IOException::class)
    fun value(value: Number?): JsonStream {
        if (value == null) {
            return nullValue()
        }

        val string = value.toString()
        if (!this.isLenient && (string == "-Infinity" || string == "Infinity" || string == "NaN")) {
            // omit this value
            deferredName = null
        } else {
            writeDeferredName()
            beforeValue()
            out.write(string)
        }
        return this
    }

    /**
     * Ensures all buffered data is written to the underlying [Writer]
     * and flushes that writer.
     */
    @Throws(IOException::class)
    override fun flush() {
        check(stackSize != 0) { "JsonStream is closed." }
        out.flush()
    }

    /**
     * Flushes and closes this writer and the underlying [Writer].
     *
     * @throws IOException if the JSON document is incomplete.
     */
    @Throws(IOException::class)
    override fun close() {
        out.close()

        val size = stackSize
        if (size > 1 || size == 1 && stack[size - 1] != JsonScope.NONEMPTY_DOCUMENT) {
            throw IOException("Incomplete document")
        }
        stackSize = 0
    }

    @Throws(IOException::class)
    private fun string(value: String) {
        val replacements: Array<String?> =
            if (this.isHtmlSafe) HTML_SAFE_REPLACEMENT_CHARS else REPLACEMENT_CHARS
        out.write("\"")
        var last = 0
        val length = value.length
        for (i in 0 until length) {
            val c = value[i]
            val replacement: String?
            if (c.code < 128) {
                replacement = replacements[c.code]
                if (replacement == null) {
                    continue
                }
            } else if (c == '\u2028') {
                replacement = "\\u2028"
            } else if (c == '\u2029') {
                replacement = "\\u2029"
            } else {
                continue
            }
            if (last < i) {
                out.write(value, last, i - last)
            }
            out.write(replacement)
            last = i + 1
        }
        if (last < length) {
            out.write(value, last, length - last)
        }
        out.write("\"")
    }

    @Throws(IOException::class)
    private fun newline() {
        if (indent == null) {
            return
        }

        out.write("\n")
        var i = 1
        val size = stackSize
        while (i < size) {
            out.write(indent)
            i++
        }
    }

    /**
     * Inserts any necessary separators and whitespace before a name. Also
     * adjusts the stack to expect the name's value.
     */
    @Throws(IOException::class)
    private fun beforeName() {
        val context = peek()
        if (context == JsonScope.NONEMPTY_OBJECT) { // first in object
            out.write(','.code)
        } else check(context == JsonScope.EMPTY_OBJECT) { "Nesting problem." }
        newline()
        replaceTop(JsonScope.DANGLING_NAME)
    }

    /**
     * Inserts any necessary separators and whitespace before a literal value,
     * inline array, or inline object. Also adjusts the stack to expect either a
     * closing bracket or another element.
     */
    @Throws(IOException::class)
    private fun beforeValue() {
        when (peek()) {
            JsonScope.NONEMPTY_DOCUMENT -> {
                check(this.isLenient) { "JSON must have only one top-level value." }
                replaceTop(JsonScope.NONEMPTY_DOCUMENT)
            }

            JsonScope.EMPTY_DOCUMENT -> replaceTop(JsonScope.NONEMPTY_DOCUMENT)
            JsonScope.EMPTY_ARRAY -> {
                replaceTop(JsonScope.NONEMPTY_ARRAY)
                newline()
            }

            JsonScope.NONEMPTY_ARRAY -> {
                out.write(','.code)
                newline()
            }

            JsonScope.DANGLING_NAME -> {
                out.write(separator)
                replaceTop(JsonScope.NONEMPTY_OBJECT)
            }

            else -> throw IllegalStateException("Nesting problem.")
        }
    }

    /**
     * Serialises an arbitrary object as JSON, handling primitive types as well as
     * Collections, Maps, and arrays.
     */
    @Throws(IOException::class)
    fun value(obj: Any?, shouldRedactKeys: Boolean): JsonStream {
        if (obj is Streamable) {
            obj.toStream(this)
        } else {
            objectJsonStreamer.objectToStream(obj, this, shouldRedactKeys)
        }
        return this
    }

    /**
     * Serialises an arbitrary object as JSON, handling primitive types as well as
     * Collections, Maps, and arrays.
     */
    @Throws(IOException::class)
    fun value(obj: Any?): JsonStream {
        if (obj is File) {
            value(obj)
        } else {
            value(obj, false)
        }

        return this
    }

    /**
     * Writes a File (its content) into the stream
     */
    @Throws(IOException::class)
    fun value(file: File?): JsonStream {
        if (file == null || file.length() <= 0) {
            return this
        }

        out.flush()
        beforeValue() // add comma if in array

        // Copy the file contents onto the stream
        var input: Reader? = null
        try {
            val fis = FileInputStream(file)
            input = BufferedReader(InputStreamReader(fis, "UTF-8"))
            IOUtils.copy(input, out)
        } finally {
            IOUtils.closeQuietly(input)
        }

        out.flush()
        return this
    }

    interface Streamable {
        @Throws(IOException::class)
        fun toStream(writer: JsonStream)
    }

    internal companion object {
        /*
         * From RFC 7159, "All Unicode characters may be placed within the
         * quotation marks except for the characters that must be escaped:
         * quotation mark, reverse solidus, and the control characters
         * (U+0000 through U+001F)."
         *
         * We also escape '\u2028' and '\u2029', which JavaScript interprets as
         * newline characters. This prevents eval() from failing with a syntax
         * error. http://code.google.com/p/google-gson/issues/detail?id=341
         */
        @JvmStatic
        internal val REPLACEMENT_CHARS: Array<String?>

        @JvmStatic
        internal val HTML_SAFE_REPLACEMENT_CHARS: Array<String?>

        init {
            REPLACEMENT_CHARS = arrayOfNulls<String>(128)
            for (i in 0..0x1f) {
                REPLACEMENT_CHARS[i] = "\\u%04x".format(i)
            }
            REPLACEMENT_CHARS['"'.code] = "\\\""
            REPLACEMENT_CHARS['\\'.code] = "\\\\"
            REPLACEMENT_CHARS['\t'.code] = "\\t"
            REPLACEMENT_CHARS['\b'.code] = "\\b"
            REPLACEMENT_CHARS['\n'.code] = "\\n"
            REPLACEMENT_CHARS['\r'.code] = "\\r"
            REPLACEMENT_CHARS['\u000c'.code] = "\\f"
            HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone()
            HTML_SAFE_REPLACEMENT_CHARS['<'.code] = "\\u003c"
            HTML_SAFE_REPLACEMENT_CHARS['>'.code] = "\\u003e"
            HTML_SAFE_REPLACEMENT_CHARS['&'.code] = "\\u0026"
            HTML_SAFE_REPLACEMENT_CHARS['='.code] = "\\u003d"
            HTML_SAFE_REPLACEMENT_CHARS['\''.code] = "\\u0027"
        }
    }
}
