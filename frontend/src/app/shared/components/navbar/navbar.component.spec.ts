import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../shared/models/user.model';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let authSpy: jasmine.SpyObj<AuthService>;

  const mockUser: User = {
    id: 'u1', firstName: 'Jean', lastName: 'Dupont',
    email: 'jean@test.com', plan: 'FREE'
  } as User;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['logout', 'getPlan', 'getCurrentUser', 'isLoggedIn', 'getTrialStatus'],
      { currentUser$: of(mockUser) }
    );
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.getTrialStatus.and.returnValue(of({
      isInTrial: false, trialEndsAt: null, daysRemaining: 0, hoursRemaining: 0, trialUsed: false, paymentsActive: true
    } as any));

    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideAnimations(),
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  it('getInitials retourne les initiales correctes', () => {
    expect(component.getInitials(mockUser)).toBe('JD');
  });

  it('getPlanClass retourne la bonne classe CSS', () => {
    expect(component.getPlanClass('FREE')).toBe('plan-free');
    expect(component.getPlanClass('PREMIUM')).toBe('plan-premium');
    expect(component.getPlanClass('PREMIUM_PLUS')).toBe('plan-premium-plus');
  });

  it('toggleDropdown bascule le dropdown', () => {
    expect(component.dropdownOpen).toBe(false);
    component.toggleDropdown();
    expect(component.dropdownOpen).toBe(true);
    component.toggleDropdown();
    expect(component.dropdownOpen).toBe(false);
  });

  it('logout appelle authService.logout', () => {
    component.logout();
    expect(authSpy.logout).toHaveBeenCalled();
  });
});
