package com.magalu.devolucao.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Arrays;

/** Executar diretamente pela IDE, usando o console do terminal. */
public class GerarSenhaHash {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) throw new IllegalStateException("Execute em um terminal interativo (ver README)");
        char[] senha = console.readPassword("Senha da nova conta: ");
        if (senha == null || senha.length < 8) throw new IllegalArgumentException("Use ao menos 8 caracteres");
        try { System.out.println(new BCryptPasswordEncoder().encode(new String(senha))); }
        finally { Arrays.fill(senha, '\0'); }
    }
}
