/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.monitor.executablelister;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for parsing & listing output from external processes
 * that are started with Runtime.getRuntime().exec()
 * Created by janne on 5.11.2015.
 */
@Slf4j
abstract class AbstractExecLister<T> {

    protected abstract String getCommand();
    protected abstract Splitter getParsedDataSplitter();
    protected abstract int numberOfColumnsToParse();
    protected abstract T parse(List<String> columns);

    boolean discardFirstDataLineFromParsed() {
        return false;
    }

    void validateSupportedOs() throws ExecListingFailedException {
        if (!SystemUtils.IS_OS_LINUX) {
            throw new ExecListingFailedException("only linux is supported");
        }
    }

    @Getter
    @Setter
    class ProcessOutputs {
        String out;
        String err;
    }

    public ListedData<T> list() throws ExecListingFailedException {
        validateSupportedOs();
        try {
            ProcessOutputs outputs = executeProcess();

            ArrayList<String> jmxRepresentation = new ArrayList<>();
            try (BufferedReader input = new BufferedReader(new StringReader(outputs.getOut()))) {
                ArrayList<T> parsedData = parseData(input, jmxRepresentation);
                ListedData<T> data = new ListedData<T>();
                data.setParsedData(parsedData);
                data.setJmxData(jmxRepresentation);
                return data;
            }
        } catch (IOException ioe) {
            throw new ExecListingFailedException(ioe);
        } catch (InterruptedException e) {
            throw new ExecListingFailedException(e);
        }
    }

    /**
     * Method for testability
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    ProcessOutputs executeProcess() throws IOException, InterruptedException {

        ProcessBuilder b = new ProcessBuilder("/bin/sh", "-c", getCommand());

        Process p = b.start();
        p.waitFor();
        ProcessOutputs outputs = new ProcessOutputs();
        // need to read all of output everytime, reading it partially causes bad problems
        outputs.setOut(CharStreams.toString(new InputStreamReader(p.getInputStream())).replace("'", ""));
        outputs.setErr(CharStreams.toString(new InputStreamReader(p.getErrorStream())));
        return outputs;
    }

    private ArrayList<T> parseData(BufferedReader input, ArrayList<String> jmxRepresentation) throws IOException {

        ArrayList<T> parsed = new ArrayList<T>();
        Splitter splitter = getParsedDataSplitter();
        if (discardFirstDataLineFromParsed()) {
            String discardedHeaderLine = input.readLine();
            jmxRepresentation.add(discardedHeaderLine);
        }
        String line = null;
        while ((line = input.readLine()) != null) {
            if (line.trim().length() > 0) {
                jmxRepresentation.add(line);
                T data = parseLine(line, splitter);
                parsed.add(data);
            }
        }
        return parsed;
    }

    private T parseLine(String line, Splitter splitter) {
        List<String> columns = splitter.splitToList(line);
        if (columns.size() != numberOfColumnsToParse()) {
            throw new ExecListingFailedException("output should have "
                    + numberOfColumnsToParse()
                    + " columns, had "
                    + columns.size()
                    + ": " + line);
        }
        T data = parse(columns);
        return data;
    }

}
