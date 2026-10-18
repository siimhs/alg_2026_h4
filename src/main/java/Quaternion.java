/*
 * Author: Claude (Anthropic), model Claude Opus 5.5 (claude-opus-5-5),
 * via Claude Code.
 */

/** Quaternions. Basic operations.
 * <p>
 * Sources:
 * <ul>
 * <li>Task description and operation formulas:
 *     https://enos.itcollege.ee/~japoia/algoritmid/quat/</li>
 * <li>Quaternion algebra (Hamilton product, conjugate, inverse):
 *     https://en.wikipedia.org/wiki/Quaternion</li>
 * <li>hashCode recipe (combining fields with an odd prime multiplier):
 *     J. Bloch, "Effective Java", 3rd ed., Item 11.</li>
 * <li>Number formats of <code>Double.toString</code> and
 *     <code>Double.parseDouble</code>: Java SE API documentation,
 *     https://docs.oracle.com/javase/8/docs/api/java/lang/Double.html</li>
 * </ul>
 */
public class Quaternion {

   /** Real numbers whose difference is less than this are considered equal. */
   private static final double DELTA = 0.000001;

   /** Units of the four parts in the string form, the real part has none. */
   private static final String[] UNITS = {"", "i", "j", "k"};

   private final double real;
   private final double iPart;
   private final double jPart;
   private final double kPart;

   /** Constructor from four double values.
    * @param a real part
    * @param b imaginary part i
    * @param c imaginary part j
    * @param d imaginary part k
    * @throws IllegalArgumentException if any part is NaN or infinite
    */
   public Quaternion (double a, double b, double c, double d) {
      if (!isFinite (a) || !isFinite (b) || !isFinite (c) || !isFinite (d))
         throw new IllegalArgumentException ("Quaternion parts must be finite "
            + "numbers, got a=" + a + ", b=" + b + ", c=" + c + ", d=" + d);
      real = a;
      iPart = b;
      jPart = c;
      kPart = d;
   }

   /** Real part of the quaternion.
    * @return real part
    */
   public double getRpart() {
      return real;
   }

   /** Imaginary part i of the quaternion.
    * @return imaginary part i
    */
   public double getIpart() {
      return iPart;
   }

   /** Imaginary part j of the quaternion.
    * @return imaginary part j
    */
   public double getJpart() {
      return jPart;
   }

   /** Imaginary part k of the quaternion.
    * @return imaginary part k
    */
   public double getKpart() {
      return kPart;
   }

   /** Conversion of the quaternion to the string.
    * @return a string form of this quaternion:
    * "a+bi+cj+dk"
    * (without any brackets)
    */
   @Override
   public String toString() {
      return Double.toString (real) + signed (iPart) + UNITS[1]
         + signed (jPart) + UNITS[2] + signed (kPart) + UNITS[3];
   }

   /** Number with an explicit sign, so that "+-" never appears.
    * @param x number
    * @return x as a string that starts with '+' or '-'
    */
   private static String signed (double x) {
      String s = Double.toString (x);
      return s.startsWith ("-") ? s : "+" + s;
   }

   /** Conversion from the string to the quaternion.
    * Reverse to <code>toString</code> method.
    * Accepted form is "a+bi+cj+dk", where each coefficient is a decimal
    * number with an optional exponent (e.g. "-2.0e-3") and each imaginary
    * part is preceded by exactly one sign ('+' or '-').
    * @throws IllegalArgumentException if string s does not represent
    *     a quaternion (defined by the <code>toString</code> method)
    * @param s string of form produced by the <code>toString</code> method
    * @return a quaternion represented by string s
    */
   public static Quaternion valueOf (String s) {
      if (s == null)
         throw new IllegalArgumentException ("Cannot convert null to a quaternion");
      if (s.isEmpty())
         throw new IllegalArgumentException ("Cannot convert an empty string to a quaternion");
      double[] parts = new double[4];
      int pos = 0;
      for (int p = 0; p < parts.length; p++) {
         int start = pos;
         String partName = p == 0 ? "real part" : "coefficient of " + UNITS[p];
         if (pos < s.length() && (s.charAt (pos) == '+' || s.charAt (pos) == '-')) {
            pos++;
         } else if (p > 0) {
            throw parseError (s, pos, "expected '+' or '-' before the " + partName);
         }
         pos = skipNumber (s, pos, partName);
         String number = s.substring (start, pos);
         parts[p] = Double.parseDouble (number);
         if (Double.isInfinite (parts[p]))
            throw parseError (s, start, "number \"" + number + "\" (" + partName
               + ") is out of the range of double");
         if (p > 0) {
            if (pos >= s.length() || s.charAt (pos) != UNITS[p].charAt (0))
               throw parseError (s, pos, "expected '" + UNITS[p] + "' after the "
                  + partName);
            pos++;
         }
      }
      if (pos < s.length())
         throw parseError (s, pos, "unexpected trailing characters \""
            + s.substring (pos) + "\"");
      return new Quaternion (parts[0], parts[1], parts[2], parts[3]);
   }

