package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Contains version information about this ProtoStream release.
 *
 * @author anistor@redhat.com
 * @since 4.2
 */
public final class Version implements Comparable<Version> {

   private static final Version VERSION = getArtifactVersion();

   /**
    * We try to obtain the Maven pom.properties of the artifact and get the build version from there.
    */
   private static Version getArtifactVersion() {
      int major = 0;
      int minor = 0;
      int micro = 0;
      String suffix = null;

      InputStream res = Version.class.getResourceAsStream("/META-INF/maven/org.infinispan.protostream/protostream/pom.properties");
      if (res != null) {
         try {
            Properties pomProps = new Properties();
            pomProps.load(res);
            String version = pomProps.getProperty("version", "0.0.0-UNKNOWN");
            String[] versionParts = version.split("[.\\-]");
            major = Integer.parseInt(versionParts[0]);
            if (versionParts.length > 1) {
               minor = Integer.parseInt(versionParts[1]);
            }
            if (versionParts.length > 2) {
               micro = Integer.parseInt(versionParts[2]);
            }
            if (versionParts.length > 3) {
               suffix = versionParts[3];
            }
         } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // ignored
         }
      }

      return new Version(major, minor, micro, suffix);
   }

   public static Version getVersion() {
      return VERSION;
   }

   public static void main(String[] args) {
      System.out.println("ProtoStream version " + getVersion());
   }

   private final int major;
   private final int minor;
   private final int micro;
   private final String suffix;
   private final String versionString;

   public Version(int major, int minor, int micro) {
      this(major, minor, micro, null);
   }

   public Version(int major, int minor, int micro, String suffix) {
      this.major = major;
      this.minor = minor;
      this.micro = micro;
      this.suffix = suffix;
      versionString = major + "." + minor + "." + micro + (suffix != null ? "." + suffix : "");
   }

   public int getMajor() {
      return major;
   }

   public int getMinor() {
      return minor;
   }

   public int getMicro() {
      return micro;
   }

   public String getSuffix() {
      return suffix;
   }

   @Override
   public String toString() {
      return versionString;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null || obj.getClass() != getClass()) {
         return false;
      }
      Version other = (Version) obj;
      return other.major == major && other.minor == minor && other.micro == micro && Objects.equals(suffix, other.suffix);
   }

   @Override
   public int hashCode() {
      return 31 * (31 * (31 * major + minor) + micro) + (suffix != null ? suffix.hashCode() : 0);
   }

   @Override
   public int compareTo(Version other) {
      if (this == other) {
         return 0;
      }
      int d = major - other.major;
      if (d == 0) {
         d = minor - other.minor;
         if (d == 0) {
            d = micro - other.micro;
            if (d == 0) {
               d = suffix.compareTo(other.suffix);
            }
         }
      }
      return d;
   }
}
