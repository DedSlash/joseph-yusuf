import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AlertService } from './alert.service';
import { environment } from '../../../environments/environment';

describe('AlertService', () => {
  let service: AlertService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/alerts`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AlertService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('se crée correctement', () => {
    expect(service).toBeTruthy();
  });

  it('deleteAll() appelle DELETE /api/alerts et remet le compteur à 0', () => {
    let unread = -1;
    service.unreadCount$.subscribe(c => unread = c);

    service.deleteAll().subscribe();

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(unread).toBe(0);
  });

  it('list(true) ajoute le param unread=true', () => {
    service.list(true).subscribe();
    const req = httpMock.expectOne(`${apiUrl}?unread=true`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
