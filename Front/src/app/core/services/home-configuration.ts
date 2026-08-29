import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HomeConfiguration, HomeConfigurationInput } from '../models/home-configuration.model';

@Service()
export class HomeConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/home/configuration`;

  get(): Promise<HomeConfiguration> {
    return firstValueFrom(this.http.get<HomeConfiguration>(this.url));
  }

  save(input: HomeConfigurationInput): Promise<HomeConfiguration> {
    return firstValueFrom(this.http.put<HomeConfiguration>(this.url, input));
  }
}
