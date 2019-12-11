package landing;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import com.synopsys.integration.synopsys.detect.scripts.landing.Semver;

public class SemverCompareTests {
    @Test
    public void compareMatches() {
        Semver same1 = new Semver(1, 2, 3, "");
        Semver same2 = new Semver(1, 2, 3, "");
        Assertions.assertEquals(0, same1.compareTo(same2));
    }

    @Test
    public void compareMajor() {
        Semver lower = new Semver(1, 2, 3, "");
        Semver higher = new Semver(2, 2, 3, "");
        Assertions.assertEquals(1, higher.compareTo(lower));
    }

    @Test
    public void compareMinor() {
        Semver lower = new Semver(1, 2, 3, "");
        Semver higher = new Semver(1, 3, 3, "");
        Assertions.assertEquals(1, higher.compareTo(lower));
    }

    @Test
    public void comparePatch() {
        Semver lower = new Semver(1, 2, 3, "");
        Semver higher = new Semver(1, 2, 4, "");
        Assertions.assertEquals(1, higher.compareTo(lower));
    }
}
