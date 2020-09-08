// IPowerManager.aidl
package android.os;

// Declare any non-default types here with import statements

interface IPowerManager {
	void acquireWakeLock(IBinder iBinder, int i, String str);

	boolean isInteractive();

	boolean isScreenOn();

	void releaseWakeLock(IBinder iBinder, int i);
}
