import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { BehaviorSubject, Subject } from 'rxjs';
import { By } from '@angular/platform-browser';
import { MatSidenav } from '@angular/material/sidenav';

import { AdminLayout } from './admin-layout';

describe('AdminLayout', () => {
  let component: AdminLayout;
  let fixture: ComponentFixture<AdminLayout>;
  let breakpoint: BehaviorSubject<BreakpointState>;
  let routerEvents: Subject<unknown>;

  /** `handset` decide de la largeur simulee au moment de la creation. */
  async function createLayout(handset: boolean): Promise<void> {
    TestBed.resetTestingModule();

    breakpoint = new BehaviorSubject<BreakpointState>({
      matches: handset,
      breakpoints: {},
    });
    routerEvents = new Subject<unknown>();

    await TestBed.configureTestingModule({
      imports: [AdminLayout],
      providers: [
        provideRouter([]),
        { provide: BreakpointObserver, useValue: { observe: () => breakpoint } },
      ],
    }).compileComponents();

    // Le composant s'abonne aux evenements du routeur des sa construction.
    Object.defineProperty(TestBed.inject(Router), 'events', { value: routerEvents });

    fixture = TestBed.createComponent(AdminLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  function sidenav(): MatSidenav {
    fixture.detectChanges();
    return fixture.debugElement.query(By.directive(MatSidenav)).componentInstance;
  }

  it('should create', async () => {
    await createLayout(false);

    expect(component).toBeTruthy();
  });

  /**
   * Le cas signale : sur telephone le menu recouvrait la zone de travail et
   * empechait d'utiliser l'ecran affiche.
   */
  it('should keep the menu closed on a handset', async () => {
    await createLayout(true);

    expect(component.isHandset()).toBe(true);
    // L'etat rendu, pas seulement le signal : c'est le panneau qui recouvrait
    // la page, et c'est la liaison du gabarit qui devait changer.
    expect(sidenav().opened).toBe(false);
    expect(sidenav().mode).toBe('over');
  });

  it('should open and close the menu on demand', async () => {
    await createLayout(true);

    component.toggleMenu();
    expect(component.menuOpen()).toBe(true);
    expect(sidenav().opened).toBe(true);

    component.toggleMenu();
    expect(component.menuOpen()).toBe(false);
    expect(sidenav().opened).toBe(false);
  });

  /** Rester ouvert apres un clic masquerait l'ecran qu'on vient de demander. */
  it('should close the menu once a page is reached', async () => {
    await createLayout(true);
    component.toggleMenu();

    routerEvents.next(new NavigationEnd(1, '/admin/products', '/admin/products'));

    expect(component.menuOpen()).toBe(false);
  });

  /** Sur grand ecran le menu reste ancre : la place ne manque pas. */
  it('should keep the menu anchored on a wide screen', async () => {
    await createLayout(false);

    expect(component.isHandset()).toBe(false);
    expect(sidenav().opened).toBe(true);
    expect(sidenav().mode).toBe('side');
  });
});
