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
import java.io.OutputStream;

/**
 * Stream that produces output based on the default encoding for the passed in objects.
 */
class ASN1OutputStream
{
    private OutputStream os;

    public ASN1OutputStream(
        OutputStream    os)
    {
        this.os = os;
    }

    void writeLength(
        int length)
        throws IOException
    {
        if (length > 127)
        {
            int size = 1;
            int val = length;

            while ((val >>>= 8) != 0)
            {
                size++;
            }

            write((byte)(size | 0x80));

            for (int i = (size - 1) * 8; i >= 0; i -= 8)
            {
                write((byte)(length >> i));
            }
        }
        else
        {
            write((byte)length);
        }
    }

    void write(int b)
        throws IOException
    {
        os.write(b);
    }

    void write(byte[] bytes)
        throws IOException
    {
        os.write(bytes);
    }

    void write(byte[] bytes, int off, int len)
        throws IOException
    {
        os.write(bytes, off, len);
    }

    void writeEncoded(
        int     tag,
        byte[]  bytes)
        throws IOException
    {
        write(tag);
        writeLength(bytes.length);
        write(bytes);
    }

    void writeTag(int flags, int tagNo)
        throws IOException
    {
        if (tagNo < 31)
        {
            write(flags | tagNo);
        }
        else
        {
            write(flags | 0x1f);
            if (tagNo < 128)
            {
                write(tagNo);
            }
            else
            {
                byte[] stack = new byte[5];
                int pos = stack.length;

                stack[--pos] = (byte)(tagNo & 0x7F);

                do
                {
                    tagNo >>= 7;
                    stack[--pos] = (byte)(tagNo & 0x7F | 0x80);
                }
                while (tagNo > 127);

                write(stack, pos, stack.length - pos);
            }
        }
    }

    void writeEncoded(int flags, int tagNo, byte[] bytes)
        throws IOException
    {
        writeTag(flags, tagNo);
        writeLength(bytes.length);
        write(bytes);
    }

    protected void writeNull()
        throws IOException
    {
        os.write(BERTags.NULL);
        os.write(0x00);
    }

    public void writeObject(
        ASN1Encodable obj)
        throws IOException
    {
        if (obj != null)
        {
            obj.toASN1Primitive().encode(this);
        }
        else
        {
            throw new IOException("null object detected");
        }
    }

    void writeImplicitObject(ASN1Primitive obj)
        throws IOException
    {
        if (obj != null)
        {
            obj.encode(new ImplicitOutputStream(os));
        }
        else
        {
            throw new IOException("null object detected");
        }
    }

    public void close()
        throws IOException
    {
        os.close();
    }

    public void flush()
        throws IOException
    {
        os.flush();
    }

    ASN1OutputStream getDERSubStream()
    {
        return new DEROutputStream(os);
    }

//    ASN1OutputStream getDLSubStream()
//    {
//        return new DLOutputStream(os);
//    }

    private class ImplicitOutputStream
        extends ASN1OutputStream
    {
        private boolean first = true;

        public ImplicitOutputStream(OutputStream os)
        {
            super(os);
        }

        public void write(int b)
            throws IOException
        {
            if (first)
            {
                first = false;
            }
            else
            {
                super.write(b);
            }
        }
    }
}







