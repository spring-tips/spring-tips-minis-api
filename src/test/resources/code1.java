import lombok.SneakyThrows;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.JPEGTranscoder;

import java.io.*;

// 1. this is how you start a class
class BatikSvgTranscoder implements SvgTranscoder {

    private final JPEGTranscoder transcoder = new JPEGTranscoder();
    private final Object monitor = new Object();

    BatikSvgTranscoder() {
        this.transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, (float) 0.8);
    }

    // 2. i love me some Lombok
    @Override
    @SneakyThrows
    public void transcode(InputStream inputStream, OutputStream outputStream) {
        synchronized (this.monitor) {
            var input = new TranscoderInput(inputStream);
            var output = new TranscoderOutput(outputStream);
            this.transcoder.transcode(input, output);
        }
    }
}