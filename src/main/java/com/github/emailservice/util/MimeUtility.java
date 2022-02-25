/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.emailservice.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to decode MIME texts.
 *
 * @since 1.3
 */
public final class MimeUtility {

    /**
     * The {@code US-ASCII} charset identifier constant.
     */
    private static final String US_ASCII_CHARSET = "US-ASCII";

    /**
     * The marker to indicate text is encoded with BASE64 algorithm.
     */
    private static final String BASE64_ENCODING_MARKER = "B";

    /**
     * The marker to indicate text is encoded with QuotedPrintable algorithm.
     */
    private static final String QUOTEDPRINTABLE_ENCODING_MARKER = "Q";

    /**
     * If the text contains any encoded tokens, those tokens will be marked with "=?".
     */
    private static final String ENCODED_TOKEN_MARKER = "=?";

    /**
     * If the text contains any encoded tokens, those tokens will terminate with "=?".
     */
    private static final String ENCODED_TOKEN_FINISHER = "?=";

    /**
     * The linear whitespace chars sequence.
     */
    private static final String LINEAR_WHITESPACE = " \t\r\n";

    /**
     * Mappings between MIME and Java charset.
     */
    private static final Map<String, String> MIME2JAVA = new HashMap<>();

    static {
        MIME2JAVA.put("ja_jp.iso2022-7", "ISO2022JP");
        MIME2JAVA.put("ja_jp.eucjp", "EUCJIS");
        MIME2JAVA.put("x-us-ascii", "ISO-8859-1");
    }

    /**
     * Hidden constructor, this class must not be instantiated.
     */
    private MimeUtility() {
        // do nothing
    }

    /**
     * Decode a string of text obtained from a mail header into
     * its proper form.  The text generally will consist of a
     * string of tokens, some of which may be encoded using
     * base64 encoding.
     *
     * @param text   The text to decode.
     *
     * @return The decoded text string.
     * @throws UnsupportedEncodingException if the detected encoding in the input text is not supported.
     */
    public static String decodeText(final String text) throws UnsupportedEncodingException {
        // if the text contains any encoded tokens, those tokens will be marked with "=?".  If the
        // source string doesn't contain that sequent, no decoding is required.
        if (!text.contains(ENCODED_TOKEN_MARKER)) {
            return text;
        }

        int offset = 0;
        final int endOffset = text.length();

        int startWhiteSpace = -1;
        int endWhiteSpace = -1;

        final StringBuilder decodedText = new StringBuilder(text.length());

        boolean previousTokenEncoded = false;

        while (offset < endOffset) {
            char ch = text.charAt(offset);

            // is this a whitespace character?
            if (LINEAR_WHITESPACE.indexOf(ch) != -1) { // whitespace found
                startWhiteSpace = offset;
                while (offset < endOffset) {
                    // step over the white space characters.
                    ch = text.charAt(offset);
                    if (LINEAR_WHITESPACE.indexOf(ch) != -1) { // whitespace found
                        offset++;
                    } else {
                        // record the location of the first non lwsp and drop down to process the
                        // token characters.
                        endWhiteSpace = offset;
                        break;
                    }
                }
            } else {
                // we have a word token.  We need to scan over the word and then try to parse it.
                final int wordStart = offset;

                while (offset < endOffset) {
                    // step over the non white space characters.
                    ch = text.charAt(offset);
                    if (LINEAR_WHITESPACE.indexOf(ch) == -1) { // not white space
                        offset++;
                    } else {
                        break;
                    }

                    //NB:  Trailing whitespace on these header strings will just be discarded.
                }
                // pull out the word token.
                final String word = text.substring(wordStart, offset);
                // is the token encoded?  decode the word
                if (word.startsWith(ENCODED_TOKEN_MARKER)) {
                    try {
                        // if this gives a parsing failure, treat it like a non-encoded word.
                        final String decodedWord = decodeWord(word);

                        // are any whitespace characters significant?  Append 'em if we've got 'em.
                        if (!previousTokenEncoded && startWhiteSpace != -1) {
                            decodedText.append(text.substring(startWhiteSpace, endWhiteSpace));
                            startWhiteSpace = -1;
                        }
                        // this is definitely a decoded token.
                        previousTokenEncoded = true;
                        // and add this to the text.
                        decodedText.append(decodedWord);
                        // we continue parsing from here...we allow parsing errors to fall through
                        // and get handled as normal text.
                        continue;

                    } catch (final IllegalArgumentException e) {
                        // just ignore it, skip to next word
                    }
                }
                // this is a normal token, so it doesn't matter what the previous token was.  Add the white space
                // if we have it.
                if (startWhiteSpace != -1) {
                    decodedText.append(text, startWhiteSpace, endWhiteSpace);
                    startWhiteSpace = -1;
                }
                // this is not a decoded token.
                previousTokenEncoded = false;
                decodedText.append(word);
            }
        }

        return decodedText.toString();
    }

