package com.magalu.devolucao.config;

import com.magalu.devolucao.repository.ContaAcessoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.time.Clock;
import java.time.LocalDateTime;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(ContaAcessoRepository contas, Clock clock) {
        return login -> {
            var conta = contas.findByLogin(login)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));

            if ((conta.getUsuario() == null) == (conta.getFuncionario() == null)) {
                throw new UsernameNotFoundException("Conta sem vinculo valido");
            }

            String role = conta.getUsuario() != null ? "CLIENTE" : "FUNCIONARIO";
            boolean isLocked = conta.isBloqueada(LocalDateTime.now(clock));

            return User.withUsername(conta.getLogin())
                    .password(conta.getSenhaHash())
                    .roles(role)
                    .accountLocked(isLocked)
                    .disabled(!conta.isAtiva())
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ContaAcessoRepository contas) throws Exception {
        // CSRF permanece habilitado: usar o token de /api/v1/csrf nas escritas.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.css", "/app.js",
                        "/vendor/jsQR.js", "/fonts/Poppins-Regular.ttf", "/fonts/Poppins-Bold.ttf", 
                        "/api/v1/csrf", "/manifest.webmanifest", "/icon.svg", "/sw.js").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/sessao").permitAll()
                .requestMatchers("/api/v1/funcionario/**").hasRole("FUNCIONARIO")
                .requestMatchers("/api/v1/devolucoes/**", "/api/v1/itens/**", "/api/v1/notificacoes/**").hasRole("CLIENTE")
                .anyRequest().authenticated()
        );

        http.httpBasic(Customizer.withDefaults());

        http.formLogin(form -> form
                .loginProcessingUrl("/api/v1/sessao")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    
                    if (ex instanceof LockedException) {
                        res.getWriter().write("{\"mensagem\":\"Sua conta foi bloqueada por questoes de seguranca. Tente novamente em 15 minutos.\"}");
                    } else {
                        String username = req.getParameter("username");
                        String msg = "Login ou senha invalidos.";
                        
                        if (username != null) {
                            var contaOpt = contas.findByLogin(username);
                            if (contaOpt.isPresent()) {
                                int falhas = contaOpt.get().getTentativasFalhas();
                                int restantes = 5 - falhas; // 5 é o limite definido no listener
                                if (restantes > 0 && restantes < 5) {
                                    msg = "Senha incorreta. Voce tem mais " + restantes + " tentativa" + (restantes == 1 ? "" : "s") + " antes do bloqueio.";
                                }
                            }
                        }
                        res.getWriter().write("{\"mensagem\":\"" + msg + "\"}");
                    }
                })
        );

        http.logout(logout -> logout
                .logoutUrl("/api/v1/sessao/sair")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
        );

        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; " +
                        "connect-src 'self'; media-src 'self' blob:; object-src 'none'; " +
                        "base-uri 'self'; frame-ancestors 'none'; form-action 'self'"
                ))
        );

        return http.build();
    }
}
