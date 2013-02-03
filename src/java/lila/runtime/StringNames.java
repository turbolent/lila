package lila.runtime;

/*
  Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  - Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  - Neither the name of Sun Microsystems nor the names of its
  contributors may be used to endorse or promote products derived
  from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Universal mangling rules for the JVM.
 *
 * @author John Rose
 * @version 1.1, 03/03/08
 * @see http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
 *
 */
public class StringNames {
    private StringNames() { }  // static only class

    /** Given a source name, produce the corresponding bytecode name.
     */
    public static String toBytecodeName(String s) {
        String bn = mangle(s);
        assert(bn == s || looksMangled(bn)) : bn;
        assert(s.equals(toSourceName(bn))) : s;
        return bn;
    }

    /** Given a bytecode name, produce the corresponding source name.
     */
    public static String toSourceName(String s) {
        assert(isSafeBytecodeName(s)) : s;
        String sn = s;
        if (looksMangled(s)) {
            sn = demangle(s);
            assert(s.equals(mangle(sn))) : s+" => "+sn+" => "+mangle(sn);
        }
        return sn;
    }

    /** Given a bytecode name, produce the corresponding display name.
     *  This is the source name, plus quotes if needed.
     */
    public static String toDisplayName(String s) {
        if (isSafeBytecodeName(s)) {
            boolean isuid = Character.isUnicodeIdentifierStart(s.charAt(0));
            for (int i = 1, slen = s.length(); i < slen; i++) {
                if (!Character.isUnicodeIdentifierPart(s.charAt(0)))
                    { isuid = false; break; }
            }
            if (isuid)
                return s;
            String ss = toSourceName(s);
            if (s.equals(toBytecodeName(ss)))
                return quoteDisplay(ss);
        }
        // Try to demangle a prefix, up to the first dangerous char.
        int dci = indexOfDangerousChar(s, 0);
        if (dci > 0) {
            // At least try to demangle a prefix.
            String p = s.substring(0, dci);
            String ps = toSourceName(p);
            if (p.equals(toBytecodeName(ps))) {
                String t = s.substring(dci+1);
                return quoteDisplay(toSourceName(p)) + s.charAt(dci) + (t.equals("") ? "" : toDisplayName(t));
            }
        }
        return "?"+quoteDisplay(s);
    }
    private static String quoteDisplay(String s) {
        // TO DO:  Replace wierd characters in s by C-style escapes.
        return "'"+s.replaceAll("['\\\\]", "\\\\$0")+"'";
    }

    private static boolean isSafeBytecodeName(String s) {
        if (s.length() == 0)  return false;
        // check occurrences of each DANGEROUS char
        for (char xc : DANGEROUS_CHARS_A) {
            if (xc == ESCAPE_C)  continue;  // not really that dangerous
            if (s.indexOf(xc) >= 0)  return false;
        }
        return true;
    }

    private static boolean looksMangled(String s) {
        return s.charAt(0) == ESCAPE_C;
    }

    private static String mangle(String s) {
        if (s.length() == 0)
            return NULL_ESCAPE;

        // build this lazily, when we first need an escape:
        StringBuilder sb = null;

        for (int i = 0, slen = s.length(); i < slen; i++) {
            char c = s.charAt(i);

            boolean needEscape = false;
            if (c == ESCAPE_C) {
                if (i+1 < slen) {
                    char c1 = s.charAt(i+1);
                    if ((i == 0 && c1 == NULL_ESCAPE_C)
                        || c1 != originalOfReplacement(c1)) {
                        // an accidental escape
                        needEscape = true;
                    }
                }
            } else {
                needEscape = isDangerous(c);
            }

            if (!needEscape) {
                if (sb != null)  sb.append(c);
                continue;
            }

            // build sb if this is the first escape
            if (sb == null) {
                sb = new StringBuilder(s.length()+10);
                // mangled names must begin with a backslash:
                if (s.charAt(0) != ESCAPE_C && i > 0)
                    sb.append(NULL_ESCAPE);
                // append the string so far, which is unremarkable:
                sb.append(s.substring(0, i));
            }

            // rewrite \ to \-, / to \|, etc.
            sb.append(ESCAPE_C);
            sb.append(replacementOf(c));
        }

        if (sb != null)   return sb.toString();

        return s;
    }

    private static String demangle(String s) {
        // build this lazily, when we first meet an escape:
        StringBuilder sb = null;

        int stringStart = 0;
        if (s.startsWith(NULL_ESCAPE))
            stringStart = 2;

        for (int i = stringStart, slen = s.length(); i < slen; i++) {
            char c = s.charAt(i);

            if (c == ESCAPE_C && i+1 < slen) {
                // might be an escape sequence
                char rc = s.charAt(i+1);
                char oc = originalOfReplacement(rc);
                if (oc != rc) {
                    // build sb if this is the first escape
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                        // append the string so far, which is unremarkable:
                        sb.append(s.substring(stringStart, i));
                    }
                    ++i;  // skip both characters
                    c = oc;
                }
            }

            if (sb != null)
                sb.append(c);
        }

        if (sb != null)   return sb.toString();

        return s.substring(stringStart);
    }

    static char ESCAPE_C = '\\';
    // empty escape sequence to avoid a null name or illegal prefix
    static char NULL_ESCAPE_C = '=';
    static String NULL_ESCAPE = ESCAPE_C+""+NULL_ESCAPE_C;

    static String DANGEROUS_CHARS     = ".;:$[]<>/\\";
    static String REPLACEMENT_CHARS   = ",?!%{}^_|-";
    static char[] DANGEROUS_CHARS_A   = DANGEROUS_CHARS.toCharArray();
    static char[] REPLACEMENT_CHARS_A = REPLACEMENT_CHARS.toCharArray();

    static final long[] SPECIAL_BITMAP = new long[2];  // 128 bits
    static {
        String SPECIAL = DANGEROUS_CHARS + REPLACEMENT_CHARS + ESCAPE_C;
        //System.out.println("SPECIAL = "+SPECIAL);
        for (char c : SPECIAL.toCharArray()) {
            SPECIAL_BITMAP[c >>> 6] |= 1L << c;
        }
    }
    static boolean isSpecial(char c) {
        if ((c >>> 6) < SPECIAL_BITMAP.length)
            return ((SPECIAL_BITMAP[c >>> 6] >> c) & 1) != 0;
        else
            return false;
    }
    static char replacementOf(char c) {
        if (!isSpecial(c))  return c;
        int i = DANGEROUS_CHARS.indexOf(c);
        if (i < 0)  return c;
        return REPLACEMENT_CHARS.charAt(i);
    }
    static char originalOfReplacement(char c) {
        if (!isSpecial(c))  return c;
        int i = REPLACEMENT_CHARS.indexOf(c);
        if (i < 0)  return c;
        return DANGEROUS_CHARS.charAt(i);
    }
    static boolean isDangerous(char c) {
        if (!isSpecial(c))  return false;
        if (c == ESCAPE_C)  return false;  // not really dangerous
        return (DANGEROUS_CHARS.indexOf(c) >= 0);
    }
    static int indexOfDangerousChar(String s, int from) {
        for (int i = from, slen = s.length(); i < slen; i++) {
            if (isDangerous(s.charAt(i)))
                return i;
        }
        return -1;
    }
}