   /** Skips an unsigned decimal number with an optional exponent,
    * i.e. digits[.digits][(e|E)[+|-]digits], where either the integer or
    * the fractional digits may be missing, but not both.
    * @param s string being parsed
    * @param pos index where the number starts
    * @param partName name of the part being read, used in error messages
    * @return index right after the number
    * @throws IllegalArgumentException if there is no valid number at pos
    */
   private static int skipNumber (String s, int pos, String partName) {
      int digitsStart = pos;
      pos = skipDigits (s, pos);
      int digits = pos - digitsStart;
      if (pos < s.length() && s.charAt (pos) == '.') {
         int fractionStart = ++pos;
         pos = skipDigits (s, pos);
         digits += pos - fractionStart;
      }
      if (digits == 0)
         throw parseError (s, digitsStart, "expected digits of the " + partName);
      if (pos < s.length() && (s.charAt (pos) == 'e' || s.charAt (pos) == 'E')) {
         pos++;
         if (pos < s.length() && (s.charAt (pos) == '+' || s.charAt (pos) == '-'))
            pos++;
         int exponentStart = pos;
         pos = skipDigits (s, pos);
         if (pos == exponentStart)
            throw parseError (s, pos, "expected digits of the exponent of the "
               + partName);
      }
      return pos;
   }

   /** Skips decimal digits.
    * @param s string being parsed
    * @param pos starting index
    * @return index of the first non-digit character (or length of s)
    */
   private static int skipDigits (String s, int pos) {
      while (pos < s.length() && s.charAt (pos) >= '0' && s.charAt (pos) <= '9')
         pos++;
      return pos;
   }

   /** Builds an exception describing a syntax error in valueOf input.
    * @param s whole input string
    * @param pos index of the error
    * @param problem description of the error
    * @return exception to be thrown
    */
   private static IllegalArgumentException parseError (String s, int pos,
                                                       String problem) {
      String found = pos < s.length()
         ? "found '" + s.charAt (pos) + "'" : "reached end of string";
      return new IllegalArgumentException ("Cannot convert \"" + s
         + "\" to a quaternion: " + problem + " at index " + pos + ", "
         + found + " (expected form a+bi+cj+dk, e.g. -1.5-2.0i+3.0e-4j-4.0k)");
   }

   /** Clone of the quaternion.
    * @return independent clone of <code>this</code>
    */
   @Override
   public Object clone() throws CloneNotSupportedException {
      return new Quaternion (real, iPart, jPart, kPart);
   }

   /** Test whether the quaternion is zero.
    * @return true, if the real part and all the imaginary parts are (close to) zero
    */
   public boolean isZero() {
      return isClose (real, 0.) && isClose (iPart, 0.)
         && isClose (jPart, 0.) && isClose (kPart, 0.);
   }

   /** Conjugate of the quaternion. Expressed by the formula
    *     conjugate(a+bi+cj+dk) = a-bi-cj-dk
    * @return conjugate of <code>this</code>
    */
   public Quaternion conjugate() {
      return new Quaternion (real, -iPart, -jPart, -kPart);
   }

   /** Opposite of the quaternion. Expressed by the formula
    *    opposite(a+bi+cj+dk) = -a-bi-cj-dk
    * @return quaternion <code>-this</code>
    */
   public Quaternion opposite() {
      return new Quaternion (-real, -iPart, -jPart, -kPart);
   }

   /** Sum of quaternions. Expressed by the formula
    *    (a1+b1i+c1j+d1k) + (a2+b2i+c2j+d2k) = (a1+a2) + (b1+b2)i + (c1+c2)j + (d1+d2)k
    * @param q addend
    * @return quaternion <code>this+q</code>
    */
   public Quaternion plus (Quaternion q) {
      requireArgument (q, "plus");
      return new Quaternion (real + q.real, iPart + q.iPart,
         jPart + q.jPart, kPart + q.kPart);
   }

