
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




import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Base class for defining an ASN.1 object.
 */
abstract class ASN1Object
    implements ASN1Encodable, Encodable
{
    /**
     * Return the default BER or DER encoding for this object.
     *
     * @return BER/DER byte encoded object.
     * @throws java.io.IOException on encoding error.
     */
    public byte[] getEncoded()
        throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ASN1OutputStream      aOut = new ASN1OutputStream(bOut);

        aOut.writeObject(this);

        return bOut.toByteArray();
    }

    /**
     * Return either the default for "BER" or a DER encoding if "DER" is specified.
     *
     * @param encoding name of encoding to use.
     * @return byte encoded object.
     * @throws IOException on encoding error.
     */
//    public byte[] getEncoded(
//        String encoding)
//        throws IOException
//    {
//        if (encoding.equals(ASN1Encoding.DER))
//        {
//            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
//            DEROutputStream         dOut = new DEROutputStream(bOut);
//
//            dOut.writeObject(this);
//
//            return bOut.toByteArray();
//        }
//        else if (encoding.equals(ASN1Encoding.DL))
//        {
//            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
//            DLOutputStream          dOut = new DLOutputStream(bOut);
//
//            dOut.writeObject(this);
//
//            return bOut.toByteArray();
//        }
//
//        return this.getEncoded();
//    }

    public int hashCode()
    {
        return this.toASN1Primitive().hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ASN1Encodable))
        {
            return false;
        }

        ASN1Encodable other = (ASN1Encodable)o;

        return this.toASN1Primitive().equals(other.toASN1Primitive());
    }

    /**
     * @deprecated use toASN1Primitive()
     * @return the underlying primitive type.
     */
    public ASN1Primitive toASN1Object()
    {
        return this.toASN1Primitive();
    }

    /**
     * Return true if obj is a byte array and represents an object with the given tag value.
     *
     * @param obj object of interest.
     * @param tagValue tag value to check for.
     * @return  true if obj is a byte encoding starting with the given tag value, false otherwise.
     */
    protected static boolean hasEncodedTagValue(Object obj, int tagValue)
    {
        return (obj instanceof byte[]) && ((byte[])obj)[0] == tagValue;
    }

    /**
     * Method providing a primitive representation of this object suitable for encoding.
     * @return a primitive representation of this object.
     */
    public abstract ASN1Primitive toASN1Primitive();
}







