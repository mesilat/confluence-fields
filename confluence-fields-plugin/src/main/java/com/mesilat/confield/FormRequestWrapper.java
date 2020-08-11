package com.mesilat.confield;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormRequestWrapper extends HttpServletRequestWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    private static final Pattern CUSTOM_FIELD_PATTERN = Pattern.compile("Content-Disposition: form-data; name=\"customfield_(\\d+)\"");
    private final ServletInputStream stream;

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return stream;
    }

    public FormRequestWrapper(HttpServletRequest request, DataService dataService) throws IOException {
        super(request);
        
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (
            BufferedReader reader = request.getReader();
            OutputStreamWriter w = new OutputStreamWriter(buf);
            PrintWriter pw = new PrintWriter(w);
        ){
            do {
                String s = reader.readLine();
                if (s == null){
                    break;
                }
                pw.println(s);

                Matcher m = CUSTOM_FIELD_PATTERN.matcher(s);
                if (m.matches()){
                    long fieldId = Long.parseLong(m.group(1));
                    if (dataService.isConfluenceField(fieldId)){
                        pw.println(reader.readLine()); // Empty line
                        pw.println(reader.readLine().replace("~[$]~", ","));
                    }
                }
            } while (true);
        }
        
        //byte[] data = IOUtils.toByteArray(request.getInputStream());
        //File f = File.createTempFile("conf", ".dat", new File(System.getProperty("java.io.tmpdir")));
        //Files.write(f.toPath(), data);
        this.stream = new MyServletInputStream(new ByteArrayInputStream(buf.toByteArray()));
    }

    public static class MyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream stream;

        @Override
        public int read() throws IOException {
            return stream.read();
        }

        public MyServletInputStream(ByteArrayInputStream stream){
            this.stream = stream;
        }
    }
}