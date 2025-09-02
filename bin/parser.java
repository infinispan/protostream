///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * A helper class to parse Java Flight Recorder (JFR) files
 * and extract insights from custom Infinispan ProtoStream events.
 *
 * It specifically looks for 'org.infinispan.protostream.ResizeEvent'
 * and 'org.infinispan.protostream.AllocateEvent' to generate a
 * summary about buffer allocation and resizing patterns.
 */
@Command(name = "parser", mixinStandardHelpOptions = true, version = "parser 0.1",
        description = "parser made with jbang")
class parser implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to JFR file")
    private File file;

    public static void main(String... args) {
        int exitCode = new CommandLine(new parser()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Path p = file.toPath();
        try (PrintWriter writer = new PrintWriter(System.out)) {
            parseAndSummarize(p, writer);
        }
        return 0;
    }

    private static final String RESIZE_EVENT_NAME = "org.infinispan.protostream.ResizeEvent";
    private static final String ALLOCATE_EVENT_NAME = "org.infinispan.protostream.AllocateEvent";

    /**
     * Parses a JFR file to analyze ProtoStream buffer events and prints a summary.
     *
     * @param jfrFilePath The path to the JFR file.
     * @param writer      The PrintWriter to which the summary report will be written.
     * @throws IOException If an error occurs while reading the JFR file.
     */
    public void parseAndSummarize(Path jfrFilePath, PrintWriter writer) throws IOException {
        if (jfrFilePath == null || writer == null) {
            throw new IllegalArgumentException("JFR file path and PrintWriter cannot be null.");
        }

        // --- Data Collectors ---
        // Metrics for Resize Events
        long resizeEventCount = 0;
        long totalBytesResized = 0;
        int maxResizeFrom = 0;
        int maxResizeTo = 0;
        Map<String, HotSpot> resizeHotspots = new HashMap<>();

        // Metrics for Allocate Events
        long allocateEventCount = 0;
        long totalBytesAllocated = 0;
        int maxAllocationSize = 0;
        Map<String, HotSpot> allocationHotspots = new HashMap<>();

        try (RecordingFile recordingFile = new RecordingFile(jfrFilePath)) {
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();

                switch (event.getEventType().getName()) {
                    case RESIZE_EVENT_NAME:
                        resizeEventCount++;
                        int fromSize = event.getValue("before");
                        int toSize = event.getValue("after");
                        totalBytesResized += (long) toSize - fromSize;

                        if (toSize > maxResizeTo) {
                            maxResizeTo = toSize;
                            maxResizeFrom = fromSize;
                        }

                        // Use the first frame of the stack trace as the hotspot identifier
                        Optional.ofNullable(event.getStackTrace()).ifPresent(stackTrace -> {
                            if (!stackTrace.getFrames().isEmpty()) {
                                String topFrame = stackTrace.getFrames().get(0).toString();
                                resizeHotspots.computeIfAbsent(topFrame, k -> new HotSpot(stackTrace)).inc();
                            }
                        });
                        break;

                    case ALLOCATE_EVENT_NAME:
                        allocateEventCount++;
                        int newSize = event.getValue("size");
                        totalBytesAllocated += newSize;

                        if (newSize > maxAllocationSize) {
                            maxAllocationSize = newSize;
                        }

                        Optional.ofNullable(event.getStackTrace()).ifPresent(stackTrace -> {
                            if (!stackTrace.getFrames().isEmpty()) {
                                String topFrame = stackTrace.getFrames().get(0).toString();
                                allocationHotspots.computeIfAbsent(topFrame, k -> new HotSpot(stackTrace)).inc();
                            }
                        });
                        break;
                }
            }
        }

        // --- Generate Summary Report ---
        generateReport(
                writer,
                resizeEventCount, totalBytesResized, maxResizeFrom, maxResizeTo, resizeHotspots,
                allocateEventCount, totalBytesAllocated, maxAllocationSize, allocationHotspots
        );
    }

    private void generateReport(PrintWriter writer,
                                long resizeEventCount, long totalBytesResized, int maxResizeFrom, int maxResizeTo, Map<String, HotSpot> resizeHotspots,
                                long allocateEventCount, long totalBytesAllocated, int maxAllocationSize, Map<String, HotSpot> allocationHotspots) {

        writer.println("=========================================================");
        writer.println("   Infinispan ProtoStream Buffer Events JFR Summary      ");
        writer.println("=========================================================");
        writer.println();

        // --- Resize Events Section ---
        writer.println("--- Buffer Resize Events (" + RESIZE_EVENT_NAME + ") ---");
        if (resizeEventCount > 0) {
            writer.printf("Total Resize Events: %,d%n", resizeEventCount);
            writer.printf("Total Bytes Added by Resizing: %,d bytes%n", totalBytesResized);
            writer.printf("Average Resize Increase: %,.2f bytes%n", (double) totalBytesResized / resizeEventCount);
            writer.printf("Largest Single Resize: from %,d to %,d bytes (an increase of %,d bytes)%n", maxResizeFrom, maxResizeTo, maxResizeTo - maxResizeFrom);
            writer.println();
            writer.println("Top 5 Most Common Resize Locations (Stack Trace):");
            printTopHotspots(writer, resizeHotspots, 5);
        } else {
            writer.println("No resize events found in this recording.");
        }
        writer.println();

        // --- Allocate Events Section ---
        writer.println("--- Buffer Allocate Events (" + ALLOCATE_EVENT_NAME + ") ---");
        if (allocateEventCount > 0) {
            writer.printf("Total Allocation Events: %,d%n", allocateEventCount);
            writer.printf("Total Bytes Allocated: %,d bytes%n", totalBytesAllocated);
            writer.printf("Average Allocation Size: %,.2f bytes%n", (double) totalBytesAllocated / allocateEventCount);
            writer.printf("Largest Single Allocation: %,d bytes%n", maxAllocationSize);
            writer.println();
            writer.println("Top 5 Most Common Allocation Locations (Stack Trace):");
            printTopHotspots(writer, allocationHotspots, 5);
        } else {
            writer.println("No allocation events found in this recording.");
        }
        writer.println();
        writer.println("=========================================================");
        writer.flush();
    }

    /**
     * Helper to sort and print the most frequent call sites.
     */
    private void printTopHotspots(PrintWriter writer, Map<String, HotSpot> hotspots, int limit) {
        if (hotspots.isEmpty()) {
            writer.println("  (No stack trace information available)");
            return;
        }

        // Sort the map by value (count) in descending order
        LinkedHashMap<String, HotSpot> sortedHotspots = hotspots.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue((a, b) -> Integer.compare(b.times(), a.times())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        int count = 0;
        for (Map.Entry<String, HotSpot> entry : sortedHotspots.entrySet()) {
            if (count++ >= limit) {
                break;
            }
            HotSpot hs = entry.getValue();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (RecordedFrame frame : hs.stackTrace.getFrames()) {
                if (first) {
                    sb.append(frameToString(frame));
                    first = false;
                } else {
                    sb.append('\t').append("at ").append(frameToString(frame));
                }
                sb.append(System.lineSeparator());
            }
            writer.printf("  - [%,d times]:%n%s%n", hs.times(), sb);
        }
    }

    private String frameToString(RecordedFrame frame) {
        RecordedMethod method = frame.getMethod();
        return String.format("%s.%s:%d [%s]", method.getType().getName(), method.getName(), frame.getLineNumber(), frame.getType());
    }

    private static class HotSpot {
        private final RecordedStackTrace stackTrace;
        private int times;

        private HotSpot(RecordedStackTrace stackTrace) {
            this.stackTrace = stackTrace;
        }

        public void inc() {
            times++;
        }

        public int times() {
            return times;
        }
    }
}
