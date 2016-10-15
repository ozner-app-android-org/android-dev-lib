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
import java.util.Enumeration;

public class DERSequence
    extends ASN1Sequence
{
    private int bodyLength = -1;

    /**
     * create an empty sequence
     */
    public DERSequence()
    {
    }

    /**
     * create a sequence containing one object
     */
    public DERSequence(
        ASN1Encodable obj)
    {
        super(obj);
    }

    /**
     * create a sequence containing a vector of objects.
     */
    public DERSequence(
        ASN1EncodableVector v)
    {
        super(v);
    }

    /**
     * create a sequence containing an array of objects.
     */
    public DERSequence(
        ASN1Encodable[]   array)
    {
        super(array);
    }

    private int getBodyLength()
        throws IOException
    {
        if (bodyLength < 0)
        {
            int length = 0;

            for (Enumeration e = this.getObjects(); e.hasMoreElements();)
            {
                Object    obj = e.nextElement();

                length += ((ASN1Encodable)obj).toASN1Primitive().toDERObject().encodedLength();
            }

            bodyLength = length;
        }

        return bodyLength;
    }

    int encodedLength()
        throws IOException
    {
        int length = getBodyLength();

        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    /*
     * A note on the implementation:
     * <p>
     * As DER requires the constructed, definite-length model to
     * be used for structured types, this varies slightly from the
     * ASN.1 descriptions given. Rather than just outputting SEQUENCE,
     * we also have to specify CONSTRUCTED, and the objects length.
     */
    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        ASN1OutputStream        dOut = out.getDERSubStream();
        int                     length = getBodyLength();

        out.write(BERTags.SEQUENCE | BERTags.CONSTRUCTED);
        out.writeLength(length);

        for (Enumeration e = this.getObjects(); e.hasMoreElements();)
        {
            Object    obj = e.nextElement();

            dOut.writeObject((ASN1Encodable)obj);
        }
    }
}














