import lombok.SneakyThrows;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.JPEGTranscoder;

import java.io.*;

class BatikSvgTranscoder implements SvgTranscoder {

    private final JPEGTranscoder transcoder = new JPEGTranscoder();

    private final Object monitor = new Object();

    private final int count  = -1;

    private final AtomicInteger atomicCounter = new AtomicInteger()  ;

    private final Map<String, String> mapping = new ConcurrentHashMap<>();

    BatikSvgTranscoder() {
        this.transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, (float) 0.8);
    }


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