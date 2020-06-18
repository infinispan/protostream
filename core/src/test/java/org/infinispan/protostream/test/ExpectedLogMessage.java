package org.infinispan.protostream.test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * The {@code ExpectedLogMessage} rule allows you to verify that your code logs a specific message or not.
 *
 * @author anistor@redhat.com
 */
public final class ExpectedLogMessage implements TestRule {

   private static final class ExpectedLoggingEvent {

      final int expectedOccurrences;           // >= 0

      final String expectedLogMessageRegexp;   // never null

      final Pattern expectedLogMessagePattern; // never null

      final Level expectedLogLevel;            // can be null -> any

      final List<LogEvent> matchingEvents = Collections.synchronizedList(new ArrayList<>());

      ExpectedLoggingEvent(int expectedOccurrences, Level expectedLogLevel, String expectedLogMessageRegexp) {
         if (expectedOccurrences < 0) {
            throw new IllegalArgumentException("Number of expected occurrences cannot be negative: " + expectedOccurrences);
         }
         if (expectedLogMessageRegexp == null || expectedLogMessageRegexp.isEmpty()) {
            throw new IllegalArgumentException("The expected message regexp cannot be null or empty: " + expectedLogMessageRegexp);
         }
         this.expectedOccurrences = expectedOccurrences;
         this.expectedLogLevel = expectedLogLevel;
         this.expectedLogMessageRegexp = expectedLogMessageRegexp;
         this.expectedLogMessagePattern = Pattern.compile(expectedLogMessageRegexp);
      }

      void match(LogEvent e) {
         if (expectedLogLevel != null && !expectedLogLevel.equals(e.getLevel())) {
            return;
         }
         Matcher matcher = expectedLogMessagePattern.matcher(e.getMessage().getFormattedMessage());
         if (matcher.matches()) {
            matchingEvents.add(e);
         }
      }

      String complain() {
         if (expectedOccurrences != matchingEvents.size()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected ").append(expectedOccurrences).append(" log messages matching pattern ").append(expectedLogMessageRegexp);
            if (expectedLogLevel != null) {
               sb.append(" and log level ").append(expectedLogLevel);
            }
            sb.append(" but found ").append(matchingEvents.size()).append(" occurrences");
            for (LogEvent e : matchingEvents) {
               sb.append("\n\t").append(e.getLevel()).append(' ').append(e.getMessage().getFormattedMessage());
            }
            return sb.toString();
         }
         return null;
      }

      @Override
      public String toString() {
         return "ExpectedLoggingEvent{occur=" + expectedOccurrences + ", level='" + expectedLogLevel + '\'' + ", regexp=" + expectedLogMessageRegexp + '}';
      }
   }

   private final List<ExpectedLoggingEvent> expectations = new ArrayList<>();

   private ExpectedLogMessage() {
   }

   /**
    * Creates a {@link TestRule rule} that does not mandate anything (test behaves as if this rule does not exist).
    * Further calls to {@link #expect(int, Level, String)} are required in order to specify expectations.
    */
   public static ExpectedLogMessage any() {
      return new ExpectedLogMessage();
   }

   /**
    * Expect 0 or more occurrences of a message.
    *
    * @param occurrences   positive number of occurrences (can be 0)
    * @param level         level, optional ({@code null} means any)
    * @param messageRegexp the regexp of the message (required)
    * @return self, for chaining
    */
   public ExpectedLogMessage expect(int occurrences, Level level, String messageRegexp) {
      expectations.add(new ExpectedLoggingEvent(occurrences, level, messageRegexp));
      return this;
   }

   @Override
   public Statement apply(Statement base, Description description) {
      return new Statement() {
         @Override
         public void evaluate() throws Throwable {
            String appenderName = "ExpectedLogMessageAppender";
            AbstractAppender appender = new AbstractAppender(appenderName, null, null, true, Property.EMPTY_ARRAY) {
               @Override
               public void append(LogEvent event) {
                  for (ExpectedLoggingEvent expect : expectations) {
                     expect.match(event);
                  }
               }
            };
            appender.start();

            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            loggerContext.getConfiguration().addAppender(appender);
            loggerContext.getRootLogger().addAppender(appender);
            loggerContext.updateLoggers();

            try {
               base.evaluate();
            } finally {
               loggerContext.getRootLogger().removeAppender(appender);
               loggerContext.updateLoggers();
               appender.stop();
            }

            StringBuilder failures = null;
            for (ExpectedLoggingEvent expect : expectations) {
               String complain = expect.complain();
               if (complain != null) {
                  if (failures == null) {
                     failures = new StringBuilder();
                  } else {
                     failures.append('\n');
                  }
                  failures.append(complain);
               }
            }
            if (failures != null) {
               fail(failures.toString());
            }
         }
      };
   }

   @Override
   public String toString() {
      return expectations.toString();
   }
}
