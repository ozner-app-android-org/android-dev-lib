/**
 * The Bouncy Castle License
 *
 * Copyright (c) 2000-2015 The Legion Of The Bouncy Castle Inc. (http://www.bouncycastle.org)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.aylanetworks.aaml.spongycastle;



import java.io.IOException;
import java.math.BigInteger;


/**
 * Class representing the ASN.1 INTEGER type.
 */
public class ASN1Integer
    extends ASN1Primitive
{
    byte[] bytes;

//    /**
//     * return an integer from the passed in object
//     *
//     * @param obj an ASN1Integer or an object that can be converted into one.
//     * @throws IllegalArgumentException if the object cannot be converted.
//     * @return an ASN1Integer instance.
//     */
//    public static ASN1Integer getInstance(
//        Object obj)
//    {
//        if (obj == null || obj instanceof ASN1Integer)
//        {
//            return (ASN1Integer)obj;
//        }
//
//        if (obj instanceof byte[])
//        {
//            try
//            {
//                return (ASN1Integer)fromByteArray((byte[])obj);
//            }
//            catch (Exception e)
//            {
//                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
//            }
//        }
//
//        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
//    }

//    /**
//     * return an Integer from a tagged object.
//     *
//     * @param obj      the tagged object holding the object we want
//     * @param explicit true if the object is meant to be explicitly
//     *                 tagged false otherwise.
//     * @throws IllegalArgumentException if the tagged object cannot
//     * be converted.
//     * @return an ASN1Integer instance.
//     */
//    public static ASN1Integer getInstance(
//        ASN1TaggedObject obj,
//        boolean explicit)
//    {
//        ASN1Primitive o = obj.getObject();
//
//        if (explicit || o instanceof ASN1Integer)
//        {
//            return getInstance(o);
//        }
//        else
//        {
//            return new ASN1Integer(ASN1OctetString.getInstance(obj.getObject()).getOctets());
//        }
//    }

    public ASN1Integer(
        long value)
    {
        bytes = BigInteger.valueOf(value).toByteArray();
    }

    public ASN1Integer(
        BigInteger value)
    {
        bytes = value.toByteArray();
    }

//    public ASN1Integer(
//        byte[] bytes)
//    {
//        this(bytes, true);
//    }

//    ASN1Integer(byte[] bytes, boolean clone)
//    {
//        this.bytes = (clone) ? Arrays.clone(bytes) : bytes;
//    }

    public BigInteger getValue()
    {
        return new BigInteger(bytes);
    }

    /**
     * in some cases positive values get crammed into a space,
     * that's not quite big enough...
     * @return the BigInteger that results from treating this ASN.1 INTEGER as unsigned.
     */
    public BigInteger getPositiveValue()
    {
        return new BigInteger(1, bytes);
    }

    boolean isConstructed()
    {
        return false;
    }

    int encodedLength()
    {
        return 1 + StreamUtil.calculateBodyLength(bytes.length) + bytes.length;
    }

    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        out.writeEncoded(BERTags.INTEGER, bytes);
    }

    public int hashCode()
    {
        int value = 0;

        for (int i = 0; i != bytes.length; i++)
        {
            value ^= (bytes[i] & 0xff) << (i % 4);
        }

        return value;
    }

    @Override
    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof ASN1Integer))
        {
            return false;
        }

        ASN1Integer other = (ASN1Integer)o;

        return Arrays.areEqual(bytes, other.bytes);
    }

    public String toString()
    {
        return getValue().toString();
    }

	
}








