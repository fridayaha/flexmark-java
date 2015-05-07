package org.commonmark.internal;

import org.commonmark.internal.util.Escaping;
import org.commonmark.internal.util.Html5Entities;
import org.commonmark.node.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineParser {

    private static final char C_NEWLINE = '\n';
    private static final char C_ASTERISK = '*';
    private static final char C_UNDERSCORE = '_';
    private static final char C_BACKTICK = '`';
    private static final char C_OPEN_BRACKET = '[';
    private static final char C_CLOSE_BRACKET = ']';
    private static final char C_LESSTHAN = '<';
    private static final char C_BANG = '!';
    private static final char C_BACKSLASH = '\\';
    private static final char C_AMPERSAND = '&';
    private static final char C_OPEN_PAREN = '(';
    private static final char C_CLOSE_PAREN = ')';
    private static final char C_COLON = ':';

    private static final String ESCAPED_CHAR = "\\\\" + Escaping.ESCAPABLE;
    private static final String REG_CHAR = "[^\\\\()\\x00-\\x20]";
    private static final String IN_PARENS_NOSP = "\\((" + REG_CHAR + '|' + ESCAPED_CHAR + ")*\\)";
    private static final String TAGNAME = "[A-Za-z][A-Za-z0-9]*";
    private static final String ATTRIBUTENAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*";
    private static final String UNQUOTEDVALUE = "[^\"'=<>`\\x00-\\x20]+";
    private static final String SINGLEQUOTEDVALUE = "'[^']*'";
    private static final String DOUBLEQUOTEDVALUE = "\"[^\"]*\"";
    private static final String ATTRIBUTEVALUE = "(?:" + UNQUOTEDVALUE + "|" + SINGLEQUOTEDVALUE
            + "|" + DOUBLEQUOTEDVALUE + ")";
    private static final String ATTRIBUTEVALUESPEC = "(?:" + "\\s*=" + "\\s*" + ATTRIBUTEVALUE
            + ")";
    private static final String ATTRIBUTE = "(?:" + "\\s+" + ATTRIBUTENAME + ATTRIBUTEVALUESPEC
            + "?)";
    private static final String OPENTAG = "<" + TAGNAME + ATTRIBUTE + "*" + "\\s*/?>";
    private static final String CLOSETAG = "</" + TAGNAME + "\\s*[>]";
    private static final String HTMLCOMMENT = "<!---->|<!--(?:-?[^>-])(?:-?[^-])*-->";
    private static final String PROCESSINGINSTRUCTION = "[<][?].*?[?][>]";
    private static final String DECLARATION = "<![A-Z]+" + "\\s+[^>]*>";
    private static final String CDATA = "<!\\[CDATA\\[[\\s\\S]*?\\]\\]>";
    private static final String HTMLTAG = "(?:" + OPENTAG + "|" + CLOSETAG + "|" + HTMLCOMMENT
            + "|" + PROCESSINGINSTRUCTION + "|" + DECLARATION + "|" + CDATA + ")";
    private static final String ENTITY = "&(?:#x[a-f0-9]{1,8}|#[0-9]{1,8}|[a-z][a-z0-9]{1,31});";

    private static final String ASCII_PUNCTUATION = "'!\"#\\$%&\\(\\)\\*\\+,\\-\\./:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~";
    private static final Pattern PUNCTUATION = Pattern
            .compile("^[" + ASCII_PUNCTUATION + "\\p{Pc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}]");

    private static final Pattern HTML_TAG = Pattern.compile('^' + HTMLTAG, Pattern.CASE_INSENSITIVE);

    private static final Pattern LINK_TITLE = Pattern.compile(
            "^(?:\"(" + ESCAPED_CHAR + "|[^\"\\x00])*\"" +
                    '|' +
                    "'(" + ESCAPED_CHAR + "|[^'\\x00])*'" +
                    '|' +
                    "\\((" + ESCAPED_CHAR + "|[^)\\x00])*\\))");

    private static final Pattern LINK_DESTINATION_BRACES = Pattern.compile(
            "^(?:[<](?:[^<>\\n\\\\\\x00]" + '|' + ESCAPED_CHAR + '|' + "\\\\)*[>])");

    private static final Pattern LINK_DESTINATION = Pattern.compile(
            "^(?:" + REG_CHAR + "+|" + ESCAPED_CHAR + '|' + IN_PARENS_NOSP + ")*");

    private static final Pattern LINK_LABEL = Pattern
            .compile("^\\[(?:[^\\\\\\[\\]]|\\\\[\\[\\]]){0,1000}\\]");

    private static final Pattern ESCAPABLE = Pattern.compile(Escaping.ESCAPABLE);

    private static final Pattern ENTITY_HERE = Pattern.compile('^' + ENTITY, Pattern.CASE_INSENSITIVE);

    private static final Pattern TICKS = Pattern.compile("`+");

    private static final Pattern TICKS_HERE = Pattern.compile("^`+");

    private static final Pattern EMAIL_AUTOLINK = Pattern
            .compile("^<([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>");

    private static final Pattern AUTOLINK = Pattern
            .compile("^<(?:coap|doi|javascript|aaa|aaas|about|acap|cap|cid|crid|data|dav|dict|dns|file|ftp|geo|go|gopher|h323|http|https|iax|icap|im|imap|info|ipp|iris|iris.beep|iris.xpc|iris.xpcs|iris.lwz|ldap|mailto|mid|msrp|msrps|mtqp|mupdate|news|nfs|ni|nih|nntp|opaquelocktoken|pop|pres|rtsp|service|session|shttp|sieve|sip|sips|sms|snmp|soap.beep|soap.beeps|tag|tel|telnet|tftp|thismessage|tn3270|tip|tv|urn|vemmi|ws|wss|xcon|xcon-userid|xmlrpc.beep|xmlrpc.beeps|xmpp|z39.50r|z39.50s|adiumxtra|afp|afs|aim|apt|attachment|aw|beshare|bitcoin|bolo|callto|chrome|chrome-extension|com-eventbrite-attendee|content|cvs|dlna-playsingle|dlna-playcontainer|dtn|dvb|ed2k|facetime|feed|finger|fish|gg|git|gizmoproject|gtalk|hcp|icon|ipn|irc|irc6|ircs|itms|jar|jms|keyparc|lastfm|ldaps|magnet|maps|market|message|mms|ms-help|msnim|mumble|mvn|notes|oid|palm|paparazzi|platform|proxy|psyc|query|res|resource|rmi|rsync|rtmp|secondlife|sftp|sgn|skype|smb|soldat|spotify|ssh|steam|svn|teamspeak|things|udp|unreal|ut2004|ventrilo|view-source|webcal|wtai|wyciwyg|xfire|xri|ymsgr):[^<>\u0000-\u0020]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPNL = Pattern.compile("^ *(?:\n *)?");

    private static final Pattern WHITESPACE_CHAR = Pattern.compile("^\\p{IsWhite_Space}");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern FINAL_SPACE = Pattern.compile(" *$");

    private static final Pattern LINE_END = Pattern.compile("^ *(?:\n|$)");

    private static final Pattern INITIAL_SPACE = Pattern.compile("^ *");

    /**
     * Matches a string of non-special characters.
     */
    private static final Pattern MAIN = Pattern.compile("^[^\n`\\[\\]\\\\!<&*_]+");

    private String subject = "";
    private int pos = 0;

    /**
     * Stack of delimiters (emphasis, strong emphasis).
     */
    private Delimiter delimiter;
    private Map<String, Link> referenceMap = new HashMap<>();

    /**
     * Parse content in block into inline children, using referenceMap to resolve referenceMap.
     */
    public void parse(Node block, String content) {
        this.subject = content.trim();
        this.pos = 0;
        this.delimiter = null;

        boolean moreToParse;
        do {
            moreToParse = parseInline(block);
        } while (moreToParse);

        processEmphasis(null);
    }

    /**
     * Attempt to parse a link reference, modifying refmap.
     *
     * @return how many characters were parsed as a reference, {@code 0} if none
     */
    public int parseReference(String s) {
        this.subject = s;
        this.pos = 0;
        String rawLabel;
        String dest;
        String title;
        int matchChars;
        int startPos = this.pos;

        // label:
        matchChars = this.parseLinkLabel();
        if (matchChars == 0) {
            return 0;
        } else {
            rawLabel = this.subject.substring(0, matchChars);
        }

        // colon:
        if (this.peek() == C_COLON) {
            this.pos++;
        } else {
            this.pos = startPos;
            return 0;
        }

        // link url
        this.spnl();

        dest = this.parseLinkDestination();
        if (dest == null || dest.length() == 0) {
            this.pos = startPos;
            return 0;
        }

        int beforeTitle = this.pos;
        this.spnl();
        title = this.parseLinkTitle();
        if (title == null) {
            // rewind before spaces
            this.pos = beforeTitle;
        }

        // make sure we're at line end:
        if (this.pos != this.subject.length() && this.match(LINE_END) == null) {
            this.pos = startPos;
            return 0;
        }

        String normalizedLabel = Escaping.normalizeReference(rawLabel);

        if (!referenceMap.containsKey(normalizedLabel)) {
            Link link = new Link(dest, title);
            referenceMap.put(normalizedLabel, link);
        }
        return this.pos - startPos;
    }

    private static Text text(String s) {
        Text node = new Text();
        node.setLiteral(s);
        return node;
    }

    /**
     * Parse the next inline element in subject, advancing subject position.
     * On success, add the result to block's children and return true.
     * On failure, return false.
     */
    private boolean parseInline(Node block) {
        boolean res;
        char c = this.peek();
        if (c == '\0') {
            return false;
        }
        switch (c) {
            case C_NEWLINE:
                res = this.parseNewline(block);
                break;
            case C_BACKSLASH:
                res = this.parseBackslash(block);
                break;
            case C_BACKTICK:
                res = this.parseBackticks(block);
                break;
            case C_ASTERISK:
            case C_UNDERSCORE:
                res = this.parseEmphasis(c, block);
                break;
            case C_OPEN_BRACKET:
                res = this.parseOpenBracket(block);
                break;
            case C_BANG:
                res = this.parseBang(block);
                break;
            case C_CLOSE_BRACKET:
                res = this.parseCloseBracket(block);
                break;
            case C_LESSTHAN:
                res = this.parseAutolink(block) || this.parseHtmlTag(block);
                break;
            case C_AMPERSAND:
                res = this.parseEntity(block);
                break;
            default:
                res = this.parseString(block);
                break;
        }
        if (!res) {
            this.pos += 1;
            // When we get here, it's only for a single special character that turned out to not have a special meaning.
            // So we shouldn't have a single surrogate here, hence it should be ok to turn it into a String.
            String literal = String.valueOf(c);
            block.appendChild(text(literal));
        }

        return true;
    }

    /**
     * If re matches at current position in the subject, advance position in subject and return the match; otherwise
     * return null.
     */
    private String match(Pattern re) {
        if (this.pos >= this.subject.length()) {
            return null;
        }
        Matcher matcher = re.matcher(this.subject.substring(this.pos));
        boolean m = matcher.find();
        if (m) {
            this.pos += matcher.end();
            return matcher.group();
        } else {
            return null;
        }
    }

    /**
     * Returns the char at the current subject position, or {@code '\0'} in case there are no more characters.
     */
    private char peek() {
        if (this.pos < this.subject.length()) {
            return this.subject.charAt(this.pos);
        } else {
            return '\0';
        }
    }

    /**
     * Parse zero or more space characters, including at most one newline.
     */
    private boolean spnl() {
        this.match(SPNL);
        return true;
    }

    /**
     * Parse a newline. If it was preceded by two spaces, return a hard line break; otherwise a soft line break.
     */
    private boolean parseNewline(Node block) {
        this.pos += 1; // assume we're at a \n
        // check previous node for trailing spaces
        Node lastChild = block.getLastChild();
        if (lastChild != null && lastChild instanceof Text) {
            Text text = (Text) lastChild;
            Matcher matcher = FINAL_SPACE.matcher(text.getLiteral());
            int sps = matcher.find() ? matcher.end() - matcher.start() : 0;
            if (sps > 0) {
                text.setLiteral(matcher.replaceAll(""));
            }
            block.appendChild(sps >= 2 ? new HardLineBreak() : new SoftLineBreak());
        } else {
            block.appendChild(new SoftLineBreak());
        }
        this.match(INITIAL_SPACE); // gobble leading spaces in next line
        return true;
    }

    /**
     * Parse a backslash-escaped special character, adding either the escaped  character, a hard line break
     * (if the backslash is followed by a newline), or a literal backslash to the block's children.
     */
    private boolean parseBackslash(Node block) {
        String subj = this.subject;
        int pos = this.pos;
        Node node;
        if (subj.charAt(pos) == C_BACKSLASH) {
            int next = pos + 1;
            if (next < subj.length() && subj.charAt(next) == '\n') {
                this.pos = this.pos + 2;
                node = new HardLineBreak();
                block.appendChild(node);
            } else if (next < subj.length() && ESCAPABLE.matcher(subj.substring(next, next + 1)).matches()) {
                this.pos = this.pos + 2;
                block.appendChild(text(subj.substring(next, next + 1)));
            } else {
                this.pos++;
                block.appendChild(text("\\"));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempt to parse backticks, adding either a backtick code span or a literal sequence of backticks.
     */
    private boolean parseBackticks(Node block) {
        String ticks = this.match(TICKS_HERE);
        if (ticks == null) {
            return false;
        }
        int afterOpenTicks = this.pos;
        String matched;
        while ((matched = this.match(TICKS)) != null) {
            if (matched.equals(ticks)) {
                Code node = new Code();
                String content = this.subject.substring(afterOpenTicks, this.pos - ticks.length());
                String literal = WHITESPACE.matcher(content.trim()).replaceAll(" ");
                node.setLiteral(literal);
                block.appendChild(node);
                return true;
            }
        }
        // If we got here, we didn't match a closing backtick sequence.
        this.pos = afterOpenTicks;
        block.appendChild(text(ticks));
        return true;
    }

    /**
     * Attempt to parse emphasis or strong emphasis.
     */
    private boolean parseEmphasis(char delimiterChar, Node block) {
        DelimiterRun res = this.scanDelims(delimiterChar);
        if (res == null) {
            return false;
        }
        int numDelims = res.numDelims;
        int startPos = this.pos;

        this.pos += numDelims;
        Text node = text(this.subject.substring(startPos, this.pos));
        block.appendChild(node);

        // Add entry to stack for this opener
        this.delimiter = new Delimiter(node, this.delimiter, startPos);
        this.delimiter.delimiterChar = delimiterChar;
        this.delimiter.numDelims = numDelims;
        this.delimiter.canOpen = res.canOpen;
        this.delimiter.canClose = res.canClose;
        this.delimiter.active = true;
        if (this.delimiter.previous != null) {
            this.delimiter.previous.next = this.delimiter;
        }

        return true;
    }

    /**
     * Add open bracket to delimiter stack and add a text node to block's children.
     */
    private boolean parseOpenBracket(Node block) {
        int startPos = this.pos;
        this.pos += 1;

        Text node = text("[");
        block.appendChild(node);

        // Add entry to stack for this opener
        this.delimiter = new Delimiter(node, this.delimiter, startPos);
        this.delimiter.delimiterChar = C_OPEN_BRACKET;
        this.delimiter.numDelims = 1;
        this.delimiter.canOpen = true;
        this.delimiter.canClose = false;
        this.delimiter.active = true;
        if (this.delimiter.previous != null) {
            this.delimiter.previous.next = this.delimiter;
        }

        return true;
    }

    /**
     * If next character is [, and ! delimiter to delimiter stack and add a text node to block's children.
     * Otherwise just add a text node.
     */
    private boolean parseBang(Node block) {
        int startPos = this.pos;
        this.pos += 1;
        if (this.peek() == C_OPEN_BRACKET) {
            this.pos += 1;

            Text node = text("![");
            block.appendChild(node);

            // Add entry to stack for this opener
            this.delimiter = new Delimiter(node, this.delimiter, startPos + 1);
            this.delimiter.delimiterChar = C_BANG;
            this.delimiter.numDelims = 1;
            this.delimiter.canOpen = true;
            this.delimiter.canClose = false;
            this.delimiter.active = true;
            if (this.delimiter.previous != null) {
                this.delimiter.previous.next = this.delimiter;
            }
        } else {
            block.appendChild(text("!"));
        }
        return true;
    }

    /**
     * Try to match close bracket against an opening in the delimiter stack. Add either a link or image, or a
     * plain [ character, to block's children. If there is a matching delimiter, remove it from the delimiter stack.
     */
    private boolean parseCloseBracket(Node block) {
        int startPos;
        boolean isImage;
        String dest = null;
        String title = null;
        boolean matched = false;
        String reflabel;
        Delimiter opener;

        this.pos += 1;
        startPos = this.pos;

        // look through stack of delimiters for a [ or ![
        opener = this.delimiter;

        while (opener != null) {
            if (opener.delimiterChar == C_OPEN_BRACKET || opener.delimiterChar == C_BANG) {
                break;
            }
            opener = opener.previous;
        }

        if (opener == null) {
            // no matched opener, just return a literal
            block.appendChild(text("]"));
            return true;
        }

        if (!opener.active) {
            // no matched opener, just return a literal
            block.appendChild(text("]"));
            // take opener off emphasis stack
            this.removeDelimiter(opener);
            return true;
        }

        // If we got here, open is a potential opener
        isImage = opener.delimiterChar == C_BANG;

        // Check to see if we have a link/image

        // Inline link?
        if (this.peek() == C_OPEN_PAREN) {
            this.pos++;
            this.spnl();
            if ((dest = this.parseLinkDestination()) != null) {
                this.spnl();
                // title needs a whitespace before
                if (WHITESPACE_CHAR.matcher(this.subject.substring(this.pos - 1, this.pos)).matches()) {
                    title = this.parseLinkTitle();
                    this.spnl();
                }
                if (this.subject.charAt(this.pos) == C_CLOSE_PAREN) {
                    this.pos += 1;
                    matched = true;
                }
            }
        } else {

            // Next, see if there's a link label
            int savepos = this.pos;
            this.spnl();
            int beforelabel = this.pos;
            int n = this.parseLinkLabel();
            if (n == 0 || n == 2) {
                // empty or missing second label
                reflabel = this.subject.substring(opener.index, startPos);
            } else {
                reflabel = this.subject.substring(beforelabel, beforelabel + n);
            }
            if (n == 0) {
                // If shortcut reference link, rewind before spaces we skipped.
                this.pos = savepos;
            }

            // lookup rawlabel in refmap
            Link link = referenceMap.get(Escaping.normalizeReference(reflabel));
            if (link != null) {
                dest = link.getDestination();
                title = link.getTitle();
                matched = true;
            }
        }

        if (matched) {
            Node node = isImage ? new Image(dest, title) : new Link(dest, title);

            Node tmp, next;
            tmp = opener.node.getNext();
            while (tmp != null) {
                next = tmp.getNext();
                node.appendChild(tmp);
                tmp = next;
            }
            block.appendChild(node);
            this.processEmphasis(opener.previous);

            opener.node.unlink();

            // processEmphasis will remove this and later delimiters.
            // Now, for a link, we also deactivate earlier link openers.
            // (no links in links)
            if (!isImage) {
                opener = this.delimiter;
                while (opener != null) {
                    if (opener.delimiterChar == C_OPEN_BRACKET) {
                        opener.active = false; // deactivate this opener
                    }
                    opener = opener.previous;
                }
            }

            return true;

        } else { // no match

            this.removeDelimiter(opener); // remove this opener from stack
            this.pos = startPos;
            block.appendChild(text("]"));
            return true;
        }
    }

    /**
     * Attempt to parse link destination, returning the string or null if no match.
     */
    private String parseLinkDestination() {
        String res = this.match(LINK_DESTINATION_BRACES);
        if (res != null) { // chop off surrounding <..>:
            if (res.length() == 2) {
                return "";
            } else {
                return Escaping.normalizeURI(Escaping.unescapeString(res.substring(1, res.length() - 1)));
            }
        } else {
            res = this.match(LINK_DESTINATION);
            if (res != null) {
                return Escaping.normalizeURI(Escaping.unescapeString(res));
            } else {
                return null;
            }
        }
    }

    /**
     * Attempt to parse link title (sans quotes), returning the string or null if no match.
     */
    private String parseLinkTitle() {
        String title = this.match(LINK_TITLE);
        if (title != null) {
            // chop off quotes from title and unescape:
            return Escaping.unescapeString(title.substring(1, title.length() - 1));
        } else {
            return null;
        }
    }

    /**
     * Attempt to parse a link label, returning number of characters parsed.
     */
    private int parseLinkLabel() {
        String m = this.match(LINK_LABEL);
        return m == null ? 0 : m.length();
    }

    /**
     * Attempt to parse an autolink (URL or email in pointy brackets).
     */
    private boolean parseAutolink(Node block) {
        String m;
        String dest;
        if ((m = this.match(EMAIL_AUTOLINK)) != null) {
            dest = m.substring(1, m.length() - 1);
            Link node = new Link(Escaping.normalizeURI("mailto:" + dest), null);
            node.appendChild(text(dest));
            block.appendChild(node);
            return true;
        } else if ((m = this.match(AUTOLINK)) != null) {
            dest = m.substring(1, m.length() - 1);
            Link node = new Link(Escaping.normalizeURI(dest), null);
            node.appendChild(text(dest));
            block.appendChild(node);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempt to parse a raw HTML tag.
     */
    private boolean parseHtmlTag(Node block) {
        String m = this.match(HTML_TAG);
        if (m != null) {
            HtmlTag node = new HtmlTag();
            node.setLiteral(m);
            block.appendChild(node);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempt to parse an entity, return Entity object if successful.
     */
    private boolean parseEntity(Node block) {
        String m;
        if ((m = this.match(ENTITY_HERE)) != null) {
            block.appendChild(text(Html5Entities.entityToString(m)));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parse a run of ordinary characters, or a single character with a special meaning in markdown, as a plain string.
     */
    private boolean parseString(Node block) {
        String m;
        if ((m = this.match(MAIN)) != null) {
            block.appendChild(text(m));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis. A utility
     * function for strong/emph parsing.
     *
     * @return information about delimiter run, or {@code null}
     */
    private DelimiterRun scanDelims(char delimiterChar) {
        int startPos = this.pos;

        int numDelims = 0;
        while (this.peek() == delimiterChar) {
            numDelims++;
            this.pos++;
        }

        if (numDelims == 0) {
            return null;
        }

        String charBefore = startPos == 0 ? "\n" :
                this.subject.substring(startPos - 1, startPos);

        char ccAfter = this.peek();
        String charAfter;
        if (ccAfter == '\0') {
            charAfter = "\n";
        } else {
            charAfter = String.valueOf(ccAfter);
        }

        boolean beforeIsPunctuation = PUNCTUATION.matcher(charBefore).matches();
        boolean beforeIsWhitespace = WHITESPACE_CHAR.matcher(charBefore).matches();
        boolean afterIsWhitespace = WHITESPACE_CHAR.matcher(charAfter).matches();
        boolean afterIsPunctuation = PUNCTUATION.matcher(charAfter).matches();

        boolean leftFlanking = !afterIsWhitespace &&
                !(afterIsPunctuation && !beforeIsWhitespace && !beforeIsPunctuation);
        boolean rightFlanking = !beforeIsWhitespace &&
                !(beforeIsPunctuation && !afterIsWhitespace && !afterIsPunctuation);
        boolean canOpen;
        boolean canClose;
        if (delimiterChar == C_UNDERSCORE) {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation);
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation);
        } else {
            canOpen = leftFlanking;
            canClose = rightFlanking;
        }

        this.pos = startPos;
        return new DelimiterRun(numDelims, canOpen, canClose);
    }

    private void processEmphasis(Delimiter stackBottom) {
        Delimiter opener, closer;
        Delimiter nextstack, tempstack;
        int useDelims;
        Node tmp, next;

        // find first closer above stackBottom:
        closer = this.delimiter;
        while (closer != null && closer.previous != stackBottom) {
            closer = closer.previous;
        }
        // move forward, looking for closers, and handling each
        while (closer != null) {
            if (closer.canClose && (closer.delimiterChar == C_UNDERSCORE || closer.delimiterChar == C_ASTERISK)) {
                // found emphasis closer. now look back for first matching opener:
                opener = closer.previous;
                while (opener != null && opener != stackBottom) {
                    if (opener.delimiterChar == closer.delimiterChar && opener.canOpen) {
                        break;
                    }
                    opener = opener.previous;
                }
                if (opener != null && opener != stackBottom) {
                    // calculate actual number of delimiters used from this closer
                    if (closer.numDelims < 3 || opener.numDelims < 3) {
                        useDelims = closer.numDelims <= opener.numDelims ?
                                closer.numDelims : opener.numDelims;
                    } else {
                        useDelims = closer.numDelims % 2 == 0 ? 2 : 1;
                    }

                    Text openerInl = opener.node;
                    Text closerInl = closer.node;

                    // remove used delimiters from stack elts and inlines
                    opener.numDelims -= useDelims;
                    closer.numDelims -= useDelims;
                    openerInl.setLiteral(
                            openerInl.getLiteral().substring(0,
                                    openerInl.getLiteral().length() - useDelims));
                    closerInl.setLiteral(
                            closerInl.getLiteral().substring(0,
                                    closerInl.getLiteral().length() - useDelims));

                    // build contents for new emph element
                    Node emph = useDelims == 1 ? new Emphasis() : new StrongEmphasis();

                    tmp = openerInl.getNext();
                    while (tmp != null && tmp != closerInl) {
                        next = tmp.getNext();
                        emph.appendChild(tmp);
                        tmp = next;
                    }

                    openerInl.insertAfter(emph);

                    // remove elts btw opener and closer in delimiters stack
                    tempstack = closer.previous;
                    while (tempstack != null && tempstack != opener) {
                        nextstack = tempstack.previous;
                        this.removeDelimiter(tempstack);
                        tempstack = nextstack;
                    }

                    // if opener has 0 delims, remove it and the inline
                    if (opener.numDelims == 0) {
                        openerInl.unlink();
                        this.removeDelimiter(opener);
                    }

                    if (closer.numDelims == 0) {
                        closerInl.unlink();
                        tempstack = closer.next;
                        this.removeDelimiter(closer);
                        closer = tempstack;
                    }

                } else {
                    closer = closer.next;
                }

            } else {
                closer = closer.next;
            }

        }

        // remove all delimiters
        while (this.delimiter != stackBottom) {
            this.removeDelimiter(this.delimiter);
        }
    }

    private void removeDelimiter(Delimiter delim) {
        if (delim.previous != null) {
            delim.previous.next = delim.next;
        }
        if (delim.next == null) {
            // top of stack
            this.delimiter = delim.previous;
        } else {
            delim.next.previous = delim.previous;
        }
    }

}
