package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);
    private static final String BRL = "BRL";

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    public CurrencyServiceJpa(CurrencyRepository repository, BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.bcb.data-cotacao:10-14-2026}")
    private String dataCotacao;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(
            String sourceCurrency,
            String targetCurrency
    ) throws Exception {
        String source = sourceCurrency.toUpperCase();
        String target = targetCurrency.toUpperCase();
        String environment = "Currency API running in Port: " + serverPort;

        // 1) Consulta a API externa do Banco Central
        try {
            Double rate = getRateFromBcb(source, target);
            if (rate != null) {
                return new CurrencyResponse(source, target, rate,
                        environment + " - Source: BCB (" + dataCotacao + ")");
            }
            log.info("BCB sem cotação para {}-{}, usando banco local", source, target);
        } catch (Exception e) {
            // Fallback Pattern: falha na comunicação externa (ou circuito aberto)
            log.warn("Falha ao consultar BCB para {}-{}: {}", source, target, e.getMessage());
        }

        // 2) Fallback: cotação cadastrada no banco de dados local
        var currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));
        return CurrencyResponse.fromEntity(currency, environment + " - Source: Local DB (fallback)");
    }

    /**
     * O BCB informa quantos BRL vale 1 unidade de cada moeda.
     * Taxa source->target = (BRL por source) / (BRL por target).
     * Retorna null quando o BCB não possui cotação para alguma das moedas.
     */
    private Double getRateFromBcb(String source, String target) {
        Double brlPorSource = getBrlPorUnidade(source);
        if (brlPorSource == null) return null;
        Double brlPorTarget = getBrlPorUnidade(target);
        if (brlPorTarget == null) return null;
        return brlPorSource / brlPorTarget;
    }

    private Double getBrlPorUnidade(String moeda) {
        if (BRL.equals(moeda)) return 1.0;
        return bcbClientService.getCotacao(moeda, dataCotacao);
    }
}
