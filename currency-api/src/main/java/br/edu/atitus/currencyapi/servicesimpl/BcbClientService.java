package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BcbClientService {

    private static final Logger log = LoggerFactory.getLogger(BcbClientService.class);

    private final BcbClient bcbClient;

    public BcbClientService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    /**
     * Retorna a cotação de venda (em BRL) da moeda na data informada,
     * ou null quando o BCB responde sem cotação para essa moeda/data.
     * Respostas sem cotação (null) não são armazenadas no cache.
     */
    @Cacheable(
            value = "bcb-api",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getCotacao(String moeda, String dataCotacao) {
        log.info("Consultando BCB: moeda={} data={}", moeda, dataCotacao);
        BcbCotacaoResponse response = bcbClient.getCotacaoMoedaDia(
                "'" + moeda + "'",
                "'" + dataCotacao + "'",
                "json");

        if (response == null || response.value() == null || response.value().isEmpty()) {
            log.info("BCB sem cotação para moeda={} data={}", moeda, dataCotacao);
            return null;
        }
        // O último boletim do dia é o de fechamento (PTAX)
        var cotacao = response.value().get(response.value().size() - 1);
        return cotacao.cotacaoVenda();
    }
}
