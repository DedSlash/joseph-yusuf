import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let onlineSpy: jasmine.Spy;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    onlineSpy = spyOnProperty(navigator, 'onLine', 'get').and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function triggerError(status: number): void {
    http.get('/api/test').subscribe({
      next: () => fail('expected error'),
      error: () => undefined
    });
    httpMock.expectOne('/api/test').flush('err', { status, statusText: 'X' });
  }

  it('erreur 0 (réseau) → navigate vers /maintenance', () => {
    triggerError(0);
    expect(router.navigate).toHaveBeenCalledWith(['/maintenance']);
  });

  it('erreur 500 → navigate vers /error/500', () => {
    triggerError(500);
    expect(router.navigate).toHaveBeenCalledWith(['/error/500']);
  });

  it('erreur 503 → navigate vers /maintenance', () => {
    triggerError(503);
    expect(router.navigate).toHaveBeenCalledWith(['/maintenance']);
  });

  it('erreur 401 → ne redirige PAS', () => {
    triggerError(401);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('erreur 403 → ne redirige PAS', () => {
    triggerError(403);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('erreur 400 → ne redirige PAS (validation)', () => {
    triggerError(400);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('hors-ligne → navigate vers /offline (prioritaire)', () => {
    onlineSpy.and.returnValue(false);
    triggerError(500);
    expect(router.navigate).toHaveBeenCalledWith(['/offline']);
  });

  it('propage l\'erreur à l\'appelant', () => {
    let caught: HttpErrorResponse | null = null;
    http.get('/api/test').subscribe({
      next: () => fail('expected error'),
      error: (err: HttpErrorResponse) => { caught = err; }
    });
    httpMock.expectOne('/api/test').flush('err', { status: 500, statusText: 'X' });
    expect(caught).not.toBeNull();
    expect(caught!.status).toBe(500);
  });
});
