import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { SettingsPage } from './settings-page';

describe('SettingsPage', () => {
  let fixture: ComponentFixture<SettingsPage>;
  let component: SettingsPage;
  let service: { getMyStore: ReturnType<typeof vi.fn>; updateDeliverySettings: ReturnType<typeof vi.fn> };

  const el = (testId: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;

  const type = (testId: string, value: string) => {
    const input = el(testId);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const submit = async () => {
    el('delivery-form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    registerLocaleData(localeFr);
    service = {
      getMyStore: vi.fn().mockResolvedValue({ deliveryFee: 6.9, freeDeliveryFrom: 100 }),
      updateDeliverySettings: vi
        .fn()
        .mockImplementation(async (input) => ({ ...input })),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsPage],
      providers: [{ provide: StoreAdminService, useValue: service }],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('pré-remplit les frais de la boutique, virgule comprise', () => {
    expect(el('delivery-fee').value).toBe('6,9');
    expect(el('free-delivery-enabled').checked).toBe(true);
    expect(el('free-delivery-from').value).toBe('100');
    expect(el('delivery-summary').textContent).toContain('offerte dès');
  });

  it('enregistre un montant saisi avec une virgule', async () => {
    type('delivery-fee', '7,5');
    type('free-delivery-from', '150');
    await submit();

    expect(service.updateDeliverySettings).toHaveBeenCalledWith({ deliveryFee: 7.5, freeDeliveryFrom: 150 });
    expect(fixture.nativeElement.textContent).toContain('Enregistré.');
  });

  it('envoie un seuil nul quand la livraison ne doit jamais être offerte', async () => {
    const box = el('free-delivery-enabled');
    box.checked = false;
    box.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(el('free-delivery-from')).toBeNull();
    expect(el('delivery-summary').textContent).toContain('quel que soit le montant');

    await submit();
    expect(service.updateDeliverySettings).toHaveBeenCalledWith({ deliveryFee: 6.9, freeDeliveryFrom: null });
  });

  it('annonce une livraison toujours offerte à zéro', () => {
    type('delivery-fee', '0');
    expect(el('delivery-summary').textContent).toContain('toujours offerte');
  });

  it("refuse un montant vide ou négatif, sans appeler le serveur", async () => {
    type('delivery-fee', '');
    await submit();

    expect(service.updateDeliverySettings).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Indiquez un montant');

    type('delivery-fee', '-2');
    await submit();
    expect(service.updateDeliverySettings).not.toHaveBeenCalled();
  });

  it('refuse plus de trois décimales', async () => {
    type('delivery-fee', '6,9001');
    await submit();

    expect(service.updateDeliverySettings).not.toHaveBeenCalled();
    expect(component.deliveryErrors().fee).toContain('Trois décimales');
  });
});
