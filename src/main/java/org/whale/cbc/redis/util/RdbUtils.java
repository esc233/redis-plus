package org.whale.cbc.redis.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author thuglife
 * @DATE 2017/12/17
 * @DESCRIPTION :
 * redis RDB反序列
 */
public class RdbUtils {
    private static final Charset ASCII = Charset.forName("ASCII");
    private static int MAX_LITERAL = 32;

    public static List<byte[]> readHashmapAsZipList(ByteBuffer buf) throws IOException {
        int valueType = readByte(buf);
        if(valueType==13)
            return get(readStringEncoded(buf));
        else if(valueType==4)
            return readHash(buf);
        else
            throw new IllegalArgumentException("该类型不是hashMap");
    }

    private static List<byte[]> readHash(ByteBuffer buf) throws IOException {
        long len = readLength(buf);
        if(len > 1073741823L) {
            throw new IllegalArgumentException("Hashes with more than 1073741823 elements are not supported.");
        } else {
            int size = (int)len;
            ArrayList kvPairs = new ArrayList(2 * size);

            for(int i = 0; i < size; ++i) {
                kvPairs.add(readStringEncoded(buf));
                kvPairs.add(readStringEncoded(buf));
            }

            return kvPairs;
        }
    }

    private static long readLength(ByteBuffer buf) throws IOException {
        int firstByte = readByte(buf);
        int flag = (firstByte & 192) >> 6;
        switch(flag) {
            case 0:
                return (long)(firstByte & 63);
            case 1:
                return ((long)firstByte & 63L) << 8 | (long)readByte(buf) & 255L;
            case 2:
                byte[] bs = readBytes(4,buf);
                return ((long)bs[0] & 255L) << 24 | ((long)bs[1] & 255L) << 16 | ((long)bs[2] & 255L) << 8 | ((long)bs[3] & 255L) << 0;
            default:
                throw new IllegalStateException("Expected a length, but got a special string encoding.");
        }
    }

    private static int readByte(ByteBuffer buf) throws IOException {
        return buf.get();
    }

    private static byte[] readBytes(int numBytes,ByteBuffer buf) throws IOException {
        int rem = numBytes;
        int pos = 0;
        byte[] bs = new byte[numBytes];

        while(rem > 0) {
            int avail = buf.remaining();
            if(avail >= rem) {
                buf.get(bs, pos, rem);
                pos += rem;
                rem = 0;
            } else {
                buf.get(bs, pos, avail);
                pos += avail;
                rem -= avail;
                fillBuffer(buf);
            }
        }

        return bs;
    }

    private static byte[] readStringEncoded(ByteBuffer buf) throws IOException {
        int firstByte = readByte(buf);
        int flag = (firstByte & 192) >> 6;
        int len;
        switch(flag) {
            case 0:
                len = firstByte & 63;
                return readBytes(len,buf);
            case 1:
                len = (firstByte & 63) << 8 | readByte(buf) & 255;
                return readBytes(len,buf);
            case 2:
                byte[] bs = readBytes(4,buf);
                len = (bs[0] & 255) << 24 | (bs[1] & 255) << 16 | (bs[2] & 255) << 8 | (bs[3] & 255) << 0;
                if(len < 0) {
                    throw new IllegalStateException("Strings longer than 2147483647bytes are not supported.");
                }

                return readBytes(len,buf);
            case 3:
                return readSpecialStringEncoded(firstByte & 63,buf);
            default:
                return null;
        }
    }

    private static void fillBuffer(ByteBuffer buf) throws IOException {
        buf.clear();
        buf.flip();
    }

