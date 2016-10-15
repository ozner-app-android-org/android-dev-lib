
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

/**
 * Base class for ASN.1 primitive objects. These are the actual objects used to generate byte encodings.
 */
abstract class ASN1Primitive
    extends ASN1Object
{
    ASN1Primitive()
    {

    }

//    /**
//     * Create a base ASN.1 object from a byte stream.
//     *
//     * @param data the byte stream to parse.
//     * @return the base ASN.1 object represented by the byte stream.
//     * @exception IOException if there is a problem parsing the data.
//     */
//    public static ASN1Primitive fromByteArray(byte[] data)
//        throws IOException
//    {
//        ASN1InputStream aIn = new ASN1InputStream(data);
//
//        try
//        {
//            return aIn.readObject();
//        }
//        catch (ClassCastException e)
//        {
//            throw new IOException("cannot recognise object in stream");
//        }
//    }

    public final boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        return (o instanceof ASN1Encodable) && asn1Equals(((ASN1Encodable)o).toASN1Primitive());
    }

    public ASN1Primitive toASN1Primitive()
    {
        return this;
    }

    /**
     * Return the current object as one which encodes using Distinguished Encoding Rules.
     *
     * @return a DER version of this.
     */
    ASN1Primitive toDERObject()
    {
        return this;
    }

    /**
     * Return the current object as one which encodes using Definite Length encoding.
     *
     * @return a DL version of this.
     */
    ASN1Primitive toDLObject()
    {
        return this;
    }

    public abstract int hashCode();

    abstract boolean isConstructed();

    abstract int encodedLength() throws IOException;

    abstract void encode(ASN1OutputStream out) throws IOException;

    abstract boolean asn1Equals(ASN1Primitive o);
}





