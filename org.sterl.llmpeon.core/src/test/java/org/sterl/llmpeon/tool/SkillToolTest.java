package org.sterl.llmpeon.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.sterl.llmpeon.AbstractMemoryFileTest;
import org.sterl.llmpeon.skill.SkillService;
import org.sterl.llmpeon.tool.tools.SkillTool;

class SkillToolTest extends AbstractMemoryFileTest {

    @Test
    void test() throws Exception {
        // GIVEN
        
        var skillDir = Files.createDirectory(tmp.resolve("foo"));
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                description: Does something useful
                ---
                body haha
                """);
        // AND
        var barDir = Files.createDirectory(skillDir.resolve("refs"));
        Files.writeString(barDir.resolve("bar.md"), "foo in bar");
        
        var subject = new SkillTool(new SkillService(tmp));
        // WHEN
        assertThat(subject.skillRead("foo")).contains("body haha");
        // THEN
        assertThat(subject.skillReadFile("foo", "refs/bar.md")).isEqualTo("foo in bar");
        assertThat(subject.skillReadFile("foo", "/refs/bar.md")).isEqualTo("foo in bar");
        
        // THEN
        assertThat(subject.skillReadFile("foo", "foo/refs/bar.md")).isEqualTo("foo in bar");
        assertThat(subject.skillReadFile("foo", "/foo/refs/bar.md")).isEqualTo("foo in bar");
    }

}
