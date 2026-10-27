package com.magalu.devolucao.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosTest {

    @Test
    void aceitaCpfValido() {
        assertThat(Documentos.cpfValido("11144477735")).isTrue();
        assertThat(Documentos.cpfValido("39053344705")).isTrue();
    }

    @Test
    void recusaCpfComDigitoErradoRepetidoOuTamanhoInvalido() {
        assertThat(Documentos.cpfValido("12345678900")).isFalse();
        assertThat(Documentos.cpfValido("11111111111")).isFalse();
        assertThat(Documentos.cpfValido("1114447773")).isFalse();
        assertThat(Documentos.cpfValido(null)).isFalse();
    }

    @Test
    void aceitaCnpjValido() {
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cnpjValido("11444777000161")).isTrue();
    }

    @Test
    void recusaCnpjInvalido() {
        assertThat(Documentos.cnpjValido("11222333000180")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
        assertThat(Documentos.cnpjValido("123")).isFalse();
    }

    @Test
    void soDigitosRemoveMascara() {
        assertThat(Documentos.soDigitos("111.444.777-35")).isEqualTo("11144477735");
        assertThat(Documentos.soDigitos(null)).isEmpty();
    }
}
