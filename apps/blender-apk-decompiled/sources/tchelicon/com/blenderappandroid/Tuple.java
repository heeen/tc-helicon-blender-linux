package tchelicon.com.blenderappandroid;

/* JADX INFO: loaded from: classes.dex */
public class Tuple<One, Two, Three> implements Cloneable {
    public byte one;
    public byte three;
    public byte two;

    public Tuple(byte b, byte b2, byte b3) {
        this.one = b;
        this.two = b2;
        this.three = b3;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException unused) {
            return null;
        }
    }
}
