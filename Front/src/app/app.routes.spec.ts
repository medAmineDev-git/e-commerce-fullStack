import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { provideLocationMocks } from '@angular/common/testing';
import { routes } from './app.routes';
import { StoreContextService } from './core/services/store-context.service';
import { CartStore } from './core/stores/cart.store';
import { AuthService } from './core/services/auth';
import { PublicStore } from './core/models/store.model';

const NOVA: PublicStore = {
  id: 1,
  name: 'NOVA',
  slug: 'nova',
  description: null,
  logoUrl: null,
  bannerUrl: null,
  bannerMobileUrl: null,
  phone: null,
  email: null,
  address: null,
  domain: null,
};

/**
 * Le routage lui-meme, avec le vrai tableau de routes.
 *
 * Les composants sont charges paresseusement et les gardes s'executent : c'est
 * le seul niveau ou l'ordre de declaration et la resolution de la boutique se
 * verifient ensemble. Une erreur ici renvoie sur la page d'accueil du site
 * principal, par la route generique finale — sans la moindre erreur visible.
 */
describe('Routage de la vitrine', () => {
  let router: Router;
  let resolveByDomain: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    resolveByDomain = vi.fn().mockResolvedValue(null);

    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideLocationMocks(),
        {
          provide: StoreContextService,
          useValue: {
            store: signal(NOVA),
            resolveBySlug: vi.fn().mockResolvedValue(NOVA),
            // Aucune boutique derriere ce nom d'hote : on est sur la plateforme.
            resolveByDomain: resolveByDomain,
            slug: signal('nova'),
            link: (...segments: string[]) => ['/boutique', 'nova', ...segments],
          },
        },
        { provide: CartStore, useValue: { hydrate: vi.fn(), items: signal([]) } },
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => false,
            isPlatformOperator: () => false,
            login: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  /**
   * Les deux routes racines repondent a la meme adresse : seule leur forme les
   * distingue. Celle de la vitrine porte des enfants, celle de la plateforme
   * non.
   */
  function matchedRootHasChildren(): boolean {
    const config = router.routerState.snapshot.root.firstChild?.routeConfig;
    return (config?.children?.length ?? 0) > 0;
  }

  it('should open the login page from the store address', async () => {
    await router.navigateByUrl('/boutique/nova/connexion');

    expect(router.url).toBe('/boutique/nova/connexion');
  });

  /** L'adresse en anglais mene a la meme page, parametre conserve. */
  it('should redirect the english address to the french one', async () => {
    await router.navigateByUrl('/boutique/nova/login');

    expect(router.url).toBe('/boutique/nova/connexion');
  });

  /**
   * La route de connexion est declaree avant celle de la vitrine : elle ne doit
   * pas lui voler l'accueil de la boutique.
   */
  it('should still open the storefront home', async () => {
    await router.navigateByUrl('/boutique/nova');

    expect(router.url).toBe('/boutique/nova');
  });

  it('should keep the main login page', async () => {
    await router.navigateByUrl('/connexion');

    expect(router.url).toBe('/connexion');
  });

  /*
   * Domaine propre a une boutique.
   *
   * Le DNS amene le visiteur a la racine, sans slug : c'est le nom d'hote qui
   * designe la boutique. Le meme chemin doit donc servir deux pages selon le
   * domaine par lequel on arrive.
   */
  it('should open the storefront at the root of a store domain', async () => {
    resolveByDomain.mockResolvedValue(NOVA);

    await router.navigateByUrl('/');

    expect(resolveByDomain).toHaveBeenCalled();
    expect(router.url).toBe('/');
    // L'adresse seule ne distingue rien : c'est la branche empruntee qui compte.
    expect(matchedRootHasChildren()).toBe(true);
  });

  it('should reach an inner page of the storefront on a store domain', async () => {
    resolveByDomain.mockResolvedValue(NOVA);

    await router.navigateByUrl('/cart');

    expect(router.url).toBe('/cart');
  });

  /** Aucune boutique derriere ce nom d'hote : la plateforme garde sa racine. */
  it('should keep the landing page when the host matches no store', async () => {
    resolveByDomain.mockResolvedValue(null);

    await router.navigateByUrl('/');

    expect(resolveByDomain).toHaveBeenCalled();
    expect(router.url).toBe('/');
    expect(matchedRootHasChildren()).toBe(false);
  });
});
