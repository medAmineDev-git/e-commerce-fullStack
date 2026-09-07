import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SeoService } from '../../../core/seo/seo.service';

type Step = { number: string; title: string; text: string };
type Feature = { title: string; text: string };
type Faq = { question: string; answer: string };

/**
 * Page de présentation du service.
 *
 * C'est la seule page prérendue : son contenu est figé dans le HTML livré, donc
 * lisible par les moteurs et par les aperçus de lien sans exécuter de JavaScript.
 * Les vitrines, elles, restent en rendu client — leur contenu est propre à
 * chaque boutique et change en permanence.
 */
@Component({
  selector: 'app-landing-page',
  imports: [RouterLink],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss',
})
export class LandingPage {
  private readonly seo = inject(SeoService);

  readonly steps: Step[] = [
    {
      number: '1',
      title: 'Créez votre compte',
      text: 'Un nom de boutique, une adresse e-mail, un mot de passe. Aucune carte bancaire demandée.',
    },
    {
      number: '2',
      title: 'Ajoutez vos articles',
      text: 'Photos, tailles, couleurs, stock. Vos catégories sont les vôtres, pas une liste imposée.',
    },
    {
      number: '3',
      title: 'Partagez votre lien',
      text: 'Votre vitrine est en ligne immédiatement, à votre adresse, prête à recevoir des commandes.',
    },
  ];

  readonly features: Feature[] = [
    {
      title: 'Paiement à la livraison',
      text: 'Vos clients commandent sans carte bancaire. Le virement manuel est également accepté, avec validation par vos soins.',
    },
    {
      title: 'Stock tenu à jour',
      text: 'Chaque commande décrémente le stock dans la même transaction. Vous ne vendez jamais un article que vous n avez plus.',
    },
    {
      title: 'Vos données restent les vôtres',
      text: 'Chaque boutique est cloisonnée. Aucune autre boutique ne voit vos produits, vos commandes ni vos clients.',
    },
    {
      title: 'Suivi des commandes',
      text: 'De la validation à la livraison, chaque commande a un statut et un historique consultables depuis votre back-office.',
    },
  ];

  readonly faqs: Faq[] = [
    {
      question: 'Combien de temps faut-il pour ouvrir ma boutique ?',
      answer:
        'Quelques minutes. La création du compte et de la boutique se fait en une étape, et votre vitrine est accessible dès le premier article publié.',
    },
    {
      question: 'Dois-je payer pour commencer ?',
      answer:
        'Non. La création de la boutique est gratuite et ne demande aucune carte bancaire.',
    },
    {
      question: 'Mes clients doivent-ils payer en ligne ?',
      answer:
        'Non. Le paiement à la livraison et le virement bancaire manuel sont pris en charge dès le départ, ce qui évite à vos clients toute saisie bancaire.',
    },
    {
      question: 'Puis-je utiliser mon propre nom de domaine ?',
      answer:
        'Oui. Votre boutique démarre sur une adresse de la plateforme, et un domaine personnalisé peut lui être rattaché ensuite sans rien reconstruire.',
    },
  ];

  constructor() {
    this.seo.apply({
      title: 'Créez votre boutique en ligne en quelques minutes',
      description:
        'Ouvrez votre boutique de vêtements en ligne en quelques minutes : catalogue, panier, commandes et paiement à la livraison. Sans carte bancaire, sans installation.',
      path: '/',
      type: 'website',
    });

    this.seo.setStructuredData([
      {
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: 'Boutique en quelques minutes',
        applicationCategory: 'BusinessApplication',
        operatingSystem: 'Web',
        description:
          'Plateforme de création de boutiques de vêtements en ligne, avec catalogue, panier, commandes et paiement à la livraison.',
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'EUR',
          description: 'Création de boutique gratuite, sans carte bancaire.',
        },
      },
      {
        '@context': 'https://schema.org',
        '@type': 'FAQPage',
        mainEntity: this.faqs.map((faq) => ({
          '@type': 'Question',
          name: faq.question,
          acceptedAnswer: { '@type': 'Answer', text: faq.answer },
        })),
      },
    ]);
  }
}
