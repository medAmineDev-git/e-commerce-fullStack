import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { StoreContextService } from '../../../core/services/store-context.service';
import { SeoService } from '../../../core/seo/seo.service';
import { PublicStore } from '../../../core/models/store.model';

import { LoginPage } from './login-page';

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

describe('LoginPage', () => {
  const mockLogin = vi.fn().mockResolvedValue(undefined);
  const mockNavigateByUrl = vi.fn().mockResolvedValue(true);
  const mockApply = vi.fn();

  /**
   * `slug` present : la page vient de l'adresse d'une boutique.
   * `null` : elle vient du site principal.
   */
  async function createPage(slug: string | null): Promise<ComponentFixture<LoginPage>> {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            login: mockLogin,
            isPlatformOperator: () => false,
          },
        },
        {
          // Le contexte garde toujours la derniere vitrine visitee.
          provide: StoreContextService,
          useValue: { store: signal(NOVA) },
        },
        {
          provide: SeoService,
          useValue: { apply: mockApply, removeStructuredData: vi.fn() },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => (key === 'slug' ? slug : null) },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigateByUrl: mockNavigateByUrl } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    mockLogin.mockClear();
    mockNavigateByUrl.mockClear();
    mockApply.mockClear();
  });

  it('should show the store when reached from its address', async () => {
    const fixture = await createPage('nova');

    expect(fixture.componentInstance.store()).toEqual(NOVA);
    expect(fixture.componentInstance.storefrontLink()).toEqual(['/boutique', 'nova']);
    expect(mockApply).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/boutique/nova/connexion', noIndex: true }),
    );
  });

  /**
   * Le cas qui compte : le contexte garde la derniere vitrine visitee. Lire la
   * boutique depuis lui ferait apparaitre une enseigne sur le site principal.
   */
  it('should show no store on the main site even after visiting one', async () => {
    const fixture = await createPage(null);

    expect(fixture.componentInstance.store()).toBeNull();
    expect(fixture.componentInstance.storefrontLink()).toBeNull();
    expect(mockApply).toHaveBeenCalledWith(expect.objectContaining({ path: '/connexion' }));
  });

  /** L'adresse de la boutique ne change rien a l'authentification. */
  it('should send the credentials to the admin area from a store address', async () => {
    const fixture = await createPage('nova');
    const component = fixture.componentInstance;

    component.identifier.set('alice');
    component.password.set('secret');
    await component.login();

    expect(mockLogin).toHaveBeenCalledWith('alice', 'secret');
    expect(mockNavigateByUrl).toHaveBeenCalledWith('/admin');
  });

  it('should refuse an empty form without calling the server', async () => {
    const fixture = await createPage('nova');

    await fixture.componentInstance.login();

    expect(mockLogin).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBeTruthy();
  });
});
