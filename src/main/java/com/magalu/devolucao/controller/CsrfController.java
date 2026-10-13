package com.magalu.devolucao.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class CsrfController {
    @GetMapping("/api/v1/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }
}
