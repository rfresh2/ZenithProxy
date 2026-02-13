package com.zenith.util.struct;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.math.BigInteger;
import java.util.Objects;

public final class Fraction extends Number implements Comparable<Fraction> {
    private static final long serialVersionUID = 65382027393090L;
    public static final Fraction ZERO = new Fraction(0, 1);
    public static final Fraction ONE = new Fraction(1, 1);
    public static final Fraction ONE_HALF = new Fraction(1, 2);
    public static final Fraction ONE_THIRD = new Fraction(1, 3);
    public static final Fraction TWO_THIRDS = new Fraction(2, 3);
    public static final Fraction ONE_QUARTER = new Fraction(1, 4);
    public static final Fraction TWO_QUARTERS = new Fraction(2, 4);
    public static final Fraction THREE_QUARTERS = new Fraction(3, 4);
    public static final Fraction ONE_FIFTH = new Fraction(1, 5);
    public static final Fraction TWO_FIFTHS = new Fraction(2, 5);
    public static final Fraction THREE_FIFTHS = new Fraction(3, 5);
    public static final Fraction FOUR_FIFTHS = new Fraction(4, 5);
    private final int numerator;
    private final int denominator;
    private transient int hashCode;
    private transient String toString;
    private transient String toProperString;

    private static int addAndCheck(int x, int y) {
        long s = (long)x + (long)y;
        if (s >= -2147483648L && s <= 2147483647L) {
            return (int)s;
        } else {
            throw new ArithmeticException("overflow: add");
        }
    }

    public static Fraction getFraction(double value) {
        int sign = value < (double)0.0F ? -1 : 1;
        value = Math.abs(value);
        if (!(value > (double)Integer.MAX_VALUE) && !Double.isNaN(value)) {
            int wholeNumber = (int)value;
            value -= (double)wholeNumber;
            int numer0 = 0;
            int denom0 = 1;
            int numer1 = 1;
            int denom1 = 0;
            int a1 = (int)value;
            double x1 = (double)1.0F;
            double y1 = value - (double)a1;
            double delta2 = Double.MAX_VALUE;
            int i = 1;

            int denom2;
            double delta1;
            do {
                delta1 = delta2;
                int a2 = (int)(x1 / y1);
                double y2 = x1 - (double)a2 * y1;
                int numer2 = a1 * numer1 + numer0;
                denom2 = a1 * denom1 + denom0;
                double fraction = (double)numer2 / (double)denom2;
                delta2 = Math.abs(value - fraction);
                a1 = a2;
                x1 = y1;
                y1 = y2;
                numer0 = numer1;
                denom0 = denom1;
                numer1 = numer2;
                denom1 = denom2;
                ++i;
            } while(delta1 > delta2 && denom2 <= 10000 && denom2 > 0 && i < 25);

            if (i == 25) {
                throw new ArithmeticException("Unable to convert double to fraction");
            } else {
                return getReducedFraction((numer0 + wholeNumber * denom0) * sign, denom0);
            }
        } else {
            throw new ArithmeticException("The value must not be greater than Integer.MAX_VALUE or NaN");
        }
    }

