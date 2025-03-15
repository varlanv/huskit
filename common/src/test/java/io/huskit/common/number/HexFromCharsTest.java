package io.huskit.common.number;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexFromCharsTest implements UnitTest {

    @Test
    @DisplayName("'intValue' when single hex char then return value")
    void intvalue_when_single_hex_char_then_return_value() {
        var subject = Hexadecimal.fromHexChars();

        assertThat(subject.intValue()).isZero();
    }

    @Test
    @DisplayName("'intValue' 'withZeroHexChar' then 'returnZero'")
    void intvalue_withzerohexchar_then_returnzero() {
        var subject = Hexadecimal.fromHexChars()
                .withHexChar('0');

        assertThat(subject.intValue()).isZero();
    }

    @Test
    @DisplayName("'intValue' with b0 then return 176")
    void intvalue_with_b0_then_return_176() {
        var subject = Hexadecimal.fromHexChars()
                .withHexChar('b')
                .withHexChar('0');

        assertThat(subject.intValue()).isEqualTo(176);
    }

    @Test
    @DisplayName("'intValue' with 1ca then return 458")
    void intvalue_with_1ca_then_return_458() {
        var subject = Hexadecimal.fromHexChars()
                .withHexChar('1')
                .withHexChar('c')
                .withHexChar('a');

        assertThat(subject.intValue()).isEqualTo(458);
    }
}
