package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductServiceTermRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Le bloc Livraison et retours de la fiche produit")
class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void shouldInstallTheProposedTermsWhenTheFieldIsAbsent() {
        Product product = mapper.toEntity(request(null));

        assertThat(product.getServiceTerms())
                .extracting(ProductServiceTerm::getLabel, ProductServiceTerm::getValue)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("Livraison", "48 à 72 heures"),
                        org.assertj.core.api.Assertions.tuple("Paiement", "À la livraison ou par virement"),
                        org.assertj.core.api.Assertions.tuple("Retour", "Sous 7 jours"));
    }

    /**
     * Le cas qui compte : un vendeur qui retire tout le bloc ne doit pas le voir
     * revenir au prochain enregistrement. Une liste vide est une decision, un
     * champ absent est une absence de decision.
     */
    @Test
    void shouldKeepTheBlockEmptyWhenTheSellerRemovedEveryLine() {
        Product product = mapper.toEntity(request(List.of()));

        assertThat(product.getServiceTerms()).isEmpty();
    }

    @Test
    void shouldKeepTheSellerLinesInOrder() {
        Product product = mapper.toEntity(request(List.of(
                new ProductServiceTermRequest("Retour", "Sous 30 jours"),
                new ProductServiceTermRequest("Garantie", "2 ans"))));

        assertThat(product.getServiceTerms())
                .extracting(ProductServiceTerm::getLabel, ProductServiceTerm::getValue)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("Retour", "Sous 30 jours"),
                        org.assertj.core.api.Assertions.tuple("Garantie", "2 ans"));
    }

    @Test
    void shouldTrimTheSellerInput() {
        Product product = mapper.toEntity(request(List.of(
                new ProductServiceTermRequest("  Retour  ", "  Sous 30 jours  "))));

        assertThat(product.getServiceTerms().getFirst().getLabel()).isEqualTo("Retour");
        assertThat(product.getServiceTerms().getFirst().getValue()).isEqualTo("Sous 30 jours");
    }

    /** La modification suit la meme regle que la creation. */
    @Test
    void shouldReplaceTheLinesOnUpdate() {
        Product existing = mapper.toEntity(request(null));

        mapper.updateEntity(existing, request(List.of(
                new ProductServiceTermRequest("Livraison", "24 heures"))));

        assertThat(existing.getServiceTerms())
                .extracting(ProductServiceTerm::getValue)
                .containsExactly("24 heures");
    }

    @Test
    void shouldExposeTheLinesInTheResponse() {
        Product product = mapper.toEntity(request(List.of(
                new ProductServiceTermRequest("Retour", "Sous 30 jours"))));
        product.setId(1L);

        assertThat(mapper.toResponse(product).serviceTerms())
                .singleElement()
                .satisfies(term -> {
                    assertThat(term.label()).isEqualTo("Retour");
                    assertThat(term.value()).isEqualTo("Sous 30 jours");
                });
    }

    /** Les tailles sont libres : vetements d'enfants, pointures, taille unique. */
    @Test
    void shouldKeepFreeSizesInTheSellerOrder() {
        Product product = mapper.toEntity(requestWithSizes(List.of("4 ans", "6 ans", "38", "Taille unique")));

        assertThat(product.getSizes()).containsExactly("4 ans", "6 ans", "38", "Taille unique");
    }

    @Test
    void shouldTrimSizesAndDropBlanksAndDuplicates() {
        Product product = mapper.toEntity(requestWithSizes(List.of(" M ", "m", "L", "  ", "M")));

        assertThat(product.getSizes()).containsExactly("M", "L");
    }

    private ProductRequest request(List<ProductServiceTermRequest> serviceTerms) {
        return request(List.of(), serviceTerms);
    }

    private ProductRequest requestWithSizes(List<String> sizes) {
        return request(sizes, null);
    }

    private ProductRequest request(List<String> sizes, List<ProductServiceTermRequest> serviceTerms) {
        return new ProductRequest(
                "Pull",
                null,
                null,
                null,
                new BigDecimal("35.00"),
                5,
                null,
                null,
                "ACTIVE",
                List.of(),
                sizes,
                List.of(),
                List.of(),
                serviceTerms,
                null,
                null);
    }
}
