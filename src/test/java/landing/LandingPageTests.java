package landing;

import com.synopsys.integration.synopsys.detect.scripts.landing.DetectVersionEntry;
import com.synopsys.integration.synopsys.detect.scripts.landing.DetectVersionSet;
import com.synopsys.integration.synopsys.detect.scripts.landing.LandingPageBuilder;
import com.synopsys.integration.synopsys.detect.scripts.landing.Semver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LandingPageTests {
    @Test
    public void parseSinglePath() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        List<String> paths = landingPageBuilder.parseDocumentListing("[{\"path\":\"example\"}]");
        Assertions.assertEquals(1, paths.size());
        Assertions.assertEquals("example", paths.get(0));
    }

    @Test
    public void parseThreePaths() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        List<String> paths = landingPageBuilder.parseDocumentListing("[{\"path\":\"one\"},{\"path\":\"two\"}, {\"path\":\"three\"}]");
        Assertions.assertEquals(3, paths.size());
        Assertions.assertEquals("one", paths.get(0));
        Assertions.assertEquals("two", paths.get(1));
        Assertions.assertEquals("three", paths.get(2));
    }

    @Test
    public void ignoresCommonFiles() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        Assertions.assertFalse(landingPageBuilder.parseVersionFromPath(".github").isPresent());
        Assertions.assertFalse(landingPageBuilder.parseVersionFromPath(".gitignore").isPresent());
        Assertions.assertFalse(landingPageBuilder.parseVersionFromPath(".travis.yml").isPresent());
        Assertions.assertFalse(landingPageBuilder.parseVersionFromPath("latest-commit-id.txt").isPresent());
    }

    @Test
    public void findsHtmlEntry() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        Optional<DetectVersionEntry> entry = landingPageBuilder.parseVersionFromPath("synopsys-detect-5.6.0-help.html");
        Assertions.assertTrue(entry.isPresent());
        Assertions.assertEquals(entry.get().getVersion(), "5.6.0");
        Assertions.assertEquals(entry.get().getSemver().getMajor(), 5);
        Assertions.assertEquals(entry.get().getSemver().getSpecial(), "");
    }

    @Test
    public void findsHtmlRcEntry() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        Optional<DetectVersionEntry> entry = landingPageBuilder.parseVersionFromPath("synopsys-detect-5.6.0-RC7-help.html");
        Assertions.assertTrue(entry.isPresent());
        Assertions.assertEquals(entry.get().getVersion(), "5.6.0-RC7");
        Assertions.assertEquals(entry.get().getSemver().getMajor(), 5);
        Assertions.assertEquals(entry.get().getSemver().getSpecial(), "RC7");
    }

    @Test
    public void findsNormalEntry() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        Optional<DetectVersionEntry> entry = landingPageBuilder.parseVersionFromPath("5.6.0");
        Assertions.assertTrue(entry.isPresent());
        Assertions.assertEquals(entry.get().getVersion(), "5.6.0");
        Assertions.assertEquals(entry.get().getSemver().getMajor(), 5);
    }

    @Test
    public void sortsSnapshotRC() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        List<DetectVersionEntry> entries = new ArrayList<>();
        entries.add(new DetectVersionEntry("5.6.0", "", new Semver(5, 6, 0, "")));
        entries.add(new DetectVersionEntry("6.0.0-SNAPSHOT", "", new Semver(6, 0, 0, "SNAPSHOT")));
        entries.add(new DetectVersionEntry("7.0.0-RC1", "", new Semver(7, 0, 0, "RC1")));
        DetectVersionSet set = landingPageBuilder.sortVersion(entries);
        Assertions.assertEquals(1, set.getReleased().size());
        Assertions.assertEquals(2, set.getSnapshot().size());
    }

    @Test
    public void latestBuildFirst() {
        LandingPageBuilder landingPageBuilder = new LandingPageBuilder();
        List<DetectVersionEntry> entries = new ArrayList<>();
        entries.add(new DetectVersionEntry("5.6.0", "", new Semver(5, 6, 0, "")));
        entries.add(new DetectVersionEntry("7.6.0", "", new Semver(5, 6, 0, "")));
        entries.add(new DetectVersionEntry("7.7.0", "", new Semver(5, 6, 0, "")));
        DetectVersionSet set = landingPageBuilder.sortVersion(entries);
        Assertions.assertEquals(3, set.getReleased().size());
        Assertions.assertEquals("7.7.0", set.getReleased().get(0).getVersion());
        Assertions.assertEquals("7.6.0", set.getReleased().get(1).getVersion());
        Assertions.assertEquals("5.6.0", set.getReleased().get(2).getVersion());
    }

}