    public static List<byte[]> get(byte[] envelope) {
        byte pos = 8;
        int var14 = pos + 1;
        int num = (envelope[pos] & 255) << 0 | (envelope[var14++] & 255) << 8;
        ArrayList list = new ArrayList(num);

        for(int idx = 0; idx < num; ++idx) {
            int prevLen = envelope[var14++] & 255;
            if(prevLen > 252) {
                var14 += 4;
            }

            int special = envelope[var14++] & 255;
            int top2bits = special >> 6;
            int len;
            byte[] buf;
            switch(top2bits) {
                case 0:
                    len = special & 63;
                    buf = new byte[len];
                    System.arraycopy(envelope, var14, buf, 0, len);
                    var14 += len;
                    list.add(buf);
                    break;
                case 1:
                    len = (special & 63) << 8 | envelope[var14++] & 255;
                    buf = new byte[len];
                    System.arraycopy(envelope, var14, buf, 0, len);
                    var14 += len;
                    list.add(buf);
                    break;
                case 2:
                    len = (envelope[var14++] & 255) << 0 | (envelope[var14++] & 255) << 8 | (envelope[var14++] & 255) << 16 | (envelope[var14++] & 255) << 24;
                    buf = new byte[len];
                    System.arraycopy(envelope, var14, buf, 0, len);
                    var14 += len;
                    list.add(buf);
                    break;
                case 3:
                    int flag = (special & 48) >> 4;
                    long val;
                    switch(flag) {
                        case 0:
                            val = (long)envelope[var14++] & 255L | (long)envelope[var14++] << 8;
                            list.add(String.valueOf(val).getBytes(ASCII));
                            break;
                        case 1:
                            val = ((long)envelope[var14++] & 255L) << 0 | ((long)envelope[var14++] & 255L) << 8 | ((long)envelope[var14++] & 255L) << 16 | (long)envelope[var14++] << 24;
                            list.add(String.valueOf(val).getBytes(ASCII));
                            break;
                        case 2:
                            val = ((long)envelope[var14++] & 255L) << 0 | ((long)envelope[var14++] & 255L) << 8 | ((long)envelope[var14++] & 255L) << 16 | ((long)envelope[var14++] & 255L) << 24 | ((long)envelope[var14++] & 255L) << 32 | ((long)envelope[var14++] & 255L) << 40 | ((long)envelope[var14++] & 255L) << 48 | (long)envelope[var14++] << 56;
                            list.add(String.valueOf(val).getBytes(ASCII));
                            break;
                        case 3:
                            int loBits = special & 15;
                            switch(loBits) {
                                case 0:
                                    val = ((long)envelope[var14++] & 255L) << 0 | ((long)envelope[var14++] & 255L) << 8 | (long)envelope[var14++] << 16;
                                    list.add(String.valueOf(val).getBytes(ASCII));
                                    break;
                                case 14:
                                    val = (long)envelope[var14++];
                                    list.add(String.valueOf(val).getBytes(ASCII));
                                    break;
                                default:
                                    list.add(String.valueOf(loBits - 1).getBytes(ASCII));
                            }
                    }
            }
        }

        return list;
    }

    private static byte[] readSpecialStringEncoded(int type,ByteBuffer buf) throws IOException {
        switch(type) {
            case 0:
                return readInteger8Bits(buf);
            case 1:
                return readInteger16Bits(buf);
            case 2:
                return readInteger32Bits(buf);
            case 3:
                return readLzfString(buf);
            default:
                throw new IllegalStateException("Unknown special encoding: " + type);
        }
    }

    private static byte[] readInteger8Bits(ByteBuffer buf) throws IOException {
        return String.valueOf(readByte(buf)).getBytes(ASCII);
    }

    private static byte[] readInteger16Bits(ByteBuffer buf) throws IOException {
        long val = ((long)readByte(buf) & 255L) << 0 | ((long)readByte(buf) & 255L) << 8;
        return String.valueOf(val).getBytes(ASCII);
    }

    private static byte[] readInteger32Bits(ByteBuffer buf) throws IOException {
        byte[] bs = readBytes(4,buf);
        long val = ((long)bs[3] & 255L) << 24 | ((long)bs[2] & 255L) << 16 | ((long)bs[1] & 255L) << 8 | ((long)bs[0] & 255L) << 0;
        return String.valueOf(val).getBytes(ASCII);
    }

    private static byte[] readLzfString(ByteBuffer buf) throws IOException {
        int clen = (int)readLength(buf);
        int ulen = (int)readLength(buf);
        byte[] src = readBytes(clen,buf);
        byte[] dest = new byte[ulen];
        expand(src, dest);
        return dest;
    }

    private static void expand(byte[] src, byte[] dest) {
        int srcPos = 0;
        int destPos = 0;

        do {
            int ctrl = src[srcPos++] & 255;
            if(ctrl < MAX_LITERAL) {
                ++ctrl;
                System.arraycopy(src, srcPos, dest, destPos, ctrl);
                destPos += ctrl;
                srcPos += ctrl;
            } else {
                int len = ctrl >> 5;
                if(len == 7) {
                    len += src[srcPos++] & 255;
                }

                len += 2;
                ctrl = -((ctrl & 31) << 8) - 1;
                ctrl -= src[srcPos++] & 255;
                ctrl += destPos;

                for(int i = 0; i < len; ++i) {
                    dest[destPos++] = dest[ctrl++];
                }
            }
        } while(destPos < dest.length);

    }
}