    public static Fraction getFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        } else {
            if (denominator < 0) {
                if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
                    throw new ArithmeticException("overflow: can't negate");
                }

                numerator = -numerator;
                denominator = -denominator;
            }

            return new Fraction(numerator, denominator);
        }
    }

    public static Fraction getFraction(int whole, int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        } else if (denominator < 0) {
            throw new ArithmeticException("The denominator must not be negative");
        } else if (numerator < 0) {
            throw new ArithmeticException("The numerator must not be negative");
        } else {
            long numeratorValue;
            if (whole < 0) {
                numeratorValue = (long)whole * (long)denominator - (long)numerator;
            } else {
                numeratorValue = (long)whole * (long)denominator + (long)numerator;
            }

            if (numeratorValue >= -2147483648L && numeratorValue <= 2147483647L) {
                return new Fraction((int)numeratorValue, denominator);
            } else {
                throw new ArithmeticException("Numerator too large to represent as an Integer.");
            }
        }
    }

    public static Fraction getFraction(String str) {
        Objects.requireNonNull(str, "str");
        int pos = str.indexOf(46);
        if (pos >= 0) {
            return getFraction(Double.parseDouble(str));
        } else {
            pos = str.indexOf(32);
            if (pos > 0) {
                int whole = Integer.parseInt(str.substring(0, pos));
                str = str.substring(pos + 1);
                pos = str.indexOf(47);
                if (pos < 0) {
                    throw new NumberFormatException("The fraction could not be parsed as the format X Y/Z");
                } else {
                    int numer = Integer.parseInt(str.substring(0, pos));
                    int denom = Integer.parseInt(str.substring(pos + 1));
                    return getFraction(whole, numer, denom);
                }
            } else {
                pos = str.indexOf(47);
                if (pos < 0) {
                    return getFraction(Integer.parseInt(str), 1);
                } else {
                    int numer = Integer.parseInt(str.substring(0, pos));
                    int denom = Integer.parseInt(str.substring(pos + 1));
                    return getFraction(numer, denom);
                }
            }
        }
    }

    public static Fraction getReducedFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        } else if (numerator == 0) {
            return ZERO;
        } else {
            if (denominator == Integer.MIN_VALUE && (numerator & 1) == 0) {
                numerator /= 2;
                denominator /= 2;
            }

            if (denominator < 0) {
                if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
                    throw new ArithmeticException("overflow: can't negate");
                }

                numerator = -numerator;
                denominator = -denominator;
            }

            int gcd = greatestCommonDivisor(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
            return new Fraction(numerator, denominator);
        }
    }

    private static int greatestCommonDivisor(int u, int v) {
        if (u != 0 && v != 0) {
            if (Math.abs(u) != 1 && Math.abs(v) != 1) {
                if (u > 0) {
                    u = -u;
                }

                if (v > 0) {
                    v = -v;
                }

                int k;
                for(k = 0; (u & 1) == 0 && (v & 1) == 0 && k < 31; ++k) {
                    u /= 2;
                    v /= 2;
                }

                if (k == 31) {
                    throw new ArithmeticException("overflow: gcd is 2^31");
                } else {
                    int t = (u & 1) == 1 ? v : -(u / 2);

                    while(true) {
                        while((t & 1) != 0) {
                            if (t > 0) {
                                u = -t;
                            } else {
                                v = t;
                            }

                            t = (v - u) / 2;
                            if (t == 0) {
                                return -u * (1 << k);
                            }
                        }

                        t /= 2;
                    }
                }
            } else {
                return 1;
            }
        } else if (u != Integer.MIN_VALUE && v != Integer.MIN_VALUE) {
            return Math.abs(u) + Math.abs(v);
        } else {
            throw new ArithmeticException("overflow: gcd is 2^31");
        }
    }

    private static int mulAndCheck(int x, int y) {
        long m = (long)x * (long)y;
        if (m >= -2147483648L && m <= 2147483647L) {
            return (int)m;
        } else {
            throw new ArithmeticException("overflow: mul");
        }
    }

    private static int mulPosAndCheck(int x, int y) {
        long m = (long)x * (long)y;
        if (m > 2147483647L) {
            throw new ArithmeticException("overflow: mulPos");
        } else {
            return (int)m;
        }
    }

    private static int subAndCheck(int x, int y) {
        long s = (long)x - (long)y;
        if (s >= -2147483648L && s <= 2147483647L) {
            return (int)s;
        } else {
            throw new ArithmeticException("overflow: add");
        }
    }

    private Fraction(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Fraction abs() {
        return this.numerator >= 0 ? this : this.negate();
    }

    public Fraction add(Fraction fraction) {
        return this.addSub(fraction, true);
    }

    private Fraction addSub(Fraction fraction, boolean isAdd) {
        Objects.requireNonNull(fraction, "fraction");
        if (this.numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        } else if (fraction.numerator == 0) {
            return this;
        } else {
            int d1 = greatestCommonDivisor(this.denominator, fraction.denominator);
            if (d1 == 1) {
                int uvp = mulAndCheck(this.numerator, fraction.denominator);
                int upv = mulAndCheck(fraction.numerator, this.denominator);
                return new Fraction(isAdd ? addAndCheck(uvp, upv) : subAndCheck(uvp, upv), mulPosAndCheck(this.denominator, fraction.denominator));
            } else {
                BigInteger uvp = BigInteger.valueOf((long)this.numerator).multiply(BigInteger.valueOf((long)(fraction.denominator / d1)));
                BigInteger upv = BigInteger.valueOf((long)fraction.numerator).multiply(BigInteger.valueOf((long)(this.denominator / d1)));
                BigInteger t = isAdd ? uvp.add(upv) : uvp.subtract(upv);
                int tmodd1 = t.mod(BigInteger.valueOf((long)d1)).intValue();
                int d2 = tmodd1 == 0 ? d1 : greatestCommonDivisor(tmodd1, d1);
                BigInteger w = t.divide(BigInteger.valueOf((long)d2));
                if (w.bitLength() > 31) {
                    throw new ArithmeticException("overflow: numerator too large after multiply");
                } else {
                    return new Fraction(w.intValue(), mulPosAndCheck(this.denominator / d1, fraction.denominator / d2));
                }
            }
        }
    }

    public int compareTo(Fraction other) {
        if (this == other) {
            return 0;
        } else if (this.numerator == other.numerator && this.denominator == other.denominator) {
            return 0;
        } else {
            long first = (long)this.numerator * (long)other.denominator;
            long second = (long)other.numerator * (long)this.denominator;
            return Long.compare(first, second);
        }
    }

    public Fraction divideBy(Fraction fraction) {
        Objects.requireNonNull(fraction, "fraction");
        if (fraction.numerator == 0) {
            throw new ArithmeticException("The fraction to divide by must not be zero");
        } else {
            return this.multiplyBy(fraction.invert());
        }
    }

    public double doubleValue() {
        return (double)this.numerator / (double)this.denominator;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Fraction)) {
            return false;
        } else {
            Fraction other = (Fraction)obj;
            return this.getNumerator() == other.getNumerator() && this.getDenominator() == other.getDenominator();
        }
    }

    public float floatValue() {
        return (float)this.numerator / (float)this.denominator;
    }

    public int getDenominator() {
        return this.denominator;
    }

    public int getNumerator() {
        return this.numerator;
    }

    public int getProperNumerator() {
        return Math.abs(this.numerator % this.denominator);
    }

    public int getProperWhole() {
        return this.numerator / this.denominator;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = 37 * (629 + this.getNumerator()) + this.getDenominator();
        }

        return this.hashCode;
    }

    public int intValue() {
        return this.numerator / this.denominator;
    }

    public Fraction invert() {
        if (this.numerator == 0) {
            throw new ArithmeticException("Unable to invert zero.");
        } else if (this.numerator == Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: can't negate numerator");
        } else {
            return this.numerator < 0 ? new Fraction(-this.denominator, -this.numerator) : new Fraction(this.denominator, this.numerator);
        }
    }

    public long longValue() {
        return (long)this.numerator / (long)this.denominator;
    }

    public Fraction multiplyBy(Fraction fraction) {
        Objects.requireNonNull(fraction, "fraction");
        if (this.numerator != 0 && fraction.numerator != 0) {
            int d1 = greatestCommonDivisor(this.numerator, fraction.denominator);
            int d2 = greatestCommonDivisor(fraction.numerator, this.denominator);
            return getReducedFraction(mulAndCheck(this.numerator / d1, fraction.numerator / d2), mulPosAndCheck(this.denominator / d2, fraction.denominator / d1));
        } else {
            return ZERO;
        }
    }

    public Fraction negate() {
        if (this.numerator == Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: too large to negate");
        } else {
            return new Fraction(-this.numerator, this.denominator);
        }
    }

    public Fraction pow(int power) {
        if (power == 1) {
            return this;
        } else if (power == 0) {
            return ONE;
        } else if (power < 0) {
            return power == Integer.MIN_VALUE ? this.invert().pow(2).pow(-(power / 2)) : this.invert().pow(-power);
        } else {
            Fraction f = this.multiplyBy(this);
            return power % 2 == 0 ? f.pow(power / 2) : f.pow(power / 2).multiplyBy(this);
        }
    }

    public Fraction reduce() {
        if (this.numerator == 0) {
            return this.equals(ZERO) ? this : ZERO;
        } else {
            int gcd = greatestCommonDivisor(Math.abs(this.numerator), this.denominator);
            return gcd == 1 ? this : getFraction(this.numerator / gcd, this.denominator / gcd);
        }
    }

    public Fraction subtract(Fraction fraction) {
        return this.addSub(fraction, false);
    }

    public String toProperString() {
        if (this.toProperString == null) {
            if (this.numerator == 0) {
                this.toProperString = "0";
            } else if (this.numerator == this.denominator) {
                this.toProperString = "1";
            } else if (this.numerator == -1 * this.denominator) {
                this.toProperString = "-1";
            } else if ((this.numerator > 0 ? -this.numerator : this.numerator) < -this.denominator) {
                int properNumerator = this.getProperNumerator();
                if (properNumerator == 0) {
                    this.toProperString = Integer.toString(this.getProperWhole());
                } else {
                    this.toProperString = this.getProperWhole() + " " + properNumerator + "/" + this.getDenominator();
                }
            } else {
                this.toProperString = this.getNumerator() + "/" + this.getDenominator();
            }
        }

        return this.toProperString;
    }

    public String toString() {
        if (this.toString == null) {
            this.toString = this.getNumerator() + "/" + this.getDenominator();
        }

        return this.toString;
    }
}

