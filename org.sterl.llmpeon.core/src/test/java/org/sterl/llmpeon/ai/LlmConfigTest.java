package org.sterl.llmpeon.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmConfigTest {

    @Test
    void planAgentConfigUsesPlanTemperatureNotDevTemperature() {
        // GIVEN
        var config = LlmConfig.builder().planTemperature(1.0).devTemperature(0.6).build();

        // WHEN / THEN
        assertThat(config.planAgentConfig().getTemperature()).isEqualTo(1.0);
        assertThat(config.devAgentConfig().getTemperature()).isEqualTo(0.6);
    }
}
