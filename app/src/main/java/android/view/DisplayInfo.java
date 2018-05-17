package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public class DisplayInfo implements Parcelable {
    public static final Creator<DisplayInfo> CREATOR = new C02801();

    static class C02801 implements Creator<DisplayInfo> {
        C02801() {
        }

        public DisplayInfo createFromParcel(Parcel source) {
            throw new AssertionError("should be a framework class?");
        }

        public DisplayInfo[] newArray(int size) {
            throw new AssertionError("should be a framework class?");
        }
    }

    public int describeContents() {
        throw new AssertionError("should be a framework class?");
    }

    public void writeToParcel(Parcel dest, int flags) {
        throw new AssertionError("should be a framework class?");
    }
}
