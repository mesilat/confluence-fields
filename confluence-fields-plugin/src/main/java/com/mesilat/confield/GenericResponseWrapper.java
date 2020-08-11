package com.mesilat.confield;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GenericResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream capture;
    private ServletOutputStream output;
    private PrintWriter writer;

    public GenericResponseWrapper(HttpServletResponse response) {
        super(response);
        this.capture = new ByteArrayOutputStream(response.getBufferSize());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (this.writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        } else {
            if (this.output == null) {
                this.output = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        GenericResponseWrapper.this.capture.write(b);
                    }

                    @Override
                    public void flush() throws IOException {
                        GenericResponseWrapper.this.capture.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        GenericResponseWrapper.this.capture.close();
                    }
                };
            }

            return this.output;
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.output != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        } else {
            if (this.writer == null) {
                this.writer = new PrintWriter(new OutputStreamWriter(this.capture, this.getCharacterEncoding()));
            }

            return this.writer;
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        super.flushBuffer();
        if (this.writer != null) {
            this.writer.flush();
        } else if (this.output != null) {
            this.output.flush();
        }

    }

    private byte[] getCaptureAsBytes() throws IOException {
        if (this.writer != null) {
            this.writer.close();
        } else if (this.output != null) {
            this.output.close();
        }

        return this.capture.toByteArray();
    }

    public String getCaptureAsString() throws IOException {
        return new String(this.getCaptureAsBytes(), this.getCharacterEncoding());
    }
}