   /** Product of quaternions. Expressed by the formula
    *  (a1+b1i+c1j+d1k) * (a2+b2i+c2j+d2k) = (a1a2-b1b2-c1c2-d1d2) + (a1b2+b1a2+c1d2-d1c2)i +
    *  (a1c2-b1d2+c1a2+d1b2)j + (a1d2+b1c2-c1b2+d1a2)k
    * @param q factor
    * @return quaternion <code>this*q</code>
    */
   public Quaternion times (Quaternion q) {
      requireArgument (q, "times");
      return new Quaternion (
         real * q.real - iPart * q.iPart - jPart * q.jPart - kPart * q.kPart,
         real * q.iPart + iPart * q.real + jPart * q.kPart - kPart * q.jPart,
         real * q.jPart - iPart * q.kPart + jPart * q.real + kPart * q.iPart,
         real * q.kPart + iPart * q.jPart - jPart * q.iPart + kPart * q.real);
   }

   /** Multiplication by a coefficient.
    * @param r coefficient
    * @return quaternion <code>this*r</code>
    */
   public Quaternion times (double r) {
      return new Quaternion (real * r, iPart * r, jPart * r, kPart * r);
   }

   /** Inverse of the quaternion. Expressed by the formula
    *     1/(a+bi+cj+dk) = a/(a*a+b*b+c*c+d*d) +
    *     ((-b)/(a*a+b*b+c*c+d*d))i + ((-c)/(a*a+b*b+c*c+d*d))j + ((-d)/(a*a+b*b+c*c+d*d))k
    * @return quaternion <code>1/this</code>
    * @throws ArithmeticException if <code>this</code> is zero
    */
   public Quaternion inverse() {
      if (isZero())
         throw new ArithmeticException ("Cannot invert a zero quaternion: " + this);
      double n = real * real + iPart * iPart + jPart * jPart + kPart * kPart;
      return new Quaternion (real / n, -iPart / n, -jPart / n, -kPart / n);
   }

   /** Difference of quaternions. Expressed as addition to the opposite.
    * @param q subtrahend
    * @return quaternion <code>this-q</code>
    */
   public Quaternion minus (Quaternion q) {
      requireArgument (q, "minus");
      return plus (q.opposite());
   }

   /** Right quotient of quaternions. Expressed as multiplication to the inverse.
    * @param q (right) divisor
    * @return quaternion <code>this*inverse(q)</code>
    * @throws ArithmeticException if q is zero
    */
   public Quaternion divideByRight (Quaternion q) {
      requireArgument (q, "divideByRight");
      if (q.isZero())
         throw new ArithmeticException ("Division by zero in divideByRight: ("
            + this + ") * inverse(" + q + ")");
      return times (q.inverse());
   }

   /** Left quotient of quaternions.
    * @param q (left) divisor
    * @return quaternion <code>inverse(q)*this</code>
    * @throws ArithmeticException if q is zero
    */
   public Quaternion divideByLeft (Quaternion q) {
      requireArgument (q, "divideByLeft");
      if (q.isZero())
         throw new ArithmeticException ("Division by zero in divideByLeft: inverse("
            + q + ") * (" + this + ")");
      return q.inverse().times (this);
   }

   /** Equality test of quaternions. Difference of equal numbers
    *     is (close to) zero.
    * @param qo second quaternion
    * @return logical value of the expression <code>this.equals(qo)</code>
    */
   @Override
   public boolean equals (Object qo) {
      if (this == qo)
         return true;
      if (!(qo instanceof Quaternion))
         return false;
      Quaternion q = (Quaternion) qo;
      return isClose (real, q.real) && isClose (iPart, q.iPart)
         && isClose (jPart, q.jPart) && isClose (kPart, q.kPart);
   }

   /** Dot product of quaternions. (p*conjugate(q) + q*conjugate(p))/2
    * @param q factor
    * @return dot product of this and q
    */
   public Quaternion dotMult (Quaternion q) {
      requireArgument (q, "dotMult");
      return times (q.conjugate()).plus (q.times (conjugate())).times (0.5);
   }

