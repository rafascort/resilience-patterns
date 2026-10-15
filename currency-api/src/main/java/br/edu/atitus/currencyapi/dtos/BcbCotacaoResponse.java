package br.edu.atitus.currencyapi.dtos;

import java.util.List;

public record BcbCotacaoResponse(
        List<Cotacao> value
) {
    public record Cotacao(
            Double cotacaoCompra,
            Double cotacaoVenda,
            String dataHoraCotacao,
            String tipoBoletim
    ) {
    }
}
