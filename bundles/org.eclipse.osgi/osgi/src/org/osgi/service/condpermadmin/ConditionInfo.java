/*
 * $Header: /cvshome/repository/org/osgi/service/permissionadmin/PermissionInfo.java,v 1.9 2003/12/16 02:15:32 hargrave Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2001, 2002).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.service.condpermadmin;

import java.io.Serializable;
import java.util.Vector;

/**
 * Condition representation used by the Conditional Permission Admin service.
 * 
 * <p>
 * This class encapsulates two pieces of information: a Condition <i>type</i>
 * (class name), which must implement <tt>Condition</tt>, and the arguments
 * passed to its constructor.
 * 
 * <p>
 * In order for a Condition represented by a <tt>ConditionInfo</tt> to be
 * instantiated and considered during a permission check, its Condition class
 * must be available from the system classpath.
 * 
 */

/* TODO This Serializable can't stay here since it is part of the public class */
public class ConditionInfo implements Serializable {
	private static final long serialVersionUID = 3689354325134618934L;

	private String type;

	private String args[];

	/**
	 * Constructs a <tt>ConditionInfo</tt> from the given type and args.
	 * 
	 * @param type
	 *            The fully qualified class name of the condition represented by
	 *            this <tt>ConditionInfo</tt>. The class must implement
	 *            <tt>Condition</tt> and must define a constructor that takes
	 *            a <tt>Bundle</tt> and the correct number of argument
	 *            strings.
	 * 
	 * @param args
	 *            The arguments that will be passed to the constructor of the
	 *            <tt>Conditon</tt> class identified by <tt>type</tt>.
	 * 
	 * @exception java.lang.NullPointerException
	 *                if <tt>type</tt> is <tt>null</tt>.
	 */
	public ConditionInfo(String type, String args[]) {
		this.type = type;
		this.args = args;
		if (type == null) {
			throw new NullPointerException("type is null");
		}
	}

