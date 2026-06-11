import { TestBed } from '@angular/core/testing';
import { PaddleService } from './paddle.service';
import { environment } from '../../../environments/environment';

describe('PaddleService', () => {
  let service: PaddleService;
  let paddleMock: {
    Environment: { set: jasmine.Spy };
    Initialize: jasmine.Spy;
    Checkout: { open: jasmine.Spy };
  };

  beforeEach(() => {
    paddleMock = {
      Environment: { set: jasmine.createSpy('Environment.set') },
      Initialize: jasmine.createSpy('Initialize'),
      Checkout: { open: jasmine.createSpy('Checkout.open') }
    };
    (window as any).Paddle = paddleMock;

    TestBed.configureTestingModule({ providers: [PaddleService] });
    service = TestBed.inject(PaddleService);
  });

  afterEach(() => {
    delete (window as any).Paddle;
  });

  it('se crée correctement', () => {
    expect(service).toBeTruthy();
  });

  it('init() sandbox → Environment.set("sandbox") + Initialize(clientToken)', () => {
    environment.paddleEnvironment = 'sandbox';
    service.init();
    expect(paddleMock.Environment.set).toHaveBeenCalledWith('sandbox');
    expect(paddleMock.Initialize).toHaveBeenCalledWith({ token: environment.paddleClientToken });
  });

  it('init() production → pas d\'appel Environment.set, Initialize seul', () => {
    environment.paddleEnvironment = 'production';
    service.init();
    expect(paddleMock.Environment.set).not.toHaveBeenCalled();
    expect(paddleMock.Initialize).toHaveBeenCalledTimes(1);
  });

  it('init() est idempotent — Initialize appelé une seule fois si réinvoqué', () => {
    service.init();
    service.init();
    service.init();
    expect(paddleMock.Initialize).toHaveBeenCalledTimes(1);
  });

  it('init() lève si Paddle.js absent', () => {
    delete (window as any).Paddle;
    expect(() => service.init()).toThrowError(/Paddle.js indisponible/);
  });

  it('openCheckout(txId, email) initialise puis appelle Paddle.Checkout.open avec les bons args', () => {
    service.openCheckout('txn_abc', 'jean@example.com');

    expect(paddleMock.Initialize).toHaveBeenCalled();
    expect(paddleMock.Checkout.open).toHaveBeenCalledTimes(1);

    const arg = paddleMock.Checkout.open.calls.mostRecent().args[0];
    expect(arg.transactionId).toBe('txn_abc');
    expect(arg.customer).toEqual({ email: 'jean@example.com' });
    expect(arg.settings.displayMode).toBe('overlay');
    expect(arg.settings.theme).toBe('dark');
    expect(arg.settings.locale).toBe('fr');
    expect(arg.settings.successUrl).toContain('/subscription/success');
  });

  it('openCheckout() init() une fois même si plusieurs appels', () => {
    service.openCheckout('txn_1', 'a@x.com');
    service.openCheckout('txn_2', 'b@x.com');
    expect(paddleMock.Initialize).toHaveBeenCalledTimes(1);
    expect(paddleMock.Checkout.open).toHaveBeenCalledTimes(2);
  });
});