   /** Integer hashCode has to be the same for equal objects.
    * Every part is rounded to a multiple of DELTA before hashing, so that
    * numbers differing only by rounding noise get the same hash code.
    * Parts are combined with different weights (powers of 31), so permuting
    * the parts changes the result.
    * @return hashcode
    */
   @Override
   public int hashCode() {
      int result = 17;
      result = 31 * result + hashPart (real);
      result = 31 * result + hashPart (iPart);
      result = 31 * result + hashPart (jPart);
      result = 31 * result + hashPart (kPart);
      return result;
   }

   /** Hash code of a single part, rounded to the precision of DELTA.
    * @param x part of the quaternion
    * @return hash code of x
    */
   private static int hashPart (double x) {
      // adding 0. turns -0.0 into 0.0, so that both get the same hash
      return Double.valueOf (Math.rint (x / DELTA) + 0.).hashCode();
   }

   /** Norm of the quaternion. Expressed by the formula
    *     norm(a+bi+cj+dk) = Math.sqrt(a*a+b*b+c*c+d*d)
    * @return norm of <code>this</code> (norm is a real number)
    */
   public double norm() {
      return Math.sqrt (real * real + iPart * iPart + jPart * jPart + kPart * kPart);
   }

   /** Comparison of real numbers with tolerance DELTA.
    * @param x first number
    * @param y second number
    * @return true, if x and y differ less than DELTA
    */
   private static boolean isClose (double x, double y) {
      return Math.abs (x - y) < DELTA;
   }

   /** Test whether a number is neither NaN nor infinite.
    * @param x number
    * @return true, if x is finite
    */
   private static boolean isFinite (double x) {
      return !Double.isNaN (x) && !Double.isInfinite (x);
   }

   /** Checks that an argument of a binary operation is present.
    * @param q argument
    * @param operation name of the operation, used in the error message
    * @throws IllegalArgumentException if q is null
    */
   private void requireArgument (Quaternion q, String operation) {
      if (q == null)
         throw new IllegalArgumentException ("Argument of " + operation
            + " must not be null (this = " + this + ")");
   }

   /** Main method for testing purposes.
    * @param arg command line parameters
    */
   public static void main (String[] arg) {
      Quaternion arv1 = new Quaternion (-1., 1, 2., -2.);
      if (arg.length > 0)
         arv1 = valueOf (arg[0]);
      System.out.println ("first: " + arv1.toString());
      System.out.println ("real: " + arv1.getRpart());
      System.out.println ("imagi: " + arv1.getIpart());
      System.out.println ("imagj: " + arv1.getJpart());
      System.out.println ("imagk: " + arv1.getKpart());
      System.out.println ("isZero: " + arv1.isZero());
      System.out.println ("conjugate: " + arv1.conjugate());
      System.out.println ("opposite: " + arv1.opposite());
      System.out.println ("hashCode: " + arv1.hashCode());
      Quaternion res = null;
      try {
         res = (Quaternion)arv1.clone();
      } catch (CloneNotSupportedException e) {};
      System.out.println ("clone equals to original: " + res.equals (arv1));
      System.out.println ("clone is not the same object: " + (res!=arv1));
      System.out.println ("hashCode: " + res.hashCode());
      res = valueOf (arv1.toString());
      System.out.println ("string conversion equals to original: "
         + res.equals (arv1));
      Quaternion arv2 = new Quaternion (1., -2.,  -1., 2.);
      if (arg.length > 1)
         arv2 = valueOf (arg[1]);
      System.out.println ("second: " + arv2.toString());
      System.out.println ("hashCode: " + arv2.hashCode());
      System.out.println ("equals: " + arv1.equals (arv2));
      res = arv1.plus (arv2);
      System.out.println ("plus: " + res);
      System.out.println ("times: " + arv1.times (arv2));
      System.out.println ("minus: " + arv1.minus (arv2));
      double mm = arv1.norm();
      System.out.println ("norm: " + mm);
      System.out.println ("inverse: " + arv1.inverse());
      System.out.println ("divideByRight: " + arv1.divideByRight (arv2));
      System.out.println ("divideByLeft: " + arv1.divideByLeft (arv2));
      System.out.println ("dotMult: " + arv1.dotMult (arv2));
      System.out.println ("exponent form: "
         + valueOf ("-2.0e-3-4.0e-1i-5.0e-2j-6.0e-3k"));
   }

}
