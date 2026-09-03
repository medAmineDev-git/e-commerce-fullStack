import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { StoreContextService } from '../../../core/services/store-context.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { Location } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
import { CartStore } from '../../../core/stores/cart.store';
import { OrderService } from '../../../core/services/order';

@Component({
  selector: 'app-checkout-page',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.scss',
})
export class CheckoutPage {
  readonly storeContext = inject(StoreContextService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly orderService = inject(OrderService);

  readonly cartStore = inject(CartStore);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showCartDetails = signal(false);
  readonly attemptedSubmit = signal(false);

  readonly form = this.fb.group({
    customerName: ['', [Validators.required, Validators.minLength(2)]],
    phone: ['', [Validators.required, Validators.minLength(8)]],
    city: ['', [Validators.required]],
    address: ['', [Validators.required, Validators.minLength(6)]],
    note: [''],
  });

  readonly formStatus = toSignal(this.form.statusChanges.pipe(startWith(this.form.status)), {
    initialValue: this.form.status,
  });

  readonly canSubmit = computed(
    () => !this.submitting() && !this.cartStore.isEmpty() && this.formStatus() === 'VALID',
  );

  readonly toggleDetailsLabel = computed(() =>
    this.showCartDetails() ? 'Masquer detail du panier' : 'Afficher detail du panier',
  );

  goBack(): void {
    this.location.back();
  }

  toggleCartDetails(): void {
    this.showCartDetails.update((value) => !value);
  }

  shouldShowError(controlName: 'customerName' | 'phone' | 'city' | 'address'): boolean {
    const control = this.form.get(controlName);
    if (!control) {
      return false;
    }
    return control.invalid && (control.touched || control.dirty || this.attemptedSubmit());
  }

  async submit(): Promise<void> {
    this.attemptedSubmit.set(true);
    if (!this.canSubmit()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    try {
      const confirmation = await this.orderService.placeOrder(
        {
          customerName: this.form.value.customerName ?? '',
          phone: this.form.value.phone ?? '',
          city: this.form.value.city ?? '',
          address: this.form.value.address ?? '',
          note: this.form.value.note ?? '',
          paymentMethod: 'cash_on_delivery',
        },
        this.cartStore.items(),
        this.cartStore.total(),
      );

      this.cartStore.clearCart();
      await this.router.navigate(this.storeContext.link('order-success'), {
        queryParams: {
          orderId: confirmation.orderId,
          eta: confirmation.estimatedDelivery,
          total: confirmation.total,
        },
      });
    } catch (error) {
      this.errorMessage.set(error instanceof Error ? error.message : 'Erreur lors de la commande');
    } finally {
      this.submitting.set(false);
    }
  }
}
