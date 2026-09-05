package com.ecommerce.backend.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Les informations proposees ne remplacent jamais celles du vendeur")
class DefaultStoreInformationTest {

    @Test
    void shouldFillAnEmptyStore() {
        Store store = new Store();

        assertThat(DefaultStoreInformation.fillBlanks(store)).isTrue();
        assertThat(store.getAddress()).isEqualTo(DefaultStoreInformation.ADDRESS);
        assertThat(store.getPhone()).isEqualTo(DefaultStoreInformation.PHONE);
        assertThat(store.getDescription()).isEqualTo(DefaultStoreInformation.DESCRIPTION);
    }

    @Test
    void shouldLeaveFilledFieldsAlone() {
        Store store = new Store();
        store.setAddress("12 rue des Archives, 75004 Paris");
        store.setPhone("01 42 78 90 12");
        store.setDescription("Prêt-à-porter depuis 1998.");

        assertThat(DefaultStoreInformation.fillBlanks(store)).isFalse();
        assertThat(store.getAddress()).isEqualTo("12 rue des Archives, 75004 Paris");
        assertThat(store.getPhone()).isEqualTo("01 42 78 90 12");
        assertThat(store.getDescription()).isEqualTo("Prêt-à-porter depuis 1998.");
    }

    @Test
    void shouldOnlyFillWhatIsMissing() {
        Store store = new Store();
        store.setAddress("12 rue des Archives, 75004 Paris");

        assertThat(DefaultStoreInformation.fillBlanks(store)).isTrue();
        assertThat(store.getAddress()).isEqualTo("12 rue des Archives, 75004 Paris");
        assertThat(store.getPhone()).isEqualTo(DefaultStoreInformation.PHONE);
    }

    /** Une chaine d'espaces est un champ vide, pas une valeur. */
    @Test
    void shouldTreatWhitespaceAsEmpty() {
        Store store = new Store();
        store.setAddress("   ");

        assertThat(DefaultStoreInformation.fillBlanks(store)).isTrue();
        assertThat(store.getAddress()).isEqualTo(DefaultStoreInformation.ADDRESS);
    }

    /**
     * Le cas qui compte pour le rattrapage au demarrage : un vendeur qui a
     * renseigne son adresse et efface son telephone ne doit pas voir le gabarit
     * revenir au redemarrage suivant.
     */
    @Test
    void shouldConsiderAContactBlockTouchedAsSoonAsOneFieldIsSet() {
        Store store = new Store();
        store.setAddress("12 rue des Archives, 75004 Paris");

        assertThat(DefaultStoreInformation.isUntouched(store)).isFalse();
    }

    @Test
    void shouldConsiderAnUnconfiguredContactBlockUntouched() {
        Store store = new Store();
        store.setDescription("Une description donnee a l'inscription.");

        // La description vient du formulaire d'inscription : elle ne dit rien
        // du bloc de contact, qui reste a configurer.
        assertThat(DefaultStoreInformation.isUntouched(store)).isTrue();
    }
}
