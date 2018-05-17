package virtualdisplay;

import android.annotation.TargetApi;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.view.Surface;

@TargetApi(21)
public class LollipopVirtualDisplayFactory implements VirtualDisplayFactory {
    MediaProjection mediaProjection;

    public LollipopVirtualDisplayFactory(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public VirtualDisplay create(String name, int width, int height, int dpi, int flags, Surface surface, Handler handler) {
        final android.hardware.display.VirtualDisplay vd = this.mediaProjection.createVirtualDisplay(name, width, height, dpi, flags, surface, null, handler);
        return new VirtualDisplay() {
            public void release() {
                vd.release();
            }
        };
    }

    public void release() {
        this.mediaProjection.stop();
    }
}
