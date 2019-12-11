package landing;

import java.util.Optional;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import com.synopsys.integration.synopsys.detect.scripts.landing.Semver;

public class SemverTests {
    @Test
    public void normalSemver() {
        assertSemver("1.2.3", 1, 2, 3, "");
    }

    @Test
    public void releaseCandidate() {
        assertSemver("1.2.3-RC3", 1, 2, 3, "RC3");
    }

    @Test
    public void snapshot() {
        assertSemver("1.2.3-SNAPSHOT", 1, 2, 3, "SNAPSHOT");
    }

    @Test
    public void invalidAlpha() {
        assertNoSemver("1.A.3");
    }

    @Test
    public void invalidDots() {
        assertNoSemver("1.A.3.4");
    }

    @Test
    public void missingValue() {
        assertNoSemver("1..4");
    }

    public void assertNoSemver(String raw) {
        Optional<Semver> semverMaybe = Semver.TryParse(raw);
        Assertions.assertFalse(semverMaybe.isPresent());
    }

    public static void assertSemver(String raw, int major, int minor, int patch, String special) {
        Optional<Semver> semverMaybe = Semver.TryParse(raw);
        Assertions.assertTrue(semverMaybe.isPresent());
        Semver semver = semverMaybe.get();
        Assertions.assertEquals(major, semver.getMajor());
        Assertions.assertEquals(minor, semver.getMinor());
        Assertions.assertEquals(patch, semver.getPatch());
        Assertions.assertEquals(special, semver.getSpecial());
    }
}
