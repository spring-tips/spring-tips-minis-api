// let's look at this function
void transcode(InputStream inputStream, OutputStream outputStream) {
    synchronized (this.monitor) {
        var input = new TranscoderInput( inputStream);
        var output = new TranscoderOutput(outputStream);
        this.transcoder.transcode(input, output);
    }
}