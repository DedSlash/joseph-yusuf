import { Pipe, PipeTransform } from '@angular/core';
import { CurrencyDisplayService } from '../../core/services/currency-display.service';

@Pipe({
  name: 'displayAmount',
  standalone: true,
  pure: false
})
export class DisplayAmountPipe implements PipeTransform {
  constructor(private currency: CurrencyDisplayService) {}

  transform(amountXof: number | null | undefined, currency?: string): string {
    return this.currency.formatAmount(amountXof, currency);
  }
}