    /**
     * Parse a string using the RFC 2047 rules for an "encoded-word"
     * type.  This encoding has the syntax:
     *
     * encoded-word = "=?" charset "?" encoding "?" encoded-text "?="
     *
     * @param word   The possibly encoded word value.
     *
     * @return The decoded word.
     * @throws IllegalArgumentException
     * @throws UnsupportedEncodingException
     */
    private static String decodeWord(final String word) throws IllegalArgumentException, UnsupportedEncodingException {
        // encoded words start with the characters "=?".  If this not an encoded word, we throw a
        // ParseException for the caller.

        if (!word.startsWith(ENCODED_TOKEN_MARKER)) {
            throw new IllegalArgumentException("Invalid RFC 2047 encoded-word: " + word);
        }

        final int charsetPos = word.indexOf('?', 2);
        if (charsetPos == -1) {
            throw new IllegalArgumentException("Missing charset in RFC 2047 encoded-word: " + word);
        }

        // pull out the character set information (this is the MIME name at this point).
        final String charset = word.substring(2, charsetPos).toLowerCase(Locale.ENGLISH);

        // now pull out the encoding token the same way.
        final int encodingPos = word.indexOf('?', charsetPos + 1);
        if (encodingPos == -1) {
            throw new IllegalArgumentException("Missing encoding in RFC 2047 encoded-word: " + word);
        }

        final String encoding = word.substring(charsetPos + 1, encodingPos);

        // and finally the encoded text.
        final int encodedTextPos = word.indexOf(ENCODED_TOKEN_FINISHER, encodingPos + 1);
        if (encodedTextPos == -1) {
            throw new IllegalArgumentException("Missing encoded text in RFC 2047 encoded-word: " + word);
        }

        final String encodedText = word.substring(encodingPos + 1, encodedTextPos);

        // seems a bit silly to encode a null string, but easy to deal with.
        if (encodedText.length() == 0) {
            return "";
        }

        try {
            // the decoder writes directly to an output stream.
            final ByteArrayOutputStream out = new ByteArrayOutputStream(encodedText.length());

            byte[] decodedData;
            // Base64 encoded?
            if (encoding.equals(BASE64_ENCODING_MARKER)) {
                decodedData = Base64.getDecoder().decode(encodedText.getBytes(Charset.forName("utf-8")));
            } else if (encoding.equals(QUOTEDPRINTABLE_ENCODING_MARKER)) { // maybe quoted printable.
                byte[] encodedData = encodedText.getBytes(US_ASCII_CHARSET);
                decode(encodedData, out);
                decodedData = out.toByteArray();
            } else {
                throw new UnsupportedEncodingException("Unknown RFC 2047 encoding: " + encoding);
            }
            // Convert decoded byte data into a string.
            return new String(decodedData, javaCharset(charset));
        } catch (final IOException e) {
            throw new UnsupportedEncodingException("Invalid RFC 2047 encoding");
        }
    }

    /**
     * Translate a MIME standard character set name into the Java
     * equivalent.
     *
     * @param charset The MIME standard name.
     *
     * @return The Java equivalent for this name.
     */
    private static String javaCharset(final String charset) {
        // nothing in, nothing out.
        if (charset == null) {
            return null;
        }

        final String mappedCharset = MIME2JAVA.get(charset.toLowerCase(Locale.ENGLISH));
        // if there is no mapping, then the original name is used.  Many of the MIME character set
        // names map directly back into Java.  The reverse isn't necessarily true.
        if (mappedCharset == null) {
            return charset;
        }
        return mappedCharset;
    }

    /**
     * The shift value required to create the upper nibble
     * from the first of 2 byte values converted from ascii hex.
     */
    private static final int UPPER_NIBBLE_SHIFT = Byte.SIZE / 2;

    /**
     * Decode the encoded byte data writing it to the given output stream.
     *
     * @param data   The array of byte data to decode.
     * @param out    The output stream used to return the decoded data.
     *
     * @return the number of bytes produced.
     * @throws IOException if a problem occurs during either decoding or
     *            writing to the stream
     */
    public static int decode(final byte[] data, final OutputStream out) throws IOException {
        int off = 0;
        final int length = data.length;
        final int endOffset = off + length;
        int bytesWritten = 0;

        while (off < endOffset) {
            final byte ch = data[off++];

            // space characters were translated to '_' on encode, so we need to translate them back.
            if (ch == '_') {
                out.write(' ');
            } else if (ch == '=') {
                // we found an encoded character.  Reduce the 3 char sequence to one.
                // but first, make sure we have two characters to work with.
                if (off + 1 >= endOffset) {
                    throw new IOException("Invalid quoted printable encoding; truncated escape sequence");
                }

                final byte b1 = data[off++];
                final byte b2 = data[off++];

                // we've found an encoded carriage return.  The next char needs to be a newline
                if (b1 == '\r') {
                    if (b2 != '\n') {
                        throw new IOException("Invalid quoted printable encoding; CR must be followed by LF");
                    }
                    // this was a soft linebreak inserted by the encoding.  We just toss this away
                    // on decode.
                } else {
                    // this is a hex pair we need to convert back to a single byte.
                    final int c1 = hexToBinary(b1);
                    final int c2 = hexToBinary(b2);
                    out.write((c1 << UPPER_NIBBLE_SHIFT) | c2);
                    // 3 bytes in, one byte out
                    bytesWritten++;
                }
            } else {
                // simple character, just write it out.
                out.write(ch);
                bytesWritten++;
            }
        }

        return bytesWritten;
    }

    /**
     * Convert a hex digit to the binary value it represents.
     *
     * @param b the ascii hex byte to convert (0-0, A-F, a-f)
     * @return the int value of the hex byte, 0-15
     * @throws IOException if the byte is not a valid hex digit.
     */
    private static int hexToBinary(final byte b) throws IOException {
        // CHECKSTYLE IGNORE MagicNumber FOR NEXT 1 LINE
        final int i = Character.digit((char) b, 16);
        if (i == -1) {
            throw new IOException("Invalid quoted printable encoding: not a valid hex digit: " + b);
        }
        return i;
    }

}
