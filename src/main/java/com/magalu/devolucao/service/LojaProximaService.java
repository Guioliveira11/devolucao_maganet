package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.Loja;
import com.magalu.devolucao.repository.LojaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LojaProximaService {

    private final LojaRepository lojaRepository;

    /**
     * Retorna lojas ativas ordenadas por distancia crescente ate o ponto
     * informado. O filtro por raio evita listar lojas muito distantes
     * quando o usuario esta em uma regiao sem cobertura proxima.
     */
    public List<Loja> buscarLojasProximas(double latitudeUsuario, double longitudeUsuario, double raioKm) {
        if (!Double.isFinite(latitudeUsuario) || latitudeUsuario < -90 || latitudeUsuario > 90 ||
                !Double.isFinite(longitudeUsuario) || longitudeUsuario < -180 || longitudeUsuario > 180 ||
                !Double.isFinite(raioKm) || raioKm <= 0 || raioKm > 200)
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Latitude deve estar entre -90 e 90, longitude entre -180 e 180 e raio entre 0 (exclusivo) e 200 km");
        return lojaRepository.findProximas(latitudeUsuario, longitudeUsuario, raioKm).stream()
                .sorted(Comparator.comparingDouble(
                        loja -> loja.calcularDistancia(latitudeUsuario, longitudeUsuario)))
                .toList();
    }
}
