/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// OutputBuffer.java

package org.bson.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.GatheringByteChannel;

public abstract class OutputBuffer extends OutputStream {

    public void write(final byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void close() {
    }

    public abstract void write(byte[] b, int off, int len);

    public abstract void write(int b);

    public abstract int getPosition();

    /**
     * @return size of data so far
     */
    public abstract int size();

    /**
     * Pipe the contents of this output buffer into the given output stream
     */
    public abstract void pipe(OutputStream out) throws IOException;

    /**
     * Pipe the contents of the buffer into the given  channel
     *
     * @param channel the channel to pipe to
     */
    public abstract void pipe(GatheringByteChannel channel) throws IOException;

    public abstract void truncateToPosition(final int newPosition);

    /**
     * mostly for testing
     */
    public byte[] toByteArray() {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream(size());
            pipe(bout);
            return bout.toByteArray();
        } catch (IOException ioe) {
            throw new RuntimeException("should be impossible", ioe);
        }
    }


    public void writeInt(final int x) {
        write(x >> 0);
        write(x >> 8);
        write(x >> 16);
        write(x >> 24);
    }

    /**
     * Backpatches the size of a document or string by writing the size into the four bytes starting at getPosition() -
     * size.
     *
     * @param size the size of the document/string
     */
    public abstract void backpatchSize(final int size);

    protected abstract void backpatchSize(final int size, final int additionalOffset);

    public void writeLong(final long x) {
        write((byte) (0xFFL & (x >> 0)));
        write((byte) (0xFFL & (x >> 8)));
        write((byte) (0xFFL & (x >> 16)));
        write((byte) (0xFFL & (x >> 24)));
        write((byte) (0xFFL & (x >> 32)));
        write((byte) (0xFFL & (x >> 40)));
        write((byte) (0xFFL & (x >> 48)));
        write((byte) (0xFFL & (x >> 56)));
    }

    public void writeDouble(final double x) {
        writeLong(Double.doubleToRawLongBits(x));
    }

    public void writeString(final String str) {
        writeInt(0); // making space for size
        final int strLen = writeCString(str);
        backpatchSize(strLen, 4);
    }

    public int writeCString(final String str) {

        final int len = str.length();
        int total = 0;

        for (int i = 0; i < len;/*i gets incremented*/) {
            final int c = Character.codePointAt(str, i);

            if (c < 0x80) {
                write((byte) c);
                total += 1;
            }
            else if (c < 0x800) {
                write((byte) (0xc0 + (c >> 6)));
                write((byte) (0x80 + (c & 0x3f)));
                total += 2;
            }
            else if (c < 0x10000) {
                write((byte) (0xe0 + (c >> 12)));
                write((byte) (0x80 + ((c >> 6) & 0x3f)));
                write((byte) (0x80 + (c & 0x3f)));
                total += 3;
            }
            else {
                write((byte) (0xf0 + (c >> 18)));
                write((byte) (0x80 + ((c >> 12) & 0x3f)));
                write((byte) (0x80 + ((c >> 6) & 0x3f)));
                write((byte) (0x80 + (c & 0x3f)));
                total += 4;
            }

            i += Character.charCount(c);
        }

        write((byte) 0);
        total++;
        return total;
    }

    public String toString() {
        return getClass().getName() + " size: " + size() + " pos: " + getPosition();
    }
}