	/**
	 * Constructs a <tt>ConditionInfo</tt> object from the given encoded
	 * <tt>ConditionInfo</tt> string.
	 * 
	 * @param encodedCondition
	 *            The encoded <tt>ConditionInfo</tt>.
	 * @see #getEncoded
	 * @exception java.lang.IllegalArgumentException
	 *                if <tt>encodedCondition</tt> is not properly formatted.
	 */
	public ConditionInfo(String encodedCondition) {
		if (encodedCondition == null) {
			throw new NullPointerException("missing encoded permission");
		}
		if (encodedCondition.length() == 0) {
			throw new IllegalArgumentException("empty encoded permission");
		}

		try {
			char[] encoded = encodedCondition.toCharArray();

			/* the first character must be '[' */
			if (encoded[0] != '[') {
				throw new IllegalArgumentException(
						"first character not open bracket");
			}

			/* type is not quoted or encoded */
			int end = 1;
			int begin = end;

			while ((encoded[end] != ' ') && (encoded[end] != ')')) {
				end++;
			}

			if (end == begin) {
				throw new IllegalArgumentException("expecting type");
			}

			this.type = new String(encoded, begin, end - begin);

			Vector args = new Vector();
			/* type may be followed by name which is quoted and encoded */
			while (encoded[end] == ' ') {
				end++;

				if (encoded[end] != '"') {
					throw new IllegalArgumentException("expecting quoted name");
				}

				end++;
				begin = end;

				while (encoded[end] != '"') {
					if (encoded[end] == '\\') {
						end++;
					}

					end++;
				}

				args.add(decodeString(encoded, begin, end));
				end++;
			}
			this.args = (String[]) args.toArray(new String[0]);
			/* the final character must be ')' */
			if ((encoded[end] != ']') || (end + 1 != encoded.length)) {
				throw new IllegalArgumentException("last character not "
						+ "close bracket");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("parsing terminated abruptly");
		}
	}

	/**
	 * Returns the string encoding of this <tt>ConditionInfo</tt> in a form
	 * suitable for restoring this <tt>ConditionInfo</tt>.
	 * 
	 * <p>
	 * The encoding format is:
	 * 
	 * <pre>
	 * 
	 *  [type &quot;arg0&quot; &quot;arg1&quot; ...]
	 *  
	 * </pre>
	 * 
	 * where <i>argX</i> are strings that are encoded for proper parsing.
	 * Specifically, the <tt>"</tt>, <tt>\</tt>, carriage return, and
	 * linefeed characters are escaped using <tt>\"</tt>, <tt>\\</tt>,
	 * <tt>\r</tt>, and <tt>\n</tt>, respectively.
	 * 
	 * <p>
	 * The encoded string must contain no leading or trailing whitespace
	 * characters. A single space character must be used between type and "<i>arg0</i>"
	 * and between all arguments.
	 * 
	 * @return The string encoding of this <tt>ConditionInfo</tt>.
	 */
	public final String getEncoded() {
		StringBuffer output = new StringBuffer();
		output.append('[');
		output.append(type);

		for (int i = 0; i < args.length; i++) {
			output.append(" \"");
			encodeString(args[i], output);
			output.append('\"');
		}

		output.append(']');

		return (output.toString());
	}

	/**
	 * Returns the string representation of this <tt>ConditionInfo</tt>. The
	 * string is created by calling the <tt>getEncoded</tt> method on this
	 * <tt>ConditionInfo</tt>.
	 * 
	 * @return The string representation of this <tt>ConditionInfo</tt>.
	 */
	public String toString() {
		return (getEncoded());
	}

	/**
	 * Returns the fully qualified class name of the condition represented by
	 * this <tt>ConditionInfo</tt>.
	 * 
	 * @return The fully qualified class name of the condition represented by
	 *         this <tt>ConditionInfo</tt>.
	 */
	public final String getType() {
		return (type);
	}

	/**
	 * Returns arguments of this <tt>ConditionInfo</tt>.
	 * 
	 * @return The arguments of this <tt>ConditionInfo</tt>. have a name.
	 */
	public final String[] getArgs() {
		return (args);
	}

	/**
	 * Determines the equality of two <tt>ConditionInfo</tt> objects.
	 * 
	 * This method checks that specified object has the same type and args as
	 * this <tt>ConditionInfo</tt> object.
	 * 
	 * @param obj
	 *            The object to test for equality with this
	 *            <tt>ConditionInfo</tt> object.
	 * @return <tt>true</tt> if <tt>obj</tt> is a <tt>ConditionInfo</tt>,
	 *         and has the same type and args as this <tt>ConditionInfo</tt>
	 *         object; <tt>false</tt> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return (true);
		}

		if (!(obj instanceof ConditionInfo)) {
			return (false);
		}

		ConditionInfo other = (ConditionInfo) obj;

		if (!type.equals(other.type) || args.length != other.args.length)
			return false;

		for (int i = 0; i < args.length; i++) {
			if (!args[i].equals(other.args[i]))
				return false;
		}
		return true;
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */

	public int hashCode() {
		int hash = type.hashCode();

		for (int i = 0; i < args.length; i++) {
			hash ^= args[i].hashCode();
		}
		return (hash);
	}

	/**
	 * This escapes the quotes, backslashes, \n, and \r in the string using a
	 * backslash and appends the newly escaped string to a StringBuffer.
	 */
	private static void encodeString(String str, StringBuffer output) {
		int len = str.length();

		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);

			switch (c) {
			case '"':
			case '\\':
				output.append('\\');
				output.append(c);
				break;
			case '\r':
				output.append("\\r");
				break;
			case '\n':
				output.append("\\n");
				break;
			default:
				output.append(c);
				break;
			}
		}
	}

	/**
	 * Takes an encoded character array and decodes it into a new String.
	 */
	private static String decodeString(char[] str, int begin, int end) {
		StringBuffer output = new StringBuffer(end - begin);

		for (int i = begin; i < end; i++) {
			char c = str[i];

			if (c == '\\') {
				i++;

				if (i < end) {
					c = str[i];

					if (c == 'n') {
						c = '\n';
					} else if (c == 'r') {
						c = '\r';
					}
				}
			}

			output.append(c);
		}

		return (output.toString());
	}
}
