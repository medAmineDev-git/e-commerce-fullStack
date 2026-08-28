import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-order-success-page',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './order-success-page.html',
  styleUrl: './order-success-page.scss',
})
export class OrderSuccessPage {
  private readonly route = inject(ActivatedRoute);

  readonly orderId = signal('');
  readonly eta = signal('');
  readonly total = signal(0);

  constructor() {
    this.orderId.set(this.route.snapshot.queryParamMap.get('orderId') ?? 'N/A');
    this.eta.set(this.route.snapshot.queryParamMap.get('eta') ?? 'A definir');
    this.total.set(Number(this.route.snapshot.queryParamMap.get('total') ?? '0'));
  }
}
